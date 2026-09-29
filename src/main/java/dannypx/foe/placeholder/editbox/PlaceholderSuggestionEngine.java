package dannypx.foe.placeholder.editbox;

import dannypx.foe.placeholder.lexer.PlaceholderTokenizer;
import dannypx.foe.placeholder.registry.PlaceholderRegistry;
import dannypx.foe.placeholder.registry.PlaceholderTreeNode;
import dannypx.foe.placeholder.token.PlaceholderParseException;
import dannypx.foe.placeholder.token.Token;
import dannypx.foe.placeholder.token.TokenType;

import java.util.*;

public class PlaceholderSuggestionEngine {
    private PlaceholderSuggestionEngine() {}

    public record Suggestion(String name, boolean isFunction) {}

    private enum FrameKind { PATH_PERCENT, PATH_LT, EXPRESSION_PARENTHESIS }

    private static final class Frame {
        final FrameKind kind;
        final List<String> committed = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        int currentStart;
        boolean expectOperand;

        Frame(FrameKind kind, int currentStart) {
            this.kind = kind;
            this.currentStart = currentStart;
            this.expectOperand = kind == FrameKind.EXPRESSION_PARENTHESIS;
        }

        boolean isPath() {
            return kind == FrameKind.PATH_PERCENT || kind == FrameKind.PATH_LT;
        }
    }

    public static PlaceholderSuggestionContext computeContext(String text, int caret) {
        if(caret < 0 || caret > text.length()) {
            return PlaceholderSuggestionContext.NONE;
        }

        List<Token> tokens;
        try {
            tokens = new PlaceholderTokenizer(text.substring(0, caret)).tokenize();
        } catch (PlaceholderParseException ignored) {
            return PlaceholderSuggestionContext.NONE;
        }

        Deque<Frame> stack = new ArrayDeque<>();

        for (Token token : tokens) {
            TokenType type = token.type();

            if(type == TokenType.EOF) break;

            switch (type) {
                case PERCENT -> {
                    if(stack.isEmpty()) stack.push(new Frame(FrameKind.PATH_PERCENT, token.end()));
                    else if(stack.size() == 1 && stack.peek().kind == FrameKind.PATH_PERCENT) stack.pop();
                }
                case LT -> {
                    Frame top = stack.peek();
                    if(top != null && top.kind == FrameKind.EXPRESSION_PARENTHESIS) {
                        if(top.expectOperand) stack.push(new Frame(FrameKind.PATH_LT, token.end()));
                        else top.expectOperand = true;
                    }
                }
                case GT -> {
                    Frame top = stack.peek();
                    if(top != null && top.kind == FrameKind.PATH_LT) {
                        stack.pop();
                        Frame parent = stack.peek();
                        if(parent != null && parent.kind == FrameKind.EXPRESSION_PARENTHESIS) parent.expectOperand = false;
                    } else if (top != null && top.kind == FrameKind.EXPRESSION_PARENTHESIS) {
                        top.expectOperand = true;
                    }
                }
                case LPARENTHESIS -> {
                    if(!stack.isEmpty()) stack.push(new Frame(FrameKind.EXPRESSION_PARENTHESIS, token.end()));
                }
                case RPARENTHESIS -> {
                    Frame top = stack.peek();
                    if(top != null && top.kind == FrameKind.EXPRESSION_PARENTHESIS) {
                        stack.pop();
                        Frame parent = stack.peek();
                        if(parent != null && parent.kind == FrameKind.EXPRESSION_PARENTHESIS) parent.expectOperand = false;
                    }
                }
                case COMMA, PLUS, MINUS, STAR, SLASH, ASSIGN, BANG -> {
                    Frame top = stack.peek();
                    if(top != null && top.kind == FrameKind.EXPRESSION_PARENTHESIS) top.expectOperand = true;
                }
                case DOT -> {
                    Frame top = stack.peek();
                    if(top != null && top.isPath()) {
                        top.committed.add(top.current.toString());
                        top.current.setLength(0);
                        top.currentStart = token.end();
                    }
                }
                case IDENTIFIER, NUMBER -> {
                    Frame top = stack.peek();
                    if(top != null) {
                        if(top.isPath()) top.current.append(token.text());
                        else if(top.kind == FrameKind.EXPRESSION_PARENTHESIS) top.expectOperand = false;
                    }
                }
                case STRING -> {
                    Frame top = stack.peek();
                    if(top != null && top.kind == FrameKind.EXPRESSION_PARENTHESIS) top.expectOperand = false;
                }
                case LITERAL, ESCAPED_LITERAL, WHITESPACE -> {
                    Frame top = stack.peek();
                    if(top != null && top.isPath()) top.current.append(token.text());
                }
                default -> {}
            }
        }

        Frame top = stack.peek();
        if(top == null || !top.isPath()) return PlaceholderSuggestionContext.NONE;

        String raw = top.current.toString();
        String prefix = raw.trim();
        int leadingWs = raw.length() - raw.stripLeading().length();
        int replaceStart = top.currentStart + leadingWs;

        if(top.committed.isEmpty()) {
            List<Suggestion> matches = PlaceholderRegistry.getRootNames().stream()
                    .filter(name -> matches(name, prefix))
                    .sorted()
                    .map(name -> new Suggestion(name, PlaceholderRegistry.getRoot(name).hasEval()))
                    .toList();
            return new PlaceholderSuggestionContext(PlaceholderSuggestionContext.Kind.ROOT, matches, replaceStart, caret);
        }

        PlaceholderTreeNode node = PlaceholderRegistry.getRoot(top.committed.getFirst().trim());
        for (int i = 1; i < top.committed.size() && node != null; i++) {
            node = node.resolveChild(top.committed.get(i).trim(), new ArrayList<>());
        }
        if(node == null) return PlaceholderSuggestionContext.NONE;

        List<Suggestion> matches = node.getChildren().entrySet().stream()
                .filter(e -> matches(e.getKey(), prefix))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new Suggestion(e.getKey(), e.getValue().hasEval()))
                .toList();
        return new PlaceholderSuggestionContext(PlaceholderSuggestionContext.Kind.CHILD, matches, replaceStart, caret);
    }

    private static boolean matches(String candidate, String prefix) {
        return prefix.isEmpty() || candidate.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
