package dannypx.foe.handler.logic;

import dannypx.foe.config.Configs;
import dannypx.foe.handler.Handler;
import dannypx.foe.handler.store.CustomEventTriggerDataHandler;
import dannypx.foe.handler.store.CustomTrackerDataHandler;
import dannypx.foe.type.event.EventTrigger;
import dannypx.foe.type.tuple.Pair;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class EventHandler extends Handler {
    private static EventHandler INSTANCE = new EventHandler();

    public static EventHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new EventHandler();
        }
        return INSTANCE;
    }

    //region Fields

    //endregion

    //region Methods
    public void onJoin() {
        if(minecraft.player != null) {
            InventoryHandler.instance().snapshotInventory();
            if(Configs.handlerConfig.openEventsOnJoin.get()) minecraft.player.connection.sendCommand("events");

            //Update UI for missing HUD elements
            minecraft.options.hideGui = true;
            minecraft.options.hideGui = false;

            this.sendEventTrigger(EventTrigger.ON_JOIN);
        }
    }

    public void onCatch() {
        this.sendEventTrigger(EventTrigger.ON_CATCH);
    }

    public void onXpGain() {
        this.sendEventTrigger(EventTrigger.ON_XP_GAIN);
    }

    public void onLocationChange() {
        this.sendEventTrigger(EventTrigger.ON_LOCATION_CHANGE);
    }

    public void onCrewJoin() {
        this.sendEventTrigger(EventTrigger.ON_CREW_JOIN);
    }

    public void onCrewLeave() {
        this.sendEventTrigger(EventTrigger.ON_CREW_LEAVE);
    }

    public void onFullInventory() {
        this.sendEventTrigger(EventTrigger.ON_FULL_INVENTORY);
    }

    public void onNearFullInventory() {
        this.sendEventTrigger(EventTrigger.ON_NEAR_FULL_INVENTORY);
    }

    public void onQuestComplete() {
        this.sendEventTrigger(EventTrigger.ON_QUEST_COMPLETE);
    }

    public void onScreenOpen() {
        this.sendEventTrigger(EventTrigger.ON_SCREEN_OPEN);
    }

    public void onScreenClose() {
        this.sendEventTrigger(EventTrigger.ON_SCREEN_CLOSE);
    }

    public void onPetEquip() {
        this.sendEventTrigger(EventTrigger.ON_PET_EQUIP);
    }

    public void onPetUnequip() {
        this.sendEventTrigger(EventTrigger.ON_PET_UNEQUIP);
    }

    private void sendEventTrigger(EventTrigger eventTrigger) {
        CustomEventTriggerDataHandler.instance().getCustomEventTriggerData().eventTriggerList.forEach((name, event) -> {
            if(event.isUseEventTrigger() && event.getEvent() == eventTrigger) {
                String[] notificationIds = event.getNotificationToTrigger().split(",");
                NotifierHandler.instance().notifyOnTrigger(notificationIds);

                String[] chatNotificationIds = event.getChatNotificationToTrigger().split(",");
                ChatNotifierHandler.instance().notifyChatOnTrigger(chatNotificationIds);

                String[] trackerIds = event.getTrackerToTrigger().split(",");
                CustomTrackerDataHandler.instance().updateTracker(trackerIds);
            }
        });
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
