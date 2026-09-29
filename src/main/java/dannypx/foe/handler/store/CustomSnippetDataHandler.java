package dannypx.foe.handler.store;

import dannypx.foe.handler.Handler;
import dannypx.foe.handler.io.DataFileHandler;
import dannypx.foe.handler.io.DataModels;
import dannypx.foe.helper.TextHelper;
import dannypx.foe.type.tuple.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CustomSnippetDataHandler extends Handler {
    private static CustomSnippetDataHandler INSTANCE = new CustomSnippetDataHandler();

    public static CustomSnippetDataHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new CustomSnippetDataHandler();
        }
        return INSTANCE;
    }

    //region Fields
    private CustomSnippetDataModel customSnippetData = new CustomSnippetDataModel();
    private boolean needsUpdate = false;

    public CustomSnippetDataModel getCustomSnippetData() {
        return customSnippetData;
    }

    public void setCustomSnippetData(CustomSnippetDataModel customSnippetData) {
        this.customSnippetData = customSnippetData;
        this.updateCustomChatNotificationData();
    }

    private void updateCustomChatNotificationData() {
        if(needsUpdate) {
            DataFileHandler.instance().saveToFile(DataModels.DataModelType.CUSTOM_SNIPPET_DATA);
        }
        this.needsUpdate = false;
    }
    //endregion

    //region Methods
    public void tick() {
        if(customSnippetData.uuid == null && minecraft.player != null) {
            customSnippetData.uuid = minecraft.player.getUUID();
        } else if(customSnippetData.uuid != null && this.needsUpdate) {
            this.updateCustomChatNotificationData();
        } else if(!CustomSnippetDataModel.CUSTOM_SNIPPET_DATA_MODEL_VERSION.equals(customSnippetData.version)) {
            customSnippetData.version = CustomSnippetDataModel.CUSTOM_SNIPPET_DATA_MODEL_VERSION;
            this.updateDefault();
            needsUpdate = true;
        }
    }

    public void init() {
        if(minecraft.player != null) this.setUUID(minecraft.player.getUUID());
    }

    private void setUUID(UUID uuid) {
        this.customSnippetData.uuid = uuid;
    }

    public void createNewSnippet(String id) {
        customSnippetData.snippetList.put(id, "Hello World. \nThis is your new snippet.");
        needsUpdate = true;
    }

    public void createNewSnippet(String id, String customSnippet) {
        customSnippetData.snippetList.put(id, customSnippet);
        needsUpdate = true;
    }

    public String deleteCustomSnippet(String id) {
        needsUpdate = true;
        return customSnippetData.snippetList.remove(id);
    }

    public void updateSnippet(String currentSelectedSnippet, String newName, String newText) {
        String newSnippet;

        if(!Objects.equals(currentSelectedSnippet, newName)) {
            deleteCustomSnippet(currentSelectedSnippet);
            currentSelectedSnippet = newName;
        }

        newSnippet = newText;

        customSnippetData.snippetList.put(currentSelectedSnippet, newSnippet);
        needsUpdate = true;
    }

    public void updateDefault() {
        CustomSnippetDataModel.defaultSnippets.forEach((key, timer) -> {
            customSnippetData.snippetList.putIfAbsent(key, timer);
        });
    }

    public void fixDefault() {
        customSnippetData.snippetList.putAll(CustomSnippetDataModel.defaultSnippets);
        needsUpdate = true;
    }

    public void resetSnippets() {
        customSnippetData.snippetList = new HashMap<>(CustomSnippetDataModel.defaultSnippets);

        needsUpdate = true;
    }
    //endregion

    //region Model
    public static class CustomSnippetDataModel extends DataModels.DataModel {
        private static final String CUSTOM_SNIPPET_DATA_MODEL_VERSION = "0.1";

        private static final Map<String, String> defaultSnippets = Map.of(

        );

        //Name Snippet, Snippet
        public Map<String, String> snippetList = new HashMap<>(defaultSnippets);

        public CustomSnippetDataModel() {
            super(CUSTOM_SNIPPET_DATA_MODEL_VERSION, null);
        }
    }
    //endregion

    //region Dev
    /// Field, Pair<Value, Tooltip>
    protected Map<String, Pair<MutableComponent, MutableComponent>> _getFields() {
        return Map.of(
                "customSnippetData", Pair.of(Component.literal("[customSnippetData]"), TextHelper.literal(getCustomSnippetData()))
        );
    }
    //endregion
}
