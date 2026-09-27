package dannypx.foe.handler.logic;

import dannypx.foe.handler.Handler;
import dannypx.foe.type.tuple.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Live HUD countdowns derived from FishOnMC's /events response.
 * No client wall-clock or fixed shop schedule is assumed.
 */
public final class EventTimesHandler extends Handler {
    private static final EventTimesHandler INSTANCE = new EventTimesHandler();
    private static final long SECOND = TimeUnit.SECONDS.toNanos(1);
    private static final long MINUTE = TimeUnit.MINUTES.toNanos(1);
    private static final long REGULAR_REFRESH = TimeUnit.MINUTES.toNanos(15);
    private static final long RESTOCK_DISPLAY = TimeUnit.SECONDS.toNanos(60);
    private static final long COMMAND_COOLDOWN = TimeUnit.SECONDS.toNanos(30);
    private static final long MAX_DURATION_SECONDS = TimeUnit.DAYS.toSeconds(7);

    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)\\s*([dhms])", Pattern.CASE_INSENSITIVE);
    private static final String TACKLE_PREFIX = "Tackle Shop Restock";
    private static final String COSMETIC_PREFIX = "Cosmetic Store";

    private final ShopTime tackle = new ShopTime();
    private final ShopTime cosmetic = new ShopTime();
    private long nextRefreshAt;
    private long lastCommandAt;

    private EventTimesHandler() {}

    public static EventTimesHandler instance() {
        return INSTANCE;
    }

    /** Call on every FishOnMC join, before chat events begin arriving. */
    @Override
    public void init() {
        tackle.reset();
        cosmetic.reset();
        lastCommandAt = 0L;
        nextRefreshAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    }

    /** Call from ChatHandler.onReceiveMessage, using only non-overlay chat. */
    public void onChat(Component message, boolean overlay) {
        if (overlay || message == null) return;
        String line = message.getString().strip();
        if (parse(line, TACKLE_PREFIX, tackle) || parse(line, COSMETIC_PREFIX, cosmetic)) {
            // An automatic /events response resynchronizes the running countdown.
            // A timer reaching zero can bring this refresh forward in tick().
            nextRefreshAt = System.nanoTime() + REGULAR_REFRESH;
        }
    }

    private boolean parse(String line, String prefix, ShopTime shop) {
        if (!line.regionMatches(true, 0, prefix, 0, prefix.length())) return false;

        String remainder = line.substring(prefix.length()).strip();
        // Strip the delimiter after the event title (e.g. "→" in /events).
        remainder = remainder.replaceFirst("^[\\s→➜»:+–-]+", "").strip();
        Matcher parts = DURATION_PART.matcher(remainder);
        long seconds = 0L;
        boolean found = false;

        while (parts.find()) {
            found = true;
            long number;
            try {
                number = Long.parseLong(parts.group(1));
            } catch (NumberFormatException exception) {
                return false;
            }
            long multiplier = switch (parts.group(2).toLowerCase(Locale.ROOT)) {
                case "d" -> 86400L;
                case "h" -> 3600L;
                case "m" -> 60L;
                default -> 1L;
            };
            if (number > MAX_DURATION_SECONDS / multiplier
                    || seconds > MAX_DURATION_SECONDS - number * multiplier) return false;
            seconds += number * multiplier;
        }

        if (!found) {
            // Do not guess a restock from a message format we do not understand.
            return false;
        }

        long now = System.nanoTime();
        shop.known = true;
        shop.deadlineAt = now + seconds * SECOND;
        shop.expiryHandled = false;
        if (seconds == 0L) {
            shop.restockedUntil = Math.max(shop.restockedUntil, now + RESTOCK_DISPLAY);
            nextRefreshAt = Math.min(nextRefreshAt, now + TimeUnit.SECONDS.toNanos(45));
        }
        return true;
    }

    /** Call each client tick after the mod has finished loading. */
    @Override
    public void tick() {
        if (minecraft.player == null || minecraft.getConnection() == null) return;

        long now = System.nanoTime();
        markExpiry(tackle, now);
        markExpiry(cosmetic, now);

        if (now >= nextRefreshAt && (lastCommandAt == 0L || now - lastCommandAt >= COMMAND_COOLDOWN)) {
            lastCommandAt = now;
            // Retry after one minute if the server does not answer this request.
            nextRefreshAt = now + MINUTE;
            minecraft.player.connection.sendCommand("events");
        }
    }

    private void markExpiry(ShopTime shop, long now) {
        if (shop.known && !shop.expiryHandled && now >= shop.deadlineAt) {
            shop.expiryHandled = true;
            shop.restockedUntil = Math.max(shop.restockedUntil, now + RESTOCK_DISPLAY);
            // Recheck the server soon after the predicted restock.
            nextRefreshAt = Math.min(nextRefreshAt, now + TimeUnit.SECONDS.toNanos(10));
        }
    }

    public String getTackleShop() {
        return format(tackle);
    }

    public String getCosmeticStore() {
        return format(cosmetic);
    }

    private String format(ShopTime shop) {
        if (!shop.known) return "SYNCING";
        long now = System.nanoTime();
        if (now < shop.restockedUntil || now >= shop.deadlineAt) return "RESTOCKED";
        // Ceiling division: avoid showing "0s" until the deadline is reached.
        long left = (shop.deadlineAt - now + SECOND - 1) / SECOND;
        long days = left / 86400;
        long hours = (left % 86400) / 3600;
        long minutes = (left % 3600) / 60;
        long seconds = left % 60;
        if (days > 0) return String.format(Locale.ROOT, "%dd %dh %dm %ds", days, hours, minutes, seconds);
        if (hours > 0) return String.format(Locale.ROOT, "%dh %dm %ds", hours, minutes, seconds);
        if (minutes > 0) return String.format(Locale.ROOT, "%dm %ds", minutes, seconds);
        return seconds + "s";
    }

    @Override
    protected Map<String, Pair<MutableComponent, MutableComponent>> _getFields() {
        return Map.of();
    }

    private static final class ShopTime {
        private boolean known;
        private long deadlineAt;
        private long restockedUntil;
        private boolean expiryHandled;

        private void reset() {
            known = false;
            deadlineAt = 0L;
            restockedUntil = 0L;
            expiryHandled = false;
        }
    }
}
