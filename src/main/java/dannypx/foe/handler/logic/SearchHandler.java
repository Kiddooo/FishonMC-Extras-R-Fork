package dannypx.foe.handler.logic;

import dannypx.foe.config.Configs;
import dannypx.foe.handler.Handler;
import dannypx.foe.helper.MathHelper;
import dannypx.foe.helper.TextHelper;
import dannypx.foe.item.TagObject;
import dannypx.foe.item.PetTagObject;
import dannypx.foe.item.ValidateItem;
import dannypx.foe.type.tuple.Pair;
import dannypx.foe.type.search.*;
import dannypx.foe.screens.widget.SearchBarWidget;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

public class SearchHandler extends Handler {
    private static SearchHandler INSTANCE = new SearchHandler();

    public static SearchHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new SearchHandler();
        }
        return INSTANCE;
    }

    //region Fields
    private List<SearchFilter> filters = new ArrayList<>();
    private String searchRemainder = "";
    private Predicate<String> nameQuery;
    private String lastInput = "";
    private boolean isFocused = false;
    private boolean isOnScreen = false;

    public List<SearchFilter> getFilters() {
        return filters;
    }

    public String getSearchRemainder() {
        return searchRemainder;
    }

    public String getLastInput() {
        return lastInput;
    }

    public boolean isFocused() {
        return isFocused;
    }

    public void setFocused(boolean focused) {
        isFocused = focused;
    }

    public void setLastInput(String lastInput) {
        this.lastInput = lastInput;
    }

    public boolean isOnScreen() {
        return isOnScreen;
    }

    public void setOnScreen(boolean onScreen) {
        isOnScreen = onScreen;
    }
    //endregion

    //region Methods
    public void parseSearch(String input) {
        if(!Objects.equals(this.lastInput, input)) {
            this.setLastInput(input);

            this.filters.clear();

            Pattern pattern = Pattern.compile(
                    "(\\w+)\\s*(>=|<=|==|!=|>|<|=)\\s*(?:\"([^\"]+)\"|(-?\\d+(?:\\.\\d+)?)|([A-Za-z_]\\w*))"
            );

            Matcher matcher = pattern.matcher(input);
            StringBuilder remaining = new StringBuilder();

            while (matcher.find()) {
                String field = matcher.group(1);
                String operator = matcher.group(2);

                FilterValue value = null;

                if (matcher.group(3) != null) {
                    value = new StringValue(matcher.group(3));
                } else if (matcher.group(4) != null) {
                    value = new FloatValue(Float.parseFloat(matcher.group(4)));
                } else if (matcher.group(5) != null) {
                    value = new BooleanValue(Boolean.parseBoolean(matcher.group(5)));
                }

                this.filters.add(new SearchFilter(field, Operator.fromSymbol(operator), value));

                matcher.appendReplacement(remaining, "");
            }
            matcher.appendTail(remaining);

            this.searchRemainder = remaining.toString().trim();
            this.nameQuery = NameSearch.parse(searchRemainder);
        }
    }

    public static SearchBarWidget getSearchBar(int x, int y, int width, int height) {
        SearchBarWidget searchBarWidget = new SearchBarWidget(minecraft.font, x, y, width, height,
                Component.literal("Search Bar"),
                new ArrayList<>(Arrays.asList(
                        Component.literal("Search Item Names in the search bar").withStyle(ChatFormatting.WHITE),
                        Component.literal("NBT fields can be compared against specific values for more granular filtering").withStyle(ChatFormatting.GRAY),
                        Component.empty(),
                        Component.literal("Granular filtering").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                        Component.literal("- Allowed Operators").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD),
                        Component.literal("=  ==  !=  <  >  <=  >=").withStyle(ChatFormatting.GOLD),
                        Component.empty(),
                        Component.literal("- Non NBT fields").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD),
                        Component.literal("Pets").withStyle(ChatFormatting.GRAY),
                        Component.literal("rating  lluck  lscale  cluck  cscale").withStyle(ChatFormatting.DARK_AQUA),
                        Component.literal("lluck_percent  lscale_percent  cluck_percent  cscale_percent").withStyle(ChatFormatting.DARK_AQUA),
                        Component.literal("Other").withStyle(ChatFormatting.GRAY),
                        Component.literal("tooltip").withStyle(ChatFormatting.DARK_AQUA),
                        Component.empty(),
                        Component.literal("- Examples").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD),
                        Component.literal("----------------------------------------").withStyle(ChatFormatting.DARK_GRAY),
                        TextHelper.concat(
                                Component.literal("Pet").withStyle(ChatFormatting.GREEN)
                        ),
                        Component.empty(),
                        TextHelper.concat(
                                Component.literal("Search any item that has the word ").withStyle(ChatFormatting.GRAY),
                                Component.literal("Pet ").withStyle(ChatFormatting.GREEN),
                                Component.literal("in it").withStyle(ChatFormatting.GRAY)
                        ).withStyle(ChatFormatting.ITALIC),
                        Component.literal("----------------------------------------").withStyle(ChatFormatting.DARK_GRAY),
                        Component.literal("Subtropical (boot|leg) | capy pet").withStyle(ChatFormatting.GREEN),
                        Component.empty(),
                        Component.literal("(Subtropical AND (boot OR leg)) OR (capy AND pet)").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                        Component.literal("----------------------------------------").withStyle(ChatFormatting.DARK_GRAY),
                        TextHelper.concat(
                                Component.literal("tooltip").withStyle(ChatFormatting.DARK_AQUA),
                                Component.literal("=").withStyle(ChatFormatting.GOLD),
                                Component.literal("\"tunas\"").withStyle(ChatFormatting.GREEN)
                        ),
                        Component.empty(),
                        TextHelper.concat(
                                Component.literal("Search item ").withStyle(ChatFormatting.GRAY),
                                Component.literal("tooltip ").withStyle(ChatFormatting.DARK_AQUA),
                                Component.literal("that has the word ").withStyle(ChatFormatting.GRAY),
                                Component.literal("tunas ").withStyle(ChatFormatting.GREEN),
                                Component.literal("in it").withStyle(ChatFormatting.GRAY)
                        ).withStyle(ChatFormatting.ITALIC),
                        Component.literal("----------------------------------------").withStyle(ChatFormatting.DARK_GRAY),
                        TextHelper.concat(
                                Component.literal("rating").withStyle(ChatFormatting.DARK_AQUA),
                                Component.literal(">").withStyle(ChatFormatting.GOLD),
                                Component.literal("90  ").withStyle(ChatFormatting.GREEN),
                                Component.literal("lscale_percent").withStyle(ChatFormatting.DARK_AQUA),
                                Component.literal(">=").withStyle(ChatFormatting.GOLD),
                                Component.literal("80").withStyle(ChatFormatting.GREEN)
                        ),
                        Component.empty(),
                        TextHelper.concat(
                                Component.literal("Search items that is of ").withStyle(ChatFormatting.GRAY),
                                Component.literal("pet rating ").withStyle(ChatFormatting.DARK_AQUA),
                                Component.literal("higher than ").withStyle(ChatFormatting.GOLD),
                                Component.literal("90").withStyle(ChatFormatting.GREEN),
                                Component.literal("%").withStyle(ChatFormatting.GRAY)
                        ).withStyle(ChatFormatting.ITALIC),
                        TextHelper.concat(
                                Component.literal("AND ").withStyle(ChatFormatting.GOLD),
                                Component.literal("is of ").withStyle(ChatFormatting.GRAY),
                                Component.literal("location scale ").withStyle(ChatFormatting.DARK_AQUA),
                                Component.literal("higher than or equal to ").withStyle(ChatFormatting.GOLD),
                                Component.literal("80").withStyle(ChatFormatting.GREEN),
                                Component.literal("%").withStyle(ChatFormatting.GRAY)
                        ).withStyle(ChatFormatting.ITALIC),
                        Component.literal("----------------------------------------").withStyle(ChatFormatting.DARK_GRAY),
                        TextHelper.concat(
                                Component.literal("type").withStyle(ChatFormatting.DARK_AQUA),
                                Component.literal("=").withStyle(ChatFormatting.GOLD),
                                Component.literal("\"armor\"  ").withStyle(ChatFormatting.GREEN),
                                Component.literal("rarity").withStyle(ChatFormatting.DARK_AQUA),
                                Component.literal("=").withStyle(ChatFormatting.GOLD),
                                Component.literal("\"mythical\"  ").withStyle(ChatFormatting.GREEN),
                                Component.literal("quality").withStyle(ChatFormatting.DARK_AQUA),
                                Component.literal(">").withStyle(ChatFormatting.GOLD),
                                Component.literal("96  ").withStyle(ChatFormatting.GREEN),
                                Component.literal("Subtropical").withStyle(ChatFormatting.GREEN)
                        ),
                        Component.empty(),
                        TextHelper.concat(
                                Component.literal("Search items that is of ").withStyle(ChatFormatting.GRAY),
                                Component.literal("type ").withStyle(ChatFormatting.DARK_AQUA),
                                Component.literal("equal to ").withStyle(ChatFormatting.GOLD),
                                Component.literal("armor").withStyle(ChatFormatting.GREEN)
                        ).withStyle(ChatFormatting.ITALIC),
                        TextHelper.concat(
                                Component.literal("AND ").withStyle(ChatFormatting.GOLD),
                                Component.literal("is of ").withStyle(ChatFormatting.GRAY),
                                Component.literal("rarity ").withStyle(ChatFormatting.DARK_AQUA),
                                Component.literal("equal to ").withStyle(ChatFormatting.GOLD),
                                Component.literal("mythical").withStyle(ChatFormatting.GREEN)
                        ).withStyle(ChatFormatting.ITALIC),
                        TextHelper.concat(
                                Component.literal("AND ").withStyle(ChatFormatting.GOLD),
                                Component.literal("is of ").withStyle(ChatFormatting.GRAY),
                                Component.literal("quality ").withStyle(ChatFormatting.DARK_AQUA),
                                Component.literal("higher than ").withStyle(ChatFormatting.GOLD),
                                Component.literal("94").withStyle(ChatFormatting.GREEN),
                                Component.literal("%").withStyle(ChatFormatting.GRAY)
                        ).withStyle(ChatFormatting.ITALIC),
                        TextHelper.concat(
                                Component.literal("AND ").withStyle(ChatFormatting.GOLD),
                                Component.literal("search any item that has the word ").withStyle(ChatFormatting.GRAY),
                                Component.literal("Subtropical ").withStyle(ChatFormatting.GREEN),
                                Component.literal("in it").withStyle(ChatFormatting.GRAY)
                        ).withStyle(ChatFormatting.ITALIC)
                ))
        );
        searchBarWidget.setMaxLength(Configs.inventoryScreenConfig.searchCharacterLimit.get());
        searchBarWidget.setHint(Component.literal("Search Item Names").withStyle(ChatFormatting.GRAY));
        searchBarWidget.setResponder(SearchHandler.instance()::parseSearch);
        searchBarWidget.setValue(SearchHandler.instance().getLastInput());

        return searchBarWidget;
    }

    public boolean filterItem(ItemStack itemStack) {
        AtomicBoolean atomicBoolean = new AtomicBoolean(true);

        // Stop when search is empty
        if(lastInput.isBlank()) {
            return false;
        }

        // Hidden items never appear in search results.
        if(itemStack.get(DataComponents.TOOLTIP_DISPLAY) != null
                && itemStack.get(DataComponents.TOOLTIP_DISPLAY).hideTooltip()) {
            return false;
        }

        if(!searchRemainder.isEmpty()
                && (nameQuery == null || !nameQuery.test(itemStack.getHoverName().getString().toLowerCase(Locale.US)))) {
            return false;
        }

        filters.forEach(searchFilter -> {
            if(!atomicBoolean.get()) return;

            // Type
            atomicBoolean.set(this.checkType(searchFilter, itemStack, atomicBoolean.get()));
            if(!atomicBoolean.get()) return;

            // Tooltip
            atomicBoolean.set(this.checkTooltip(searchFilter, itemStack, atomicBoolean.get()));
            if(!atomicBoolean.get()) return;

            // Pet Specific
            String petCheck = this.checkPet(searchFilter, itemStack, atomicBoolean.get());
            if(petCheck.equals("false")) atomicBoolean.set(false);
            else if(petCheck.equals("true_pet")) return;
            if(!atomicBoolean.get()) return;

            // Other
            atomicBoolean.set(this.checkOther(searchFilter, itemStack, atomicBoolean.get()));
        });

        return atomicBoolean.get();
    }

    private boolean checkTooltip(SearchFilter searchFilter, ItemStack itemStack, boolean b) {
        if(searchFilter.value == null || !b) {
            return false;
        }

        return switch (searchFilter.value) {
            case StringValue stringValue -> {
                if(searchFilter.key.equalsIgnoreCase("tooltip")) {
                    if(searchFilter.operator == Operator.EQUAL || searchFilter.operator == Operator.SHORT_EQUAL) {
                        if(itemStack.get(DataComponents.LORE) != null) {
                            String query = stringValue.value().toLowerCase(Locale.US);
                            for (Component line : itemStack.get(DataComponents.LORE).lines()) {
                                String text = TextHelper.normalLetter(line.getString()).toLowerCase(Locale.US);
                                if (TooltipSearch.containsAny(text, query)) {
                                    yield true;
                                }
                            }
                            yield false;
                        } else {
                            yield false;
                        }
                    } else {
                        yield false;
                    }
                } else {
                    yield true;
                }
            }
            default -> !searchFilter.key.equalsIgnoreCase("tooltip");
        };
    }

    private boolean checkType(SearchFilter searchFilter, ItemStack itemStack, boolean b) {
        if(searchFilter.value == null || !b) {
            return false;
        }

        return switch (searchFilter.value) {
            case StringValue str -> {
                if(searchFilter.key.equalsIgnoreCase("type")) {
                    if(searchFilter.operator == Operator.SHORT_EQUAL) {
                        Pair<Boolean, TagObject> validatedItem = ValidateItem.isType(itemStack);
                        yield validatedItem.value2().getType().toLowerCase(Locale.US).contains(str.value().toLowerCase(Locale.US));
                    } else if (searchFilter.operator == Operator.EQUAL) {
                        Pair<Boolean, TagObject> validatedItem = ValidateItem.isType(itemStack);
                        yield validatedItem.value2().getType().toLowerCase(Locale.US).equalsIgnoreCase(str.value().toLowerCase(Locale.US));
                    } else {
                        yield false;
                    }
                } else {
                    yield true;
                }
            }
            default -> !searchFilter.key.equalsIgnoreCase("type");
        };
    }

    private String checkPet(SearchFilter searchFilter, ItemStack itemStack, boolean b) {
        if(searchFilter.value == null || !b) {
            return "false";
        }

        Pair<Boolean, PetTagObject> validatedPet = ValidateItem.isPet(itemStack);
        if(validatedPet.value1()) {
            return switch (searchFilter.value) {
                // Rating
                case StringValue stringValue -> {
                    if(searchFilter.key.equalsIgnoreCase("rating") || searchFilter.key.equalsIgnoreCase("pet_rating")) {
                        if(searchFilter.operator == Operator.EQUAL || searchFilter.operator == Operator.SHORT_EQUAL) {
                            yield TextHelper.normalLetter(validatedPet.value2().getRatingComponent().getString()).toLowerCase(Locale.US).contains(stringValue.value().toLowerCase(Locale.US)) ? "true_pet" : "false";
                        }
                        yield "false";
                    }
                    yield "true";
                }
                // Rating
                case FloatValue floatValue -> {
                    // in percent
                    if(searchFilter.key.equalsIgnoreCase("lluck_percent")) {
                        yield MathHelper.checkOperation(searchFilter, floatValue, validatedPet.value2().getLocationPercentMaxLuck() * 100f) ? "true_pet" : "false";
                    } else if(searchFilter.key.equalsIgnoreCase("lscale_percent")) {
                        yield MathHelper.checkOperation(searchFilter, floatValue, validatedPet.value2().getLocationPercentMaxScale() * 100f) ? "true_pet" : "false";
                    } else if(searchFilter.key.equalsIgnoreCase("cluck_percent")) {
                        yield MathHelper.checkOperation(searchFilter, floatValue, validatedPet.value2().getClimatePercentMaxLuck() * 100f) ? "true_pet" : "false";
                    } else if(searchFilter.key.equalsIgnoreCase("cscale_percent")) {
                        yield MathHelper.checkOperation(searchFilter, floatValue, validatedPet.value2().getClimatePercentMaxScale() * 100f) ? "true_pet" : "false";
                    } else if(searchFilter.key.equalsIgnoreCase("lluck")) {
                        yield MathHelper.checkOperation(searchFilter, floatValue, validatedPet.value2().getLocationMaxLuck()) ? "true_pet" : "false";
                    } else if(searchFilter.key.equalsIgnoreCase("lscale")) {
                        yield MathHelper.checkOperation(searchFilter, floatValue, validatedPet.value2().getLocationMaxScale()) ? "true_pet" : "false";
                    } else if(searchFilter.key.equalsIgnoreCase("cluck")) {
                        yield MathHelper.checkOperation(searchFilter, floatValue, validatedPet.value2().getClimateMaxLuck()) ? "true_pet" : "false";
                    } else if(searchFilter.key.equalsIgnoreCase("cscale")) {
                        yield MathHelper.checkOperation(searchFilter, floatValue, validatedPet.value2().getClimateMaxScale()) ? "true_pet" : "false";
                    } else if(searchFilter.key.equalsIgnoreCase("rating") || searchFilter.key.equalsIgnoreCase("pet_rating")) {
                        yield MathHelper.checkOperation(searchFilter, floatValue, validatedPet.value2().getTotalPercent() * 100f) ? "true_pet" : "false";
                    }
                    yield "true";
                }
                default -> "true";
            };
        } else {
            return !customPetTypes().contains(searchFilter.key) ? "true" : "false";
        }
    }

    private boolean checkOther(SearchFilter searchFilter, ItemStack itemStack, boolean b) {
        if(searchFilter.value == null || !b
        ) {
            return false;
        }

        if(customPetTypes().contains(searchFilter.key)
                || searchFilter.key.equalsIgnoreCase("type")
                || searchFilter.key.equalsIgnoreCase("tooltip")
        ) {
            return true;
        }

        return switch (searchFilter.value) {
            case StringValue stringValue -> {
                if(searchFilter.operator == Operator.SHORT_EQUAL) {
                    Pair<Boolean, TagObject> validatedItem = ValidateItem.isServerItem(itemStack, true);
                    if(validatedItem.value1() && validatedItem.value2().contains(searchFilter.key)) {
                        yield validatedItem.value2().getString(searchFilter.key).toLowerCase(Locale.US).contains(stringValue.value().toLowerCase(Locale.US));
                    }
                } else if(searchFilter.operator == Operator.EQUAL) {
                    Pair<Boolean, TagObject> validatedItem = ValidateItem.isServerItem(itemStack, true);
                    if(validatedItem.value1() && validatedItem.value2().contains(searchFilter.key)) {
                        yield validatedItem.value2().getString(searchFilter.key).toLowerCase(Locale.US).equalsIgnoreCase(stringValue.value().toLowerCase(Locale.US));
                    }
                }
                yield false;
            }
            case BooleanValue booleanValue -> {
                if(searchFilter.operator == Operator.EQUAL || searchFilter.operator == Operator.SHORT_EQUAL) {
                    Pair<Boolean, TagObject> validatedItem = ValidateItem.isServerItem(itemStack, true);
                    if(validatedItem.value1() && validatedItem.value2().contains(searchFilter.key)) {
                        yield validatedItem.value2().getBoolean(searchFilter.key) == booleanValue.value();
                    }
                }
                yield false;
            }
            case FloatValue floatValue -> {
                Pair<Boolean, TagObject> validatedItem = ValidateItem.isServerItem(itemStack, true);
                if(validatedItem.value1() && validatedItem.value2().contains(searchFilter.key)) {
                    yield switch (validatedItem.value2().getType(searchFilter.key)) {
                        case 3 -> MathHelper.checkOperation( searchFilter, floatValue, (float) validatedItem.value2().getInt(searchFilter.key));
                        case 5 -> MathHelper.checkOperation(searchFilter, floatValue, validatedItem.value2().getFloat(searchFilter.key));
                        default -> false;
                    };
                }
                yield false;
            }
        };
    }

    private List<String> customPetTypes() {
        return new ArrayList<>(Arrays.asList(
                "rating",
                "pet_rating",
                "lluck",
                "lscale",
                "cluck",
                "cscale",
                "lluck_percent",
                "lscale_percent",
                "cluck_percent",
                "cscale_percent"
        ));
    }
    //endregion

    //region Dev
    /// Field, Pair<Value, Tooltip>
    @Override
    protected Map<String, Pair<MutableComponent, MutableComponent>> _getFields() {
        return Map.of(
                "searchRemainder", Pair.of(Component.literal(getSearchRemainder()), Component.empty()),
                "lastInput", Pair.of(Component.literal(getLastInput()), Component.empty()),
                "isFocused", Pair.of(TextHelper.literal(isFocused()), Component.empty()),
                "filters", Pair.of(Component.literal("[filters]"), TextHelper.literal(getFilters()))
        );
    }
    //endregion
}
