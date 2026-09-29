package dannypx.foe.handler.io;

import java.util.UUID;

public class DataModels {

    public static abstract class DataModel {
        public String version;
        public UUID uuid;

        protected DataModel(String version, UUID uuid) {
            this.version = version;
            this.uuid = uuid;
        }
    }

    public enum DataModelType {
        PROFILE_DATA("profile"),
        STATS_DATA("stats"),
        CONSTANT_DATA("constant"),
        QUEST_DATA("quest"),
        CREW_DATA("crew"),
        CUSTOM_HUD_DATA("custom_hud"),
        CUSTOM_HUD_ICON_DATA("custom_hud_icon"),
        CUSTOM_BUTTON_DATA("custom_button"),
        CUSTOM_NOTIFICATION_DATA("custom_notification"),
        CUSTOM_CHAT_NOTIFICATION_DATA("custom_chat_notification"),
        CUSTOM_CHAT_TRIGGER_DATA("custom_chat_trigger"),
        CUSTOM_EVENT_TRIGGER_DATA("custom_event_trigger"),
        CUSTOM_TIMER_DATA("custom_timer"),
        CUSTOM_TRACKER_DATA("custom_tracker"),
        CUSTOM_SNIPPET_DATA("custom_snippet");

        public final String FILENAME;

        DataModelType(String fileName) {
            FILENAME = fileName;
        }
    }
}
