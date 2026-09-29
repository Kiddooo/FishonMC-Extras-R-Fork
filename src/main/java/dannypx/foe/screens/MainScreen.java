package dannypx.foe.screens;

import dannypx.foe.FishOnMCExtras;
import dannypx.foe.config.Configs;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MainScreen extends DefaultModScreen {
    //region Fields
    private static final Identifier ICON_TEXTURE = Identifier.fromNamespaceAndPath(FishOnMCExtras.MOD_ID, "elements/icon");
    private static final int iconSize = 150;
    //endregion

    //region Methods
    public MainScreen(Screen parent) {
        super(parent, Component.literal("Main Screen"));
    }

    @Override
    protected void init() {
        super.init();
        this.renderWidgets();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                ICON_TEXTURE,
                screenWidth / 2 - iconSize / 2,
                screenHeight / 2 - iconSize + (BUTTON_HEIGHT + PADDING_QUART) * -2 + PADDING,
                iconSize, iconSize
        );

        Component creatorHUDComponent = Component.literal("Creator: HUD Elements");
        guiGraphics.drawString(font, creatorHUDComponent,
                width / 2 - font.width(creatorHUDComponent) / 2,
                height / 2 - (BUTTON_HEIGHT + PADDING_QUART) * 2 - font.lineHeight - PADDING_QUART, CommonColors.WHITE, true
        );

        Component creatorTriggerComponent = Component.literal("Creator: Triggers");
        guiGraphics.drawString(font, creatorTriggerComponent,
                width / 2 - font.width(creatorTriggerComponent) / 2,
                height / 2 - font.lineHeight - PADDING_QUART - PADDING_HALF, CommonColors.WHITE, true
        );

        Component creatorObserverComponent = Component.literal("Creator: Observers");
        guiGraphics.drawString(font, creatorObserverComponent,
                width / 2 - font.width(creatorObserverComponent) / 2,
                height / 2 + (BUTTON_HEIGHT + PADDING_QUART) * 3 - font.lineHeight - PADDING_QUART - (PADDING_HALF * 2), CommonColors.WHITE, true
        );

        Component settingsComponent = Component.literal("Settings");
        guiGraphics.drawString(font, settingsComponent,
                width / 2 - font.width(settingsComponent) / 2,
                height / 2 + (BUTTON_HEIGHT + PADDING_QUART) * 6 - font.lineHeight - PADDING_QUART - (PADDING_HALF * 3), CommonColors.WHITE, true
        );

        //Versions
//        guiGraphics.drawString(font, Component.literal("Mod Version: v" + FishOnMCExtras.VERSION).withStyle(ChatFormatting.DARK_GRAY), PADDING_QUART, height - font.lineHeight - PADDING_QUART, CommonColors.WHITE, true);
        guiGraphics.drawString(font, Component.literal("HUD Version: v" + FishOnMCExtras.HUD_VERSION).withStyle(ChatFormatting.DARK_GRAY), PADDING_QUART, height - (font.lineHeight + PADDING_QUART) * 2, CommonColors.WHITE, true);
        guiGraphics.drawString(font, Component.literal("Chat Trigger Version: v" + FishOnMCExtras.CHAT_TRIGGER_VERSION).withStyle(ChatFormatting.DARK_GRAY), PADDING_QUART, height - (font.lineHeight + PADDING_QUART) * 3, CommonColors.WHITE, true);
        guiGraphics.drawString(font, Component.literal("Timer Version: v" + FishOnMCExtras.TIMER_VERSION).withStyle(ChatFormatting.DARK_GRAY), PADDING_QUART, height - (font.lineHeight + PADDING_QUART) * 4, CommonColors.WHITE, true);
        guiGraphics.drawString(font, Component.literal("Notification Version: v" + FishOnMCExtras.NOTIFICATION_VERSION).withStyle(ChatFormatting.DARK_GRAY), PADDING_QUART, height - (font.lineHeight + PADDING_QUART) * 5, CommonColors.WHITE, true);
        guiGraphics.drawString(font, Component.literal("Button Version: v" + FishOnMCExtras.BUTTON_VERSION).withStyle(ChatFormatting.DARK_GRAY), PADDING_QUART, height - (font.lineHeight + PADDING_QUART) * 6, CommonColors.WHITE, true);
        guiGraphics.drawString(font, Component.literal("Chat Notification Version: v" + FishOnMCExtras.CHAT_NOTIFICATION_VERSION).withStyle(ChatFormatting.DARK_GRAY), PADDING_QUART, height - (font.lineHeight + PADDING_QUART) * 7, CommonColors.WHITE, true);
        guiGraphics.drawString(font, Component.literal("Event Trigger Version: v" + FishOnMCExtras.EVENT_TRIGGER_VERSION).withStyle(ChatFormatting.DARK_GRAY), PADDING_QUART, height - (font.lineHeight + PADDING_QUART) * 8, CommonColors.WHITE, true);
        guiGraphics.drawString(font, Component.literal("Tracker Version: v" + FishOnMCExtras.TRACKER_VERSION).withStyle(ChatFormatting.DARK_GRAY), PADDING_QUART, height - (font.lineHeight + PADDING_QUART) * 9, CommonColors.WHITE, true);
        guiGraphics.drawString(font, Component.literal("HUD Icon Version: v" + FishOnMCExtras.HUD_VERSION).withStyle(ChatFormatting.DARK_GRAY), PADDING_QUART, height - (font.lineHeight + PADDING_QUART) * 10, CommonColors.WHITE, true);
        guiGraphics.drawString(font, Component.literal("Snippet Version: v" + FishOnMCExtras.SNIPPET_VERSION).withStyle(ChatFormatting.DARK_GRAY), PADDING_QUART, height - (font.lineHeight + PADDING_QUART) * 11, CommonColors.WHITE, true);
    }

    private void renderWidgets() {
        List<AbstractWidget> widgets = new ArrayList<>();

        widgets.add(Button.builder(
                Component.literal("HUD Texts"),
                button -> this.minecraft.setScreen(new CustomHudMakerScreen(this.minecraft.screen)))
                .pos(width / 2 - BUTTON_WIDTH / 2,
                        height / 2 - (BUTTON_HEIGHT + PADDING_QUART) * 2)
                .size(BUTTON_WIDTH / 2 - PADDING_HALF, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Open Custom HUD Text Creator Screen")))
                .build()
        );

        widgets.add(Button.builder(
                        Component.literal("HUD Icons"),
                        button -> this.minecraft.setScreen(new CustomHudIconMakerScreen(this.minecraft.screen)))
                .pos(width / 2,
                        height / 2 - (BUTTON_HEIGHT + PADDING_QUART) * 2)
                .size(BUTTON_WIDTH / 2 - PADDING_HALF, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Open Custom HUD Icon Creator Screen")))
                .build()
        );

        widgets.add(Button.builder(
                        Component.literal("M"),
                        button -> this.minecraft.setScreen(new MoveElementScreen(this.minecraft.screen)))
                .pos(width / 2 + BUTTON_WIDTH / 2,
                        height / 2 - (BUTTON_HEIGHT + PADDING_QUART) * 2)
                .size(BUTTON_HEIGHT, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Move HUDs")))
                .build()
        );

        widgets.add(Button.builder(
                        Component.literal("S"),
                        button -> this.minecraft.setScreen(new CustomSnippetMakerScreen(this.minecraft.screen)))
                .pos(width / 2 - BUTTON_WIDTH / 2 - BUTTON_HEIGHT - PADDING_QUART,
                        height / 2 - (BUTTON_HEIGHT + PADDING_QUART) * 2)
                .size(BUTTON_HEIGHT, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Make placeholder snippets")))
                .build()
        );

        widgets.add(Button.builder(
                        Component.literal("Chat Triggers"),
                        button -> this.minecraft.setScreen(new CustomChatTriggerMakerScreen(this.minecraft.screen)))
                .pos(width / 2 - BUTTON_WIDTH / 2,
                        height / 2  - PADDING_HALF)
                .size(BUTTON_WIDTH / 2 - PADDING_HALF, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Open Custom Chat Trigger Creator Screen")))
                .build()
        );

        widgets.add(Button.builder(
                        Component.literal("Event Triggers"),
                        button -> this.minecraft.setScreen(new CustomEventTriggerMakerScreen(this.minecraft.screen)))
                .pos(width / 2,
                        height / 2  - PADDING_HALF)
                .size(BUTTON_WIDTH / 2 - PADDING_HALF, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Open Custom Event Trigger Creator Screen")))
                .build()
        );

        widgets.add(Button.builder(
                        Component.literal("Timers"),
                        button -> this.minecraft.setScreen(new CustomTimerMakerScreen(this.minecraft.screen)))
                .pos(width / 2 - BUTTON_WIDTH / 2,
                        height / 2 + (BUTTON_HEIGHT + PADDING_QUART) - PADDING_HALF)
                .size(BUTTON_WIDTH / 2 - PADDING_HALF, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Open Custom Timer Creator Screen")))
                .build()
        );



        widgets.add(Button.builder(
                        Component.literal("Notifications"),
                        button -> this.minecraft.setScreen(new CustomNotificationMakerScreen(this.minecraft.screen)))
                .pos(width / 2 - BUTTON_WIDTH / 2,
                        height / 2 + (BUTTON_HEIGHT + PADDING_QUART) * 3 - (PADDING_HALF * 2))
                .size(BUTTON_WIDTH / 2 - PADDING_HALF, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Open Custom Notification Creator Screen")))
                .build()
        );

        widgets.add(Button.builder(
                        Component.literal("Chat Notifications"),
                        button -> this.minecraft.setScreen(new CustomChatNotificationMakerScreen(this.minecraft.screen)))
                .pos(width / 2,
                        height / 2 + (BUTTON_HEIGHT + PADDING_QUART) * 3 - (PADDING_HALF * 2))
                .size(BUTTON_WIDTH / 2 - PADDING_HALF, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Open Custom Chat Notification Creator Screen")))
                .build()
        );

        widgets.add(Button.builder(
                        Component.literal("Trackers"),
                        button -> this.minecraft.setScreen(new CustomTrackerMakerScreen(this.minecraft.screen)))
                .pos(width / 2 - BUTTON_WIDTH / 2,
                        height / 2 + (BUTTON_HEIGHT + PADDING_QUART) * 4 - (PADDING_HALF * 2))
                .size(BUTTON_WIDTH / 2 - PADDING_HALF, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Open Custom Tracker Creator Screen")))
                .build()
        );



        widgets.add(Button.builder(
                        Component.literal("Configuration"),
                        button -> ConfigApiJava.INSTANCE.openScreen(FishOnMCExtras.MOD_ID))
                .pos(width / 2 - BUTTON_WIDTH / 2,
                        height / 2 + (BUTTON_HEIGHT + PADDING_QUART) * 6 - (PADDING_HALF * 3))
                .size(BUTTON_WIDTH / 2 - PADDING_HALF, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Open Configuration Screen")))
                .build()
        );

        widgets.add(Button.builder(
                        Component.literal("FOER Controls"),
                        button -> ConfigApiJava.INSTANCE.openScreen(Configs.keyBindConfig.translationKey()))
                .pos(width / 2,
                        height / 2 + (BUTTON_HEIGHT + PADDING_QUART) * 6 - (PADDING_HALF * 3))
                .size(BUTTON_WIDTH / 2 - PADDING_HALF, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("Open Controls Screen")))
                .build()
        );

        Component modVersion = Component.literal("Mod Version: v" + FishOnMCExtras.VERSION + " (Click for Changelog)").withStyle(ChatFormatting.YELLOW);
        widgets.add(
                new PlainTextButton(
                        PADDING_QUART,
                        height - font.lineHeight - PADDING_QUART,
                        font.width(modVersion),
                        font.lineHeight,
                        modVersion,
                        button -> minecraft.setScreen(new ChangelogScreen(this)),
                        font
                )
        );

        widgets.forEach(this::addRenderableWidget);
    }
    //endregion
}
