package dannypx.foe.handler.logic;

/** Matches pipe-separated alternatives against one lore line. Both inputs must be lowercase. */
final class TooltipSearch {
    private TooltipSearch() {
    }

    static boolean containsAny(String line, String query) {
        if (query.indexOf('|') < 0) {
            return line.contains(query);
        }
        int start = 0;
        while (start < query.length()) {
            int end = query.indexOf('|', start);
            if (end == -1) {
                end = query.length();
            }
            int length = end - start;
            if (length > 0) {
                for (int offset = 0; offset <= line.length() - length; offset++) {
                    if (line.regionMatches(offset, query, start, length)) {
                        return true;
                    }
                }
            }
            start = end + 1;
        }
        return false;
    }
}
