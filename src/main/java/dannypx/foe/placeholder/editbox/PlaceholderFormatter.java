package dannypx.foe.placeholder.editbox;

import dannypx.foe.placeholder.lexer.PlaceholderTokenizer;
import dannypx.foe.placeholder.token.PlaceholderParseException;
import dannypx.foe.placeholder.token.Token;
import dannypx.foe.placeholder.token.TokenType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class PlaceholderFormatter {
    private static final String INDENT = "  ";

    private enum FrameKind { BLOCK, PATH_LT, CALL, GROUP }

    private static final class Frame {
        final FrameKind kind;
        final StringBuilder segment = new StringBuilder();
        boolean expectOperand;
        boolean hasContent;

        Frame(FrameKind kind) {
            this.kind = kind;
            this.expectOperand = isExpression();
        }

        boolean isPath() {
            return kind == FrameKind.BLOCK || kind == FrameKind.PATH_LT;
        }

        boolean isExpression() {
            return kind == FrameKind.CALL || kind == FrameKind.GROUP;
        }
    }

    private final String source;
    private final boolean compact;
    private final List<Token> tokens;
    private final StringBuilder out = new StringBuilder();
    private final Deque<Frame> stack = new ArrayDeque<>();
    private int depth = 0;
    private boolean pendingBreak = false;
    private boolean sawWhitespace = false;

    private PlaceholderFormatter(String source, boolean compact) {
        this.source = source;
        this.compact = compact;
        this.tokens = new PlaceholderTokenizer(source).tokenize();
    }

    public static String format(String text) {
        String source = text.indexOf('\n') < 0 ? text : text.replace("\n", "");
        String result = layout(source, false);
        return result != null ? result : text;
    }

    public static String minify(String text) {
        String source = text.indexOf('\n') < 0 ? text : text.replace("\n", "");
        String result = layout(source, true);
        if (result == null) return text;

        String expected = layout(source, false);
        String actual = layout(result, false);
        return expected != null && expected.equals(actual) ? result : text;
    }

    private static String layout(String source, boolean compact) {
        try {
            String result = new PlaceholderFormatter(source, compact).run();
            return result != null && sameSignificantChars(source, result) ? result : null;
        } catch (PlaceholderParseException ignored) {
            return null;
        }
    }

    private String run() {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            TokenType type = token.type();
            if (type == TokenType.EOF) break;

            Frame top = stack.peek();

            if (top == null) {
                if (type == TokenType.PERCENT) {
                    i = this.openBlock(i);
                    if (i < 0) return null;
                } else {
                    out.append(raw(token));
                }
                continue;
            }

            if (type == TokenType.WHITESPACE) {
                if (top.isPath()) top.segment.append(raw(token));
                else sawWhitespace = true;
                continue;
            }

            if (top.isPath()) {
                this.handlePath(top, token);
            } else {
                i = this.handleExpression(top, i);
            }
            sawWhitespace = false;
        }

        return stack.isEmpty() ? out.toString() : null;
    }

    private int openBlock(int index) {
        int close = -1;
        for (int j = index + 1; j < tokens.size(); j++) {
            TokenType t = tokens.get(j).type();
            if (t == TokenType.LPARENTHESIS) break;
            if (t == TokenType.PERCENT) {
                close = j;
                break;
            }
            if (t == TokenType.EOF) return -1;
        }

        if (close >= 0) {
            if (!compact && !out.isEmpty() && out.charAt(out.length() - 1) == '%') out.append('\n');
            out.append(source, tokens.get(index).start(), tokens.get(close).end());
            return close;
        }

        if (!compact) this.startLine();
        out.append('%');
        stack.push(new Frame(FrameKind.BLOCK));
        pendingBreak = !compact;
        return index;
    }

    private void handlePath(Frame top, Token token) {
        switch (token.type()) {
            case DOT -> {
                this.flushSegment(top);
                this.emit(".");
            }
            case LPARENTHESIS -> {
                this.flushSegment(top);
                this.emit("(");
                depth++;
                pendingBreak = !compact;
                stack.push(new Frame(FrameKind.CALL));
            }
            case GT -> {
                if (top.kind == FrameKind.PATH_LT) {
                    this.flushSegment(top);
                    this.emit(">");
                    stack.pop();
                    Frame parent = stack.peek();
                    if (parent != null && parent.isExpression()) parent.expectOperand = false;
                } else {
                    top.segment.append(raw(token));
                }
            }
            case PERCENT -> {
                if (stack.size() == 1 && top.kind == FrameKind.BLOCK) {
                    this.flushSegment(top);
                    stack.pop();
                    if (!compact) out.append('\n');
                    out.append('%');
                    pendingBreak = false;
                } else {
                    top.segment.append(raw(token));
                }
            }
            default -> top.segment.append(raw(token));
        }
    }

    private int handleExpression(Frame top, int index) {
        Token token = tokens.get(index);

        switch (token.type()) {
            case LT -> {
                if (top.expectOperand) {
                    this.emit("<");
                    stack.push(new Frame(FrameKind.PATH_LT));
                } else {
                    index = this.operator(index, top);
                }
            }
            case GT, PLUS, MINUS, STAR, SLASH, ASSIGN, BANG -> index = this.operator(index, top);
            case COMMA -> {
                this.trimTrailingSpaces();
                this.emit(",");
                top.expectOperand = true;
                if (compact) break;
                if (top.kind == FrameKind.CALL) pendingBreak = true;
                else out.append(' ');
            }
            case LPARENTHESIS -> {
                if (!top.expectOperand) this.separate();
                this.emit("(");
                stack.push(new Frame(FrameKind.GROUP));
            }
            case RPARENTHESIS -> {
                stack.pop();
                this.trimTrailingSpaces();
                if (top.kind == FrameKind.CALL) {
                    depth--;
                    if (top.hasContent && !compact) out.append('\n').repeat(INDENT, depth + 1);
                    out.append(')');
                    pendingBreak = false;
                } else {
                    this.emit(")");
                }
                Frame parent = stack.peek();
                if (parent != null && parent.isExpression()) parent.expectOperand = false;
            }
            case NUMBER, STRING, IDENTIFIER -> {
                if (!top.expectOperand) this.separate();
                this.emit(raw(token));
                top.expectOperand = false;
            }
            default -> {
                this.separate();
                this.emit(raw(token));
            }
        }

        return index;
    }

    private int operator(int index, Frame frame) {
        Token token = tokens.get(index);
        TokenType type = token.type();
        String op = raw(token);

        if ((type == TokenType.ASSIGN || type == TokenType.BANG || type == TokenType.LT || type == TokenType.GT)
                && index + 1 < tokens.size()) {
            Token next = tokens.get(index + 1);
            if (next.type() == TokenType.ASSIGN && next.start() == token.end()) {
                op += raw(next);
                index++;
            }
        }

        boolean spaced = !compact || op.charAt(0) == '<' || op.charAt(0) == '>';
        if (frame.expectOperand || !spaced) {
            this.emit(op);
        } else {
            this.emit(" " + op + " ");
        }
        frame.expectOperand = true;
        return index;
    }


    private void startLine() {
        if (!out.isEmpty() && out.charAt(out.length() - 1) != '\n') out.append('\n');
    }

    private void emit(String text) {
        if (pendingBreak) {
            out.append('\n').repeat(INDENT, depth + 1);
            pendingBreak = false;
        }
        out.append(text);

        Frame top = stack.peek();
        if (top != null && top.kind == FrameKind.CALL) top.hasContent = true;
    }

    private void flushSegment(Frame frame) {
        String segment = frame.segment.toString().strip();
        frame.segment.setLength(0);
        if (!segment.isEmpty()) this.emit(segment);
    }

    private void separate() {
        if (!sawWhitespace || pendingBreak || out.isEmpty()) return;
        char last = out.charAt(out.length() - 1);
        if (last != ' ' && last != '\n' && last != '(') out.append(' ');
    }

    private void trimTrailingSpaces() {
        int end = out.length();
        while (end > 0 && out.charAt(end - 1) == ' ') end--;
        out.setLength(end);
    }

    private String raw(Token token) {
        return source.substring(token.start(), token.end());
    }

    private static boolean sameSignificantChars(String a, String b) {
        int i = 0;
        int j = 0;
        while (true) {
            while (i < a.length() && Character.isWhitespace(a.charAt(i))) i++;
            while (j < b.length() && Character.isWhitespace(b.charAt(j))) j++;
            if (i == a.length() || j == b.length()) return i == a.length() && j == b.length();
            if (a.charAt(i++) != b.charAt(j++)) return false;
        }
    }
}
