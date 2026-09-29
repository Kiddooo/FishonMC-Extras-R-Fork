package dannypx.foe.screens.widget;

import dannypx.foe.handler.logic.CodeExecuterHandler;
import dannypx.foe.handler.store.CustomHudDataHandler;
import dannypx.foe.placeholder.editbox.PlaceholderEditBox;
import dannypx.foe.screens.interfaces.ScreenConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
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

public class EditCustomHUDWidget extends AbstractWidget implements ScreenConstants {
    Minecraft minecraft = Minecraft.getInstance();

    private final List<LineEntry> entries = new ArrayList<>();
    private LineEntry focusedEntry = null;

    private Component header;
    private final int headerHeight = 20;

    private final int editBoxHeight = 20;

    private final EditBox idEditBox;
    public String newName;

    private final EditBox scaleEditBox;
    public float scale;

    private final Checkbox showBackgroundCheckBox;
    public boolean showBackground;

    private final Checkbox showBarsCheckBox;
    public boolean showBars;

    private final Checkbox showElementCheckBox;
    public boolean showElement;

    public boolean hasSelectedOption = false;
    public String currentSelectedHud = null;

    private int scrollOffset = 0;
    private final int scrollbarWidth = 6;

    public EditCustomHUDWidget(int x, int y, int width, int height, Component header) {
        super(x, y, width, height, Component.empty());
        this.header = header;
        idEditBox = new EditBox(
                minecraft.font,
                getX() + PADDING,
                getY() + headerHeight + PADDING,
                width / 3 - PADDING - PADDING_HALF,
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

        scaleEditBox = new EditBox(
                minecraft.font,
                getX() + width / 3 + minecraft.font.width("Scale") + PADDING_HALF,
                getY() + headerHeight + PADDING,
                40,
                editBoxHeight,
                Component.empty()
        );
        scaleEditBox.setMaxLength(5);

        scale = 1.0f;

        scaleEditBox.setResponder(s -> {
            if (hasSelectedOption) {
                try {
                    scale = Float.parseFloat(s);
                } catch (Exception e) {
                    scale = 1.0f;
                }
            }
            scaleEditBox.setHint(Component.literal(s));
        });

        scaleEditBox.setValue("");

        showBackgroundCheckBox = Checkbox.builder(Component.literal("Show Background"), minecraft.font)
                .pos(
                        getX() + PADDING,
                        getY() + headerHeight + PADDING + editBoxHeight + PADDING
                )
                .selected(true)
                .onValueChange((checkbox, checked) -> showBackground = checked)
                .build();
        showBackground = true;

        showBarsCheckBox = Checkbox.builder(Component.literal("Show Bars"), minecraft.font)
                .pos(
                        getX() + PADDING + minecraft.font.width("Show Background") + PADDING_HALF + 16 + PADDING_HALF,
                        getY() + headerHeight + PADDING + editBoxHeight + PADDING
                )
                .selected(true)
                .onValueChange((checkbox, checked) -> showBars = checked)
                .build();
        showBars = true;

        showElementCheckBox = Checkbox.builder(Component.literal("Show Element"), minecraft.font)
                .pos(
                        getX() + width / 3 + minecraft.font.width("Scale") + PADDING_HALF + 40 + PADDING_HALF,
                        getY() + headerHeight + PADDING
                )
                .selected(true)
                .onValueChange((checkbox, checked) -> showElement = checked)
                .build();
        showElement = true;
    }

    public List<LineEntry> getEntries() {
        return entries;
    }

    public void selectHud(String id, CustomHudDataHandler.CustomHud customHud) {
        removeAllEntries();
        hasSelectedOption = true;
        newName = id;
        scale = customHud.getScale();
        currentSelectedHud = id;
        header = Component.literal(id);
        idEditBox.setValue(id);
        idEditBox.setHint(Component.literal(id));
        scaleEditBox.setValue(String.format(Locale.US, "%f", customHud.getScale()));
        scaleEditBox.setHint(Component.literal(String.format(Locale.US, "%f", customHud.getScale())));
        showBackground = customHud.isShowBackground();
        if(customHud.isShowBackground() != showBackgroundCheckBox.selected()) {
            showBackgroundCheckBox.onPress(null);
        }
        showBars = customHud.isShowBars();
        if(customHud.isShowBars() != showBarsCheckBox.selected()) {
            showBarsCheckBox.onPress(null);
        }
        showElement = customHud.isShowElement();
        if(customHud.isShowElement() != showElementCheckBox.selected()) {
            showElementCheckBox.onPress(null);
        }

        customHud.getStringLines().forEach(line -> this.addEntry(new LineEntry(
                line.value1(),
                line.value2(),
                line.value3(),
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
        return new EditCustomHUDWidget.LineEntry(
                "Example text",
                false,
                false,
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
        scale = 1.0f;
        currentSelectedHud = null;
        showBackground = true;
        if(!showBackgroundCheckBox.selected()) {
            showBackgroundCheckBox.onPress(null);
        }
        showBars = true;
        if(!showBarsCheckBox.selected()) {
            showBarsCheckBox.onPress(null);
        }
        showElement = true;
        if(!showElementCheckBox.selected()) {
            showElementCheckBox.onPress(null);
        }
        scaleEditBox.setValue("1.0");
        scaleEditBox.setHint(Component.literal("1.0"));
        idEditBox.setValue("");
        idEditBox.setHint(Component.literal(""));
        header = Component.literal("No Hud Selected");

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

        // Draw scale text
        guiGraphics.drawString(
                minecraft.font,
                "Scale",
                getX() + width / 3,
                getY() + PADDING + headerHeight + headerHeight / 2 - minecraft.font.lineHeight / 2,
                CommonColors.WHITE,
                true
        );

        idEditBox.render(guiGraphics, mouseX, mouseY, delta);
        scaleEditBox.render(guiGraphics, mouseX, mouseY, delta);
        showBackgroundCheckBox.render(guiGraphics, mouseX, mouseY, delta);
        showBarsCheckBox.render(guiGraphics, mouseX, mouseY, delta);
        showElementCheckBox.render(guiGraphics, mouseX, mouseY, delta);

        guiGraphics.enableScissor(
                getX() + PADDING,
                entryStartY,
                getRight() - PADDING,
                getBottom() - PADDING
        );

        int startY = entryStartY - scrollOffset;
        List<LineEntry> renderedEntries = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            int entryY = startY + i * LineEntry.HEIGHT;
            if (entryY + LineEntry.HEIGHT < entryStartY || entryY > getBottom() - PADDING)
                continue;

            LineEntry entry = entries.get(i);
            entry.setPosition(getX() + PADDING, entryY, width - PADDING - PADDING - scrollbarWidth - PADDING);
            entry.render(guiGraphics, mouseX, mouseY, delta);
            renderedEntries.add(entry);
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

        for (LineEntry entry : renderedEntries) {
            entry.renderSuggestions(guiGraphics, mouseX, mouseY);
        }
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
            showBackgroundCheckBox.setFocused(false);
            showBarsCheckBox.setFocused(false);
            showElementCheckBox.setFocused(false);
            idEditBox.setFocused(true);
            scaleEditBox.setFocused(false);
            return true;
        }

        if (scaleEditBox.mouseClicked(mouseButtonEvent, doubled)) {
            if (focusedEntry != null) {
                focusedEntry.setFocused(false);
                focusedEntry = null;
            }
            showBackgroundCheckBox.setFocused(false);
            showBarsCheckBox.setFocused(false);
            showElementCheckBox.setFocused(false);
            scaleEditBox.setFocused(true);
            idEditBox.setFocused(false);
            return true;
        }

        if(showBackgroundCheckBox.mouseClicked(mouseButtonEvent, doubled)) {
            if (focusedEntry != null) {
                focusedEntry.setFocused(false);
                focusedEntry = null;
            }
            showBackgroundCheckBox.setFocused(true);
            showBarsCheckBox.setFocused(false);
            showElementCheckBox.setFocused(false);
            scaleEditBox.setFocused(false);
            idEditBox.setFocused(false);
            return true;
        }

        if(showBarsCheckBox.mouseClicked(mouseButtonEvent, doubled)) {
            if (focusedEntry != null) {
                focusedEntry.setFocused(false);
                focusedEntry = null;
            }
            showBackgroundCheckBox.setFocused(false);
            showBarsCheckBox.setFocused(true);
            showElementCheckBox.setFocused(false);
            scaleEditBox.setFocused(false);
            idEditBox.setFocused(false);
            return true;
        }

        if(showElementCheckBox.mouseClicked(mouseButtonEvent, doubled)) {
            if (focusedEntry != null) {
                focusedEntry.setFocused(false);
                focusedEntry = null;
            }
            showBackgroundCheckBox.setFocused(false);
            showBarsCheckBox.setFocused(false);
            showElementCheckBox.setFocused(true);
            scaleEditBox.setFocused(false);
            idEditBox.setFocused(false);
            return true;
        }

        for (LineEntry entry : entries) {
            if (entry.mouseClicked(mouseButtonEvent, doubled)) {
                if (focusedEntry != null && focusedEntry != entry) focusedEntry.setFocused(false);
                focusedEntry = entry;
                entry.setFocused(true);
                idEditBox.setFocused(false);
                scaleEditBox.setFocused(false);
                showBackgroundCheckBox.setFocused(false);
                showBarsCheckBox.setFocused(false);
                showElementCheckBox.setFocused(false);
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent keyEvent) {
        if (idEditBox.isFocused()) return idEditBox.keyPressed(keyEvent);
        if (scaleEditBox.isFocused()) return scaleEditBox.keyPressed(keyEvent);
        if (showBackgroundCheckBox.isFocused()) return showBackgroundCheckBox.keyPressed(keyEvent);
        if (showBarsCheckBox.isFocused()) return showBarsCheckBox.keyPressed(keyEvent);
        if (showElementCheckBox.isFocused()) return showElementCheckBox.keyPressed(keyEvent);
        if (focusedEntry != null) return focusedEntry.keyPressed(keyEvent);
        return false;
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent characterEvent) {
        if (idEditBox.isFocused()) return idEditBox.charTyped(characterEvent);
        if (scaleEditBox.isFocused()) return scaleEditBox.charTyped(characterEvent);
        if (showBackgroundCheckBox.isFocused()) return showBackgroundCheckBox.charTyped(characterEvent);
        if (showBarsCheckBox.isFocused()) return showBarsCheckBox.charTyped(characterEvent);
        if (showElementCheckBox.isFocused()) return showElementCheckBox.charTyped(characterEvent);
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

        int entryStartY = getY() + headerHeight + PADDING + (editBoxHeight + PADDING) * 2;

        int visibleHeight = getBottom() - PADDING - entryStartY;
        int totalContentHeight = entries.size() * LineEntry.HEIGHT;
        int maxScroll = Math.max(0, totalContentHeight - visibleHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        return true;
    }

    public static class LineEntry {
        Minecraft minecraftClient = Minecraft.getInstance();

        private final PlaceholderEditBox editBoxWidget;
        private final Checkbox isCentreWidget;
        private final Checkbox isSmallWidget;
        private final Button addButton;
        private final Button deleteButton;

        public String lineString;
        public boolean isCentre;
        public boolean isSmall;
        public int width;

        private static final String isCentreString = "Centered";
        private static final int CENTRE_STRING_SPACING = Minecraft.getInstance().font.width(isCentreString);
        private static final String isSmallString = "Small Text";
        private static final int SMALL_STRING_SPACING = Minecraft.getInstance().font.width(isSmallString);

        public static final int HEIGHT = 24;
        private static final int SPACING = 6;
        private static final int CHECKBOX_SIZE = 20;
        private static final int BUTTON_SIZE = 25;

        public LineEntry(String defaultLine, boolean defaultIsCentre, boolean defaultIsSmall, int width, Callback callback) {
            lineString = defaultLine;
            isCentre = defaultIsCentre;
            isSmall = defaultIsSmall;
            this.width = width;

            editBoxWidget = new PlaceholderEditBox(
                    minecraftClient.font,
                    0, 0,
                    0, 20,
                    Component.empty()
            );
            editBoxWidget.setMaxLength(Integer.MAX_VALUE);

            editBoxWidget.setValue(defaultLine);
            int stringWidth = width - PADDING - PADDING - 6 - PADDING - SPACING - BUTTON_SIZE * 2 - PADDING_QUART - SPACING - SMALL_STRING_SPACING - CHECKBOX_SIZE - SPACING - CHECKBOX_SIZE - CENTRE_STRING_SPACING - 20;
            editBoxWidget.setHint(Component.literal(
                    minecraftClient.font.width(defaultLine) > stringWidth
                    ? minecraftClient.font.plainSubstrByWidth(defaultLine, stringWidth) + "..."
                    : defaultLine
            ));

            editBoxWidget.setResponder(s -> {
                lineString = s;
                // editBoxWidget.setHint(Component.literal(s));
            });

            isCentreWidget = Checkbox.builder(Component.literal(isCentreString), minecraftClient.font)
                    .selected(defaultIsCentre)
                    .onValueChange((checkbox, checked) -> isCentre = checked)
                    .build();


            isSmallWidget = Checkbox.builder(Component.literal(isSmallString), minecraftClient.font)
                    .selected(defaultIsSmall)
                    .onValueChange((checkbox, checked) -> isSmall = checked)
                    .build();

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

            int stringWidth = fullWidth - SPACING - BUTTON_SIZE * 2 - PADDING_QUART - SPACING - SMALL_STRING_SPACING - CHECKBOX_SIZE - SPACING - CHECKBOX_SIZE - CENTRE_STRING_SPACING - SPACING;

            editBoxWidget.setPosition(x, y);
            editBoxWidget.setWidth(stringWidth);

            isCentreWidget.setPosition(
                    x + fullWidth - SPACING - BUTTON_SIZE * 2 - PADDING_QUART - SPACING - SMALL_STRING_SPACING - CHECKBOX_SIZE - SPACING - CHECKBOX_SIZE - CENTRE_STRING_SPACING,
                    y
            );

            isSmallWidget.setPosition(
                    x + fullWidth - SPACING - BUTTON_SIZE * 2 - PADDING_QUART - SPACING - SMALL_STRING_SPACING - CHECKBOX_SIZE,
                    y
            );



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
            isCentreWidget.render(guiGraphics, mouseX, mouseY, delta);
            isSmallWidget.render(guiGraphics, mouseX, mouseY, delta);
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

        public void renderSuggestions(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            editBoxWidget.renderSuggestions(guiGraphics, mouseX, mouseY);
        }

        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubled) {
            if (editBoxWidget.mouseClicked(mouseButtonEvent, doubled)) return true;
            if (isCentreWidget.mouseClicked(mouseButtonEvent, doubled)) return false;
            if (isSmallWidget.mouseClicked(mouseButtonEvent, doubled)) return false;
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
