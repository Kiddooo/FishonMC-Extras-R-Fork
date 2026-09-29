package dannypx.foe.placeholder.editbox;

import dannypx.foe.placeholder.lexer.PlaceholderTokenizer;
import dannypx.foe.placeholder.token.PlaceholderParseException;
import dannypx.foe.placeholder.token.Token;
import dannypx.foe.placeholder.token.TokenType;

import java.util.*;

public class PlaceholderStructure {
    public enum FrameKind { NONE, BLOCK, PATH_LT, CALL, GROUP }

    public record Delimiter(int index, char character, int partner) {
        public boolean matched() {
            return partner >= 0;
        }
    }

    public record Context(FrameKind frame, boolean expectOperand, boolean inString) {
        public static final Context NONE = new Context(FrameKind.NONE, false, false);

        public boolean isExpression() {
            return frame == FrameKind.CALL || frame == FrameKind.GROUP;
        }
    }

    private static final PlaceholderStructure EMPTY = new PlaceholderStructure(new HashMap<>());

    private final Map<Integer, Delimiter> delimiters;
    private final List<Delimiter> unmatched;

    private PlaceholderStructure(Map<Integer, Delimiter> delimiters) {
        this.delimiters = delimiters;
        List<Delimiter> open = new ArrayList<>();
        for (Delimiter delimiter : delimiters.values()) {
            if (!delimiter.matched()) open.add(delimiter);
        }
        this.unmatched = Collections.unmodifiableList(open);
    }

    public Delimiter delimiterAt(int index) {
        return delimiters.get(index);
    }

    public List<Delimiter> unmatched() {
        return unmatched;
    }

    public static PlaceholderStructure analyze(String text) {
        Scan scan;
        try {
            scan = scan(new PlaceholderTokenizer(text).tokenize());
        } catch (PlaceholderParseException e) {
            if (e.position < 0 || e.position > text.length()) return EMPTY;
            try {
                scan = scan(new PlaceholderTokenizer(text.substring(0, e.position)).tokenize());
            } catch (PlaceholderParseException ignored) {
                return EMPTY;
            }
        }

        Map<Integer, Delimiter> result = new HashMap<>();
        for (MutableDelimiter delimiter : scan.delimiters.values()) {
            result.put(delimiter.index, new Delimiter(delimiter.index, delimiter.character, delimiter.partner));
        }
        return new PlaceholderStructure(result);
    }

    public static Context contextAt(String text, int caret) {
        if (caret < 0 || caret > text.length()) return Context.NONE;

        String before = text.substring(0, caret);
        try {
            return contextOf(scan(new PlaceholderTokenizer(before).tokenize()), false);
        } catch (PlaceholderParseException e) {
            if (e.position < 0 || e.position > before.length()) return new Context(FrameKind.NONE, false, true);
            try {
                return contextOf(scan(new PlaceholderTokenizer(before.substring(0, e.position)).tokenize()), true);
            } catch (PlaceholderParseException ignored) {
                return new Context(FrameKind.NONE, false, true);
            }
        }
    }

    private static Context contextOf(Scan scan, boolean inString) {
        Frame top = scan.stack.peek();
        if (top == null) return new Context(FrameKind.NONE, false, inString);
        return new Context(top.kind, top.isExpression() && top.expectOperand, inString);
    }


    private static final class MutableDelimiter {
        final int index;
        final char character;
        int partner = -1;

        MutableDelimiter(int index, char character) {
            this.index = index;
            this.character = character;
        }
    }

    private static final class Frame {
        final FrameKind kind;
        final MutableDelimiter open;
        boolean expectOperand;

        Frame(FrameKind kind, MutableDelimiter open) {
            this.kind = kind;
            this.open = open;
            this.expectOperand = isExpression();
        }

        boolean isPath() {
            return kind == FrameKind.BLOCK || kind == FrameKind.PATH_LT;
        }

        boolean isExpression() {
            return kind == FrameKind.CALL || kind == FrameKind.GROUP;
        }
    }

    private static final class Scan {
        final Map<Integer, MutableDelimiter> delimiters = new HashMap<>();
        final Deque<Frame> stack = new ArrayDeque<>();

        MutableDelimiter delimiter(Token token, char character) {
            MutableDelimiter delimiter = new MutableDelimiter(token.start(), character);
            delimiters.put(delimiter.index, delimiter);
            return delimiter;
        }

        void push(FrameKind kind, Token token, char character) {
            stack.push(new Frame(kind, this.delimiter(token, character)));
        }

        void close(Token token, char character) {
            Frame frame = stack.pop();
            MutableDelimiter closer = this.delimiter(token, character);
            closer.partner = frame.open.index;
            frame.open.partner = closer.index;
        }
    }

    private static Scan scan(List<Token> tokens) {
        Scan scan = new Scan();
        Deque<Frame> stack = scan.stack;

        for (Token token : tokens) {
            TokenType type = token.type();
            if (type == TokenType.EOF) break;

            Frame top = stack.peek();

            switch (type) {
                case PERCENT -> {
                    if (stack.isEmpty()) scan.push(FrameKind.BLOCK, token, '%');
                    else if (stack.size() == 1 && top.kind == FrameKind.BLOCK) scan.close(token, '%');
                }
                case LT -> {
                    if (top != null && top.isExpression()) {
                        if (top.expectOperand) scan.push(FrameKind.PATH_LT, token, '<');
                        else top.expectOperand = true;
                    }
                }
                case GT -> {
                    if (top != null && top.kind == FrameKind.PATH_LT) {
                        scan.close(token, '>');
                        Frame parent = stack.peek();
                        if (parent != null && parent.isExpression()) parent.expectOperand = false;
                    } else if (top != null && top.isExpression()) {
                        top.expectOperand = true;
                    }
                }
                case LPARENTHESIS -> {
                    if (top != null) scan.push(top.isPath() ? FrameKind.CALL : FrameKind.GROUP, token, '(');
                }
                case RPARENTHESIS -> {
                    if (top != null) {
                        if (top.isExpression()) {
                            scan.close(token, ')');
                            Frame parent = stack.peek();
                            if (parent != null && parent.isExpression()) parent.expectOperand = false;
                        } else {
                            scan.delimiter(token, ')');
                        }
                    }
                }
                case COMMA, PLUS, MINUS, STAR, SLASH, ASSIGN, BANG -> {
                    if (top != null && top.isExpression()) top.expectOperand = true;
                }
                case NUMBER, STRING, IDENTIFIER -> {
                    if (top != null && top.isExpression()) top.expectOperand = false;
                }
                default -> {}
            }
        }

        return scan;
    }
}
