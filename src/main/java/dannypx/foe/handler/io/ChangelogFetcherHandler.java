package dannypx.foe.handler.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dannypx.foe.FishOnMCExtras;
import dannypx.foe.handler.Handler;
import dannypx.foe.handler.logic.LoggerHandler;
import dannypx.foe.type.tuple.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChangelogFetcherHandler extends Handler {
    private static ChangelogFetcherHandler INSTANCE = new ChangelogFetcherHandler();

    public static ChangelogFetcherHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new ChangelogFetcherHandler();
        }
        return INSTANCE;
    }

    //region Fields
    private boolean needsUpdate = false;

    public boolean isNeedsUpdate() {
        return needsUpdate;
    }

    public void setNeedsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }

    private static final String API_URL = "https://api.modrinth.com/v2/project/" + FishOnMCExtras.SLUG + "/version";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private volatile List<ChangelogEntry> entries;
    private volatile String error;
    private volatile boolean fetching = false;

    public interface Listener {
        void onChangelogUpdated();
    }

    public record ChangelogEntry(String versionNumber, String name, String datePublished, String changelog) { }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public List<ChangelogEntry> getEntries() {
        return this.entries;
    }

    public String getError() {
        return this.error;
    }

    public boolean isFetching() {
        return this.fetching;
    }

    public boolean hasData() {
        return this.entries != null || this.error != null;
    }
    //endregion

    //region Methods
    public boolean loadFromCache(String json) {
        if (json == null || json.isBlank()) return false;

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String cachedVersion = getAsStringOrNull(root, "modVersion");
            if (!FishOnMCExtras.VERSION.equals(cachedVersion)) return false;

            List<ChangelogEntry> parsed = new ArrayList<>();
            JsonArray array = root.getAsJsonArray("entries");
            if (array != null) {
                for (JsonElement element : array) {
                    JsonObject obj = element.getAsJsonObject();
                    parsed.add(new ChangelogEntry(
                            getAsStringOrNull(obj, "versionNumber"),
                            getAsStringOrNull(obj, "name"),
                            getAsStringOrNull(obj, "datePublished"),
                            getAsStringOrNull(obj, "changelog")
                    ));
                }
            }

            this.entries = parsed;
            this.error = null;
            this.notifyListeners();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String toCacheJson() {
        if (this.entries == null) return null;

        JsonObject root = new JsonObject();
        root.addProperty("modVersion", FishOnMCExtras.VERSION);

        JsonArray array = new JsonArray();
        for (ChangelogEntry entry : this.entries) {
            JsonObject obj = new JsonObject();
            obj.addProperty("versionNumber", entry.versionNumber());
            obj.addProperty("name", entry.name());
            obj.addProperty("datePublished", entry.datePublished());
            obj.addProperty("changelog", entry.changelog());
            array.add(obj);
        }
        root.add("entries", array);

        return root.toString();
    }

    public void fetch(boolean forceRefresh) {
        if (this.fetching) return;
        if (!forceRefresh && this.hasData()) return;

        if (!forceRefresh && this.loadFromCache(DataFileHandler.instance().getChangelogFile())) {
            LoggerHandler._debug("Load changelog from cache");
            return;
        }

        LoggerHandler.info("Fetch changelog from Modrinth");

        this.fetching = true;
        this.error = null;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("User-Agent", "fishonmcextras/changelog-fetcher")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        this.finishWithError("Modrinth returned HTTP " + response.statusCode());
                        return;
                    }
                    try {
                        List<ChangelogEntry> parsed = parseChangelog(response.body());
                        this.finishWithSuccess(parsed);
                    } catch (Exception e) {
                        this.finishWithError("Failed to parse response: " + e.getMessage());
                    }
                })
                .exceptionally(throwable -> {
                    this.finishWithError("Request failed: " + throwable.getMessage());
                    return null;
                });
    }

    private void finishWithSuccess(List<ChangelogEntry> parsed) {
        Minecraft.getInstance().execute(() -> {
            this.entries = parsed;
            this.error = null;
            this.fetching = false;

            String json = toCacheJson();
            if (json != null) {
                DataFileHandler.instance().saveChangelogToFile(json);
                this.needsUpdate = true;
            }

            this.notifyListeners();
        });
    }

    private void finishWithError(String message) {
        Minecraft.getInstance().execute(() -> {
            this.error = message;
            this.fetching = false;
            LoggerHandler.error(message);
            this.notifyListeners();
        });
    }

    private void notifyListeners() {
        for (Listener listener : this.listeners) {
            listener.onChangelogUpdated();
        }
    }

    private List<ChangelogEntry> parseChangelog(String json) {
        String currentVersion = FishOnMCExtras.getMinecraftVersion();

        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        List<ChangelogEntry> result = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            String versionNumber = getAsStringOrNull(obj, "version_number");
            String name = getAsStringOrNull(obj, "name");
            String changelog = getAsStringOrNull(obj, "changelog");
            String datePublished = getAsStringOrNull(obj, "date_published");
            if (changelog == null || changelog.isBlank()) {
                continue;
            }

            List<String> gameVersions = new ArrayList<>();
            JsonElement gameVersionElement = obj.get("game_versions");
            if (gameVersionElement != null && gameVersionElement.isJsonArray()) {
                for (JsonElement gv : gameVersionElement.getAsJsonArray()) {
                    gameVersions.add(gv.getAsString());
                }
            }

            if (currentVersion != null && !gameVersions.isEmpty() && !gameVersions.contains(currentVersion)) {
                continue;
            }

            result.add(new ChangelogEntry(
                    versionNumber != null ? versionNumber : "unknown",
                    name != null ? name : "",
                    datePublished != null ? datePublished.substring(0, Math.min(10, datePublished.length())) : "",
                    changelog
            ));
        }
        return result;
    }

    private static String getAsStringOrNull(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el == null || el.isJsonNull()) ? null : el.getAsString();
    }
    //endregion

    //region Dev

    /// Field, Pair<Value, Tooltip>
    @Override
    protected Map<String, Pair<MutableComponent, MutableComponent>> _getFields() {
        return Map.of(
                "key", Pair.of(Component.literal("value"), Component.empty())
        );
    }
    //endregion
}
