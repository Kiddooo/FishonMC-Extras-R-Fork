package dannypx.foe.screens.widget;

import dannypx.foe.mixin.accessor.AbstractSelectionListAccessor;
import dannypx.foe.screens.interfaces.ScreenConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.NotNull;

public class ButtonListWidget extends AbstractSelectionList<ButtonListWidget.@NotNull ButtonEntry> implements ScreenConstants {

    private final String headerTitle;

    public ButtonListWidget(Minecraft minecraft,
                            int width,
                            int height,
                            int top,
                            int bottom,
                            int itemHeight,
                            String title) {
        super(minecraft, width, height, bottom, itemHeight);
        headerTitle = title;
    }

    @Override
    protected int scrollBarX() {
        return width / 2 + (BUTTON_WIDTH + PADDING * 2) / 2;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {

    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float deltaTicks) {
        super.renderWidget(guiGraphics, mouseX, mouseY, deltaTicks);

        guiGraphics.drawCenteredString(
                minecraft.font,
                Component.literal(headerTitle),
                getX() + getRowWidth() / 2,
                8,
                CommonColors.WHITE
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public int addEntry(ButtonEntry entry) {
        entry.setX(this.getRowLeft());
        entry.setWidth(this.getRowWidth());
        entry.setY(this.getNextY());
        entry.setHeight(this.defaultEntryHeight);

        ((AbstractSelectionListAccessor) this).getChildren().add(entry);
        ((AbstractSelectionListAccessor) this).callRepositionEntries();
        return this.children().size() - 1;
    }

    @SuppressWarnings("unchecked")
    public int addEntryAtPos(ButtonEntry entry, int pos) {
        entry.setX(this.getRowLeft());
        entry.setWidth(this.getRowWidth());
        entry.setY(this.getNextY());
        entry.setHeight(this.defaultEntryHeight);

        ((AbstractSelectionListAccessor) this).getChildren().add(pos, entry);
        ((AbstractSelectionListAccessor) this).callRepositionEntries();
        return this.children().size() - 1;
    }

    public void swapUp(int pos) {
        Collections.swap(((AbstractSelectionListAccessor) this).getChildren(), pos, pos - 1);
        ((AbstractSelectionListAccessor) this).callRepositionEntries();
    }

    public void swapDown(int pos) {
        Collections.swap(((AbstractSelectionListAccessor) this).getChildren(), pos, pos + 1);
        ((AbstractSelectionListAccessor) this).callRepositionEntries();
    }

    public int entryAt(ButtonEntry entry) {
        return ((AbstractSelectionListAccessor) this).getChildren().indexOf(entry);
    }

    @Override
    public void removeEntry(ButtonEntry entry) {
        super.removeEntry(entry);
    }

    public static class ButtonEntry extends ContainerObjectSelectionList.Entry<@NotNull ButtonEntry> {

        private final Button button;
        private final Button smallButton;
        private final Button upButton;
        private final Button downButton;

        public ButtonEntry(Button button) {
            this.button = button;
            this.smallButton = null;
            this.upButton = null;
            this.downButton = null;
        }

        public ButtonEntry(Button button, Button smallButton, Button upButton, Button downButton) {
            this.button = button;
            this.smallButton = smallButton;
            this.upButton = upButton;
            this.downButton = downButton;
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            List<Button> children = new ArrayList<>(List.of(button));

            if(smallButton != null) {
                children.add(smallButton);
            }

            if(upButton != null) children.add(upButton);
            if(downButton != null) children.add(downButton);

            return children;
        }

        @Override
        public @NotNull List<? extends NarratableEntry> narratables() {
            List<Button> children = new ArrayList<>(List.of(button));

            if(smallButton != null) {
                children.add(smallButton);
            }

            if(upButton != null) children.add(upButton);
            if(downButton != null) children.add(downButton);

            return children;
        }

        @Override
        public void renderContent(
                @NotNull GuiGraphics guiGraphics,
                int mouseX,
                int mouseY,
                boolean hovered,
                float delta
        ) {
            button.setPosition(
                    getX() + (getContentWidth() - button.getWidth()) / 2,
                    getY()
            );
            button.render(guiGraphics, mouseX, mouseY, delta);

            if(smallButton != null) {
                smallButton.setPosition(
                        getX() + getContentWidth() - smallButton.getWidth() - CONTENT_PADDING,
                        getY()
                );

                smallButton.render(guiGraphics, mouseX, mouseY, delta);
            }

            if(upButton != null) {
                upButton.setPosition(
                        getX() + CONTENT_PADDING,
                        getY()
                );

                upButton.render(guiGraphics, mouseX, mouseY, delta);
            }

            if(downButton != null) {
                downButton.setPosition(
                        getX() + CONTENT_PADDING,
                        getY() + getContentHeight() / 2 + 2
                );

                downButton.render(guiGraphics, mouseX, mouseY, delta);
            }
        }
    }
}
