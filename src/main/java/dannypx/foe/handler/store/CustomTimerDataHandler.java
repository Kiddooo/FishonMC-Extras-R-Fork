package dannypx.foe.handler.store;

import dannypx.foe.handler.Handler;
import dannypx.foe.handler.io.DataFileHandler;
import dannypx.foe.handler.io.DataModels;
import dannypx.foe.helper.TextHelper;
import dannypx.foe.type.tuple.Pair;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class CustomTimerDataHandler extends Handler {
    private static CustomTimerDataHandler INSTANCE = new CustomTimerDataHandler();

    public static CustomTimerDataHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new CustomTimerDataHandler();
        }
        return INSTANCE;
    }

    //region Fields
    private CustomTimerDataModel customTimerData = new CustomTimerDataModel();
    private boolean needsUpdate = false;

    public CustomTimerDataModel getCustomTimerData() {
        return customTimerData;
    }

    public void setCustomTimerData(CustomTimerDataModel customTimerData) {
        this.customTimerData = customTimerData;
        this.updateCustomTimerData();
    }

    private void updateCustomTimerData() {
        if(needsUpdate) {
            DataFileHandler.instance().saveToFile(DataModels.DataModelType.CUSTOM_TIMER_DATA);
        }
        this.needsUpdate = false;
    }


    //endregion

    //region Methods
    public void tick() {
        if(customTimerData.uuid == null && minecraft.player != null) {
            customTimerData.uuid = minecraft.player.getUUID();
        } else if(customTimerData.uuid != null && this.needsUpdate) {
            this.updateCustomTimerData();
        } else if(!CustomTimerDataModel.CUSTOM_TIMER_DATA_MODEL_VERSION.equals(customTimerData.version)) {
            customTimerData.version = CustomTimerDataModel.CUSTOM_TIMER_DATA_MODEL_VERSION;
            this.updateDefault();
            needsUpdate = true;
        }
    }

    public void init() {
        if(minecraft.player != null) this.setUUID(minecraft.player.getUUID());
    }

    private void setUUID(UUID uuid) {
        this.customTimerData.uuid = uuid;
    }

    public void createNewCustomTimer(String id) {
        customTimerData.timerList.put(id, new CustomTimer(id));
        needsUpdate = true;
    }

    public void createNewCustomTimer(String id, CustomTimer customTimer) {
        customTimerData.timerList.put(id, customTimer);
        needsUpdate = true;
    }

    public CustomTimer deleteCustomTimer(String id) {
        needsUpdate = true;
        return customTimerData.timerList.remove(id);
    }

    public CustomTimer updateTimer(String currentSelectedTimer,
                            String newName,
                            int timer,
                            int offset,
                            String notificationToTrigger,
                            String chatNotificationToTrigger,
                            String trackerToTrigger,
                            String cleanUpChatTrigger,
                            boolean useTimer,
                            boolean isPeriod
    ) {
        return updateTimer(currentSelectedTimer,
                newName,
                timer,
                0,
                offset,
                notificationToTrigger,
                "",
                chatNotificationToTrigger,
                "",
                trackerToTrigger,
                "",
                cleanUpChatTrigger,
                useTimer,
                isPeriod
        );
    }

    public CustomTimer updateTimer(String currentSelectedTimer,
                            String newName,
                            int timer,
                            int offTimer,
                            int offset,
                            String notificationToTrigger,
                            String notificationToTriggerEnd,
                            String chatNotificationToTrigger,
                            String chatNotificationToTriggerEnd,
                            String trackerToTrigger,
                            String trackerToTriggerEnd,
                            String cleanUpChatTrigger,
                            boolean useTimer,
                            boolean isPeriod
    ) {
        if(!Objects.equals(currentSelectedTimer, newName)) {
            deleteCustomTimer(currentSelectedTimer);
            currentSelectedTimer = newName;
        }

        if(isPeriod) {
            CustomTimerPeriod newCustomTimerPeriod = new CustomTimerPeriod(newName,
                    timer,
                    offTimer,
                    offset,
                    notificationToTrigger,
                    notificationToTriggerEnd,
                    chatNotificationToTrigger,
                    chatNotificationToTriggerEnd,
                    trackerToTrigger,
                    trackerToTriggerEnd,
                    cleanUpChatTrigger,
                    useTimer,
                    true
            );

            customTimerData.timerList.put(currentSelectedTimer, newCustomTimerPeriod);
            needsUpdate = true;

            return newCustomTimerPeriod;
        } else {
            CustomTimer newCustomTimer = new CustomTimer(newName,
                    timer,
                    offset,
                    notificationToTrigger,
                    chatNotificationToTrigger,
                    trackerToTrigger,
                    cleanUpChatTrigger,
                    useTimer,
                    false
            );

            customTimerData.timerList.put(currentSelectedTimer, newCustomTimer);
            needsUpdate = true;

            return newCustomTimer;
        }
    }

    public void updateDefault() {
        CustomTimerDataModel.defaultTimers.forEach((key, timer) -> {
            customTimerData.timerList.putIfAbsent(key, timer);
        });
    }

    public void fixDefault() {
        customTimerData.timerList.putAll(CustomTimerDataModel.defaultTimers);
        needsUpdate = true;
    }

    public void resetTimers() {
        customTimerData.timerList = new HashMap<>(CustomTimerDataModel.defaultTimers);

        needsUpdate = true;
    }
    //endregion

    //region Model
    public static class CustomTimerDataModel extends DataModels.DataModel {
        private static final String CUSTOM_TIMER_DATA_MODEL_VERSION = "0.3";

        private static final Map<String, CustomTimer> defaultTimers = Map.of(
                "Contest Timer", new CustomTimerPeriod(
                        "Contest Timer",
                        1800,
                        1800,
                        -5,
                        "Contest Start",
                        "Contest End",
                        "",
                        "",
                        "",
                        "",
                        "Contest Type, Contest Location, Contest Level, Contest 1st, Contest 2nd, Contest 3rd, Contest Placement",
                        true,
                        true
                )
        );

        //Name Chat Trigger, Chat Trigger
        public Map<String, CustomTimer> timerList = new HashMap<>(defaultTimers);

        public CustomTimerDataModel() {
            super(CUSTOM_TIMER_DATA_MODEL_VERSION, null);
        }
    }
    //endregion

    //region Notification Object
    public static class CustomTimer {
        private String name;
        private int timer;
        private int offset;
        private String notificationToTrigger;
        private String chatNotificationToTrigger;
        private String trackerToTrigger;
        private String cleanUpChatTrigger;
        private boolean useTimer;
        private boolean isPeriod;

        public String getName() {
            return name != null ? name : "";
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getTimer() {
            return timer;
        }

        public int getOffset() {
            return offset;
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

        public String getCleanUpChatTrigger() {
            return cleanUpChatTrigger != null ? cleanUpChatTrigger : "";
        }

        public boolean isUseTimer() {
            return useTimer;
        }

        public boolean isPeriod() {
            return isPeriod;
        }

        public CustomTimer(String name,
                           int timer,
                           int offset,
                           String notificationToTrigger,
                           String chatNotificationToTrigger,
                           String trackerToTrigger,
                           String cleanUpChatTrigger,
                           boolean useTimer,
                           boolean isPeriod) {
            this.name = name;
            this.timer = timer;
            this.offset = offset;
            this.notificationToTrigger = notificationToTrigger;
            this.chatNotificationToTrigger = chatNotificationToTrigger;
            this.trackerToTrigger = trackerToTrigger;
            this.cleanUpChatTrigger = cleanUpChatTrigger;
            this.useTimer = useTimer;
            this.isPeriod = isPeriod;
        }

        public CustomTimer(String name) {
            this.name = name;
            this.timer = 60;
            this.offset = 0;
            this.notificationToTrigger = "";
            this.chatNotificationToTrigger = "";
            this.trackerToTrigger = "";
            this.cleanUpChatTrigger = "";
            this.useTimer = true;
            this.isPeriod = false;
        }
    }

    public static class CustomTimerPeriod extends CustomTimer {
        private int offTimer;
        private String notificationToTriggerEnd;
        private String chatNotificationToTriggerEnd;
        private String trackerToTriggerEnd;

        public int getOffTimer() {
            return offTimer;
        }

        public String getNotificationToTriggerEnd() {
            return notificationToTriggerEnd != null ? notificationToTriggerEnd : "";
        }

        public String getChatNotificationToTriggerEnd() {
            return chatNotificationToTriggerEnd != null ? chatNotificationToTriggerEnd : "";
        }

        public String getTrackerToTriggerEnd() {
            return trackerToTriggerEnd != null ? trackerToTriggerEnd : "";
        }

        public CustomTimerPeriod(String name,
                                 int timer,
                                 int offTimer,
                                 int offset,
                                 String notificationToTrigger,
                                 String notificationToTriggerEnd,
                                 String chatNotificationToTrigger,
                                 String chatNotificationToTriggerEnd,
                                 String trackerToTrigger,
                                 String trackerToTriggerEnd,
                                 String cleanUpChatTrigger,
                                 boolean useTimer,
                                 boolean isPeriod
        ) {
            super(name,
                    timer,
                    offset,
                    notificationToTrigger,
                    chatNotificationToTrigger,
                    trackerToTrigger,
                    cleanUpChatTrigger,
                    useTimer,
                    isPeriod
            );
            this.offTimer = offTimer;
            this.notificationToTriggerEnd = notificationToTriggerEnd;
            this.chatNotificationToTriggerEnd = chatNotificationToTriggerEnd;
            this.trackerToTriggerEnd = trackerToTriggerEnd;
        }

        public CustomTimerPeriod(String name) {
            super(name);
            this.offTimer = 60;
            this.notificationToTriggerEnd = "";
            this.chatNotificationToTriggerEnd = "";
            this.trackerToTriggerEnd = "";
        }
    }
    //endregion

    //region Dev
    /// Field, Pair<Value, Tooltip>
    protected Map<String, Pair<MutableComponent, MutableComponent>> _getFields() {
        return Map.of(
                "customTimerData", Pair.of(Component.literal("[customTimerData]"), TextHelper.literal(getCustomTimerData()))
        );
    }
    //endregion
}
