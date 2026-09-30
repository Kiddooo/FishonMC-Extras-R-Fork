package dannypx.foe.handler.logic;

import java.util.Locale;
import java.util.function.Predicate;

/** Parses item-name searches: spaces are AND, pipes are OR, parentheses group terms. */
final class NameSearch {
    private final String input;
    private int index;
    private boolean invalid;

    private NameSearch(String input) {
        this.input = input.toLowerCase(Locale.US);
    }

    static Predicate<String> parse(String input) {
        NameSearch parser = new NameSearch(input);
        Predicate<String> query = parser.parseOr();
        parser.skipSpaces();
        return parser.invalid || parser.index != parser.input.length() ? null : query;
    }

    private Predicate<String> parseOr() {
        Predicate<String> result = parseAnd();
        while (!invalid) {
            skipSpaces();
            if (index == input.length() || input.charAt(index) != '|') {
                break;
            }
            index++;
            Predicate<String> right = parseAnd();
            if (right != null) {
                if (result == null) {
                    result = right;
                } else {
                    Predicate<String> left = result;
                    result = name -> left.test(name) || right.test(name);
                }
            }
        }
        return result;
    }

    private Predicate<String> parseAnd() {
        Predicate<String> result = parsePrimary();
        while (!invalid) {
            skipSpaces();
            if (index == input.length() || input.charAt(index) == ')' || input.charAt(index) == '|') {
                break;
            }
            Predicate<String> right = parsePrimary();
            if (right == null) {
                invalid = true;
                break;
            }
            if (result == null) {
                result = right;
            } else {
                Predicate<String> left = result;
                result = name -> left.test(name) && right.test(name);
            }
        }
        return result;
    }

    private Predicate<String> parsePrimary() {
        skipSpaces();
        if (index == input.length() || input.charAt(index) == ')' || input.charAt(index) == '|') {
            return null;
        }
        if (input.charAt(index) == '(') {
            index++;
            Predicate<String> group = parseOr();
            skipSpaces();
            if (group == null || index == input.length() || input.charAt(index) != ')') {
                invalid = true;
                return null;
            }
            index++;
            return group;
        }
        int start = index;
        while (index < input.length()
                && !Character.isWhitespace(input.charAt(index))
                && input.charAt(index) != '|'
                && input.charAt(index) != '('
                && input.charAt(index) != ')') {
            index++;
        }
        String term = input.substring(start, index);
        return name -> name.contains(term);
    }

    private void skipSpaces() {
        while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
            index++;
        }
    }
}
