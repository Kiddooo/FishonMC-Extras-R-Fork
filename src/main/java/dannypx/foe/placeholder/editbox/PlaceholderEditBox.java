package dannypx.foe.placeholder.editbox;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class PlaceholderEditBox extends EditBox {
    private static final int MAX_VISIBLE_ROWS = 8;
    private static final int ROW_HEIGHT = 12;

    private static final int DEFAULT_TEXT_COLOR = 0xE0E0E0;

    private final Font font;
    private PlaceholderSuggestionContext context = PlaceholderSuggestionContext.NONE;
    private int highlighted = 0;

    public PlaceholderEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.font = font;
        this.addFormatter(this::formatLine);
    }

    public boolean hasActiveSuggestions() {
        return this.isSuggesting();
    }

    private FormattedCharSequence formatLine(String text, int firstCharacterIndex) {
        String full = this.getValue();
        List<PlaceholderSyntaxHighlighter.Span> spans = PlaceholderSyntaxHighlighter.highlight(full);
        int windowEnd = firstCharacterIndex + text.length();

        MutableComponent result = Component.empty();
        int cursor = firstCharacterIndex;
        for (PlaceholderSyntaxHighlighter.Span span : spans) {
            int start = Math.max(span.start(), firstCharacterIndex);
            int end = Math.min(span.end(), windowEnd);
            if(end <= start) continue;
            if(start > cursor) result.append(plain(full.substring(cursor, start), DEFAULT_TEXT_COLOR));
            result.append(plain(full.substring(start, end), span.color() & 0xFFFFFF, span.error()));
            cursor = end;
        }
        if(cursor < windowEnd) result.append(plain(full.substring(cursor, windowEnd), DEFAULT_TEXT_COLOR));
        return Language.getInstance().getVisualOrder(result);
    }

    private static MutableComponent plain(String text, int rgb) {
        return plain(text, rgb, false);
    }

    private static MutableComponent plain(String text, int rgb, boolean error) {
        return error
                ? Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)).applyFormats(ChatFormatting.UNDERLINE))
                : Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent characterEvent) {
        boolean handled = super.charTyped(characterEvent);
        this.refreshSuggestions();
        return handled;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if(this.isSuggesting()) {
            if(keyEvent.key() == GLFW.GLFW_KEY_TAB) {
                this.acceptHighlighted();
                return true;
            }
            if(keyEvent.key() == GLFW.GLFW_KEY_DOWN) {
                highlighted = Math.min(highlighted + 1, context.suggestions().size() - 1);
                return true;
            }
            if(keyEvent.key() == GLFW.GLFW_KEY_UP) {
                highlighted = Math.max(highlighted - 1, 0);
                return true;
            }
            if(keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) {
                context = PlaceholderSuggestionContext.NONE;
                return true;
            }
        }
        boolean handled = super.keyPressed(keyEvent);
        this.refreshSuggestions();
        return handled;
    }

    @Override
    public void onClick(MouseButtonEvent mouseButtonEvent, boolean bl) {
        super.onClick(mouseButtonEvent, bl);
        this.refreshSuggestions();
    }

    private void refreshSuggestions() {
        context = PlaceholderSuggestionEngine.computeContext(this.getValue(), this.getCursorPosition());
        highlighted = 0;
    }

    private boolean isSuggesting() {
        return context.kind() != PlaceholderSuggestionContext.Kind.NONE && !context.suggestions().isEmpty();
    }

    private void acceptHighlighted() {
        if (!isSuggesting()) return;
        PlaceholderSuggestionEngine.Suggestion picked = context.suggestions().get(highlighted);
        String value = this.getValue();
        String newValue = value.substring(0, context.replaceStart())
                + picked.name()
                + value.substring(context.replaceEnd());
        this.setValue(newValue);
        this.moveCursorTo(context.replaceStart() + picked.name().length(), false);
        this.refreshSuggestions();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    public void renderSuggestions(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if(!isFocused() || !isSuggesting()) return;

        guiGraphics.nextStratum();

        List<PlaceholderSuggestionEngine.Suggestion> items = context.suggestions();
        int visible = Math.min(items.size(), MAX_VISIBLE_ROWS);
        int x = this.getX();
        int y = this.getY() + this.getHeight();
        int width = this.getWidth();
        int panelHeight = visible * ROW_HEIGHT;

        guiGraphics.fill(x, y, x + width, y + panelHeight, 0xF0101010);
        for (int i = 0; i < visible; i++) {
            PlaceholderSuggestionEngine.Suggestion item = items.get(i);
            int rowTop = y + i * ROW_HEIGHT;
            if(i == highlighted) guiGraphics.fill(x, rowTop, x + width, rowTop + ROW_HEIGHT, 0x803366CC);
            int textColor = (i == highlighted) ? 0xFFFFFF55 : 0xFFCCCCCC;
            String label = item.name() + (item.isFunction() ? "(...)" : "");
            guiGraphics.drawString(font, label, x + 3, rowTop + 2, textColor, false);
        }
    }
}
