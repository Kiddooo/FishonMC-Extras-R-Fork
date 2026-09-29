package dannypx.foe.screens.widget;

import dannypx.foe.handler.logic.CodeExecuterHandler;
import dannypx.foe.handler.store.CustomTrackerDataHandler;
import dannypx.foe.placeholder.editbox.PlaceholderEditBox;
import dannypx.foe.screens.interfaces.ScreenConstants;
import dannypx.foe.type.custom_value.*;
import dannypx.foe.type.tracker.TrackerAction;
import dannypx.foe.type.tracker.TrackerType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class EditCustomTrackerWidget extends AbstractWidget implements ScreenConstants {
    Minecraft minecraft = Minecraft.getInstance();

    private final List<LineEntry> entries = new ArrayList<>();
    private LineEntry focusedEntry = null;

    private Component header;
    private final int headerHeight = 20;

    private final int editBoxHeight = 20;

    private final EditBox idEditBox;
    public String idName;

    private final Checkbox useTrackerCheckbox;
    public Boolean useTracker;

    private final Button trackerTypeButton;
    public TrackerType trackerType;

    private final Checkbox isPersistentCheckbox;
    public Boolean isPersistent;

    private final EditBox defaultValueEditBox;
    public String defaultValue;

    public boolean hasSelectedOption = false;
    public String currentSelectedTracker = null;

    private int scrollOffset = 0;
    private final int scrollbarWidth = 6;

    public EditCustomTrackerWidget(int x, int y, int width, int height, Component header) {
        super(x, y, width, height, Component.empty());
        this.header = header;
        idEditBox = new EditBox(
                minecraft.font,
                getX() + PADDING,
                getY() + headerHeight + PADDING,
                width / 2 - PADDING - PADDING_HALF,
                editBoxHeight,
                Component.empty()
        );
        idEditBox.setMaxLength(Integer.MAX_VALUE);

        idName = "";

        idEditBox.setResponder(s -> {
            if (hasSelectedOption) {
                idName = s;
            }
            idEditBox.setHint(Component.literal(s));
        });

        idEditBox.setValue("");

        useTrackerCheckbox = Checkbox.builder(Component.literal("Use Tracker"), minecraft.font)
                .pos(
                        getX() + width / 2,
                        getY() + headerHeight + PADDING
                )
                .selected(true)
                .onValueChange((checkbox, checked) -> useTracker = checked)
                .build();
        useTracker = true;

        trackerTypeButton = Button.builder(
                Component.literal(TrackerType.BOOLEAN.name()), button -> {
                    this.changeTrackerType();
                        })
                .pos(
                        getX() + width / 2,
                        getY() + headerHeight + PADDING + editBoxHeight + PADDING
                )
                .size(50, 20)
                .build();
        trackerType = TrackerType.BOOLEAN;

        isPersistentCheckbox = Checkbox.builder(Component.literal("Is Persistent"), minecraft.font)
                .pos(
                        getX() + width / 2 + PADDING + 50,
                        getY() + headerHeight + PADDING + editBoxHeight + PADDING
                )
                .selected(true)
                .onValueChange((checkbox, checked) -> isPersistent = checked)
                .build();
        isPersistent = true;

        defaultValueEditBox = new EditBox(
                minecraft.font,
                getX() + PADDING,
                getY() + headerHeight + PADDING + editBoxHeight + PADDING,
                width / 2 - PADDING - PADDING_HALF,
                editBoxHeight,
                Component.empty()
        );
        defaultValueEditBox.setMaxLength(Integer.MAX_VALUE);

        defaultValue = "";

        defaultValueEditBox.setResponder(s -> {
            if (hasSelectedOption) {
                defaultValue = s;
            }
            defaultValueEditBox.setHint(Component.literal(s));
        });

        defaultValueEditBox.setValue("");
    }

    public List<LineEntry> getEntries() {
        return entries;
    }

    public void selectTracker(String id, CustomTrackerDataHandler.CustomTracker customTracker) {
        removeAllEntries();
        hasSelectedOption = true;
        idName = id;
        currentSelectedTracker = id;
        header = Component.literal(id);

        idEditBox.setValue(id);
        idEditBox.setHint(Component.literal(id));

        trackerType = customTracker.getTrackerType();
        trackerTypeButton.setMessage(Component.literal(customTracker.getTrackerType().name()));

        useTracker = customTracker.isUseTracker();
        if(customTracker.isUseTracker() != useTrackerCheckbox.selected()) {
            useTrackerCheckbox.onPress(null);
        }

        isPersistent = customTracker.isPersistent();
        if(customTracker.isPersistent() != isPersistentCheckbox.selected()) {
            isPersistentCheckbox.onPress(null);
        }

        switch (customTracker.getDefaultValue()) {
            case BooleanValue booleanValue -> {
                defaultValueEditBox.setValue(String.valueOf(booleanValue.value()));
                defaultValueEditBox.setHint(Component.literal(String.valueOf(booleanValue.value())));
            }
            case NumberValue numberValue -> {
                defaultValueEditBox.setValue(String.valueOf(numberValue.value()));
                defaultValueEditBox.setHint(Component.literal(String.valueOf(numberValue.value())));
            }
            case PlaceholderStringValue placeholderStringValue -> {
                defaultValueEditBox.setValue(placeholderStringValue.value());
                defaultValueEditBox.setHint(Component.literal(placeholderStringValue.value()));
            }
            case ItemStackValue itemStackValue -> {
                defaultValueEditBox.setValue(itemStackValue.value().value2());
                defaultValueEditBox.setHint(Component.literal(itemStackValue.value().value2()));
            }
            default -> {
                defaultValueEditBox.setValue("");
                defaultValueEditBox.setHint(Component.empty());
            }
        }

        customTracker.actions.forEach((actionId, action) -> this.addEntry(new LineEntry(
                actionId,
                action.value1(),
                action.value2(),
                action.value3(),
                width,
                getDefaultCallback()
        )));
    }

    public void addEntry(LineEntry entry) {
        entries.add(entry);
    }

    public void addEntry(int pos, LineEntry entry) {
        entries.add(pos, entry);
    }

    public void addNewEntry() {
        this.addEntry(getDefaultEntry());
    }

    public void addNewEntry(int pos) {
        this.addEntry(pos, getDefaultEntry());
    }

    private LineEntry getDefaultEntry() {
        return new LineEntry(
                "Action #" + UUID.randomUUID(),
                TrackerAction.SET,
                "",
                BooleanValue.getFalse(),
                width,
                getDefaultCallback()
        );
    }

    private LineEntry.Callback getDefaultCallback() {
        return new LineEntry.Callback() {
            @Override
            public void onDelete(LineEntry lineEntry) {
                CodeExecuterHandler.runLater(1, () -> removeEntry(lineEntry));
            }

            @Override
            public void onAdd(LineEntry lineEntry) {
                CodeExecuterHandler.runLater(1, () -> addNewEntry(entries.indexOf(lineEntry)));
            }
        };
    }

    public void removeEntry(LineEntry entry) {
        entries.remove(entry);
    }

    public void removeAllEntries() {
        entries.clear();
    }

    public void reset() {
        this.removeAllEntries();
        hasSelectedOption = false;
        currentSelectedTracker = null;

        idName = "";
        idEditBox.setValue("");
        idEditBox.setHint(Component.literal(""));

        useTracker = true;
        if(!useTrackerCheckbox.selected()) {
            useTrackerCheckbox.onPress(null);
        }

        isPersistent = true;
        if(!isPersistentCheckbox.selected()) {
            isPersistentCheckbox.onPress(null);
        }

        trackerType = TrackerType.BOOLEAN;
        trackerTypeButton.setMessage(Component.literal(TrackerType.BOOLEAN.name()));

        defaultValue = "";
        defaultValueEditBox.setValue("");
        defaultValueEditBox.setHint(Component.literal(""));

        header = Component.literal("No Tracker Selected");

    }

    public void changeTrackerType() {
        this.trackerTypeButton.setMessage(Component.literal(trackerType.next().name()));
        this.trackerType = trackerType.next();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int entryStartY = getY() + headerHeight + PADDING + (editBoxHeight + PADDING) * 2;

        guiGraphics.fill(getX(), getY(), getRight(), getBottom(), 0x55000000);
        guiGraphics.hLine(getX(), getRight(), getBottom(), CommonColors.GRAY);
        guiGraphics.vLine(getX(), 0, getBottom(), CommonColors.GRAY);
        guiGraphics.drawCenteredString(
                minecraft.font,
                header,
                getX() + width / 2,
                getY() + PADDING,
                CommonColors.WHITE
        );

        idEditBox.render(guiGraphics, mouseX, mouseY, delta);
        useTrackerCheckbox.render(guiGraphics, mouseX, mouseY, delta);
        isPersistentCheckbox.render(guiGraphics, mouseX, mouseY, delta);
        trackerTypeButton.render(guiGraphics, mouseX, mouseY, delta);
        defaultValueEditBox.render(guiGraphics, mouseX, mouseY, delta);

        if(defaultValueEditBox.isMouseOver(mouseX, mouseY)) {
            guiGraphics.setComponentTooltipForNextFrame(minecraft.font, List.of(
                    Component.literal("Default Value"),
                    Component.empty(),
                    Component.literal("This is the default value once the tracker is made").withStyle(ChatFormatting.GRAY),
                    Component.empty(),
                    Component.literal("BOOLEAN - false, true").withStyle(ChatFormatting.GRAY),
                    Component.literal("INTEGER - whole numbers").withStyle(ChatFormatting.GRAY),
                    Component.literal("ITEMSTACK - slot index, placeholder index, minecraft item name").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        }

        if(isPersistentCheckbox.isMouseOver(mouseX, mouseY)) {
            guiGraphics.setComponentTooltipForNextFrame(minecraft.font, List.of(
                    Component.literal("To keep the value across sessions").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        }

        guiGraphics.enableScissor(
                getX() + PADDING,
                entryStartY,
                getRight() - PADDING,
                getBottom() - PADDING
        );

        int startY = entryStartY - scrollOffset;

        for (int i = 0; i < entries.size(); i++) {
            int entryY = startY + i * LineEntry.HEIGHT;
            if (entryY + LineEntry.HEIGHT < entryStartY || entryY > getBottom() - PADDING)
                continue;

            LineEntry entry = entries.get(i);
            entry.setPosition(getX() + PADDING, entryY, width - PADDING - PADDING - scrollbarWidth - PADDING);
            entry.render(guiGraphics, mouseX, mouseY, delta);
        }

        int totalContentHeight = entries.size() * LineEntry.HEIGHT;
        int visibleHeight = getBottom() - PADDING - entryStartY;

        if (totalContentHeight > visibleHeight) {
            int scrollbarHeight = Math.max(10, visibleHeight * visibleHeight / totalContentHeight);

            int scrollbarY = entryStartY + scrollOffset * visibleHeight / totalContentHeight;

            int scrollbarX = getX() + width - PADDING - scrollbarWidth;

            guiGraphics.fill(
                    scrollbarX,
                    scrollbarY,
                    scrollbarX + scrollbarWidth,
                    scrollbarY + scrollbarHeight,
                    CommonColors.LIGHT_GRAY
            );
        }

        guiGraphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubled) {
        if (!isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            if (focusedEntry != null) {
                focusedEntry.setFocused(false);
                focusedEntry = null;
            }
            return false;
        }

        if (idEditBox.mouseClicked(mouseButtonEvent, doubled)) {
            if (focusedEntry != null) {
                focusedEntry.setFocused(false);
                focusedEntry = null;
            }
            idEditBox.setFocused(true);
            useTrackerCheckbox.setFocused(false);
            isPersistentCheckbox.setFocused(false);
            trackerTypeButton.setFocused(false);
            defaultValueEditBox.setFocused(false);
            return true;
        }

        if (useTrackerCheckbox.mouseClicked(mouseButtonEvent, doubled)) {
            if (focusedEntry != null) {
                focusedEntry.setFocused(false);
                focusedEntry = null;
            }
            idEditBox.setFocused(false);
            useTrackerCheckbox.setFocused(true);
            isPersistentCheckbox.setFocused(false);
            trackerTypeButton.setFocused(false);
            defaultValueEditBox.setFocused(false);
            return true;
        }

        if (isPersistentCheckbox.mouseClicked(mouseButtonEvent, doubled)) {
            if (focusedEntry != null) {
                focusedEntry.setFocused(false);
                focusedEntry = null;
            }
            idEditBox.setFocused(false);
            useTrackerCheckbox.setFocused(false);
            isPersistentCheckbox.setFocused(true);
            trackerTypeButton.setFocused(false);
            defaultValueEditBox.setFocused(false);
            return true;
        }

        if (trackerTypeButton.mouseClicked(mouseButtonEvent, doubled)) {
            if (focusedEntry != null) {
                focusedEntry.setFocused(false);
                focusedEntry = null;
            }
            idEditBox.setFocused(false);
            useTrackerCheckbox.setFocused(false);
            isPersistentCheckbox.setFocused(false);
            trackerTypeButton.setFocused(true);
            defaultValueEditBox.setFocused(false);
            return true;
        }

        if (defaultValueEditBox.mouseClicked(mouseButtonEvent, doubled)) {
            if (focusedEntry != null) {
                focusedEntry.setFocused(false);
                focusedEntry = null;
            }
            idEditBox.setFocused(false);
            useTrackerCheckbox.setFocused(false);
            isPersistentCheckbox.setFocused(false);
            trackerTypeButton.setFocused(false);
            defaultValueEditBox.setFocused(true);
            return true;
        }

        for (LineEntry entry : entries) {
            if (entry.mouseClicked(mouseButtonEvent, doubled)) {
                if (focusedEntry != null && focusedEntry != entry) focusedEntry.setFocused(false);
                focusedEntry = entry;
                entry.setFocused(true);
                idEditBox.setFocused(false);
                useTrackerCheckbox.setFocused(false);
                isPersistentCheckbox.setFocused(false);
                trackerTypeButton.setFocused(false);
                defaultValueEditBox.setFocused(false);
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent keyEvent) {
        if (idEditBox.isFocused()) return idEditBox.keyPressed(keyEvent);
        if (useTrackerCheckbox.isFocused()) return useTrackerCheckbox.keyPressed(keyEvent);
        if (isPersistentCheckbox.isFocused()) return isPersistentCheckbox.keyPressed(keyEvent);
        if (trackerTypeButton.isFocused()) return trackerTypeButton.keyPressed(keyEvent);
        if (defaultValueEditBox.isFocused()) return defaultValueEditBox.keyPressed(keyEvent);
        if (focusedEntry != null) return focusedEntry.keyPressed(keyEvent);
        return false;
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent characterEvent) {
        if (idEditBox.isFocused()) return idEditBox.charTyped(characterEvent);
        if (useTrackerCheckbox.isFocused()) return useTrackerCheckbox.charTyped(characterEvent);
        if (isPersistentCheckbox.isFocused()) return isPersistentCheckbox.charTyped(characterEvent);
        if (trackerTypeButton.isFocused()) return trackerTypeButton.charTyped(characterEvent);
        if (defaultValueEditBox.isFocused()) return defaultValueEditBox.charTyped(characterEvent);
        if (focusedEntry != null) return focusedEntry.charTyped(characterEvent);
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int) (verticalAmount * 10);

        int entryStartY = getY() + headerHeight + PADDING + (editBoxHeight + PADDING) * 2;

        int visibleHeight = getBottom() - PADDING - entryStartY;
        int totalContentHeight = entries.size() * LineEntry.HEIGHT;
        int maxScroll = Math.max(0, totalContentHeight - visibleHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        return true;
    }

    public static class LineEntry {
        Minecraft minecraftClient = Minecraft.getInstance();

        private final EditBox actionIdEditBoxWidget;
        private final EditBox trackerActionEditBoxWidget;
        private final PlaceholderEditBox conditionEditBoxWidget;
        private final PlaceholderEditBox valueToUseEditBoxWidget;
        private final Button addButton;
        private final Button deleteButton;

        public String actionId;
        public String trackerAction;
        public String condition;
        public String valueToUse;
        public int width;

        public String getActionId() {
            return actionId;
        }

        public static final int HEIGHT = 24;
        private static final int SPACING = 6;
        private static final int BUTTON_SIZE = 25;

        public LineEntry(String actionId, TrackerAction trackerAction, String condition, TrackerValue valueToUse, int width, Callback callback) {
            this.actionId = actionId;
            this.trackerAction = trackerAction.name();
            this.condition = condition;
            this.valueToUse = String.valueOf(switch (valueToUse) {
                case BooleanValue booleanValue -> booleanValue.value();
                case NumberValue numberValue -> numberValue.value();
                case PlaceholderStringValue placeholderStringValue -> placeholderStringValue.value();
                case ItemStackValue itemStackValue -> itemStackValue.value().value2();
                default -> "";
            });
            this.width = width;

            actionIdEditBoxWidget = new EditBox(
                    minecraftClient.font,
                    0, 0,
                    0, 20,
                    Component.empty()
            );
            actionIdEditBoxWidget.setMaxLength(Integer.MAX_VALUE);

            actionIdEditBoxWidget.setValue(actionId);
            int maxStringWidth = (width - BUTTON_SIZE * 2 - PADDING_QUART * 2 - SPACING - 20) / 4 - 32;
            actionIdEditBoxWidget.setHint(Component.literal(
                    minecraftClient.font.width(actionId) > maxStringWidth
                    ? minecraftClient.font.plainSubstrByWidth(actionId, maxStringWidth) + "..."
                    : actionId
            ));
            actionIdEditBoxWidget.setResponder(s -> {
                this.actionId = s;
                actionIdEditBoxWidget.setHint(Component.literal(s));
            });

            trackerActionEditBoxWidget = new EditBox(
                    minecraftClient.font,
                    0, 0,
                    0, 20,
                    Component.empty()
            );
            trackerActionEditBoxWidget.setMaxLength(Integer.MAX_VALUE);

            trackerActionEditBoxWidget.setValue(trackerAction.name());
            trackerActionEditBoxWidget.setHint(Component.literal(
                    minecraftClient.font.width(trackerAction.name()) > maxStringWidth
                            ? minecraftClient.font.plainSubstrByWidth(trackerAction.name(), maxStringWidth) + "..."
                            : trackerAction.name()
            ));

            trackerActionEditBoxWidget.setResponder(s -> {
                this.trackerAction = s;
                trackerActionEditBoxWidget.setHint(Component.literal(s));

                if (s.isEmpty()) {
                    trackerActionEditBoxWidget.setSuggestion(null);
                    return;
                }

                for (String trackerActionName : Arrays.stream(TrackerAction.values()).map(Enum::name).toList()) {
                    if (trackerActionName.toLowerCase().startsWith(s.toLowerCase()) &&
                            !trackerActionName.equalsIgnoreCase(s)) {

                        trackerActionEditBoxWidget.setSuggestion(
                                trackerActionName.substring(s.length())
                        );
                        return;
                    }
                }

                trackerActionEditBoxWidget.setSuggestion(null);
            });

            conditionEditBoxWidget = new PlaceholderEditBox(
                    minecraftClient.font,
                    0, 0,
                    0, 20,
                    Component.empty()
            );
            conditionEditBoxWidget.setMaxLength(Integer.MAX_VALUE);

            conditionEditBoxWidget.setValue(condition);
            conditionEditBoxWidget.setHint(Component.literal(
                    minecraftClient.font.width(condition) > maxStringWidth
                            ? minecraftClient.font.plainSubstrByWidth(condition, maxStringWidth) + "..."
                            : condition
            ));
            conditionEditBoxWidget.setResponder(s -> {
                this.condition = s;
                conditionEditBoxWidget.setHint(Component.literal(s));
            });

            valueToUseEditBoxWidget = new PlaceholderEditBox(
                    minecraftClient.font,
                    0, 0,
                    0, 20,
                    Component.empty()
            );
            valueToUseEditBoxWidget.setMaxLength(Integer.MAX_VALUE);

            valueToUseEditBoxWidget.setValue(this.valueToUse);
            valueToUseEditBoxWidget.setHint(Component.literal(
                    minecraftClient.font.width(this.valueToUse) > maxStringWidth
                            ? minecraftClient.font.plainSubstrByWidth(this.valueToUse, maxStringWidth) + "..."
                            : this.valueToUse
            ));
            valueToUseEditBoxWidget.setResponder(s -> {
                this.valueToUse = s;
                valueToUseEditBoxWidget.setHint(Component.literal(s));
            });

            addButton = Button.builder(Component.literal("Add"),
                            (buttonWidget) -> callback.onAdd(this))
                    .size(BUTTON_SIZE, 20)
                    .tooltip(Tooltip.create(Component.literal("Add line")))
                    .build();

            deleteButton = Button.builder(Component.literal("Del"),
                    (buttonWidget) -> callback.onDelete(this))
                    .size(BUTTON_SIZE, 20)
                    .tooltip(Tooltip.create(Component.literal("Delete line")))
                    .build();
        }

        public void setPosition(int x, int y, int fullWidth) {
            int maxBoxWidth = (fullWidth - BUTTON_SIZE * 2 - PADDING_QUART * 2 - SPACING) / 4 - PADDING_QUART;

            actionIdEditBoxWidget.setPosition(x, y);
            actionIdEditBoxWidget.setWidth(maxBoxWidth);

            trackerActionEditBoxWidget.setPosition(x + ((fullWidth - BUTTON_SIZE * 2 - PADDING_QUART * 2 - SPACING) / 4), y);
            trackerActionEditBoxWidget.setWidth(maxBoxWidth);

            conditionEditBoxWidget.setPosition(x + ((fullWidth - BUTTON_SIZE * 2 - PADDING_QUART * 2 - SPACING) / 4) * 2, y);
            conditionEditBoxWidget.setWidth(maxBoxWidth);

            valueToUseEditBoxWidget.setPosition(x + ((fullWidth - BUTTON_SIZE * 2 - PADDING_QUART * 2 - SPACING) / 4) * 3, y);
            valueToUseEditBoxWidget.setWidth(maxBoxWidth);

            addButton.setPosition(
                    x + fullWidth - SPACING - BUTTON_SIZE * 2 - PADDING_QUART,
                    y
            );

            deleteButton.setPosition(
                    x + fullWidth - SPACING - BUTTON_SIZE,
                    y
            );
        }

        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
            actionIdEditBoxWidget.render(guiGraphics, mouseX, mouseY, delta);
            trackerActionEditBoxWidget.render(guiGraphics, mouseX, mouseY, delta);
            conditionEditBoxWidget.render(guiGraphics, mouseX, mouseY, delta);
            valueToUseEditBoxWidget.render(guiGraphics, mouseX, mouseY, delta);
            addButton.render(guiGraphics, mouseX, mouseY, delta);
            deleteButton.render(guiGraphics, mouseX, mouseY, delta);

            this.renderTooltips(guiGraphics, mouseX, mouseY, delta);
        }

        private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
            if(actionIdEditBoxWidget.isMouseOver(mouseX, mouseY)) {
                guiGraphics.setTooltipForNextFrame(minecraftClient.font, Component.literal("Action ID"), mouseX, mouseY);
            }

            if(trackerActionEditBoxWidget.isMouseOver(mouseX, mouseY)) {
                guiGraphics.setComponentTooltipForNextFrame(minecraftClient.font, new ArrayList<>(Arrays.asList(
                        Component.literal("Tracker Action"),
                        Component.empty(),
                        Component.literal("BOOLEAN").withStyle(ChatFormatting.GRAY),
                        Component.literal(" - SET").withStyle(ChatFormatting.GRAY),
                        Component.literal(" - TOGGLE").withStyle(ChatFormatting.GRAY),
                        Component.empty(),
                        Component.literal("INTEGER").withStyle(ChatFormatting.GRAY),
                        Component.literal(" - SET").withStyle(ChatFormatting.GRAY),
                        Component.literal(" - ADD").withStyle(ChatFormatting.GRAY),
                        Component.literal(" - SUBTRACT").withStyle(ChatFormatting.GRAY),
                        Component.empty(),
                        Component.literal("ITEMSTACK").withStyle(ChatFormatting.GRAY),
                        Component.literal(" - SET").withStyle(ChatFormatting.GRAY)
                )), mouseX, mouseY);
            }

            if(conditionEditBoxWidget.isMouseOver(mouseX, mouseY)) {
                guiGraphics.setComponentTooltipForNextFrame(minecraftClient.font, new ArrayList<>(Arrays.asList(
                        Component.literal("Condition"),
                        Component.literal("Optional").withStyle(ChatFormatting.DARK_GRAY),
                        Component.empty(),
                        Component.literal("Use placeholder conditions. See wiki").withStyle(ChatFormatting.GRAY)
                )), mouseX, mouseY);
            }

            if(valueToUseEditBoxWidget.isMouseOver(mouseX, mouseY)) {
                guiGraphics.setComponentTooltipForNextFrame(minecraftClient.font, new ArrayList<>(Arrays.asList(
                        Component.literal("Value to use"),
                        Component.literal("Optional if TOGGLE action").withStyle(ChatFormatting.DARK_GRAY),
                        Component.empty(),
                        Component.literal("For all actions (except TOGGLE),").withStyle(ChatFormatting.GRAY),
                        Component.literal("this is the value used to Set, Add").withStyle(ChatFormatting.GRAY),
                        Component.literal("or Subtract, to its current value").withStyle(ChatFormatting.GRAY),
                        Component.empty(),
                        Component.literal("BOOLEAN - false, true").withStyle(ChatFormatting.GRAY),
                        Component.literal("INTEGER - whole numbers").withStyle(ChatFormatting.GRAY),
                        Component.literal("ITEMSTACK - slot index, placeholder index, minecraft item name").withStyle(ChatFormatting.GRAY),
                        Component.empty(),
                        Component.literal("Placeholder values are also supported. See wiki").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                )), mouseX, mouseY);
            }
        }

        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubled) {
            if(actionIdEditBoxWidget.mouseClicked(mouseButtonEvent, doubled)) {
                actionIdEditBoxWidget.setFocused(true);
                trackerActionEditBoxWidget.setFocused(false);
                conditionEditBoxWidget.setFocused(false);
                valueToUseEditBoxWidget.setFocused(false);
                return true;
            }
            if(trackerActionEditBoxWidget.mouseClicked(mouseButtonEvent, doubled)) {
                actionIdEditBoxWidget.setFocused(false);
                trackerActionEditBoxWidget.setFocused(true);
                conditionEditBoxWidget.setFocused(false);
                valueToUseEditBoxWidget.setFocused(false);
                return true;
            }
            if(conditionEditBoxWidget.mouseClicked(mouseButtonEvent, doubled)) {
                actionIdEditBoxWidget.setFocused(false);
                trackerActionEditBoxWidget.setFocused(false);
                conditionEditBoxWidget.setFocused(true);
                valueToUseEditBoxWidget.setFocused(false);
                return true;
            }
            if(valueToUseEditBoxWidget.mouseClicked(mouseButtonEvent, doubled)) {
                actionIdEditBoxWidget.setFocused(false);
                trackerActionEditBoxWidget.setFocused(false);
                conditionEditBoxWidget.setFocused(false);
                valueToUseEditBoxWidget.setFocused(true);
                return true;
            }
            if(addButton.mouseClicked(mouseButtonEvent, doubled)) return false;
            if(deleteButton.mouseClicked(mouseButtonEvent, doubled)) return false;
            return false;
        }

        public void setFocused(boolean focused) {
            if(!focused) {
                actionIdEditBoxWidget.setFocused(false);
                trackerActionEditBoxWidget.setFocused(false);
                conditionEditBoxWidget.setFocused(false);
                valueToUseEditBoxWidget.setFocused(false);
            }
        }

        public boolean keyPressed(KeyEvent keyEvent) {
            if (trackerActionEditBoxWidget.isFocused() && keyEvent.key() == GLFW.GLFW_KEY_TAB) {
                String current = trackerActionEditBoxWidget.getValue();

                for (String trackerAction : Arrays.stream(TrackerAction.values()).map(Enum::name).toList()) {
                    if (trackerAction.toLowerCase().startsWith(current.toLowerCase())) {
                        trackerActionEditBoxWidget.setValue(trackerAction);
                        this.trackerAction = trackerAction;
                        trackerActionEditBoxWidget.setSuggestion(null);
                    }
                }
            }

            if (actionIdEditBoxWidget.keyPressed(keyEvent)) return true;
            if (trackerActionEditBoxWidget.keyPressed(keyEvent)) return true;
            if (conditionEditBoxWidget.keyPressed(keyEvent)) return true;
            return valueToUseEditBoxWidget.keyPressed(keyEvent);
        }

        public boolean charTyped(CharacterEvent characterEvent) {
            if (actionIdEditBoxWidget.charTyped(characterEvent)) return true;
            if (trackerActionEditBoxWidget.charTyped(characterEvent)) return true;
            if (conditionEditBoxWidget.charTyped(characterEvent)) return true;
            return valueToUseEditBoxWidget.charTyped(characterEvent);
        }

        public interface Callback {
            void onDelete(LineEntry lineEntry);
            void onAdd(LineEntry lineEntry);
        }
    }
}
