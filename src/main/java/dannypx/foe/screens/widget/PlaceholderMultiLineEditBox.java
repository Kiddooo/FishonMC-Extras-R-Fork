package dannypx.foe.screens.widget;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dannypx.foe.helper.TextHelper;
import dannypx.foe.placeholder.editbox.*;
import dannypx.foe.placeholder.evaluator.PlaceholderResult;
import dannypx.foe.placeholder.handler.PlaceholderHandlerV2;
import dannypx.foe.screens.interfaces.ScreenConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class PlaceholderMultiLineEditBox extends AbstractWidget implements ScreenConstants {
    private static final int MAX_VISIBLE_ROWS = PADDING;
    private static final int ROW_HEIGHT = 12;
    private static final int PANEL_WIDTH = 160;

    private static final int DEFAULT_TEXT_COLOR = 0xFFE0E0E0;
    private static final int BACKGROUND_COLOR = 0x55000000;
    private static final int BORDER_COLOR = 0xFF5A5A5A;
    private static final int CURSOR_COLOR = 0xFFD0D0D0;
    private static final int SELECTION_COLOR = 0x8055AAFF;
    private static final int PAD = PADDING_HALF;
    private static final long CURSOR_BLINK_INTERVAL_MS = 500;

    private static final int GUTTER_LEFT_PAD = PADDING_HALF;
    private static final int GUTTER_RIGHT_PAD = PADDING_HALF;
    private static final int GUTTER_NUMBER_COLOR = 0xFF6A6A6A;
    private static final int GUTTER_CURRENT_LINE_COLOR = 0xFFCFCFCF;
    private static final int GUTTER_DIVIDER_COLOR = 0xFF3A3A3A;
    private static final int INDENT_GUIDE_COLOR = 0x883A3A3A;

    private static final int RESULT_AWAITING_COLOR = 0xFF808080;
    private static final long RESOLVE_DEBOUNCE_MS = 1000;

    private static final int MAX_HISTORY = 100;
    private static final long HISTORY_COALESCE_MS = 250;

    private static final long MULTI_CLICK_MS = 400;

    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_COLOR = 0xFF6A6A6A;
    private static final int SCROLLBAR_ACTIVE_COLOR = 0xFF9A9A9A;
    private static final int SCROLLBAR_GRAB_SLOP = 3;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 10;

    private static final String INDENT = "  ";

    private static final int BRACKET_MATCH_COLOR = 0x55FFFFFF;
    private static final int BRACKET_ERROR_BOX_COLOR = 0x66FF5555;
    private static final int BRACKET_ERROR_COLOR = 0xFFFF5555;

    private static final int BUTTON_SIZE = 11;
    private static final int BUTTON_MARGIN = 3;
    private static final int BUTTON_COLOR = 0x88303030;
    private static final int BUTTON_HOVER_COLOR = 0xCC505050;
    private static final int BUTTON_ICON_COLOR = 0xFFB0B0B0;
    private static final int BUTTON_ICON_HOVER_COLOR = 0xFFFFFFFF;

    private static final List<Component> SHORTCUTS_TOOLTIP = List.of(
            Component.literal("Keyboard shortcuts").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
            shortcut("Ctrl+A", "Select all"),
            shortcut("Ctrl+C / Ctrl+X / Ctrl+V", "Copy / Cut / Paste"),
            shortcut("Ctrl+Z / Ctrl+Y", "Undo / Redo"),
            shortcut("Ctrl+S", "Save"),
            shortcut("Tab", "Insert 2 spaces / indent selected lines"),
            shortcut("Shift+Tab", "Un-indent line / selected lines"),
            shortcut("Ctrl+Backspace / Ctrl+Delete", "Delete word"),
            shortcut("Ctrl+Left / Ctrl+Right", "Jump by word"),
            shortcut("Home / End", "Start / end of line"),
            shortcut("Page Up / Page Down", "Move one page"),
            shortcut("Shift + any movement", "Extend selection"),
            Component.empty(),
            Component.literal("While suggestions are shown").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
            shortcut("Tab / Click", "Accept suggestion"),
            shortcut("Up / Down", "Choose suggestion"),
            shortcut("Mouse wheel", "Scroll suggestions"),
            shortcut("Esc", "Close suggestions")
    );

    private static Component shortcut(String keys, String action) {
        return Component.literal(keys).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - " + action).withStyle(ChatFormatting.GRAY));
    }

    private final Font font;
    private final int lineHeight;
    private final int resultBoxHeight;

    private String value = "";
    private int cursor = 0;
    private int cursorLine = 0;
    private int selectionAnchor = 0;

    private int scrollLines = 0;
    private boolean draggingScrollbar = false;
    private int scrollbarGrabOffset = 0;

    private boolean pressedButton = false;

    private long lastEditTimeMs = System.currentTimeMillis();
    private long lastHistoryPushMs = 0;
    private boolean pendingResolve = true;
    private PlaceholderResult resolvedResult = null;

    private long lastClickMs = 0;
    private int clickCount = 0;

    private record HistoryEntry(String value, int cursor) {}

    private final Deque<HistoryEntry> undoStack = new ArrayDeque<>();
    private final Deque<HistoryEntry> redoStack = new ArrayDeque<>();

    private record VisualLine(int start, int end, int lineNumber, boolean continuation) {}

    private record SuggestionPanel(int x, int y, int rows) {}

    private List<VisualLine> visualLines = List.of(new VisualLine(0, 0, 1, false));
    private int gutterNumberWidth = 0;
    private int gutterWidth = 0;

    private PlaceholderSuggestionContext context = PlaceholderSuggestionContext.NONE;
    private int highlighted = 0;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private boolean mouseMoved = false;
    private int suggestionScroll = 0;
    private String structureFor = null;
    private PlaceholderStructure structure = null;

    public PlaceholderMultiLineEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        this.font = font;
        this.lineHeight = font.lineHeight + 2;
        this.resultBoxHeight = PADDING + lineHeight + PADDING + 1;
        this.relayout();
    }

    public String getValue() {
        return value;
    }
    public String getResolvedValue() {
        return stripNewlines(value);
    }

    public void setValue(String newValue) {
        this.value = normalizeNewlines(newValue);
        this.cursor = 0;
        this.selectionAnchor = 0;
        this.scrollLines = 0;
        this.resolvedResult = null;
        this.clearHistory();
        this.relayout();
        this.scrollToCursor();
        this.refreshSuggestions();
    }

    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    private SuggestionPanel suggestionPanel() {
        if (!isFocused() || !this.isSuggesting()) return null;

        int row = cursorLine - scrollLines;
        if (row < 0 || row >= this.visibleRows()) return null;

        VisualLine vl = visualLines.get(cursorLine);
        int caretX = getX() + gutterWidth + font.width(value.substring(vl.start(), cursor));
        int caretY = getY() + PAD + row * lineHeight;

        int rows = Math.min(context.suggestions().size(), MAX_VISIBLE_ROWS);
        int panelHeight = rows * ROW_HEIGHT;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int panelX = Math.min(caretX, screenWidth - PANEL_WIDTH);
        int panelY = caretY + lineHeight;
        if (panelY + panelHeight > screenHeight) {
            panelY = caretY - panelHeight;
        }

        return new SuggestionPanel(panelX, panelY, rows);
    }

    public boolean hasActiveSuggestions() {
        return this.isSuggesting();
    }

    private static String stripNewlines(String text) {
        return text.indexOf('\n') < 0 ? text : text.replace("\n", "");
    }

    private static String normalizeNewlines(String text) {
        if (text.indexOf('\r') < 0) return text;
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }

    private int rawToStrippedOffset(int rawOffset) {
        int count = 0;
        for (int i = 0; i < rawOffset && i < value.length(); i++) {
            if (value.charAt(i) != '\n') count++;
        }
        return count;
    }

    private int strippedToRawOffset(int strippedOffset) {
        int count = 0;
        int len = value.length();
        for (int i = 0; i < len; i++) {
            if (value.charAt(i) != '\n') {
                if (count == strippedOffset) return i;
                count++;
            }
        }
        return len;
    }

    private int[] strippedToRawMap() {
        int strippedLen = 0;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) != '\n') strippedLen++;
        int[] map = new int[strippedLen + 1];
        int k = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '\n') map[k++] = i;
        }
        map[strippedLen] = value.length();
        return map;
    }

    private void relayout() {
        int paragraphCount = 1;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) == '\n') paragraphCount++;

        gutterNumberWidth = font.width(String.valueOf(paragraphCount));
        gutterWidth = GUTTER_LEFT_PAD + gutterNumberWidth + GUTTER_LEFT_PAD + 1 + GUTTER_RIGHT_PAD;

        List<VisualLine> result = new ArrayList<>();
        int maxWidth = Math.max(10, this.width - gutterWidth - PADDING - SCROLLBAR_WIDTH);
        int paragraphStart = 0;
        int lineNumber = 1;
        for (int i = 0; i <= value.length(); i++) {
            if (i == value.length() || value.charAt(i) == '\n') {
                this.wrapParagraph(paragraphStart, i, maxWidth, lineNumber, result);
                paragraphStart = i + 1;
                lineNumber++;
            }
        }
        if (result.isEmpty()) result.add(new VisualLine(0, 0, 1, false));
        this.visualLines = result;
        this.scrollLines = Math.min(this.scrollLines, this.maxScroll());
        this.cursorLine = defaultLineIndexForOffset(cursor);
        this.lastEditTimeMs = System.currentTimeMillis();
        this.pendingResolve = true;
    }

    private void wrapParagraph(int start, int end, int maxWidth, int lineNumber, List<VisualLine> out) {
        if (start == end) {
            out.add(new VisualLine(start, end, lineNumber, false));
            return;
        }
        int lineStart = start;
        float lineWidth = 0f;
        int lastBreakable = -1;
        boolean first = true;

        for (int i = start; i < end; i++) {
            char c = value.charAt(i);
            float charWidth = font.width(String.valueOf(c));

            if (lineWidth + charWidth > maxWidth && i > lineStart) {
                int breakAt = (lastBreakable >= lineStart) ? lastBreakable + 1 : i;
                out.add(new VisualLine(lineStart, breakAt, lineNumber, !first));
                first = false;
                lineStart = breakAt;
                lastBreakable = -1;
                lineWidth = font.width(value.substring(lineStart, i));
                i--;
                continue;
            }

            lineWidth += charWidth;
            if (c == ' ') lastBreakable = i;
        }
        out.add(new VisualLine(lineStart, end, lineNumber, !first));
    }

    private int defaultLineIndexForOffset(int offset) {
        int idx = 0;
        for (int i = 0; i < visualLines.size(); i++) {
            if (visualLines.get(i).start() <= offset) idx = i;
            else break;
        }
        return idx;
    }

    private int visibleRows() {
        int editorHeight = this.height - resultBoxHeight;
        return Math.max(1, (editorHeight - PADDING * 2) / lineHeight);
    }

    private void scrollToCursor() {
        int visible = this.visibleRows();
        if (cursorLine < scrollLines) scrollLines = cursorLine;
        else if (cursorLine >= scrollLines + visible) scrollLines = cursorLine - visible + 1;
        scrollLines = Math.max(0, scrollLines);
    }

    private int maxScroll() {
        return Math.max(0, visualLines.size() - this.visibleRows());
    }

    private boolean isScrollbarVisible() {
        return visualLines.size() > this.visibleRows();
    }

    private int scrollTrackTop() {
        return getY() + 1;
    }

    private int scrollTrackHeight() {
        return (getBottom() - resultBoxHeight - 1) - this.scrollTrackTop();
    }

    private int scrollThumbHeight() {
        int track = this.scrollTrackHeight();
        int proportional = track * this.visibleRows() / visualLines.size();
        return Math.clamp(proportional, Math.min(SCROLLBAR_MIN_THUMB_HEIGHT, track), track);
    }

    private int scrollThumbY() {
        int max = this.maxScroll();
        if (max == 0) return this.scrollTrackTop();
        int travel = this.scrollTrackHeight() - this.scrollThumbHeight();
        return this.scrollTrackTop() + travel * Math.min(scrollLines, max) / max;
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        if (!this.isScrollbarVisible()) return false;
        int left = getRight() - 1 - SCROLLBAR_WIDTH - SCROLLBAR_GRAB_SLOP;
        int top = this.scrollTrackTop();
        return mouseX >= left && mouseX < getRight() - 1
                && mouseY >= top && mouseY < top + this.scrollTrackHeight();
    }

    private boolean isOverThumb(double mouseY) {
        int thumbY = this.scrollThumbY();
        return mouseY >= thumbY && mouseY < thumbY + this.scrollThumbHeight();
    }

    private void scrollToThumbPosition(double mouseY) {
        int max = this.maxScroll();
        int travel = this.scrollTrackHeight() - this.scrollThumbHeight();
        if (max == 0 || travel <= 0) return;
        double thumbTop = mouseY - scrollbarGrabOffset - this.scrollTrackTop();
        scrollLines = (int) Math.round(Math.clamp(thumbTop / travel, 0.0, 1.0) * max);
    }

    private int formatRight() {
        return getRight() - 1 - SCROLLBAR_WIDTH - SCROLLBAR_GRAB_SLOP - 1;
    }

    private int formatLeft() {
        return this.formatRight() - BUTTON_SIZE;
    }

    private int formatBottom() {
        return getBottom() - resultBoxHeight - 1 - BUTTON_MARGIN;
    }

    private int formatTop() {
        return this.formatBottom() - BUTTON_SIZE;
    }

    private boolean isOverFormatButton(double mouseX, double mouseY) {
        return mouseX >= this.formatLeft() && mouseX < this.formatRight()
                && mouseY >= this.formatTop() && mouseY < this.formatBottom();
    }

    private void renderFormatButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int left = this.formatLeft();
        int top = this.formatTop();
        boolean hovered = this.isOverFormatButton(mouseX, mouseY) && !this.isOverSuggestionPanel(mouseX, mouseY);

        guiGraphics.fill(left, top, left + BUTTON_SIZE, top + BUTTON_SIZE,
                hovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR);

        int icon = hovered ? BUTTON_ICON_HOVER_COLOR : BUTTON_ICON_COLOR;
        guiGraphics.fill(left + 2, top + 2, left + 9, top + 3, icon);
        guiGraphics.fill(left + 4, top + 4, left + 9, top + 5, icon);
        guiGraphics.fill(left + 4, top + 6, left + 9, top + 7, icon);
        guiGraphics.fill(left + 2, top + 8, left + 9, top + 9, icon);

        if (hovered) {
            guiGraphics.setTooltipForNextFrame(font, Component.literal("Click to format"), mouseX, mouseY);
        }
    }

    private void format() {
        this.applyLayout(PlaceholderFormatter.format(value));
    }

    private void minify() {
        this.applyLayout(PlaceholderFormatter.minify(value));
    }

    private void applyLayout(String formatted) {
        if (formatted.equals(value)) return;

        int newCursor = mapCursorAfterFormat(value, cursor, formatted);

        lastHistoryPushMs = 0;
        this.pushHistory();
        lastHistoryPushMs = 0;

        value = formatted;
        cursor = newCursor;
        selectionAnchor = cursor;
        this.relayout();
        this.scrollToCursor();
        this.refreshSuggestions();
    }

    private static int mapCursorAfterFormat(String oldText, int oldCursor, String newText) {
        if (oldCursor >= oldText.length()) return newText.length();

        int significant = 0;
        for (int i = 0; i < oldCursor; i++) {
            if (!Character.isWhitespace(oldText.charAt(i))) significant++;
        }
        if (significant == 0) return 0;

        int seen = 0;
        for (int i = 0; i < newText.length(); i++) {
            if (!Character.isWhitespace(newText.charAt(i)) && ++seen == significant) return i + 1;
        }
        return newText.length();
    }

    private int minifyRight() {
        return this.formatLeft() - 2;
    }

    private int minifyLeft() {
        return this.minifyRight() - BUTTON_SIZE;
    }

    private boolean isOverMinifyButton(double mouseX, double mouseY) {
        return mouseX >= this.minifyLeft() && mouseX < this.minifyRight()
                && mouseY >= this.formatTop() && mouseY < this.formatBottom();
    }

    private void renderMinifyButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int left = this.minifyLeft();
        int top = this.formatTop();
        boolean hovered = this.isOverMinifyButton(mouseX, mouseY) && !this.isOverSuggestionPanel(mouseX, mouseY);

        guiGraphics.fill(left, top, left + BUTTON_SIZE, top + BUTTON_SIZE,
                hovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR);

        int icon = hovered ? BUTTON_ICON_HOVER_COLOR : BUTTON_ICON_COLOR;
        guiGraphics.fill(left + 2, top + 3, left + 9, top + 4, icon);
        guiGraphics.fill(left + 2, top + 5, left + 9, top + 6, icon);
        guiGraphics.fill(left + 2, top + 7, left + 9, top + 8, icon);

        if (hovered) {
            guiGraphics.setTooltipForNextFrame(font, Component.literal("Minify"), mouseX, mouseY);
        }
    }

    private int helpTop() {
        return getY() + 1 + BUTTON_MARGIN;
    }

    private boolean isOverHelpButton(double mouseX, double mouseY) {
        return mouseX >= this.formatLeft() && mouseX < this.formatRight()
                && mouseY >= this.helpTop() && mouseY < this.helpTop() + BUTTON_SIZE;
    }

    private void renderHelpButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int left = this.formatLeft();
        int top = this.helpTop();
        boolean hovered = this.isOverHelpButton(mouseX, mouseY) && !this.isOverSuggestionPanel(mouseX, mouseY);

        guiGraphics.fill(left, top, left + BUTTON_SIZE, top + BUTTON_SIZE,
                hovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR);

        String mark = "?";
        guiGraphics.drawString(font, mark,
                left + (BUTTON_SIZE - font.width(mark)) / 2 + 1,
                top + (BUTTON_SIZE - font.lineHeight) / 2 + 1,
                hovered ? BUTTON_ICON_HOVER_COLOR : BUTTON_ICON_COLOR, false);

        if (hovered) {
            guiGraphics.setTooltipForNextFrame(font, SHORTCUTS_TOOLTIP, Optional.empty(), mouseX, mouseY);
        }
    }

    private PlaceholderResult resolvePlaceholder(String resolved) {
        return PlaceholderHandlerV2.instance().resolve(resolved, false);
    }

    private boolean hasSelection() {
        return selectionAnchor != cursor;
    }

    private int selStart() {
        return Math.min(selectionAnchor, cursor);
    }

    private int selEnd() {
        return Math.max(selectionAnchor, cursor);
    }

    private void deleteSelection() {
        int s = this.selStart(), e = this.selEnd();
        value = value.substring(0, s) + value.substring(e);
        cursor = s;
        selectionAnchor = cursor;
        this.relayout();
    }

    private void pushHistory() {
        long now = System.currentTimeMillis();
        boolean coalesce = !undoStack.isEmpty() && (now - lastHistoryPushMs) < HISTORY_COALESCE_MS;
        lastHistoryPushMs = now;
        redoStack.clear();
        if (coalesce) return;
        undoStack.addLast(new HistoryEntry(value, cursor));
        while (undoStack.size() > MAX_HISTORY) undoStack.removeFirst();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        HistoryEntry entry = undoStack.removeLast();
        redoStack.addLast(new HistoryEntry(value, cursor));
        while (redoStack.size() > MAX_HISTORY) redoStack.removeFirst();
        value = entry.value();
        cursor = Math.min(entry.cursor(), value.length());
        selectionAnchor = cursor;
        this.relayout();
        this.scrollToCursor();
        this.refreshSuggestions();
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        HistoryEntry entry = redoStack.removeLast();
        undoStack.addLast(new HistoryEntry(value, cursor));
        while (undoStack.size() > MAX_HISTORY) undoStack.removeFirst();
        value = entry.value();
        cursor = Math.min(entry.cursor(), value.length());
        selectionAnchor = cursor;
        this.relayout();
        this.scrollToCursor();
        this.refreshSuggestions();
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private int prevWordBoundary(int offset) {
        int i = offset;
        while (i > 0 && Character.isWhitespace(value.charAt(i - 1))) i--;
        if (i > 0 && isWordChar(value.charAt(i - 1))) {
            while (i > 0 && isWordChar(value.charAt(i - 1))) i--;
        } else if (i == offset && i > 0) {
            i--;
        }
        return i;
    }

    private int nextWordBoundary(int offset) {
        int i = offset;
        int len = value.length();
        while (i < len && Character.isWhitespace(value.charAt(i))) i++;
        if (i < len && isWordChar(value.charAt(i))) {
            while (i < len && isWordChar(value.charAt(i))) i++;
        } else if (i == offset && i < len) {
            i++;
        }
        return i;
    }

    private void deleteWordBack() {
        int target = prevWordBoundary(cursor);
        if (target == cursor) return;
        this.pushHistory();
        value = value.substring(0, target) + value.substring(cursor);
        cursor = target;
        selectionAnchor = cursor;
        this.relayout();
        this.scrollToCursor();
    }

    private void deleteWordForward() {
        int target = nextWordBoundary(cursor);
        if (target == cursor) return;
        this.pushHistory();
        value = value.substring(0, cursor) + value.substring(target);
        selectionAnchor = cursor;
        this.relayout();
        this.scrollToCursor();
    }

    private void insert(String text) {
        if (text.isEmpty()) return;
        text = normalizeNewlines(text);
        this.pushHistory();
        if (this.hasSelection()) this.deleteSelection();
        value = value.substring(0, cursor) + text + value.substring(cursor);
        cursor += text.length();
        selectionAnchor = cursor;
        this.relayout();
        this.scrollToCursor();
    }

    private List<Integer> selectedLineStarts() {
        int from = this.selStart();
        int to = this.selEnd();
        int lastPos = (to > from && value.charAt(to - 1) == '\n') ? to - 1 : to;

        List<Integer> starts = new ArrayList<>();
        int lineStart = value.lastIndexOf('\n', from - 1) + 1;
        while (true) {
            starts.add(lineStart);
            int lineEnd = value.indexOf('\n', lineStart);
            if (lineEnd < 0 || lineEnd + 1 > lastPos) break;
            lineStart = lineEnd + 1;
        }
        return starts;
    }

    private void indent() {
        List<Integer> insertions = new ArrayList<>();
        for (int lineStart : this.selectedLineStarts()) {
            boolean empty = lineStart >= value.length() || value.charAt(lineStart) == '\n';
            if (!empty) insertions.add(lineStart);
        }
        if (insertions.isEmpty()) return;

        this.pushHistory();

        StringBuilder sb = new StringBuilder(value.length() + insertions.size() * INDENT.length());
        int copied = 0;
        for (int lineStart : insertions) {
            sb.append(value, copied, lineStart).append(INDENT);
            copied = lineStart;
        }
        sb.append(value, copied, value.length());

        int newCursor = cursor + INDENT.length() * insertedBefore(cursor, insertions);
        int newAnchor = selectionAnchor + INDENT.length() * insertedBefore(selectionAnchor, insertions);
        value = sb.toString();
        cursor = newCursor;
        selectionAnchor = newAnchor;
        this.relayout();
    }

    private void unindent() {
        int from = this.selStart();
        int to = this.selEnd();
        int lastPos = (to > from && value.charAt(to - 1) == '\n') ? to - 1 : to;

        List<int[]> removals = new ArrayList<>();
        int lineStart = value.lastIndexOf('\n', from - 1) + 1;
        while (true) {
            int count = 0;
            while (count < INDENT.length() && lineStart + count < value.length() && value.charAt(lineStart + count) == ' ') count++;
            if (count == 0 && lineStart < value.length() && value.charAt(lineStart) == '\t') count = 1;
            if (count > 0) removals.add(new int[]{lineStart, count});

            int lineEnd = value.indexOf('\n', lineStart);
            if (lineEnd < 0 || lineEnd + 1 > lastPos) break;
            lineStart = lineEnd + 1;
        }
        if (removals.isEmpty()) return;

        this.pushHistory();

        StringBuilder sb = new StringBuilder(value.length());
        int copied = 0;
        for (int[] removal : removals) {
            sb.append(value, copied, removal[0]);
            copied = removal[0] + removal[1];
        }
        sb.append(value, copied, value.length());

        int newCursor = cursor - removedBefore(cursor, removals);
        int newAnchor = selectionAnchor - removedBefore(selectionAnchor, removals);
        value = sb.toString();
        cursor = newCursor;
        selectionAnchor = newAnchor;
        this.relayout();
    }

    private static int insertedBefore(int position, List<Integer> insertions) {
        int inserted = 0;
        for (int lineStart : insertions) {
            if (lineStart < position) inserted++;
        }
        return inserted;
    }

    private static int removedBefore(int position, List<int[]> removals) {
        int removed = 0;
        for (int[] removal : removals) removed += Math.clamp(position - removal[0], 0, removal[1]);
        return removed;
    }

    private void smartEnter() {
        String indent = this.leadingWhitespaceOfCurrentLine();

        if (!this.hasSelection() && cursor > 0) {
            PlaceholderStructure.Context ctx = PlaceholderStructure.contextAt(this.getResolvedValue(), this.rawToStrippedOffset(cursor));
            char before = value.charAt(cursor - 1);
            char after = cursor < value.length() ? value.charAt(cursor) : '\0';

            boolean afterParen = before == '(' && ctx.isExpression();
            boolean afterBlockStart = before == '%' && ctx.frame() == PlaceholderStructure.FrameKind.BLOCK;
            if (!ctx.inString() && (afterParen || afterBlockStart)) {
                String inner = "\n" + indent + INDENT;
                boolean closerFollows = afterParen ? after == ')' : after == '%';
                if (closerFollows) {
                    String closing = "\n" + indent;
                    this.insert(inner + closing);
                    cursor -= closing.length();
                    selectionAnchor = cursor;
                    cursorLine = this.defaultLineIndexForOffset(cursor);
                } else {
                    this.insert(inner);
                }
                return;
            }
        }

        this.insert("\n" + indent);
    }

    private String leadingWhitespaceOfCurrentLine() {
        VisualLine vl = visualLines.get(cursorLine);
        int i = vl.start();
        int end = Math.min(vl.end(), value.length());
        StringBuilder sb = new StringBuilder();
        while (i < end && (value.charAt(i) == ' ' || value.charAt(i) == '\t')) {
            sb.append(value.charAt(i));
            i++;
        }
        return sb.toString();
    }

    private void selectWordAt(int offset) {
        int start = offset, end = offset;
        while (start > 0 && isWordChar(value.charAt(start - 1))) start--;
        while (end < value.length() && isWordChar(value.charAt(end))) end++;
        if (start == end) return;
        selectionAnchor = start;
        cursor = end;
    }

    private void backspace() {
        if (this.hasSelection()) {
            this.pushHistory();
            this.deleteSelection();
            this.scrollToCursor();
            return;
        }
        if (cursor == 0) return;
        this.pushHistory();

        int removeAfter = this.isEmptyPairAroundCaret() ? 1 : 0;
        value = value.substring(0, cursor - 1) + value.substring(cursor + removeAfter);
        cursor--;
        selectionAnchor = cursor;
        this.relayout();
        this.scrollToCursor();
    }

    private void deleteForward() {
        if (this.hasSelection()) {
            this.pushHistory();
            this.deleteSelection();
            this.scrollToCursor();
            return;
        }
        if (cursor >= value.length()) return;
        this.pushHistory();
        value = value.substring(0, cursor) + value.substring(cursor + 1);
        selectionAnchor = cursor;
        this.relayout();
        this.scrollToCursor();
    }

    private void moveVertical(int delta) {
        VisualLine cur = visualLines.get(cursorLine);
        int caretX = font.width(value.substring(cur.start(), cursor));

        int targetIdx = Math.clamp(cursorLine + delta, 0, visualLines.size() - 1);
        VisualLine target = visualLines.get(targetIdx);
        String targetText = value.substring(target.start(), target.end());

        int col = font.plainSubstrByWidth(targetText, caretX).length();
        cursor = target.start() + col;
        cursorLine = targetIdx;
        this.scrollToCursor();
    }

    private void moveHome() {
        cursor = visualLines.get(cursorLine).start();
        this.scrollToCursor();
    }

    private void moveEnd() {
        cursor = visualLines.get(cursorLine).end();
        this.scrollToCursor();
    }

    private void refreshSuggestions() {
        highlighted = 0;
        suggestionScroll = 0;

        if (this.hasSelection()) {
            context = PlaceholderSuggestionContext.NONE;
            return;
        }

        String resolved = this.getResolvedValue();
        int resolvedCursor = this.rawToStrippedOffset(cursor);
        context = PlaceholderSuggestionEngine.computeContext(resolved, resolvedCursor);
    }

    private void keepHighlightVisible() {
        int size = context.suggestions().size();
        int rows = Math.min(size, MAX_VISIBLE_ROWS);
        if (rows == 0) {
            suggestionScroll = 0;
            return;
        }
        if (highlighted < suggestionScroll) suggestionScroll = highlighted;
        else if (highlighted >= suggestionScroll + rows) suggestionScroll = highlighted - rows + 1;
        suggestionScroll = Math.clamp(suggestionScroll, 0, size - rows);
    }

    private void scrollSuggestions(int delta) {
        int size = context.suggestions().size();
        int rows = Math.min(size, MAX_VISIBLE_ROWS);
        if (rows == 0) return;
        suggestionScroll = Math.clamp(suggestionScroll + delta, 0, size - rows);
        highlighted = Math.clamp(highlighted, suggestionScroll, suggestionScroll + rows - 1);
    }

    private boolean isSuggesting() {
        return context.kind() != PlaceholderSuggestionContext.Kind.NONE && !context.suggestions().isEmpty();
    }

    private void acceptHighlighted() {
        if (!this.isSuggesting()) return;
        PlaceholderSuggestionEngine.Suggestion picked = context.suggestions().get(highlighted);
        int rawEnd = this.cursor;
        int rawStart = Math.min(this.strippedToRawOffset(context.replaceStart()), rawEnd);

        int tail = rawEnd;
        while (tail < value.length() && Character.isWhitespace(value.charAt(tail))) tail++;
        boolean addCall = picked.isFunction() && !value.startsWith(".(", tail);
        String inserted = addCall ? picked.name() + ".()" : picked.name();

        this.pushHistory();
        value = value.substring(0, rawStart) + inserted + value.substring(rawEnd);
        cursor = rawStart + picked.name().length() + (addCall ? ".(".length() : 0);
        selectionAnchor = cursor;
        this.relayout();
        this.scrollToCursor();
        this.refreshSuggestions();
    }

    private boolean handlePairTyped(char typed) {
        boolean opener = typed == '(' || typed == '<' || typed == '"';
        boolean closer = typed == ')' || typed == '>' || typed == '"';
        if ((!opener && !closer) || this.hasSelection()) return false;

        PlaceholderStructure.Context ctx = PlaceholderStructure.contextAt(this.getResolvedValue(), this.rawToStrippedOffset(cursor));
        char before = cursor > 0 ? value.charAt(cursor - 1) : '\0';
        char after = cursor < value.length() ? value.charAt(cursor) : '\0';

        if (closer && after == typed) {
            boolean skip = switch (typed) {
                case ')' -> !ctx.inString() && ctx.isExpression();
                case '>' -> !ctx.inString() && ctx.frame() == PlaceholderStructure.FrameKind.PATH_LT;
                default -> ctx.inString() && ctx.isExpression() && before != '\\';
            };
            if (skip) {
                cursor++;
                selectionAnchor = cursor;
                cursorLine = this.defaultLineIndexForOffset(cursor);
                this.scrollToCursor();
                return true;
            }
        }

        if (opener && !ctx.inString()) {
            boolean pair = switch (typed) {
                case '(' -> ctx.frame() != PlaceholderStructure.FrameKind.NONE;
                case '<' -> ctx.isExpression() && ctx.expectOperand();
                default -> ctx.isExpression() && ctx.expectOperand() && before != '\\';
            };
            boolean boundary = after == '\0' || Character.isWhitespace(after) || ")>,%\"".indexOf(after) >= 0;
            if (pair && boundary) {
                char closing = typed == '(' ? ')' : typed == '<' ? '>' : '"';
                this.insert("" + typed + closing);
                cursor--;
                selectionAnchor = cursor;
                cursorLine = this.defaultLineIndexForOffset(cursor);
                return true;
            }
        }

        return false;
    }

    private boolean isEmptyPairAroundCaret() {
        if (cursor <= 0 || cursor >= value.length()) return false;

        char open = value.charAt(cursor - 1);
        char close = value.charAt(cursor);
        boolean pair = (open == '(' && close == ')') || (open == '<' && close == '>') || (open == '"' && close == '"');
        if (!pair) return false;

        PlaceholderStructure.Context ctx = PlaceholderStructure.contextAt(this.getResolvedValue(), this.rawToStrippedOffset(cursor));
        return ctx.frame() != PlaceholderStructure.FrameKind.NONE;
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        String typed = event.codepointAsString();
        if (typed.length() != 1 || !this.handlePairTyped(typed.charAt(0))) {
            this.insert(typed);
        }
        this.refreshSuggestions();
        return true;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent keyEvent) {
        if (this.isSuggesting()) {
            if (keyEvent.key() == GLFW.GLFW_KEY_TAB && !keyEvent.hasShiftDown()) {
                this.acceptHighlighted();
                return true;
            }
            if (keyEvent.key() == GLFW.GLFW_KEY_DOWN) {
                highlighted = Math.min(highlighted + 1, context.suggestions().size() - 1);
                this.keepHighlightVisible();
                return true;
            }
            if (keyEvent.key() == GLFW.GLFW_KEY_UP) {
                highlighted = Math.max(highlighted - 1, 0);
                this.keepHighlightVisible();
                return true;
            }
            if (keyEvent.key() == GLFW.GLFW_KEY_ESCAPE) {
                context = PlaceholderSuggestionContext.NONE;
                return true;
            }
        }

        if (keyEvent.isSelectAll()) {
            selectionAnchor = 0;
            cursor = value.length();
            cursorLine = this.defaultLineIndexForOffset(cursor);
            this.scrollToCursor();
            this.refreshSuggestions();
            return true;
        }
        if (keyEvent.isCopy() || keyEvent.isCut()) {
            if (this.hasSelection()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(value.substring(this.selStart(), this.selEnd()));
                if (keyEvent.isCut()) {
                    this.pushHistory();
                    this.deleteSelection();
                    this.scrollToCursor();
                    this.refreshSuggestions();
                }
            }
            return true;
        }
        if (keyEvent.isPaste()) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (!clip.isEmpty()) {
                this.insert(clip);
                this.refreshSuggestions();
            }
            return true;
        }


        boolean shift = keyEvent.hasShiftDown();
        boolean ctrl = keyEvent.hasControlDown();

        if (ctrl && keyEvent.key() == GLFW.GLFW_KEY_Z) {
            this.undo();
            return true;
        }
        if (ctrl && keyEvent.key() == GLFW.GLFW_KEY_Y) {
            this.redo();
            return true;
        }

        switch (keyEvent.key()) {
            case GLFW.GLFW_KEY_TAB -> {
                if (shift) this.unindent();
                else if (this.hasSelection()) this.indent();
                else this.insert(INDENT);
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (ctrl && !this.hasSelection()) this.deleteWordBack();
                else this.backspace();
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (ctrl && !this.hasSelection()) this.deleteWordForward();
                else this.deleteForward();
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> this.smartEnter();
            case GLFW.GLFW_KEY_LEFT -> {
                if (ctrl) {
                    cursor = this.prevWordBoundary(cursor);
                    cursorLine = this.defaultLineIndexForOffset(cursor);
                } else if (!shift && this.hasSelection()) {
                    cursor = this.selStart();
                    cursorLine = this.defaultLineIndexForOffset(cursor);
                } else {
                    VisualLine cur = visualLines.get(cursorLine);
                    if (cursor > cur.start()) {
                        cursor--;
                    } else if (cursorLine > 0) {
                        cursorLine--;
                        cursor = visualLines.get(cursorLine).end();
                    }
                }
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (ctrl) {
                    cursor = this.nextWordBoundary(cursor);
                    cursorLine = this.defaultLineIndexForOffset(cursor);
                } else if (!shift && this.hasSelection()) {
                    cursor = this.selEnd();
                    cursorLine = this.defaultLineIndexForOffset(cursor);
                } else {
                    VisualLine cur = visualLines.get(cursorLine);
                    if (cursor < cur.end()) {
                        cursor++;
                    } else if (cursorLine < visualLines.size() - 1) {
                        cursorLine++;
                        cursor = visualLines.get(cursorLine).start();
                    }
                }
            }
            case GLFW.GLFW_KEY_UP -> this.moveVertical(-1);
            case GLFW.GLFW_KEY_DOWN -> this.moveVertical(1);
            case GLFW.GLFW_KEY_HOME -> this.moveHome();
            case GLFW.GLFW_KEY_END -> this.moveEnd();
            case GLFW.GLFW_KEY_PAGE_UP -> this.moveVertical(-this.visibleRows());
            case GLFW.GLFW_KEY_PAGE_DOWN -> this.moveVertical(this.visibleRows());
            default -> {
                return false;
            }
        }

        if (!shift) selectionAnchor = cursor;
        this.scrollToCursor();
        this.refreshSuggestions();
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || this.isOverSuggestionPanel(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        if (mouseButtonEvent.buttonInfo().button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int index = this.suggestionIndexAt(this.suggestionPanel(), mouseButtonEvent.x(), mouseButtonEvent.y());
            if (index >= 0) {
                pressedButton = true;
                highlighted = index;
                this.acceptHighlighted();
                return true;
            }
        }
        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    public void onClick(MouseButtonEvent mouseButtonEvent, boolean bl) {
        if (mouseButtonEvent.buttonInfo().button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;

        draggingScrollbar = false;
        pressedButton = false;

        if (this.isOverFormatButton(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            pressedButton = true;
            this.format();
            return;
        }

        if (this.isOverMinifyButton(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            pressedButton = true;
            this.minify();
            return;
        }

        if (this.isOverHelpButton(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            pressedButton = true;
            return;
        }

        if (this.isOverScrollbar(mouseButtonEvent.x(), mouseButtonEvent.y())) {
            scrollbarGrabOffset = this.isOverThumb(mouseButtonEvent.y())
                    ? (int) mouseButtonEvent.y() - this.scrollThumbY()
                    : this.scrollThumbHeight() / 2;
            draggingScrollbar = true;
            this.scrollToThumbPosition(mouseButtonEvent.y());
            return;
        }

        int localY = (int) mouseButtonEvent.y() - getY() - PAD;
        int lineIdx = Math.clamp(scrollLines + localY / lineHeight, scrollLines, visualLines.size() - 1);
        VisualLine vl = visualLines.get(lineIdx);
        String lineText = value.substring(vl.start(), vl.end());

        int localX = (int) mouseButtonEvent.x() - getX() - gutterWidth;
        int col = font.plainSubstrByWidth(lineText, Math.max(0, localX)).length();

        long now = System.currentTimeMillis();
        clickCount = (now - lastClickMs < MULTI_CLICK_MS) ? Math.min(clickCount + 1, 3) : 1;
        lastClickMs = now;

        cursor = vl.start() + col;
        cursorLine = lineIdx;
        selectionAnchor = cursor;

        if (clickCount == 2) {
            this.selectWordAt(cursor);
        } else if (clickCount == 3) {
            selectionAnchor = vl.start();
            cursor = vl.end();
        }

        this.scrollToCursor();
        this.refreshSuggestions();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        if (mouseButtonEvent.buttonInfo().button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        if (pressedButton) return true;

        if (draggingScrollbar) {
            this.scrollToThumbPosition(mouseButtonEvent.y());
            return true;
        }

        int localY = (int) mouseButtonEvent.y() - getY() - PAD;
        int lineIdx = Math.clamp(scrollLines + localY / lineHeight, scrollLines, visualLines.size() - 1);
        VisualLine vl = visualLines.get(lineIdx);
        String lineText = value.substring(vl.start(), vl.end());

        int localX = (int) mouseButtonEvent.x() - getX() - gutterWidth;
        int col = font.plainSubstrByWidth(lineText, Math.max(0, localX)).length();

        cursor = vl.start() + col;
        cursorLine = lineIdx;
        this.scrollToCursor();
        this.refreshSuggestions();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.buttonInfo().button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingScrollbar = false;
            pressedButton = false;
        }
        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return false;
        if (this.suggestionIndexAt(this.suggestionPanel(), mouseX, mouseY) >= 0) {
            this.scrollSuggestions(scrollY > 0 ? -1 : 1);
            return true;
        }
        scrollLines = Math.clamp(scrollLines + (scrollY > 0 ? -1 : 1), 0, this.maxScroll());
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.updateFrameState(mouseX, mouseY);
        this.resolvePendingPlaceholder();

        int editorBottom = getBottom() - resultBoxHeight;
        this.renderEditorFrame(guiGraphics, editorBottom);

        guiGraphics.enableScissor(getX() + 1, getY() + 1, getRight() - 1, editorBottom - 1);
        this.renderEditorText(guiGraphics);
        this.renderCaret(guiGraphics);
        this.renderScrollbar(guiGraphics, mouseX, mouseY);
        this.renderFormatButton(guiGraphics, mouseX, mouseY);
        this.renderMinifyButton(guiGraphics, mouseX, mouseY);
        this.renderHelpButton(guiGraphics, mouseX, mouseY);
        guiGraphics.disableScissor();

        this.renderResultFrame(guiGraphics, editorBottom);

        guiGraphics.enableScissor(getX() + 1, editorBottom + 1, getRight() - 1, getBottom() - 1);
        this.renderResultText(guiGraphics, editorBottom);
        guiGraphics.disableScissor();

        this.requestMouseCursor(guiGraphics, mouseX, mouseY);
    }

    private void updateFrameState(int mouseX, int mouseY) {
        mouseMoved = mouseX != lastMouseX || mouseY != lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        if (draggingScrollbar && GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) draggingScrollbar = false;
    }

    private void resolvePendingPlaceholder() {
        if (!this.getResolvedValue().isEmpty() && pendingResolve && System.currentTimeMillis() - lastEditTimeMs >= RESOLVE_DEBOUNCE_MS) {
            resolvedResult = this.resolvePlaceholder(this.getResolvedValue());
            pendingResolve = false;
        }
    }

    private void renderEditorFrame(GuiGraphics guiGraphics, int editorBottom) {
        guiGraphics.fill(getX(), getY(), getRight(), editorBottom, BACKGROUND_COLOR);
        guiGraphics.fill(getX(), getY(), getRight(), getY() + 1, BORDER_COLOR);
        guiGraphics.fill(getX(), editorBottom - 1, getRight(), editorBottom, BORDER_COLOR);
        guiGraphics.fill(getX(), getY(), getX() + 1, editorBottom, BORDER_COLOR);
        guiGraphics.fill(getRight() - 1, getY(), getRight(), editorBottom, BORDER_COLOR);

        int dividerX = getX() + GUTTER_LEFT_PAD + gutterNumberWidth + GUTTER_LEFT_PAD;
        guiGraphics.fill(dividerX, getY() + 1, dividerX + 1, editorBottom - 1, GUTTER_DIVIDER_COLOR);
    }

    private void renderEditorText(GuiGraphics guiGraphics) {
        int[] strippedToRaw = strippedToRawMap();
        List<PlaceholderSyntaxHighlighter.Span> spans = this.highlightSpans(strippedToRaw);
        PlaceholderStructure structure = this.structure();
        boolean selecting = this.hasSelection();
        BracketHighlight bracket = this.findBracketHighlight(structure, strippedToRaw, selecting);
        int textX = getX() + gutterWidth;

        int visible = this.visibleRows();
        for (int row = 0; row < visible; row++) {
            int lineIdx = scrollLines + row;
            if (lineIdx >= visualLines.size()) break;

            VisualLine vl = visualLines.get(lineIdx);
            int drawY = getY() + PAD + row * lineHeight;

            if (!vl.continuation()) this.renderGutterNumber(guiGraphics, vl, lineIdx, drawY);
            this.renderIndentGuides(guiGraphics, vl, textX, drawY);
            if (selecting) this.renderSelection(guiGraphics, vl, textX, drawY);
            this.renderBrackets(guiGraphics, bracket, structure, strippedToRaw, vl, textX, drawY);
            this.renderLineText(guiGraphics, spans, vl, textX, drawY);
        }
    }

    private List<PlaceholderSyntaxHighlighter.Span> highlightSpans(int[] strippedToRaw) {
        List<PlaceholderSyntaxHighlighter.Span> strippedSpans = PlaceholderSyntaxHighlighter.highlight(this.getResolvedValue());
        List<PlaceholderSyntaxHighlighter.Span> spans = new ArrayList<>(strippedSpans.size());
        for (PlaceholderSyntaxHighlighter.Span span : strippedSpans) {
            int rawStart = strippedToRaw[Math.min(span.start(), strippedToRaw.length - 1)];
            int rawEnd = strippedToRaw[Math.min(span.end(), strippedToRaw.length - 1)];
            spans.add(PlaceholderSyntaxHighlighter.Span.of(rawStart, rawEnd, span.color(), span.error()));
        }
        return spans;
    }

    private record BracketHighlight(int first, int second, boolean unmatched) {
        static final BracketHighlight NONE = new BracketHighlight(-1, -1, false);
    }

    private BracketHighlight findBracketHighlight(PlaceholderStructure structure, int[] strippedToRaw, boolean selecting) {
        if (!isFocused() || selecting) return BracketHighlight.NONE;

        int caretStripped = this.rawToStrippedOffset(cursor);
        PlaceholderStructure.Delimiter near = structure.delimiterAt(caretStripped - 1);
        if (near == null) near = structure.delimiterAt(caretStripped);
        if (near == null) return BracketHighlight.NONE;

        int first = strippedToRaw[near.index()];
        return near.matched()
                ? new BracketHighlight(first, strippedToRaw[near.partner()], false)
                : new BracketHighlight(first, -1, true);
    }

    private void renderGutterNumber(GuiGraphics guiGraphics, VisualLine vl, int lineIdx, int drawY) {
        String numStr = String.valueOf(vl.lineNumber());
        int numX = getX() + GUTTER_LEFT_PAD + (gutterNumberWidth - font.width(numStr));
        int numColor = (lineIdx == cursorLine) ? GUTTER_CURRENT_LINE_COLOR : GUTTER_NUMBER_COLOR;
        guiGraphics.drawString(font, numStr, numX, drawY, numColor, false);
    }

    private int logicalLineStart(int pos) {
        int i = value.lastIndexOf('\n', pos - 1);
        return i < 0 ? 0 : i + 1;
    }

    private void renderIndentGuides(GuiGraphics guiGraphics, VisualLine vl, int textX, int drawY) {
        int logicalStart = this.logicalLineStart(vl.start());
        int i = logicalStart;
        while (i < value.length() && value.charAt(i) == ' ') i++;

        int levels = (i - logicalStart) / INDENT.length();
        if (levels <= 0) return;

        int indentWidth = font.width(INDENT);
        for (int level = 1; level <= levels; level++) {
            int x = textX + (level - 1) * indentWidth;
            guiGraphics.fill(x, drawY, x + 1, drawY + lineHeight, INDENT_GUIDE_COLOR);
        }
    }

    private void renderSelection(GuiGraphics guiGraphics, VisualLine vl, int textX, int drawY) {
        int hs = Math.max(this.selStart(), vl.start());
        int he = Math.min(this.selEnd(), vl.end());
        if (he <= hs) return;

        int hx1 = textX + font.width(value.substring(vl.start(), hs));
        int hx2 = textX + font.width(value.substring(vl.start(), he));
        guiGraphics.fill(hx1, drawY, hx2, drawY + lineHeight, SELECTION_COLOR);
    }

    private void renderBrackets(GuiGraphics guiGraphics, BracketHighlight bracket, PlaceholderStructure structure, int[] strippedToRaw, VisualLine vl, int textX, int drawY) {
        int lineStart = vl.start(), lineEnd = vl.end();

        if (bracket.first() >= 0) {
            int boxColor = bracket.unmatched() ? BRACKET_ERROR_BOX_COLOR : BRACKET_MATCH_COLOR;
            this.markChar(guiGraphics, bracket.first(), lineStart, lineEnd, textX, drawY, boxColor, false);
            if (bracket.second() >= 0) this.markChar(guiGraphics, bracket.second(), lineStart, lineEnd, textX, drawY, boxColor, false);
        }
        for (PlaceholderStructure.Delimiter unmatched : structure.unmatched()) {
            this.markChar(guiGraphics, strippedToRaw[unmatched.index()], lineStart, lineEnd, textX, drawY, BRACKET_ERROR_COLOR, true);
        }
    }

    private void renderLineText(GuiGraphics guiGraphics, List<PlaceholderSyntaxHighlighter.Span> spans, VisualLine vl, int textX, int drawY) {
        int lineStart = vl.start(), lineEnd = vl.end();

        MutableComponent rendered = Component.empty();
        int cur = lineStart;
        for (PlaceholderSyntaxHighlighter.Span span : spans) {
            int start = Math.max(span.start(), lineStart);
            int end = Math.min(span.end(), lineEnd);
            if (end <= start) continue;
            if (start > cur) rendered.append(plain(value.substring(cur, start), DEFAULT_TEXT_COLOR));
            rendered.append(plain(value.substring(start, end), span.color() & 0xFFFFFF));
            cur = end;
        }
        if (cur < lineEnd) rendered.append(plain(value.substring(cur, lineEnd), DEFAULT_TEXT_COLOR));

        guiGraphics.drawString(font, Language.getInstance().getVisualOrder(rendered), textX, drawY, DEFAULT_TEXT_COLOR, false);
        this.underlineErrors(guiGraphics, spans, lineStart, lineEnd, textX, drawY);
    }

    private void renderCaret(GuiGraphics guiGraphics) {
        if (!isFocused() || this.hasSelection() || (System.currentTimeMillis() / CURSOR_BLINK_INTERVAL_MS) % 2 != 0) return;

        int row = cursorLine - scrollLines;
        if (row < 0 || row >= this.visibleRows()) return;

        VisualLine vl = visualLines.get(cursorLine);
        int cx = getX() + gutterWidth + font.width(value.substring(vl.start(), cursor));
        int cy = getY() + PAD + row * lineHeight - 2;
        guiGraphics.fill(cx, cy, cx + 1, cy + font.lineHeight + 2, CURSOR_COLOR);
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.isScrollbarVisible()) return;

        int thumbY = this.scrollThumbY();
        int thumbHeight = this.scrollThumbHeight();
        int barX = getRight() - SCROLLBAR_WIDTH - 1;
        boolean thumbActive = draggingScrollbar
                || (this.isOverScrollbar(mouseX, mouseY) && this.isOverThumb(mouseY));
        guiGraphics.fill(barX, thumbY, barX + SCROLLBAR_WIDTH, thumbY + thumbHeight,
                thumbActive ? SCROLLBAR_ACTIVE_COLOR : SCROLLBAR_COLOR);
    }

    private void renderResultFrame(GuiGraphics guiGraphics, int editorBottom) {
        guiGraphics.fill(getX(), editorBottom, getRight(), getBottom(), BACKGROUND_COLOR);
        guiGraphics.fill(getX(), getBottom() - 1, getRight(), getBottom(), BORDER_COLOR);
        guiGraphics.fill(getX(), editorBottom, getX() + 1, getBottom(), BORDER_COLOR);
        guiGraphics.fill(getRight() - 1, editorBottom, getRight(), getBottom(), BORDER_COLOR);
    }

    private void renderResultText(GuiGraphics guiGraphics, int editorBottom) {
        if (pendingResolve || resolvedResult == null || this.getResolvedValue().isEmpty()) {
            guiGraphics.drawString(font, "awaiting", getX() + PADDING, editorBottom + PADDING, RESULT_AWAITING_COLOR, false);
        } else {
            if(resolvedResult.success()[1]) {
                guiGraphics.drawString(font, TextHelper.concat(
                        Component.literal("Hidden ").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                        resolvedResult.text()
                ), getX() + PADDING, editorBottom + PADDING, DEFAULT_TEXT_COLOR, false);
            } else {
                guiGraphics.drawString(font, resolvedResult.text(), getX() + PADDING, editorBottom + PADDING, DEFAULT_TEXT_COLOR, false);
            }
        }
    }

    private void requestMouseCursor(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean overPanel = this.isOverSuggestionPanel(mouseX, mouseY);
        if (draggingScrollbar || overPanel || (this.isHovered() && !this.isOverHelpButton(mouseX, mouseY))) {
            boolean pointer = draggingScrollbar
                    || overPanel
                    || this.isOverScrollbar(mouseX, mouseY)
                    || this.isOverFormatButton(mouseX, mouseY)
                    || this.isOverMinifyButton(mouseX, mouseY);
            guiGraphics.requestCursor(pointer ? CursorTypes.POINTING_HAND : CursorTypes.IBEAM);
        }
    }

    private static MutableComponent plain(String text, int rgb) {
        return Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    private void underlineErrors(GuiGraphics guiGraphics, List<PlaceholderSyntaxHighlighter.Span> spans, int lineStart, int lineEnd, int textX, int drawY) {
        for (PlaceholderSyntaxHighlighter.Span span : spans) {
            if (!span.error()) continue;

            int start = Math.max(span.start(), lineStart);
            int end = Math.min(span.end(), lineEnd);
            if (end <= start) continue;

            int x1 = textX + font.width(value.substring(lineStart, start));
            int x2 = textX + font.width(value.substring(lineStart, end));
            guiGraphics.fill(x1, drawY + font.lineHeight, x2, drawY + font.lineHeight + 1, span.color());
        }
    }

    private void markChar(GuiGraphics guiGraphics, int rawIndex, int lineStart, int lineEnd, int textX, int drawY, int color, boolean underline) {
        if (rawIndex < lineStart || rawIndex >= lineEnd) return;

        int x1 = textX + font.width(value.substring(lineStart, rawIndex));
        int x2 = x1 + font.width(value.substring(rawIndex, rawIndex + 1));
        if (underline) guiGraphics.fill(x1, drawY + lineHeight - 2, x2, drawY + lineHeight - 1, color);
        else guiGraphics.fill(x1, drawY, x2, drawY + lineHeight, color);
    }

    private PlaceholderStructure structure() {
        if (structure == null || !Objects.equals(structureFor, value)) {
            structure = PlaceholderStructure.analyze(this.getResolvedValue());
            structureFor = value;
        }
        return structure;
    }

    private int suggestionIndexAt(SuggestionPanel panel, double mouseX, double mouseY) {
        if (panel == null) return -1;
        if (mouseX < panel.x() || mouseX >= panel.x() + PANEL_WIDTH) return -1;
        if (mouseY < panel.y() || mouseY >= panel.y() + panel.rows() * ROW_HEIGHT) return -1;
        return suggestionScroll + (int) ((mouseY - panel.y()) / ROW_HEIGHT);
    }

    private boolean isOverSuggestionPanel(double mouseX, double mouseY) {
        return this.suggestionIndexAt(this.suggestionPanel(), mouseX, mouseY) >= 0;
    }

    public void renderSuggestions(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        SuggestionPanel panel = this.suggestionPanel();
        if (panel == null) return;

        guiGraphics.nextStratum();

        int hovered = this.suggestionIndexAt(panel, mouseX, mouseY);
        if (hovered >= 0 && mouseMoved) highlighted = hovered;

        List<PlaceholderSuggestionEngine.Suggestion> items = context.suggestions();
        int panelHeight = panel.rows() * ROW_HEIGHT;

        guiGraphics.fill(panel.x(), panel.y(), panel.x() + PANEL_WIDTH, panel.y() + panelHeight, 0xF0101010);
        for (int i = 0; i < panel.rows(); i++) {
            int itemIndex = suggestionScroll + i;
            PlaceholderSuggestionEngine.Suggestion item = items.get(itemIndex);
            int rowTop = panel.y() + i * ROW_HEIGHT;
            if (itemIndex == highlighted) guiGraphics.fill(panel.x(), rowTop, panel.x() + PANEL_WIDTH, rowTop + ROW_HEIGHT, 0x803366CC);
            int textColor = (itemIndex == highlighted) ? 0xFFFFFF55 : 0xFFCCCCCC;
            String label = item.name() + (item.isFunction() ? "(...)" : "");
            guiGraphics.drawString(font, label, panel.x() + 3, rowTop + 2, textColor, false);
        }

        if (items.size() > panel.rows()) {
            int thumbHeight = Math.max(6, panelHeight * panel.rows() / items.size());
            int thumbY = panel.y() + (panelHeight - thumbHeight) * suggestionScroll / (items.size() - panel.rows());
            guiGraphics.fill(panel.x() + PANEL_WIDTH - 3, thumbY, panel.x() + PANEL_WIDTH - 1, thumbY + thumbHeight, SCROLLBAR_COLOR);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }
}