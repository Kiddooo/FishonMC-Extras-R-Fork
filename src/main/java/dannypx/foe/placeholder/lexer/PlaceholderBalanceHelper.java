package dannypx.foe.placeholder.lexer;

import dannypx.foe.placeholder.token.PlaceholderParseException;

public class PlaceholderBalanceHelper {
    public static void checkBalanced(String source) {
        int count = 0;
        boolean inQuotes = false;
        int i = 0;

        while(i < source.length()) {
            char c = source.charAt(i);

            if(c == '\\' && i + 1 < source.length()) {
                i += 2;
                continue;
            }

            if(c == '"') {
                inQuotes = !inQuotes;
                i++;
                continue;
            }

            if (c == '%' && !inQuotes) {
                count++;
            }
            i++;
        }

        if(count % 2 != 0) {
            throw new PlaceholderParseException(
                    "Unbalanced number of '%' characters in placeholder string (found " + count + ")"
            );
        }
    }
}
