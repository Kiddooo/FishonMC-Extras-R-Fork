package dannypx.foe.handler.logic;

import dannypx.foe.FishOnMCExtras;
import dannypx.foe.handler.Handler;
import dannypx.foe.handler.store.CustomChatNotificationDataHandler;
import dannypx.foe.handler.store.CustomHudDataHandler;
import dannypx.foe.handler.store.CustomTrackerDataHandler;
import dannypx.foe.handler.store.ProfileDataHandler;
import dannypx.foe.helper.TextHelper;
import dannypx.foe.type.tuple.Pair;
import dannypx.foe.type.version.Version;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class UpdateHandler extends Handler {
    private static UpdateHandler INSTANCE = new UpdateHandler();

    public static UpdateHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new UpdateHandler();
        }
        return INSTANCE;
    }

    //region Fields
    public static final Version v038 = Version.of("0.3.8");
    public static final Version v039 = Version.of("0.3.9");
    //endregion

    //region Methods
    public static void checkUpdate() {
        Version before = Version.of(ProfileDataHandler.instance().getProfileData().modVersion);
        Version now = Version.of(FishOnMCExtras.VERSION);

        if(before.get() == null || now.compareTo(before) > 0) {
            // Put all update cycles here
            if(before.get() == null  || (v038.compareTo(before) > 0)) updateToV038();
            if(before.get() == null  || (v039.compareTo(before) > 0)) updateToV039();

            ProfileDataHandler.instance().updateVersion();
        }
    }

    public static void updateToV038() {
        LoggerHandler.info("Update to 0.3.8 defaults");
        LoggerHandler.info("- CustomTrackerDataHandler");
        LoggerHandler.info("- CustomHudDataHandler");
        LoggerHandler.info("- CustomChatNotificationDataHandler");

        CustomTrackerDataHandler.instance().fixDefault();
        CustomHudDataHandler.instance().fixDefault();
        CustomChatNotificationDataHandler.instance().fixDefault();
    }

    private static void updateToV039() {
        LoggerHandler.info("Update to 0.3.9 defaults");
        LoggerHandler.info("- CustomChatNotificationDataHandler");

        CustomChatNotificationDataHandler.instance().fixDefault();
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
