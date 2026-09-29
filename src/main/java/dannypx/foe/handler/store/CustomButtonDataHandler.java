package dannypx.foe.handler.store;

import dannypx.foe.handler.Handler;
import dannypx.foe.handler.io.DataFileHandler;
import dannypx.foe.handler.io.DataModels;
import dannypx.foe.helper.TextHelper;
import dannypx.foe.type.tuple.Pair;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class CustomButtonDataHandler extends Handler {
    private static CustomButtonDataHandler INSTANCE = new CustomButtonDataHandler();

    public static CustomButtonDataHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new CustomButtonDataHandler();
        }
        return INSTANCE;
    }

    //region Fields
    private CustomButtonDataModel customButtonData = new CustomButtonDataModel();
    private boolean needsUpdate = false;

    public CustomButtonDataModel getCustomButtonData() {
        return customButtonData;
    }

    public void setCustomButtonData(CustomButtonDataModel customButtonData) {
        this.customButtonData = customButtonData;
        this.updateCustomButtonData();
    }

    private void updateCustomButtonData() {
        if(needsUpdate) {
            DataFileHandler.instance().saveToFile(DataModels.DataModelType.CUSTOM_BUTTON_DATA);
        }
        this.needsUpdate = false;
    }
    //endregion

    //region Methods
    public void tick() {
        if(customButtonData.uuid == null && minecraft.player != null) {
            customButtonData.uuid = minecraft.player.getUUID();
        } else if(customButtonData.uuid != null && this.needsUpdate) {
            this.updateCustomButtonData();
        } else if(!CustomButtonDataModel.CUSTOM_BUTTON_DATA_MODEL_VERSION.equals(customButtonData.version)) {
            customButtonData.version = CustomButtonDataModel.CUSTOM_BUTTON_DATA_MODEL_VERSION;
            needsUpdate = true;
        }

        if(customButtonData.uuid != null
                && (!this.customButtonData.buttonList.containsKey(CustomButtonDataModel.INVENTORY_SCREEN)

        )) {
            this.resetButtons();
        }
    }

    public void init() {
        if(minecraft.player != null) this.setUUID(minecraft.player.getUUID());
    }

    public void init(String screenID) {
        if(!this.customButtonData.buttonList.containsKey(screenID)) {
            this.customButtonData.buttonList.put(screenID, Pair.of(new ArrayList<>(), false));
            needsUpdate = true;
        }
    }

    private void setUUID(UUID uuid) {
        this.customButtonData.uuid = uuid;
    }

    public void createNewButton(String namespace, String id) {
        Pair<List<CustomButton>, Boolean> buttonList = customButtonData.buttonList.getOrDefault(namespace, Pair.of(new ArrayList<>(), false));

        buttonList.value1().add(new CustomButton(id));

        customButtonData.buttonList.put(namespace, buttonList);

        needsUpdate = true;
    }

    public void createNewButton(String namespace, String id, int pos) {
        Pair<List<CustomButton>, Boolean> buttonList = customButtonData.buttonList.getOrDefault(namespace, Pair.of(new ArrayList<>(), false));

        buttonList.value1().add(pos, new CustomButton(id));

        customButtonData.buttonList.put(namespace, buttonList);

        needsUpdate = true;
    }

    public void swapUp(String namespace, int pos) {
        Pair<List<CustomButton>, Boolean> buttonList = customButtonData.buttonList.getOrDefault(namespace, Pair.of(new ArrayList<>(), false));

        Collections.swap(buttonList.value1(), pos, pos - 1);

        customButtonData.buttonList.put(namespace, buttonList);

        needsUpdate = true;
    }

    public void swapDown(String namespace, int pos) {
        Pair<List<CustomButton>, Boolean> buttonList = customButtonData.buttonList.getOrDefault(namespace, Pair.of(new ArrayList<>(), false));

        Collections.swap(buttonList.value1(), pos, pos + 1);

        customButtonData.buttonList.put(namespace, buttonList);

        needsUpdate = true;
    }

    public void createNewButton(String namespace, CustomButton newButton) {
        customButtonData.buttonList.getOrDefault(namespace, Pair.of(new ArrayList<>(), false)).value1().add(newButton);

        needsUpdate = true;
    }

    public void deleteButton(String namespace, String id) {
        Pair<List<CustomButton>, Boolean> buttonList = customButtonData.buttonList.getOrDefault(namespace, Pair.of(new ArrayList<>(), false));

        buttonList.value1().removeIf(button -> Objects.equals(button.name, id));

        customButtonData.buttonList.put(namespace, buttonList);

        needsUpdate = true;
    }

    public CustomButton updateButton(String namespace, CustomButton customButton, String newName, String desc, String action, String icon, boolean showButton) {
        Pair<List<CustomButton>, Boolean> buttonList = customButtonData.buttonList.getOrDefault(namespace, Pair.of(new ArrayList<>(), false));
        int index = buttonList.value1().indexOf(customButton);
        CustomButton updatedButton = buttonList.value1().get(index);

        updatedButton.name = newName;
        updatedButton.description = desc;
        updatedButton.action = action;
        updatedButton.icon = icon;
        updatedButton.showButton = showButton;

        buttonList.value1().set(index, updatedButton);

        customButtonData.buttonList.put(namespace, buttonList);

        needsUpdate = true;

        return updatedButton;
    }

    public void updateButton(String namespace, boolean showButton) {
        Pair<List<CustomButton>, Boolean> buttonList = customButtonData.buttonList.getOrDefault(namespace, Pair.of(new ArrayList<>(), false));
        buttonList = Pair.of(buttonList.value1(), showButton);

        customButtonData.buttonList.put(namespace, buttonList);

        needsUpdate = true;
    }



    public void resetButtons() {
        customButtonData.buttonList = new HashMap<>(Map.ofEntries(
                Map.entry(CustomButtonDataModel.INVENTORY_SCREEN, CustomButtonDataModel.defaultInventoryButtons)
        ));

        needsUpdate = true;
    }
    //endregion

    //region Model
    public static class CustomButtonDataModel extends DataModels.DataModel {
        private static final String CUSTOM_BUTTON_DATA_MODEL_VERSION = "0.4";

        public static final String INVENTORY_SCREEN = "Inventory Screen";

        private static final Pair<List<CustomButton>, Boolean> defaultInventoryButtons = Pair.of(new ArrayList<>(List.of(
                new CustomButton("&6&lInstances",
                        "&7Change Instances",
                        "/instances", "\u24BE", true),
                new CustomButton("&6&lTurbo Travel",
                        "&7Change Location",
                        "/tt", "\u26A1", true),
                new CustomButton("&6&lCrew Home",
                        "&7Go to your crew home",
                        "/crew home", "\u2693", true),
                new CustomButton("&6&lSpawn",
                        "&7Go to spawn",
                        "/spawn", "\u24C8", true),
                new CustomButton("&6&lQuests",
                        "&7Open your quests",
                        "/quests", "\u2690", true),
                new CustomButton("&6&lPersonal Vault",
                        "&7Open your personal vault",
                        "/pv", "\u26E8", true),
                new CustomButton("&6&lAuction House",
                        "&7Open the auction house",
                        "/ah", "\u2B50", true),
                new CustomButton("&6&lCraft",
                        "&7Open the crafting station",
                        "/craft", "\u26CF", true),

                new CustomButton("&6&lArtisan",
                        "&7Open the Artisan Menu",
                        "/artisan", "A", true),
                new CustomButton("&6&lIdentifier",
                        "&7Open the Identifier Menu",
                        "/identifier", "I", true),
                new CustomButton("&6&lForge",
                        "&7Open the Forge Menu",
                        "/forge", "F", true),
                new CustomButton("&6&lScrapper",
                        "&7Open the Scrapper Menu",
                        "/scrapper", "S", true),
                new CustomButton("&6&lCalibrator",
                        "&7Open the Calibrator Menu",
                        "/calibrator", "C", true),
                new CustomButton("Spacer #1",
                        "Used to show empty spot",
                        "/", "minecraft:barrier", false),
                new CustomButton("Spacer #2",
                        "Used to show empty spot",
                        "/", "minecraft:barrier", false),
                new CustomButton("&6&lSell",
                        "&7Open the sell menu",
                        "/sell", "$", true)
        )), true);

        //Screen <Button List, isMenuOpen>
        public Map<String, Pair<List<CustomButton>, Boolean>> buttonList = new HashMap<>(Map.ofEntries(
                Map.entry("Inventory Screen", defaultInventoryButtons)
        ));

        public CustomButtonDataModel() {
            super(CUSTOM_BUTTON_DATA_MODEL_VERSION, null);
        }
    }
    //endregion

    //region Button Object
    public static class CustomButton {
        public String name;
        public String description;
        public String action;
        public String icon;
        public boolean showButton;

        public CustomButton(String name) {
            this.name = name;
            this.description = "Example description";
            this.action = "/";
            this.icon = "E";
            this.showButton = true;
        }

        public CustomButton(String name, String description, String action, String icon, boolean showButton) {
            this.name = name;
            this.description = description;
            this.action = action;
            this.icon = icon;
            this.showButton = showButton;
        }
    }
    //endregion

    //region Dev
    /// Field, Pair<Value, Tooltip>
    protected Map<String, Pair<MutableComponent, MutableComponent>> _getFields() {
        return Map.of(
                "customButtonData", Pair.of(Component.literal("[customButtonData]"), TextHelper.literal(getCustomButtonData()))
        );
    }
    //endregion
}
