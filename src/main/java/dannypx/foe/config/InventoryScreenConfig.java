package dannypx.foe.config;

import dannypx.foe.FishOnMCExtras;
import me.fzzyhmstrs.fzzy_config.annotations.Version;
import me.fzzyhmstrs.fzzy_config.api.FileType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

@Version(version = 1)
@Translatable.Name("Inventory Configuration")
@Translatable.Desc("§7Configure Inventory elements")
public class InventoryScreenConfig extends Config {
    public InventoryScreenConfig() {
        super(Identifier.fromNamespaceAndPath(FishOnMCExtras.MOD_ID, "inventory_config"));
    }

    @Name("Show Stats Element")
    public ValidatedBoolean showStatsElement = new ValidatedBoolean(true);

    @Name("Search Character Limit")
    @Desc("§7Maximum number of characters allowed in the item search bar")
    public ValidatedInt searchCharacterLimit = new ValidatedInt(256, 1024, 32, ValidatedNumber.WidgetType.SLIDER);

    @Override
    public @NotNull FileType fileType() {
        return FileType.JSON;
    }
}
