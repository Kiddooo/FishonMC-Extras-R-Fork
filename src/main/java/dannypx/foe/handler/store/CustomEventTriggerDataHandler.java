package dannypx.foe.handler.store;

import dannypx.foe.handler.Handler;
import dannypx.foe.handler.io.DataFileHandler;
import dannypx.foe.handler.io.DataModels;
import dannypx.foe.helper.TextHelper;
import dannypx.foe.type.event.EventTrigger;
import dannypx.foe.type.tuple.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CustomEventTriggerDataHandler extends Handler {
    private static CustomEventTriggerDataHandler INSTANCE = new CustomEventTriggerDataHandler();

    public static CustomEventTriggerDataHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new CustomEventTriggerDataHandler();
        }
        return INSTANCE;
    }

    //region Fields
    private CustomEventTriggerDataModel customEventTriggerData = new CustomEventTriggerDataModel();
    private boolean needsUpdate = false;

    public CustomEventTriggerDataModel getCustomEventTriggerData() {
        return customEventTriggerData;
    }

    public void setCustomEventTriggerData(CustomEventTriggerDataModel customEventTriggerData) {
        this.customEventTriggerData = customEventTriggerData;
        this.updateCustomEventTriggerData();
    }

    private void updateCustomEventTriggerData() {
        if(needsUpdate) {
            DataFileHandler.instance().saveToFile(DataModels.DataModelType.CUSTOM_EVENT_TRIGGER_DATA);
        }
        this.needsUpdate = false;
    }
    //endregion

    //region Methods
    public void tick() {
        if(customEventTriggerData.uuid == null && minecraft.player != null) {
            customEventTriggerData.uuid = minecraft.player.getUUID();
        } else if(customEventTriggerData.uuid != null && this.needsUpdate) {
            this.updateCustomEventTriggerData();
        } else if(!CustomEventTriggerDataModel.CUSTOM_EVENT_TRIGGER_DATA_MODEL_VERSION.equals(customEventTriggerData.version)) {
            customEventTriggerData.version = CustomEventTriggerDataModel.CUSTOM_EVENT_TRIGGER_DATA_MODEL_VERSION;
            this.updateDefault();
            needsUpdate = true;
        }
    }

    public void init() {
        if(minecraft.player != null) this.setUUID(minecraft.player.getUUID());
    }

    private void setUUID(UUID uuid) {
        this.customEventTriggerData.uuid = uuid;
    }

    public void createNewCustomEventTrigger(String id) {
        customEventTriggerData.eventTriggerList.put(id, new CustomEventTrigger(id));
        needsUpdate = true;
    }

    public void createNewCustomEventTrigger(String id, CustomEventTrigger customEventTrigger) {
        customEventTriggerData.eventTriggerList.put(id, customEventTrigger);
        needsUpdate = true;
    }

    public CustomEventTrigger deleteCustomEventTrigger(String id) {
        needsUpdate = true;
        return customEventTriggerData.eventTriggerList.remove(id);
    }

    public CustomEventTrigger updateEventTrigger(String currentSelectedEventTrigger,
                                   String newName,
                                   EventTrigger eventTrigger,
                                   String notificationToTrigger,
                                   String chatNotificationToTrigger,
                                   String trackerToTrigger,
                                   boolean useEventTrigger
    ) {
        CustomEventTrigger newEventTrigger = customEventTriggerData.eventTriggerList.get(currentSelectedEventTrigger);

        if(!Objects.equals(currentSelectedEventTrigger, newName)) {
            newEventTrigger = deleteCustomEventTrigger(currentSelectedEventTrigger);
            currentSelectedEventTrigger = newName;
        }

        newEventTrigger.name = newName;
        newEventTrigger.event = eventTrigger;
        newEventTrigger.notificationToTrigger = notificationToTrigger;
        newEventTrigger.chatNotificationToTrigger = chatNotificationToTrigger;
        newEventTrigger.trackerToTrigger = trackerToTrigger;
        newEventTrigger.useEventTrigger = useEventTrigger;

        customEventTriggerData.eventTriggerList.put(currentSelectedEventTrigger, newEventTrigger);
        needsUpdate = true;

        return newEventTrigger;
    }

    public void updateDefault() {
        CustomEventTriggerDataModel.defaultEventTriggers.forEach((key, timer) -> {
            customEventTriggerData.eventTriggerList.putIfAbsent(key, timer);
        });
    }

    public void fixDefault() {
        customEventTriggerData.eventTriggerList.putAll(CustomEventTriggerDataModel.defaultEventTriggers);
        needsUpdate = true;
    }

    public void resetEventTrigger() {
        customEventTriggerData.eventTriggerList = new HashMap<>(CustomEventTriggerDataModel.defaultEventTriggers);

        needsUpdate = true;
    }
    //endregion

    //region Model
    public static class CustomEventTriggerDataModel extends DataModels.DataModel {
        private static final String CUSTOM_EVENT_TRIGGER_DATA_MODEL_VERSION = "0.2";

        private static final Map<String, CustomEventTrigger> defaultEventTriggers = Map.ofEntries(
                Map.entry("Variant On Catch Trigger", new CustomEventTrigger(
                        "Variant On Catch Trigger",
                        EventTrigger.ON_CATCH,
                        "",
                        "Variant Notification",
                        "",
                        true
                )),
                Map.entry("Fabled On Catch Add", new CustomEventTrigger(
                        "Fabled On Catch Add",
                        EventTrigger.ON_CATCH,
                        "",
                        "",
                        "FabledDrystreak.Add",
                        true
                )),
                Map.entry("Fabled On Catch Set", new CustomEventTrigger(
                        "Fabled On Catch Set",
                        EventTrigger.ON_CATCH,
                        "",
                        "",
                        "FabledDrystreak.Set",
                        true
                ))
        );

        //Name Notification, Notification
        public Map<String, CustomEventTrigger> eventTriggerList = new HashMap<>(defaultEventTriggers);

        public CustomEventTriggerDataModel() {
            super(CUSTOM_EVENT_TRIGGER_DATA_MODEL_VERSION, null);
        }
    }
    //endregion

    //region Event Trigger Object
    public static class CustomEventTrigger {
        private String name;
        private EventTrigger event;
        private String notificationToTrigger;
        private String chatNotificationToTrigger;
        private String trackerToTrigger;
        private boolean useEventTrigger;

        public String getName() {
            return name != null ? name : "";
        }

        public void setName(String name) {
            this.name = name;
        }

        public EventTrigger getEvent() {
            return event != null ? event : EventTrigger.DEFAULT;
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

        public boolean isUseEventTrigger() {
            return useEventTrigger;
        }

        public CustomEventTrigger(String name,
                                  EventTrigger event,
                                  String notificationToTrigger,
                                  String chatNotificationToTrigger,
                                  String trackerToTrigger,
                                  boolean useEventTrigger
        ) {
            this.name = name;
            this.event = event;
            this.notificationToTrigger = notificationToTrigger;
            this.chatNotificationToTrigger = chatNotificationToTrigger;
            this.trackerToTrigger = trackerToTrigger;
            this.useEventTrigger = useEventTrigger;
        }

        public CustomEventTrigger(String name) {
            this.name = name;
            this.event = EventTrigger.DEFAULT;
            this.notificationToTrigger = "";
            this.chatNotificationToTrigger = "";
            this.trackerToTrigger = "";
            this.useEventTrigger = true;
        }
    }
    //endregion

    //region Dev
    /// Field, Pair<Value, Tooltip>
    protected Map<String, Pair<MutableComponent, MutableComponent>> _getFields() {
        return Map.of(
                "customEventTriggerData", Pair.of(Component.literal("[customEventTriggerData]"), TextHelper.literal(getCustomEventTriggerData()))
        );
    }
    //endregion
}
