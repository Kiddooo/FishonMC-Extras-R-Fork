package dannypx.foe.screens.widget;

import dannypx.foe.handler.logic.CodeExecuterHandler;
import dannypx.foe.handler.store.CustomNotificationDataHandler;
import dannypx.foe.placeholder.editbox.PlaceholderEditBox;
import dannypx.foe.screens.interfaces.ScreenConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class EditCustomNotificationWidget extends AbstractWidget implements ScreenConstants {
    Minecraft minecraft = Minecraft.getInstance();

    private final List<LineEntry> entries = new ArrayList<>();
    private LineEntry focusedEntry = null;

    private Component header;
    private final int headerHeight = 20;

    private final int editBoxHeight = 20;

    private final EditBox idEditBox;
    public String newName;

    private final EditBox iconEditBox;
    public String icon;

    public boolean hasSelectedOption = false;
    public String currentSelectedNotification = null;

    private int scrollOffset = 0;
    private final int scrollbarWidth = 6;

    public EditCustomNotificationWidget(int x, int y, int width, int height, Component header) {
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

        newName = "";

        idEditBox.setResponder(s -> {
            if (hasSelectedOption) {
                newName = s;
            }
            idEditBox.setHint(Component.literal(s));
        });

        idEditBox.setValue("");

        iconEditBox = new EditBox(
                minecraft.font,
                getX() + width / 2 + minecraft.font.width("Icon") + PADDING_HALF,
                getY() + headerHeight + PADDING,
                width / 2 - minecraft.font.width("Icon") - PADDING - PADDING_HALF,
                editBoxHeight,
                Component.empty()
        );
        iconEditBox.setMaxLength(Integer.MAX_VALUE);

        icon = "";

        iconEditBox.setResponder(s -> {
            if (hasSelectedOption) {
                icon = s;
            }
            iconEditBox.setHint(Component.literal(s));
        });

        iconEditBox.setValue("");
    }

    public List<LineEntry> getEntries() {
        return entries;
    }

    public void selectNotification(String id, CustomNotificationDataHandler.CustomNotification customNotification) {
        removeAllEntries();
        hasSelectedOption = true;
        newName = id;
        icon = customNotification.getIcon();
        currentSelectedNotification = id;
        header = Component.literal(id);
        idEditBox.setValue(id);
        idEditBox.setHint(Component.literal(id));
        iconEditBox.setValue(customNotification.getIcon());
        iconEditBox.setHint(Component.literal(customNotification.getIcon()));

        customNotification.getStringLines().forEach(line -> this.addEntry(new LineEntry(
                line,
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
        return new EditCustomNotificationWidget.LineEntry(
                "Example text",
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
        newName = "";
        icon = "";
        currentSelectedNotification = null;
        iconEditBox.setValue("");
        iconEditBox.setHint(Component.literal(""));
        idEditBox.setValue("");
        idEditBox.setHint(Component.literal(""));
        header = Component.literal("No Notification Selected");

    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int entryStartY = getY() + headerHeight + PADDING + editBoxHeight + PADDING;

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

        guiGraphics.drawString(
                minecraft.font,
                "Icon",
                getX() + width / 2,
                getY() + PADDING + headerHeight + headerHeight / 2 - minecraft.font.lineHeight / 2,
                CommonColors.WHITE,
                true
        );

        idEditBox.render(guiGraphics, mouseX, mouseY, delta);
        iconEditBox.render(guiGraphics, mouseX, mouseY, delta);

        if(iconEditBox.isMouseOver(mouseX, mouseY)) {
            guiGraphics.setComponentTooltipForNextFrame(minecraft.font, List.of(
                    Component.literal("Optional").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC),
                    Component.empty(),
                    Component.literal("Must be an item").withStyle(ChatFormatting.GRAY),
                    Component.literal("using one of the following formats: ").withStyle(ChatFormatting.GRAY),
                    Component.literal("\"minecraft:<id>\"").withStyle(ChatFormatting.GOLD),
                    Component.literal("\"minecraft:<id>[<componentData>]\"").withStyle(ChatFormatting.GOLD)
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
        int visibleHeight = height - PADDING - PADDING - headerHeight - editBoxHeight - PADDING * 2;

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
            iconEditBox.setFocused(false);
            return true;
        }

        if (iconEditBox.mouseClicked(mouseButtonEvent, doubled)) {
            if (focusedEntry != null) {
                focusedEntry.setFocused(false);
                focusedEntry = null;
            }
            iconEditBox.setFocused(true);
            idEditBox.setFocused(false);
            return true;
        }

        for (LineEntry entry : entries) {
            if (entry.mouseClicked(mouseButtonEvent, doubled)) {
                if (focusedEntry != null && focusedEntry != entry) focusedEntry.setFocused(false);
                focusedEntry = entry;
                entry.setFocused(true);
                idEditBox.setFocused(false);
                iconEditBox.setFocused(false);
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent keyEvent) {
        if (idEditBox.isFocused()) return idEditBox.keyPressed(keyEvent);
        if (iconEditBox.isFocused()) return iconEditBox.keyPressed(keyEvent);
        if (focusedEntry != null) return focusedEntry.keyPressed(keyEvent);
        return false;
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent characterEvent) {
        if (idEditBox.isFocused()) return idEditBox.charTyped(characterEvent);
        if (iconEditBox.isFocused()) return iconEditBox.charTyped(characterEvent);
        if (focusedEntry != null) return focusedEntry.charTyped(characterEvent);
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {

        scrollOffset -= (int) (verticalAmount * 10);

        int visibleHeight = height - PADDING - PADDING - headerHeight - editBoxHeight - PADDING * 2;
        int totalContentHeight = entries.size() * LineEntry.HEIGHT;
        int maxScroll = Math.max(0, totalContentHeight - visibleHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        return true;
    }

    public static class LineEntry {
        Minecraft minecraftClient = Minecraft.getInstance();

        private final PlaceholderEditBox editBoxWidget;
        private final Button addButton;
        private final Button deleteButton;

        public String lineString;
        public int width;

        public static final int HEIGHT = 24;
        private static final int SPACING = 6;
        private static final int BUTTON_SIZE = 25;

        public LineEntry(String defaultLine, int width, Callback callback) {
            lineString = defaultLine;
            this.width = width;

            editBoxWidget = new PlaceholderEditBox(
                    minecraftClient.font,
                    0, 0,
                    0, 20,
                    Component.empty()
            );
            editBoxWidget.setMaxLength(Integer.MAX_VALUE);

            editBoxWidget.setValue(defaultLine);
            int maxStringWidth = width - BUTTON_SIZE * 2 - PADDING_QUART * 2 - SPACING - 20;
            editBoxWidget.setHint(Component.literal(
                    minecraftClient.font.width(defaultLine) > maxStringWidth
                    ? minecraftClient.font.plainSubstrByWidth(defaultLine, maxStringWidth) + "..."
                    : defaultLine
            ));
            editBoxWidget.setResponder(s -> {
                lineString = s;
                editBoxWidget.setHint(Component.literal(s));
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
            int stringWidth = fullWidth - BUTTON_SIZE * 2 - PADDING_QUART * 2 - SPACING;

            editBoxWidget.setPosition(x, y);
            editBoxWidget.setWidth(stringWidth);

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
            editBoxWidget.render(guiGraphics, mouseX, mouseY, delta);
            addButton.render(guiGraphics, mouseX, mouseY, delta);
            deleteButton.render(guiGraphics, mouseX, mouseY, delta);

            this.renderTooltips(guiGraphics, mouseX, mouseY, delta);
        }

        private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
            if(editBoxWidget.isFocused()
                    && editBoxWidget.isMouseOver(mouseX, mouseY)) {
                guiGraphics.setTooltipForNextFrame(minecraftClient.font, Component.literal("You can also use placeholders. See wiki"), mouseX, mouseY);
            }
        }

        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubled) {
            if (editBoxWidget.mouseClicked(mouseButtonEvent, doubled)) return true;
            if(addButton.mouseClicked(mouseButtonEvent, doubled)) return false;
            if(deleteButton.mouseClicked(mouseButtonEvent, doubled)) return false;
            return false;
        }

        public void setFocused(boolean focused) {
            editBoxWidget.setFocused(focused);
        }

        public boolean keyPressed(KeyEvent keyEvent) {
            return editBoxWidget.keyPressed(keyEvent);
        }

        public boolean charTyped(CharacterEvent characterEvent) {
            return editBoxWidget.charTyped(characterEvent);
        }


        public interface Callback {
            void onDelete(LineEntry lineEntry);
            void onAdd(LineEntry lineEntry);
        }
    }
}
