 package dannypx.foe.handler.logic;

import dannypx.foe.handler.Handler;
import dannypx.foe.handler.fetch.BossEventHandler;
import dannypx.foe.type.tuple.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocationXpHandler extends Handler {

    private static final LocationXpHandler INSTANCE =
            new LocationXpHandler();

    public static LocationXpHandler instance() {
        return INSTANCE;
    }

    private enum Phase {
        IDLE,
        WAIT_INDEX,
        WAIT_DETAIL
    }

    private static final Pattern LEVEL_PATTERN = Pattern.compile(
            "Current Level:\\s*\\[(\\d+)\\]",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XP_PATTERN = Pattern.compile(
            "\\((\\d+(?:\\.\\d+)?[KMB]?)/(\\d+(?:\\.\\d+)?[KMB]?)\\)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "([\\d,]+(?:\\.\\d+)?)([KMB]?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final long REFRESH_INTERVAL =
            5L * 60L * 1_000_000_000L;

    private static final long REQUEST_TIMEOUT =
            5L * 1_000_000_000L;

    private static final int LOCATION_STABLE_TICKS = 30;

    private final Map<String, Progress> progressByLocation =
            new HashMap<>();

    private Phase phase = Phase.IDLE;

    private String location = "";
    private String requestedLocation = "";

    private int stableTicks;
    private int expectedContainer = -1;

    private long tickCount;
    private long requestStartedAt;
    private long nextAttemptAt;

    private boolean ready;

    private LocationXpHandler() {
    }

    /**
     * A fresh server connection must not display XP retained
     * from the previous connection.
     */
    @Override
    public void init() {
        phase = Phase.IDLE;

        location = "";
        requestedLocation = "";

        stableTicks = 0;
        expectedContainer = -1;

        tickCount = 0;
        requestStartedAt = 0;
        nextAttemptAt = 0;

        ready = false;
        progressByLocation.clear();
    }

    /**
     * Runs after FOER's existing BossEventHandler.tick().
     */
    @Override
    public void tick() {
        Minecraft mc = minecraft;

        if (mc.player == null
                || mc.getConnection() == null
                || !ConnectionHandler.instance().isOnServer()) {
            return;
        }

        tickCount++;

        long now = System.nanoTime();

        /*
         * While a request is active, wait for the expected
         * server packets. Do not initiate another request.
         */
        if (phase != Phase.IDLE) {
            if (mc.screen != null
                    || now - requestStartedAt > REQUEST_TIMEOUT) {
                abort(mc);
            }

            return;
        }

        /*
         * FOER already extracts the current location from
         * FishOnMC's boss bar. Reuse that value.
         *
         * Check every 10 ticks rather than on every frame.
         */
        if (tickCount % 10 != 0) {
            return;
        }

        String detectedLocation =
                BossEventHandler.instance()
                        .getLocation()
                        .getString()
                        .trim();

        if (detectedLocation.isEmpty()) {
            return;
        }

        /*
         * Changing location invalidates the current display.
         * The new location gets its own fresh server lookup.
         */
        if (!detectedLocation.equals(location)) {
            String previousLocation = location;
            location = detectedLocation;

            stableTicks = 0;
            nextAttemptAt = 0;
            ready = false;

            if (!previousLocation.isEmpty()) {
                EventHandler.instance().onLocationChange();
            }
        } else {
            stableTicks += 10;
        }

        /*
         * Wait until the location has been stable for
         * approximately 1.5 seconds.
         *
         * Never start while another screen or server-side
         * inventory menu is already open.
         */
        if (stableTicks < LOCATION_STABLE_TICKS
                || now < nextAttemptAt
                || mc.screen != null
                || mc.gameMode == null
                || mc.player.containerMenu != mc.player.inventoryMenu) {
            return;
        }

        Progress known = progressByLocation.get(location);

        /*
         * A valid result is reused until its refresh interval
         * expires.
         */
        if (ready
                && known != null
                && now < known.refreshAfter) {
            return;
        }

        startRequest(mc, now);
    }

    /**
     * Begin the invisible request.
     *
     * /tt opens the main Compendium menu. The packet hooks
     * below handle the rest of the sequence.
     */
    private void startRequest(Minecraft mc, long now) {
        requestedLocation = location;

        phase = Phase.WAIT_INDEX;
        expectedContainer = -1;

        requestStartedAt = now;

        /*
         * Also serves as a retry cooldown if the request fails.
         * Do not repeatedly send /tt on every tick.
         */
        nextAttemptAt = now + REFRESH_INTERVAL;

        mc.getConnection().sendCommand("tt");
    }

    /**
     * Called by our Minecraft mixin whenever Minecraft tries
     * to display a screen.
     *
     * Only suppress a screen while our request is active,
     * and only if it looks like the expected Compendium UI.
     */
    public boolean shouldHideScreen(Screen nextScreen) {
        if (phase == Phase.IDLE
                || !(nextScreen instanceof AbstractContainerScreen<?> menu)) {
            return false;
        }

        Minecraft mc = minecraft;

        if (mc.screen != null || mc.player == null) {
            return false;
        }

        /*
         * Both observed FishOnMC Compendium screens have
         * 90 total slots and a title starting with this glyph.
         */
        boolean expected =
                menu.getMenu().slots.size() == 90
                        && nextScreen.getTitle()
                        .getString()
                        .startsWith("\uEEE4");

        if (!expected) {
            abort(mc);
            return false;
        }

        return true;
    }

    /**
     * Called after the server opens an inventory menu.
     *
     * Container IDs are allocated dynamically, so never
     * hardcode the IDs observed in the diagnostic logs.
     */
    public void onOpenScreen(ClientboundOpenScreenPacket packet) {
        if (phase == Phase.IDLE) {
            return;
        }

        Minecraft mc = minecraft;

        if (mc.player == null || mc.screen != null) {
            abort(mc);
            return;
        }

        if (!packet.getTitle()
                .getString()
                .startsWith("\uEEE4")) {
            abort(mc);
            return;
        }

        expectedContainer = packet.getContainerId();
    }

    /**
     * Called after the client receives a menu's item contents.
     */
    public void onContainerContent(
            ClientboundContainerSetContentPacket packet
    ) {
        if (phase == Phase.IDLE) {
            return;
        }

        Minecraft mc = minecraft;

        if (mc.player == null
                || packet.containerId() != expectedContainer
                || mc.player.containerMenu.containerId
                != expectedContainer) {
            return;
        }

        if (phase == Phase.WAIT_INDEX) {
            selectLocation(mc, packet);
            return;
        }

        if (phase == Phase.WAIT_DETAIL) {
            readDetail(mc, packet.items());
        }
    }

    /**
     * Main Compendium:
     *
     * Find the item named after the current location and
     * right-click it to request the detailed location menu.
     */
    private void selectLocation(
            Minecraft mc,
            ClientboundContainerSetContentPacket packet
    ) {
        if (mc.gameMode == null || mc.screen != null) {
            abort(mc);
            return;
        }

        int targetSlot = -1;

        List<ItemStack> items = packet.items();

        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack item = items.get(slot);

            if (item.isEmpty()
                    || !item.getHoverName()
                    .getString()
                    .equalsIgnoreCase(requestedLocation)) {
                continue;
            }

            /*
             * This distinguishes the location-selection icon
             * from unrelated items with a matching name.
             */
            if (!loreText(item).contains("View Species")) {
                continue;
            }

            targetSlot = slot;
            break;
        }

        if (targetSlot < 0) {
            abort(mc);
            return;
        }

        /*
         * These values came from your successful packet probe:
         *
         * button 1 = right-click
         * PICKUP   = normal inventory click
         *
         * The slot and container ID are discovered at runtime.
         */
        phase = Phase.WAIT_DETAIL;

        mc.gameMode.handleInventoryMouseClick(
                packet.containerId(),
                targetSlot,
                1,
                ClickType.PICKUP,
                mc.player
        );
    }

    /**
     * Some menus populate their items with individual slot
     * updates after sending the initial contents packet.
     */
    public void onContainerSlot(
            ClientboundContainerSetSlotPacket packet
    ) {
        if (phase != Phase.WAIT_DETAIL
                || packet.getContainerId() != expectedContainer) {
            return;
        }

        Minecraft mc = minecraft;

        if (mc.player == null
                || mc.player.containerMenu.containerId
                != expectedContainer) {
            return;
        }

        ItemStack item = packet.getItem();

        if (!item.isEmpty()
                && item.getHoverName()
                .getString()
                .equalsIgnoreCase(requestedLocation)) {
            readItem(mc, item);
        }
    }

    /**
     * Detailed Compendium:
     *
     * Search the received item data for the current location's
     * level/XP item. There is no need to hover over it.
     */
    private void readDetail(
            Minecraft mc,
            List<ItemStack> items
    ) {
        for (ItemStack item : items) {
            if (item.isEmpty()
                    || !item.getHoverName()
                    .getString()
                    .equalsIgnoreCase(requestedLocation)) {
                continue;
            }

            if (readItem(mc, item)) {
                return;
            }
        }
    }

    /**
     * Parse the server-provided lore:
     *
     * Current Level: [6]
     * (36.41K/85.75K)
     * Progress to Level 7
     */
    private boolean readItem(
            Minecraft mc,
            ItemStack item
    ) {
        String lore = loreText(item);

        Matcher levelMatch =
                LEVEL_PATTERN.matcher(lore);

        Matcher xpMatch =
                XP_PATTERN.matcher(lore);

        if (!levelMatch.find() || !xpMatch.find()) {
            return false;
        }

        int level = Integer.parseInt(levelMatch.group(1));

        double currentXp = expandNumber(xpMatch.group(1));
        double requiredXp = expandNumber(xpMatch.group(2));

        if (!Double.isFinite(currentXp)
                || !Double.isFinite(requiredXp)
                || requiredXp <= 0) {
            return false;
        }

        Progress progress = new Progress();

        progress.level = level;
        progress.currentXp = currentXp;
        progress.requiredXp = requiredXp;

        progress.refreshAfter =
                System.nanoTime() + REFRESH_INTERVAL;

        progressByLocation.put(
                requestedLocation,
                progress
        );

        ready = true;

        /*
         * The server has supplied the data. Close only the
         * inventory menu opened by this request.
         */
        finish(mc);

        return true;
    }

    /**
     * Read an item's server-provided lore component.
     */
    private static String loreText(ItemStack item) {
        ItemLore lore = item.get(DataComponents.LORE);

        if (lore == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (Component line : lore.lines()) {
            result.append(line.getString()).append('\n');
        }

        return result.toString();
    }

    /**
     * Expand FishOnMC's rounded notation:
     *
     * 36.41K -> 36410
     * 85.75K -> 85750
     * 92     -> 92
     *
     * This does NOT recover the server's unrounded XP.
     */
    private static double expandNumber(String text) {
        Matcher match = NUMBER_PATTERN.matcher(text);

        if (!match.matches()) {
            return Double.NaN;
        }

        double value = Double.parseDouble(
                match.group(1).replace(",", "")
        );

        double multiplier = switch (
                match.group(2).toUpperCase(Locale.ROOT)
                ) {
            case "K" -> 1_000D;
            case "M" -> 1_000_000D;
            case "B" -> 1_000_000_000D;
            default -> 1D;
        };

        return value * multiplier;
    }

    /**
     * Update the estimated current balance when FOER detects a new
     * location XP award.
     *
     * Do not use this for the session XP tracker; that remains
     * the job of FOER's existing custom trackers.
     */
    public void onLocationXpAward(long amount) {
        if (amount <= 0 || !ready || location.isEmpty()) {
            return;
        }

        Progress progress = progressByLocation.get(location);

        if (progress == null) {
            return;
        }

        progress.currentXp += amount;

        /*
         * Once the estimate reaches the next-level threshold,
         * refresh the level and XP requirement immediately.
         * Keep the cached progress available while that request
         * runs so the location XP display does not disappear.
         */
        if (progress.currentXp >= progress.requiredXp) {
            progress.refreshAfter = 0;
            nextAttemptAt = 0;
        }
    }

    /**
     * Mute only the specific Compendium sound during an
     * automatically requested menu sequence.
     */
    public boolean shouldMuteSound(
            net.minecraft.client.resources.sounds.SoundInstance sound
    ) {
        return phase != Phase.IDLE
                && sound.getIdentifier()
                .toString()
                .equals("minecraft:open_compendium");
    }

    /**
     * Return control to normal gameplay.
     */
    private void finish(Minecraft mc) {
        int ownedContainer = expectedContainer;

        phase = Phase.IDLE;
        expectedContainer = -1;

        if (mc.player != null
                && ownedContainer >= 0
                && mc.player.containerMenu.containerId
                == ownedContainer
                && mc.player.containerMenu
                != mc.player.inventoryMenu) {

            mc.player.closeContainer();
        }

        requestedLocation = "";
    }

    /**
     * A failed request should never leave FOER waiting
     * indefinitely or repeatedly issuing /tt.
     */
    private void abort(Minecraft mc) {
        if (phase == Phase.IDLE) {
            return;
        }

        ready = progressByLocation.containsKey(location);
        finish(mc);
    }

    // ---------------------------------------------------------
    // Placeholder getters
    // ---------------------------------------------------------

    public String getLocation() {
        return location;
    }

    public boolean isAvailable() {
        return ready
                && progressByLocation.containsKey(location);
    }

    public int getLevel() {
        Progress progress = progressByLocation.get(location);

        return isAvailable() ? progress.level : 0;
    }

    public double getCurrentXp() {
        Progress progress = progressByLocation.get(location);

        return isAvailable() ? progress.currentXp : 0;
    }

    public double getRequiredXp() {
        Progress progress = progressByLocation.get(location);

        return isAvailable() ? progress.requiredXp : 0;
    }

    public double getProgressPercent() {
        if (!isAvailable()) {
            return 0;
        }

        double required = getRequiredXp();

        if (required <= 0) {
            return 0;
        }

        return Math.round(Math.min(100, getCurrentXp() / required * 100) * 100) / 100.0;
    }

    @Override
    protected Map<String, Pair<MutableComponent, MutableComponent>>
    _getFields() {
        return Map.of();
    }

    private static final class Progress {
        int level;

        double currentXp;
        double requiredXp;

        long refreshAfter;
    }
}