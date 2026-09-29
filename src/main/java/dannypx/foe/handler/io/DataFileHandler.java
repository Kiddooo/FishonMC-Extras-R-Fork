package dannypx.foe.handler.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dannypx.foe.FishOnMCExtras;
import dannypx.foe.handler.Handler;
import dannypx.foe.handler.logic.LoadingHandler;
import dannypx.foe.handler.logic.LoggerHandler;
import dannypx.foe.handler.logic.UpdateHandler;
import dannypx.foe.handler.store.*;
import dannypx.foe.placeholder.registry.PlaceholderRegistry;
import dannypx.foe.type.custom_value.TrackerValue;
import dannypx.foe.type.tuple.Pair;
import dannypx.foe.type.type_adapter.*;
import dannypx.foe.type.version.Version;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class DataFileHandler extends Handler {
    private static DataFileHandler INSTANCE = new DataFileHandler();

    public static DataFileHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new DataFileHandler();
        }
        return INSTANCE;
    }

    //region Fields
    private final Path DATA_FOLDER = Path.of("data");
    private boolean isDataLoaded = false;

    public boolean isDataLoaded() {
        return isDataLoaded;
    }
    //endregion

    //region Methods
    public void tick() {
        ProfileDataHandler.instance().tick();
        StatsDataHandler.instance().tick();
        ConstantDataHandler.instance().tick();
        QuestDataHandler.instance().tick();
        CrewDataHandler.instance().tick();
        CustomHudDataHandler.instance().tick();
        CustomHudIconDataHandler.instance().tick();
        CustomButtonDataHandler.instance().tick();
        CustomNotificationDataHandler.instance().tick();
        CustomChatNotificationDataHandler.instance().tick();
        CustomChatTriggerDataHandler.instance().tick();
        CustomEventTriggerDataHandler.instance().tick();
        CustomTimerDataHandler.instance().tick();
        CustomTrackerDataHandler.instance().tick();
        CustomSnippetDataHandler.instance().tick();
    }

    public void init() {
        for (DataModels.DataModelType value : DataModels.DataModelType.values()) {
            this.loadDataToMemory(value);
        }
        UpdateHandler.checkUpdate();
    }

    private boolean loadDataToMemory(DataModels.DataModelType dataModelType) {
        DataModels.DataModel data = this.getData(dataModelType);
        try {
            Path configDir = getUserConfigDir(data.uuid);
            Files.createDirectories(configDir);
            Path filePath = configDir.resolve(dataModelType.FILENAME + ".json");
            if(!checkIfFileExist(filePath)) {
                Files.createFile(filePath);
                this.isDataLoaded = true;
                return this.saveToFile(dataModelType);
            }
            String jsonFromFile = Files.readString(filePath);
            setData(dataModelType, jsonFromFile);
            this.isDataLoaded = true;

        } catch (IOException e) {
            LoggerHandler.error(e);
            LoadingHandler.instance().setError(true);
        }
        return false;
    }

    public boolean saveToFile(DataModels.DataModelType dataModelType) {
        DataModels.DataModel data = this.getData(dataModelType);
        try {
            Path configDir = getUserConfigDir(data.uuid);
            Files.createDirectories(configDir);
            Path filePath = configDir.resolve(dataModelType.FILENAME + ".json");
            String resultJson = dataModelToJson(data);
            Files.writeString(filePath, resultJson);

            LoggerHandler._debug("Updating file: " + dataModelType.FILENAME + ".json");
        } catch (IOException e) {
            LoggerHandler.error(e);
            LoadingHandler.instance().setError(true);
        }
        return true;
    }

    public boolean saveSchemaToFile() {
        try {
            Path exportDir = getExportDevConfigDir();
            Files.createDirectories(exportDir);
            Path listPath = exportDir.resolve("placeholder-list-" + Version.of(FishOnMCExtras.VERSION).get() +  ".json");
            Path schemaPath = exportDir.resolve("placeholder-schema-" + Version.of(FishOnMCExtras.VERSION).get() +  ".json");
            Files.writeString(listPath, PlaceholderRegistry.toJsonPathListString());
            Files.writeString(schemaPath, PlaceholderRegistry.toJsonSchemaString());

            LoggerHandler._debug("Exported placeholder schema and list");
        } catch (IOException e) {
            LoggerHandler.error(e);
            LoadingHandler.instance().setError(true);
        }
        return true;
    }

    public boolean saveChangelogToFile(String json) {
        try {
            Path exportDir = getExportDevConfigDir();
            Files.createDirectories(exportDir);
            Path changelogPath = exportDir.resolve("changelog.json");
            Files.writeString(changelogPath, json);
        } catch (IOException e) {
            LoggerHandler.error(e);
        }
        return false;
    }

    public String getChangelogFile() {
        try {
            Path exportDir = getExportDevConfigDir();
            Files.createDirectories(exportDir);
            Path changelogPath = exportDir.resolve("changelog.json");
            if(!checkIfFileExist(changelogPath)) {
                return null;
            }
            return Files.readString(changelogPath);

        } catch (IOException e) {
            LoggerHandler.error(e);
        }
        return null;
    }

    private Path getUserConfigDir(UUID uuid) {
        return getConfigDir()
                .resolve(DATA_FOLDER)
                .resolve(uuid.toString());
    }

    private Path getExportDevConfigDir() {
        return getConfigDir()
                .resolve("export");
    }

    private Path getConfigDir() {
        return FabricLoader
                .getInstance()
                .getConfigDir()
                .resolve(FishOnMCExtras.MOD_ID);
    }

    private boolean checkIfFileExist(Path filePath) {
        return Files.exists(filePath);
    }

    private String dataModelToJson(DataModels.DataModel dataModel) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .registerTypeAdapter(ItemStack.class, new ItemStackAdapter())
                .registerTypeAdapter(Component.class, new ComponentAdapter())
                .registerTypeAdapter(Pattern.class, new PatternAdapter())
                .registerTypeAdapter(CustomTimerDataHandler.CustomTimer.class, new CustomTimerAdapter())
                .registerTypeAdapter(TrackerValue.class, new TrackerValueAdapter())
                .create();
        return gson.toJson(dataModel);
    }

    private DataModels.DataModel getData(DataModels.DataModelType dataModelType) {
        return switch (dataModelType) {
            case PROFILE_DATA -> ProfileDataHandler.instance().getProfileData();
            case STATS_DATA -> StatsDataHandler.instance().getStatsData();
            case CONSTANT_DATA -> ConstantDataHandler.instance().getConstantData();
            case QUEST_DATA -> QuestDataHandler.instance().getQuestData();
            case CREW_DATA -> CrewDataHandler.instance().getCrewData();
            case CUSTOM_HUD_DATA -> CustomHudDataHandler.instance().getCustomHudData();
            case CUSTOM_HUD_ICON_DATA -> CustomHudIconDataHandler.instance().getCustomHudIconData();
            case CUSTOM_BUTTON_DATA -> CustomButtonDataHandler.instance().getCustomButtonData();
            case CUSTOM_NOTIFICATION_DATA -> CustomNotificationDataHandler.instance().getCustomNotificationData();
            case CUSTOM_CHAT_TRIGGER_DATA -> CustomChatTriggerDataHandler.instance().getCustomChatTriggerData();
            case CUSTOM_TIMER_DATA -> CustomTimerDataHandler.instance().getCustomTimerData();
            case CUSTOM_CHAT_NOTIFICATION_DATA -> CustomChatNotificationDataHandler.instance().getCustomChatNotificationData();
            case CUSTOM_EVENT_TRIGGER_DATA -> CustomEventTriggerDataHandler.instance().getCustomEventTriggerData();
            case CUSTOM_TRACKER_DATA -> CustomTrackerDataHandler.instance().getCustomTrackerData();
            case CUSTOM_SNIPPET_DATA -> CustomSnippetDataHandler.instance().getCustomSnippetData();
        };
    }

    private void setData(DataModels.DataModelType dataModelType, String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(ItemStack.class, new ItemStackAdapter())
                .registerTypeAdapter(Component.class, new ComponentAdapter())
                .registerTypeAdapter(Pattern.class, new PatternAdapter())
                .registerTypeAdapter(CustomTimerDataHandler.CustomTimer.class, new CustomTimerAdapter())
                .registerTypeAdapter(TrackerValue.class, new TrackerValueAdapter())
                .create();

        LoggerHandler._debug("Setting data from: " + dataModelType.FILENAME + ".json");

        switch (dataModelType) {
            case PROFILE_DATA ->
                    ProfileDataHandler.instance().setProfileData(gson.fromJson(json, ProfileDataHandler.ProfileDataModel.class));
            case STATS_DATA ->
                    StatsDataHandler.instance().setStatsData(gson.fromJson(json, StatsDataHandler.StatsDataModel.class));
            case CONSTANT_DATA ->
                    ConstantDataHandler.instance().setConstantData(gson.fromJson(json, ConstantDataHandler.ConstantDataModel.class));
            case QUEST_DATA ->
                    QuestDataHandler.instance().setQuestData(gson.fromJson(json, QuestDataHandler.QuestDataModel.class));
            case CREW_DATA ->
                    CrewDataHandler.instance().setCrewData(gson.fromJson(json, CrewDataHandler.CrewDataModel.class));
            case CUSTOM_HUD_DATA ->
                    CustomHudDataHandler.instance().setCustomHudData(gson.fromJson(json, CustomHudDataHandler.CustomHudDataModel.class));
            case CUSTOM_HUD_ICON_DATA ->
                    CustomHudIconDataHandler.instance().setCustomHudIconData(gson.fromJson(json, CustomHudIconDataHandler.CustomHudIconDataModel.class));
            case CUSTOM_BUTTON_DATA ->
                    CustomButtonDataHandler.instance().setCustomButtonData(gson.fromJson(json, CustomButtonDataHandler.CustomButtonDataModel.class));
            case CUSTOM_NOTIFICATION_DATA ->
                    CustomNotificationDataHandler.instance().setCustomNotificationData(gson.fromJson(json, CustomNotificationDataHandler.CustomNotificationDataModel.class));
            case CUSTOM_CHAT_TRIGGER_DATA ->
                    CustomChatTriggerDataHandler.instance().setCustomChatTriggerData(gson.fromJson(json, CustomChatTriggerDataHandler.CustomChatTriggerDataModel.class));
            case CUSTOM_TIMER_DATA ->
                    CustomTimerDataHandler.instance().setCustomTimerData(gson.fromJson(json, CustomTimerDataHandler.CustomTimerDataModel.class));
            case CUSTOM_CHAT_NOTIFICATION_DATA ->
                    CustomChatNotificationDataHandler.instance().setCustomChatNotificationData(gson.fromJson(json, CustomChatNotificationDataHandler.CustomChatNotificationDataModel.class));
            case CUSTOM_EVENT_TRIGGER_DATA ->
                    CustomEventTriggerDataHandler.instance().setCustomEventTriggerData(gson.fromJson(json, CustomEventTriggerDataHandler.CustomEventTriggerDataModel.class));
            case CUSTOM_TRACKER_DATA ->
                    CustomTrackerDataHandler.instance().setCustomTrackerData(gson.fromJson(json, CustomTrackerDataHandler.CustomTrackerDataModel.class));
            case CUSTOM_SNIPPET_DATA ->
                    CustomSnippetDataHandler.instance().setCustomSnippetData(gson.fromJson(json, CustomSnippetDataHandler.CustomSnippetDataModel.class));
        }
    }
    //endregion

    //region Dev
    /// Field, Pair<Value, Tooltip>
    protected Map<String, Pair<MutableComponent, MutableComponent>> _getFields() {
        return Map.of(
                "isDataLoaded", Pair.of(Component.literal(Boolean.toString(isDataLoaded())), Component.empty())
        );
    }
    //endregion
}
