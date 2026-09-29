package dannypx.foe.handler.store;

import dannypx.foe.handler.Handler;
import dannypx.foe.handler.io.DataFileHandler;
import dannypx.foe.handler.io.DataModels;
import dannypx.foe.helper.TextHelper;
import dannypx.foe.type.tuple.Pair;
import java.util.*;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class CustomChatTriggerDataHandler extends Handler {
    private static CustomChatTriggerDataHandler INSTANCE = new CustomChatTriggerDataHandler();

    public static CustomChatTriggerDataHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new CustomChatTriggerDataHandler();
        }
        return INSTANCE;
    }

    //region Fields
    private CustomChatTriggerDataModel customChatTriggerData = new CustomChatTriggerDataModel();
    private boolean needsUpdate = false;

    public CustomChatTriggerDataModel getCustomChatTriggerData() {
        return customChatTriggerData;
    }

    public void setCustomChatTriggerData(CustomChatTriggerDataModel customChatTriggerData) {
        this.customChatTriggerData = customChatTriggerData;
        this.updateCustomChatTriggerData();
    }

    private void updateCustomChatTriggerData() {
        if(needsUpdate) {
            DataFileHandler.instance().saveToFile(DataModels.DataModelType.CUSTOM_CHAT_TRIGGER_DATA);
        }
        this.needsUpdate = false;
    }
    //endregion

    //region Methods
    public void tick() {
        if(customChatTriggerData.uuid == null && minecraft.player != null) {
            customChatTriggerData.uuid = minecraft.player.getUUID();
        } else if(customChatTriggerData.uuid != null && this.needsUpdate) {
            this.updateCustomChatTriggerData();
        } else if(!CustomChatTriggerDataModel.CUSTOM_CHAT_TRIGGER_DATA_MODEL_VERSION.equals(customChatTriggerData.version)) {
            customChatTriggerData.version = CustomChatTriggerDataModel.CUSTOM_CHAT_TRIGGER_DATA_MODEL_VERSION;
            this.updateDefault();
            needsUpdate = true;
        }
    }

    public void init() {
        if(minecraft.player != null) this.setUUID(minecraft.player.getUUID());
    }

    private void setUUID(UUID uuid) {
        this.customChatTriggerData.uuid = uuid;
    }

    public void createNewCustomChatTrigger(String id) {
        customChatTriggerData.chatTriggerList.put(id, new CustomChatTrigger(id));
        needsUpdate = true;
    }

    public void createNewCustomChatTrigger(String id, CustomChatTrigger customChatTrigger) {
        customChatTriggerData.chatTriggerList.put(id, customChatTrigger);
        needsUpdate = true;
    }

    public CustomChatTrigger deleteCustomChatTrigger(String id) {
        needsUpdate = true;
        return customChatTriggerData.chatTriggerList.remove(id);
    }

    public CustomChatTrigger updateChatTrigger(String currentSelectedChatTrigger,
                                  String newName,
                                  String regex,
                                  String notificationToTrigger,
                                  String chatNotificationToTrigger,
                                  String trackerToTrigger,
                                  boolean useChatTrigger
    ) {
        CustomChatTrigger newChatTrigger = customChatTriggerData.chatTriggerList.get(currentSelectedChatTrigger);

        if(!Objects.equals(currentSelectedChatTrigger, newName)) {
            newChatTrigger = deleteCustomChatTrigger(currentSelectedChatTrigger);
            currentSelectedChatTrigger = newName;
        }

        newChatTrigger.name = newName;
        newChatTrigger.regex = regex;
        newChatTrigger.pattern = Pattern.compile(regex);
        newChatTrigger.notificationToTrigger = notificationToTrigger;
        newChatTrigger.chatNotificationToTrigger = chatNotificationToTrigger;
        newChatTrigger.trackerToTrigger = trackerToTrigger;
        newChatTrigger.useChatTrigger = useChatTrigger;

        customChatTriggerData.chatTriggerList.put(currentSelectedChatTrigger, newChatTrigger);
        needsUpdate = true;

        return newChatTrigger;
    }

    public void updateDefault() {
        CustomChatTriggerDataModel.defaultChatTriggers.forEach((key, timer) -> {
            customChatTriggerData.chatTriggerList.putIfAbsent(key, timer);
        });
    }

    public void fixDefault() {
        customChatTriggerData.chatTriggerList.putAll(CustomChatTriggerDataModel.defaultChatTriggers);
        needsUpdate = true;
    }

    public void resetChatTriggers() {
        customChatTriggerData.chatTriggerList = new HashMap<>(CustomChatTriggerDataModel.defaultChatTriggers);

        needsUpdate = true;
    }
    //endregion

    //region Model
    public static class CustomChatTriggerDataModel extends DataModels.DataModel {
        private static final String CUSTOM_CHAT_TRIGGER_DATA_MODEL_VERSION = "0.3";

        private static final Map<String, CustomChatTrigger> defaultChatTriggers = Map.ofEntries(
                Map.entry("Contest Type", new CustomChatTrigger(
                        "Contest Type",
                        "^Type:.*",
                        "",
                        "",
                        "",
                        true
                )),
                Map.entry("Contest Location", new CustomChatTrigger(
                        "Contest Location",
                        "^Location:.*",
                        "",
                        "",
                        "",
                        true
                )),
                Map.entry("Contest Level", new CustomChatTrigger(
                        "Contest Level",
                        "^Level:.*",
                        "",
                        "",
                        "",
                        true
                )),
                Map.entry("Contest 1st", new CustomChatTrigger(
                        "Contest 1st",
                        "^\uF060.*",
                        "",
                        "",
                        "",
                        true
                )),
                Map.entry("Contest 2nd", new CustomChatTrigger(
                        "Contest 2nd",
                        "^\uF061.*",
                        "",
                        "",
                        "",
                        true
                )),
                Map.entry("Contest 3rd", new CustomChatTrigger(
                        "Contest 3rd",
                        "^\uF062.*",
                        "",
                        "",
                        "",
                        true
                )),
                Map.entry("Contest Placement", new CustomChatTrigger(
                        "Contest Placement",
                        "^You →.*",
                        "",
                        "",
                        "",
                        true
                )),
                Map.entry("Fabled Start Chat", new CustomChatTrigger(
                        "Fabled Start Chat",
                        "^Visit the (.+) to$",
                        "",
                        "",
                        "FabledEvent.SetTrue",
                        true
                )),
                Map.entry("Fabled Start events", new CustomChatTrigger(
                        "Fabled Start events",
                        "^FABLED » A Fabled Event is active at (.+)$",
                        "",
                        "",
                        "FabledEvent.SetTrue",
                        true
                )),
                Map.entry("Fabled End Chat", new CustomChatTrigger(
                        "Fabled End Chat",
                        "^Caught by (.+)$",
                        "",
                        "",
                        "FabledEvent.SetFalse",
                        true
                ))
        );

        //Name Chat Trigger, Chat Trigger
        public Map<String, CustomChatTrigger> chatTriggerList = new HashMap<>(defaultChatTriggers);

        public CustomChatTriggerDataModel() {
            super(CUSTOM_CHAT_TRIGGER_DATA_MODEL_VERSION, null);
        }
    }
    //endregion

    //region Notification Object
    public static class CustomChatTrigger {
        private String name;
        private String regex;
        private Pattern pattern;
        private String notificationToTrigger;
        private String chatNotificationToTrigger;
        private String trackerToTrigger;
        private boolean useChatTrigger;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRegex() {
            return regex != null ? regex : "";
        }

        public Pattern getPattern() {
            return pattern != null ? pattern : Pattern.compile("");
        }

        public String getNotificationToTrigger() {
            return notificationToTrigger != null ? notificationToTrigger : "";
        }

        public String getChatNotificationToTrigger() {
            return chatNotificationToTrigger != null ? chatNotificationToTrigger : "";
        }

        public String getTrackerToTrigger() {
            return trackerToTrigger != null ? trackerToTrigger : "";
        }

        public boolean isUseChatTrigger() {
            return useChatTrigger;
        }

        public CustomChatTrigger(String name,
                                 String regex,
                                 String notificationToTrigger,
                                 String chatNotificationToTrigger,
                                 String trackerToTrigger,
                                 boolean useChatTrigger
        ) {
            this.name = name;
            this.regex = regex;
            this.pattern = Pattern.compile(regex);
            this.notificationToTrigger = notificationToTrigger;
            this.chatNotificationToTrigger = chatNotificationToTrigger;
            this.trackerToTrigger = trackerToTrigger;
            this.useChatTrigger = useChatTrigger;
        }

        public CustomChatTrigger(String name) {
            this.name = name;
            this.regex = "";
            this.pattern = Pattern.compile("");
            this.notificationToTrigger = "";
            this.chatNotificationToTrigger = "";
            this.trackerToTrigger = "";
            this.useChatTrigger = true;
        }
    }
    //endregion

    //region Dev
    /// Field, Pair<Value, Tooltip>
    protected Map<String, Pair<MutableComponent, MutableComponent>> _getFields() {
        return Map.of(
                "customChatTriggerData", Pair.of(Component.literal("[customChatTriggerData]"), TextHelper.literal(getCustomChatTriggerData()))
        );
    }
    //endregion
}
