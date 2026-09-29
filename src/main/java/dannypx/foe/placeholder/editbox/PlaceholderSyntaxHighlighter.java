package dannypx.foe.placeholder.editbox;

import dannypx.foe.placeholder.lexer.PlaceholderTokenizer;
import dannypx.foe.placeholder.registry.PlaceholderRegistry;
import dannypx.foe.placeholder.registry.PlaceholderTreeNode;
import dannypx.foe.placeholder.token.PlaceholderParseException;
import dannypx.foe.placeholder.token.Token;
import dannypx.foe.placeholder.token.TokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class PlaceholderSyntaxHighlighter {
    private PlaceholderSyntaxHighlighter() {}

    public record Span(int start, int end, int color, boolean error) {
        public static Span of(int start, int end, int color) {
            return new Span(start, end, color, false);
        };

        public static Span of(int start, int end, int color, boolean error) {
            return new Span(start, end, color, error);
        }
    }

    private static final int[] DEPTH_PALETTE = {
            0xFFFFA726,
            0xFFFFC107,
            0xFFB8D83E,
            0xFF4FCB5A,
            0xFF20C9A6,
            0xFF29BBD6,
            0xFF4295F4,
            0xFF6C6FEF,
            0xFFA45BE3,
            0xFFE65A9E
    };

    private static final int COLOR_INVALID = 0xFFFF5555;
    private static final int COLOR_NUMBER = 0xFFFFFFFF;
    private static final int COLOR_STRING_LITERAL = 0xFF7FFF7F;
    private static final int COLOR_OPERATOR = 0xFFAAAAAA;

    private static int depthColor(int depth) {
        return DEPTH_PALETTE[depth % DEPTH_PALETTE.length];
    }

    private enum FrameKind { PATH_PERCENT, PATH_LT, EXPRESSION_PARENTHESIS }
    private enum FinalUse { MID, CLOSE, CALL }

    private static final class Frame {
        final FrameKind kind;
        final int depth;
        boolean expectOperand;
        PlaceholderTreeNode node;
        boolean failed;
        int segmentStart;

        Frame(FrameKind kind, int depth, int segmentStart) {
            this.kind = kind;
            this.depth = depth;
            this.segmentStart = segmentStart;
            this.expectOperand = (kind == FrameKind.EXPRESSION_PARENTHESIS);
        }

        boolean isPath() {
            return kind == FrameKind.PATH_PERCENT || kind == FrameKind.PATH_LT;
        }
    }

    public static List<Span> highlight(String text) {
        List<Span> spans = new ArrayList<>();

        List<Token> tokens;
        try {
            tokens = new PlaceholderTokenizer(text).tokenize();
        } catch (PlaceholderParseException e) {
            if(e.position >= 0 && e.position < text.length()) {
                spans.addAll(highlight(text.substring(0, e.position)));
                spans.add(Span.of(e.position, text.length(), COLOR_STRING_LITERAL));
            }

            return spans;
        }

        Deque<Frame> stack = new ArrayDeque<>();

        for (Token token : tokens) {
            TokenType type = token.type();
            if(type == TokenType.EOF) break;

            switch (type) {
                case PERCENT -> {
                    if(stack.isEmpty()) {
                        int depth = 0;
                        stack.push(new Frame(FrameKind.PATH_PERCENT, depth, token.end()));
                        spans.add(Span.of(token.start(), token.end(), depthColor(depth)));
                    } else if (stack.size() == 1 && stack.peek().kind == FrameKind.PATH_PERCENT) {
                        Frame frame = stack.pop();
                        resolveAndEmit(frame, text, token.start(), spans, FinalUse.CLOSE);
                        spans.add(Span.of(token.start(), token.end(), depthColor(frame.depth)));
                    }
                }
                case LT -> {
                    Frame top = stack.peek();
                    if(top != null && top.kind == FrameKind.EXPRESSION_PARENTHESIS && top.expectOperand) {
                        int depth = stack.size();
                        stack.push(new Frame(FrameKind.PATH_LT, depth, token.end()));
                        spans.add(Span.of(token.start(), token.end(), depthColor(depth)));
                    } else if (top != null) {
                        spans.add(Span.of(token.start(), token.end(), COLOR_OPERATOR));
                        if (top.kind == FrameKind.EXPRESSION_PARENTHESIS) top.expectOperand = true;
                    }
                }
                case GT -> {
                    Frame top = stack.peek();
                    if(top != null && top.kind == FrameKind.PATH_LT) {
                        Frame frame = stack.pop();
                        resolveAndEmit(frame, text, token.start(), spans, FinalUse.CLOSE);
                        spans.add(Span.of(token.start(), token.end(), depthColor(frame.depth)));
                        Frame parent = stack.peek();
                        if(parent != null && parent.kind == FrameKind.EXPRESSION_PARENTHESIS) {
                            parent.expectOperand = false;
                        }
                    } else if (top != null) {
                        spans.add(Span.of(token.start(), token.end(), COLOR_OPERATOR));
                        if(top.kind == FrameKind.EXPRESSION_PARENTHESIS) {
                            top.expectOperand = true;
                        }
                    }
                }
                case LPARENTHESIS -> {
                    Frame top = stack.peek();
                    if(top != null && top.isPath()) {
                        resolveAndEmit(top, text, token.start(), spans, FinalUse.CALL);
                        int depth = stack.size();
                        stack.push(new Frame(FrameKind.EXPRESSION_PARENTHESIS, depth, token.end()));
                        spans.add(Span.of(token.start(), token.end(), depthColor(depth)));
                    } else if (top != null) {
                        int depth = stack.size();
                        stack.push(new Frame(FrameKind.EXPRESSION_PARENTHESIS, depth, token.end()));
                        spans.add(Span.of(token.start(), token.end(), depthColor(depth)));
                    }
                }
                case RPARENTHESIS -> {
                    Frame top = stack.peek();
                    if(top != null && top.kind == FrameKind.EXPRESSION_PARENTHESIS) {
                        Frame frame = stack.pop();
                        spans.add(Span.of(token.start(), token.end(), depthColor(frame.depth)));
                        Frame parent = stack.peek();
                        if(parent != null && parent.kind == FrameKind.EXPRESSION_PARENTHESIS) parent.expectOperand = false;
                        else if(parent != null && parent.isPath()) parent.segmentStart = token.end();
                    }
                }
                case COMMA, PLUS, MINUS, STAR, SLASH, ASSIGN, BANG -> {
                    Frame top = stack.peek();
                    if(top != null && top.kind == FrameKind.EXPRESSION_PARENTHESIS) {
                        top.expectOperand = true;
                        spans.add(Span.of(token.start(), token.end(), COLOR_OPERATOR));
                    }
                }
                case NUMBER -> {
                    Frame top = stack.peek();
                    if(top != null) {
                        if(top.isPath()) {
                        } else if (top.kind == FrameKind.EXPRESSION_PARENTHESIS) {
                            top.expectOperand = false;
                            spans.add(Span.of(token.start(), token.end(), COLOR_NUMBER));
                        }
                    }
                }
                case STRING -> {
                    Frame top = stack.peek();
                    if(top != null && top.kind == FrameKind.EXPRESSION_PARENTHESIS) {
                        top.expectOperand = false;
                        spans.add(Span.of(token.start(), token.end(), COLOR_STRING_LITERAL));
                    }
                }
                case IDENTIFIER -> {
                    Frame top = stack.peek();
                    if(top != null && !top.isPath() && top.kind == FrameKind.EXPRESSION_PARENTHESIS) top.expectOperand = false;
                }
                case DOT -> {
                    Frame top = stack.peek();
                    if(top != null && top.isPath()) {
                        resolveAndEmit(top, text, token.start(), spans, FinalUse.MID);
                        top.segmentStart = token.end();
                    }
                }
                default -> {}
            }
        }

        Frame top = stack.peek();
        if(top != null && top.isPath()) resolveAndEmit(top, text, text.length(), spans, FinalUse.MID);

        return spans;
    }

    private static void resolveAndEmit(Frame frame, String text, int resolvePos, List<Span> spans, FinalUse use) {
        int start = frame.segmentStart;
        if (resolvePos <= start) return;
        String raw = text.substring(start, resolvePos);
        String seg = raw.strip();
        if (seg.isEmpty()) return;

        int leadingWs = raw.length() - raw.stripLeading().length();
        int trimStart = start + leadingWs;
        int trimEnd = trimStart + seg.length();

        if (!frame.failed) {
            PlaceholderTreeNode next = (frame.node == null)
                    ? PlaceholderRegistry.getRoot(seg)
                    : frame.node.resolveChild(seg, new ArrayList<>());
            if (next == null) frame.failed = true;
            else {
                frame.node = next;
                if (use == FinalUse.CLOSE && !next.hasResolver()) frame.failed = true;
                else if (use == FinalUse.CALL && !next.hasEval()) frame.failed = true;
            }
        }

        int color = frame.failed ? COLOR_INVALID : depthColor(frame.depth);
        spans.add(Span.of(trimStart, trimEnd, color, frame.failed));
    }
}
