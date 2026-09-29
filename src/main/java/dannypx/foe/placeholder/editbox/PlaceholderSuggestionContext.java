package dannypx.foe.placeholder.editbox;

import java.util.List;

public record PlaceholderSuggestionContext(
        Kind kind,
        List<PlaceholderSuggestionEngine.Suggestion> suggestions,
        int replaceStart,
        int replaceEnd

) {
    public enum Kind {
        NONE, ROOT, CHILD
    }

    public static final PlaceholderSuggestionContext NONE = new PlaceholderSuggestionContext(Kind.NONE, List.of(), -1, -1);
}
