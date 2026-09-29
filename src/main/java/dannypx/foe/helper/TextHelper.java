package dannypx.foe.helper;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dannypx.foe.handler.io.DataModels;
import dannypx.foe.item.TagObject;
import dannypx.foe.type.tuple.Pair;
import dannypx.foe.type.type_adapter.ItemStackAdapter;
import dannypx.foe.type.type_adapter.ComponentAdapter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TextHelper {
    private static final GsonBuilder gson = new GsonBuilder();

    public static MutableComponent concat(Component... components) {
        MutableComponent component = Component.empty();
        for (Component t : components) {
            component.append(t);
        }
        return component;
    }

    public static MutableComponent literal(boolean b) {
        return Component.literal(Boolean.toString(b));
    }

    public static MutableComponent literal(boolean b, boolean fancy) {
        return fancy ? (
                    b
                    ? Component.literal("✔").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    : Component.literal("✖").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                ) : literal(b);
    }

    public static MutableComponent literal(DataModels.DataModel dataModel) {
        return Component.literal(gson
                .registerTypeAdapter(ItemStack.class, new ItemStackAdapter())
                .registerTypeAdapter(Component.class, new ComponentAdapter())
                .setPrettyPrinting()
                .create().toJson(dataModel));
    }

    public static MutableComponent literal(int i) {
        return Component.literal(Integer.toString(i));
    }

    public static MutableComponent literal(char c) {
        return Component.literal(String.valueOf(c));
    }

    public static MutableComponent literal(float f) {
        return Component.literal(Float.toString(f));
    }

    public static MutableComponent literal(Component component) {
        return concat(component);
    }

    @SuppressWarnings("unchecked")
    public static <T> MutableComponent literal(List<T> list) {
        try {
            if(!list.isEmpty()) {
                Object first = list.getFirst();
                if(first instanceof ItemStack) return Component.empty().append(ItemStackHelper.itemStackListToJson((List<ItemStack>) list));
                return Component.literal(
                        gson.setPrettyPrinting()
                                .registerTypeAdapter(ItemStack.class, new ItemStackAdapter())
                                .registerTypeAdapter(Component.class, new ComponentAdapter())
                                .create()
                                .toJson(list)
                );
            }
            return Component.empty();
        } catch (IllegalStateException e) {
            return Component.empty();
        }


    }

    public static MutableComponent literal(String s) {
        return Component.empty().append(Component.literal(s));
    }

    public static MutableComponent literal(ItemStack i) {
        return Component.empty().append(ItemStackHelper.itemStackToJson(i));
    }

    public static MutableComponent literal(TagObject currentHeldItem) {
        return TextHelper.concat(
                Component.literal("name: "), currentHeldItem.getName(), Component.literal("\n"),
                Component.literal("rarity: "), Component.literal(currentHeldItem.getRarity()), Component.literal("\n"),
                Component.literal("type: "), Component.literal(currentHeldItem.getType())
        );
    }

    public static String componentListToJson(List<Component> list) {
        return gson.setPrettyPrinting().create().toJson(ComponentSerialization.CODEC.listOf().encodeStart(JsonOps.INSTANCE, list).getOrThrow());
    }

    public static String smallCaps(String string) {
        return smallLetter(smallNumber(string));
    }

    public static char smallChar(char c) {
        if(isNumber(c)) {
            return smallNumber(c);
        } else if(isLetter(c)) {
            return smallLetter(c);
        } else {
            return c;
        }
    }

    public static String smallNumber(String string) {
        // based on numeric ping
        char[] characters = new char[string.length()];

        for (int index = 0; index < string.length(); index++) {
            characters[index] = string.charAt(index);

            if (isNumber(characters[index]))
                characters[index] = smallNumber(characters[index]);
        }

        return String.valueOf(characters);
    }

    public static char smallNumber(char c) {
        return (char) (c + 8272);
    }

    public static String smallLetter(String string) {
        // based on numeric ping
        char[] characters = new char[string.length()];

        for (int index = 0; index < string.length(); index++) {
            characters[index] = string.charAt(index);

            characters[index] = smallLetter(characters[index]);
        }

        return String.valueOf(characters);
    }

    public static char smallLetter(char c) {
        return switch (c) {
            case 'A', 'a' -> 'ᴀ';
            case 'B', 'b' -> 'ʙ';
            case 'C', 'c' -> 'ᴄ';
            case 'D', 'd' -> 'ᴅ';
            case 'E', 'e' -> 'ᴇ';
            case 'F', 'f' -> 'ꜰ';
            case 'G', 'g' -> 'ɢ';
            case 'H', 'h' -> 'ʜ';
            case 'I', 'i' -> 'ɪ';
            case 'J', 'j' -> 'ᴊ';
            case 'K', 'k' -> 'ᴋ';
            case 'L', 'l' -> 'ʟ';
            case 'M', 'm' -> 'ᴍ';
            case 'N', 'n' -> 'ɴ';
            case 'O', 'o' -> 'ᴏ';
            case 'P', 'p' -> 'ᴘ';
            case 'Q', 'q' -> 'ꞯ';
            case 'R', 'r' -> 'ʀ';
            case 'S', 's' -> 's';
            case 'T', 't' -> 'ᴛ';
            case 'U', 'u' -> 'ᴜ';
            case 'V', 'v' -> 'ᴠ';
            case 'W', 'w' -> 'ᴡ';
            case 'X', 'x' -> 'x';
            case 'Y', 'y' -> 'ʏ';
            case 'Z', 'z' -> 'ᴢ';
            default -> c;
        };
    }

    public static String normalLetter(String string) {
        // based on numeric ping
        char[] characters = new char[string.length()];

        for (int index = 0; index < string.length(); index++) {
            characters[index] = string.charAt(index);

            characters[index] = normalLetter(characters[index]);
        }

        return String.valueOf(characters);
    }

    public static char normalLetter(char c) {
        return switch (c) {
            case 'ᴀ' -> 'a';
            case 'ʙ' -> 'b';
            case 'ᴄ' -> 'c';
            case 'ᴅ' -> 'd';
            case 'ᴇ' -> 'e';
            case 'ꜰ' -> 'f';
            case 'ɢ' -> 'g';
            case 'ʜ' -> 'h';
            case 'ɪ' -> 'i';
            case 'ᴊ' -> 'j';
            case 'ᴋ' -> 'k';
            case 'ʟ' -> 'l';
            case 'ᴍ' -> 'm';
            case 'ɴ' -> 'n';
            case 'ᴏ' -> 'o';
            case 'ᴘ' -> 'p';
            case 'ꞯ' -> 'q';
            case 'ʀ' -> 'r';
            case 's' -> 's';
            case 'ᴛ' -> 't';
            case 'ᴜ' -> 'u';
            case 'ᴠ' -> 'v';
            case 'ᴡ' -> 'w';
            case 'x' -> 'x';
            case 'ʏ' -> 'y';
            case 'ᴢ' -> 'z';
            default -> c;
        };
    }

    public static boolean isNumber(char c) {
        return (c >= '0' && c <= '9');
    }

    public static boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    public static boolean isSmallNumber(char c) {
        return (c >= '₀' && c <= '₉');
    }

    public static boolean isSmallLetter(char c) {
        return switch (c) {
            case 'ᴀ', 'ʙ', 'ᴄ', 'ᴅ', 'ᴇ', 'ꜰ', 'ɢ', 'ʜ', 'ɪ', 'ᴊ', 'ᴋ', 'ʟ', 'ᴍ', 'ɴ', 'ᴏ', 'ᴘ', 'ꞯ', 'ʀ', 's', 'ᴛ',
                 'ᴜ', 'ᴠ', 'ᴡ', 'x', 'ʏ', 'ᴢ', '.', ',', ':', ';', '_' -> true;
            default -> false;
        };
    }

    public static boolean isCustomFont(char c) {
        return (c >= '\uF000' && c <= '\uF999');
    }

    public static boolean isRank(char c) {
        return (c >= '\uF028' && c <= '\uF032');
    }

    public static String shortenNumber(float d, int decimals) {
        if(d >= 1000 && d < 1000000) {
            String s = String.format(Locale.US, "%." + decimals + "f", d / 1000);
            return (s.contains(".") ? s.replaceAll("0*$","").replaceAll("\\.$","") : s) + "K";
        } else if (d >= 1000000 && d < 1000000000 ){
            String s = String.format(Locale.US, "%." + decimals + "f", d / 1000000);
            return (s.contains(".") ? s.replaceAll("0*$","").replaceAll("\\.$","") : s) + "M";
        } else if (d >= 1000000000) {
            String s = String.format(Locale.US, "%." + decimals + "f", d / 1000000000);
            return (s.contains(".") ? s.replaceAll("0*$","").replaceAll("\\.$","") : s) + "B";
        } else {
            String s = String.format(Locale.US, "%.0f", d);
            return s.contains(".") ? s.replaceAll("0*$","").replaceAll("\\.$","") : s;
        }
    }

    public static int toIntFromString(String value) {
        value = value.trim().replace(",", "");
        if(value.contains("K")) {
            return (int) (Float.parseFloat(value.substring(0, value.indexOf("K"))) * 1000f);
        } else if(value.contains("M")) {
            return (int) (Float.parseFloat(value.substring(0, value.indexOf("M"))) * 1000000f);
        } else {
            return Integer.parseInt(value);
        }
    }

    public static String floatToString(float f) {
        return floatToString(f, 0);
    }

    public static String floatToString(float f, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", f);
    }

    public static String doubleToString(double d) {
        return doubleToString(d, 0);
    }

    public static String doubleToString(double d, int decimals) {
        return floatToString(Double.valueOf(d).floatValue(), decimals);
    }

    public static float lbToKg(float f) {
        return f * 0.4535924f;
    }

    public static float inchToCm(float f) {
        return f * 2.54f;
    }

    public static String capitalize(String s) {
        return StringUtils.capitalize(s);
    }

    public static String splitTitleCase(String s) {
        return String.join(" ", s.split("(?<!^)(?=[A-Z])"));
    }

    public static String convertField(String s) {
        return splitTitleCase(capitalize(s).replace("_", " "));
    }

    public static int getWidth(Font font, Component component, boolean isSmall) {
        AtomicInteger width = new AtomicInteger(0);
        if(component.getSiblings().isEmpty()) {
            int calculatedWidth = font.width(component);
            if(isSmall) {
                calculatedWidth = font.width(Component.literal(TextHelper.smallCaps(component.getString())).setStyle(component.getStyle()));
            }
            width.set(width.get() + calculatedWidth);
        } else {
            component.getSiblings().forEach(text1 -> {
                int calculatedWidth = getWidth(font, text1, isSmall);
                width.set(width.get() + calculatedWidth);
            });
        }
        return width.get();
    }

    public static String componentToJson(Component component) {
        return gson.create().toJson(ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component).getOrThrow());
    }

    public static String componentToJsonPretty(Component component) {
        return gson.setPrettyPrinting().create().toJson(ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component).getOrThrow());
    }

    public static Pair<MutableComponent, Style> parseLegacyWithStyle(String input, Style startingStyle) {
        MutableComponent component = Component.empty();
        Pattern pattern = Pattern.compile("(§#[0-9A-Fa-f]{6}|§[0-9A-FK-ORa-fk-or])");
        Matcher matcher = pattern.matcher(input);

        int lastEnd = 0;
        Style currentStyle = startingStyle;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                component.append(Component.literal(input.substring(lastEnd, matcher.start())).setStyle(currentStyle));
            }

            String code = matcher.group();
            if (code.equalsIgnoreCase("§r")) {
                currentStyle = Style.EMPTY;
            } else if (code.startsWith("§#")) {
                int rgb = Integer.parseInt(code.substring(2), 16);
                currentStyle = currentStyle.withColor(TextColor.fromRgb(rgb));
            } else {
                ChatFormatting fmt = ChatFormatting.getByCode(code.charAt(1));
                currentStyle = currentStyle.applyFormat(fmt);
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < input.length()) {
            component.append(Component.literal(input.substring(lastEnd)).setStyle(currentStyle));
        }

        return Pair.of(component, currentStyle);
    }

    public static Pair<MutableComponent, Style> parseLegacyWithStyle(String input) {
        return parseLegacyWithStyle(input, Style.EMPTY);
    }

    public static List<Component> wrapStyledComponent(Component component, int maxWidth, boolean smallCaps, Font font) {
        List<Component> lines = new ArrayList<>();
        AtomicReference<MutableComponent> currentLine = new AtomicReference<>(Component.empty());
        AtomicInteger currentLineWidth = new AtomicInteger();

        List<Component> currentWord = new ArrayList<>();
        AtomicInteger currentWordWidth = new AtomicInteger();

        component.visit((style, string) -> {
            for (char c : string.toCharArray()) {
                if (c == ' ') {
                    if (currentLineWidth.get() + currentWordWidth.get() > maxWidth && currentLineWidth.get() > 0) {
                        lines.add(currentLine.get());
                        currentLine.set(Component.empty());
                        currentLineWidth.set(0);
                    }

                    for (Component part : currentWord) {
                        currentLine.get().append(part);
                    }

                    Component space = Component.literal(" ").setStyle(style);
                    currentLine.get().append(space);

                    currentLineWidth.addAndGet(currentWordWidth.get() + TextHelper.getWidth(font, Component.literal(" "), smallCaps));

                    currentWord.clear();
                    currentWordWidth.set(0);
                    continue;
                }

                if (c == '\n') {
                    for (Component part : currentWord) {
                        currentLine.get().append(part);
                    }
                    currentWord.clear();
                    currentWordWidth.set(0);

                    lines.add(currentLine.get());
                    currentLine.set(Component.empty());
                    currentLineWidth.set(0);
                    continue;
                }

                Component charComponent = Component.literal(String.valueOf(c)).setStyle(style);
                currentWord.add(charComponent);

                currentWordWidth.addAndGet(TextHelper.getWidth(font, Component.literal(String.valueOf(c)), smallCaps));
            }
            return Optional.empty();
        }, Style.EMPTY);

        if (!currentWord.isEmpty()) {
            if (currentWordWidth.get() > maxWidth) {
                for (Component part : currentWord) {
                    int charWidth = TextHelper.getWidth(font, part, smallCaps);
                    if (currentLineWidth.get() + charWidth > maxWidth && currentLineWidth.get() > 0) {
                        lines.add(currentLine.get());
                        currentLine.set(Component.empty());
                        currentLineWidth.set(0);
                    }
                    currentLine.get().append(part);
                    currentLineWidth.addAndGet(charWidth);
                }
            } else {
                if (currentLineWidth.get() + currentWordWidth.get() > maxWidth && currentLineWidth.get() > 0) {
                    lines.add(currentLine.get());
                    currentLine.set(Component.empty());
                    currentLineWidth.set(0);
                }
                for (Component part : currentWord) currentLine.get().append(part);
                currentLineWidth.addAndGet(currentWordWidth.get());
            }
        }

        if (!currentLine.get().getString().isEmpty()) {
            lines.add(currentLine.get());
        }

        return lines;
    }

    public static MutableComponent substring(Component component, int start) {
        return substring(component, start, component.getString().length());
    }

    public static MutableComponent substring(Component component, int start, int end) {
        int length = component.getString().length();

        if (start < 0 || end < 0 || start > end || end > length) {
            return Component.empty();
        }

        MutableComponent result = Component.empty();
        AtomicInteger index = new AtomicInteger(0);

        component.visit((style, string) -> {
            int strStart = index.get();
            int strEnd = strStart + string.length();

            if (strEnd > start && strStart < end) {
                int from = Math.max(0, start - strStart);
                int to = Math.min(string.length(), end - strStart);

                String sub = string.substring(from, to);
                result.append(Component.literal(sub).setStyle(style));
            }

            index.addAndGet(string.length());
            return Optional.empty();
        }, Style.EMPTY);

        return result;
    }

    public static MutableComponent toLowercase(Component component) {
        MutableComponent result = Component.empty();

        component.visit((style, string) -> {
            result.append(Component.literal(string.toLowerCase(Locale.US)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);

        return result;
    }

    public static MutableComponent toUppercase(Component component) {
        MutableComponent result = Component.empty();

        component.visit((style, string) -> {
            result.append(Component.literal(string.toUpperCase(Locale.US)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);

        return result;
    }

    public static MutableComponent replace(Component component, String target, String replacement) {
        if (target.isEmpty()) {
            return component.copy();
        }

        String flat = component.getString();
        List<Integer> matchStarts = new ArrayList<>();
        int searchFrom = 0;
        int found;
        while ((found = flat.indexOf(target, searchFrom)) != -1) {
            matchStarts.add(found);
            searchFrom = found + target.length();
        }

        if (matchStarts.isEmpty()) {
            return component.copy();
        }

        MutableComponent result = Component.empty();
        int cursor = 0;
        for (int matchStart : matchStarts) {
            int matchEnd = matchStart + target.length();

            result.append(substring(component, cursor, matchStart));

            Style replacementStyle = matchStart > 0 ? styleAt(component, matchStart - 1) : Style.EMPTY;
            result.append(Component.literal(replacement).setStyle(replacementStyle));

            cursor = matchEnd;
        }
        result.append(substring(component, cursor));

        return result;
    }

    public static MutableComponent replaceFirst(Component component, String target, String replacement) {
        if (target.isEmpty()) {
            return component.copy();
        }

        String flat = component.getString();
        int matchStart = flat.indexOf(target);
        if (matchStart == -1) {
            return component.copy();
        }
        int matchEnd = matchStart + target.length();

        MutableComponent before = substring(component, 0, matchStart);
        MutableComponent after = substring(component, matchEnd);
        Style replacementStyle = matchStart > 0 ? styleAt(component, matchStart - 1) : Style.EMPTY;

        return before.append(Component.literal(replacement).setStyle(replacementStyle)).append(after);
    }

    private static Style styleAt(Component component, int index) {
        AtomicInteger cursor = new AtomicInteger(0);
        AtomicReference<Style> found = new AtomicReference<>(Style.EMPTY);

        component.visit((style, string) -> {
            int stringStart = cursor.get();
            int stringEnd = stringStart + string.length();
            if (index >= stringStart && index < stringEnd) {
                found.set(style);
            }
            cursor.addAndGet(string.length());
            return Optional.empty();
        }, Style.EMPTY);

        return found.get();
    }

    public static MutableComponent trim(Component component) {
        String full = component.getString();
        int length = full.length();

        int start = 0;
        while (start < length && Character.isWhitespace(full.charAt(start))) {
            start++;
        }

        int end = length;
        while (end > start && Character.isWhitespace(full.charAt(end - 1))) {
            end--;
        }

        if (start >= end) {
            return Component.empty();
        }

        return substring(component, start, end);
    }

    public static MutableComponent capitalize(Component component) {
        MutableComponent result = Component.empty();
        final boolean[] visited = {false};

        component.visit((style, string) -> {
            if(!visited[0]) {
                result.append(Component.literal(StringUtils.capitalize(string)).setStyle(style));
                visited[0] = true;
            } else {
                result.append(Component.literal(string).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        return result;
    }

    public static MutableComponent reverse(Component component) {
        AtomicReference<MutableComponent> result = new AtomicReference<>(Component.empty());

        component.visit((style, string) -> {
            result.set(Component.literal(new StringBuilder(string).reverse().toString()).setStyle(style).append(result.get()));
            return Optional.empty();
        }, Style.EMPTY);

        return result.get();
    }

    public static byte[] compress(final String str) throws IOException {
        if ((str == null) || (str.isEmpty())) {
            return null;
        }
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write(str.getBytes("UTF-8"));
        gzip.flush();
        gzip.close();
        return obj.toByteArray();
    }

    public static String decompress(final byte[] compressed) throws IOException {
        final StringBuilder outStr = new StringBuilder();
        if ((compressed == null) || (compressed.length == 0)) {
            return "";
        }
        if (isCompressed(compressed)) {
            final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, "UTF-8"));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                outStr.append(line);
            }
        } else {
            outStr.append(compressed);
        }
        return outStr.toString();
    }

    public static boolean isCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    public static Component jsonToComponent(String json) {
        return ComponentSerialization.CODEC
                .decode(JsonOps.INSTANCE, gson.create().fromJson(json, JsonElement.class))
                .mapOrElse((com.mojang.datafixers.util.Pair::getFirst), (pairError -> Component.empty()));

    }
}
