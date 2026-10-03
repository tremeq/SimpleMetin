package me.simplemetin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text parsing shared by messages, holograms, boss bars and items.
 * Both formats work, also mixed in one string: legacy codes (&e, &l, &#RRGGBB, §e) and MiniMessage
 * (&lt;gradient:gold:yellow&gt;, &lt;#ff8800&gt;, &lt;hover&gt;, &lt;click&gt;...).
 */
public final class TextUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final Pattern HEX = Pattern.compile("[&§]#([0-9a-fA-F]{6})");
    private static final Pattern LEGACY = Pattern.compile("[&§]([0-9a-fk-orA-FK-OR])");

    private static final Map<Character, String> CODES = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"), Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"), Map.entry('7', "gray"), Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
            Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"),
            Map.entry('f', "white"), Map.entry('k', "obfuscated"), Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"), Map.entry('n', "underlined"), Map.entry('o', "italic"),
            Map.entry('r', "reset")
    );

    private TextUtils() {
    }

    /** Parses legacy (&amp;) and MiniMessage formatting into a component. */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MINI_MESSAGE.deserialize(legacyToMiniMessage(text));
    }

    /** Like {@link #parse(String)} but without the italic style Minecraft applies to item names and lore. */
    public static Component parseItemText(String text) {
        return parse(text).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Section-sign string for APIs that still take strings (Bukkit boss bars). Hex colors are kept. */
    public static String legacy(Component component) {
        return SECTION.serialize(component);
    }

    public static String legacy(String text) {
        return legacy(parse(text));
    }

    /** Duration for messages: "1h 5m", "5m", "45s" (same format as before the messages files). */
    public static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m";
        return Math.max(0, seconds) + "s";
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Converts legacy codes to MiniMessage tags. A legacy color resets previous formatting
     * (that is how "&amp;l&amp;e" behaves in vanilla), so colors are emitted as &lt;reset&gt;&lt;color&gt;.
     */
    static String legacyToMiniMessage(String text) {
        if (text.indexOf('&') < 0 && text.indexOf('§') < 0) return text;

        Matcher hex = HEX.matcher(text);
        text = hex.replaceAll(m -> "<reset><#" + m.group(1) + ">");

        Matcher legacy = LEGACY.matcher(text);
        var out = new StringBuilder();
        while (legacy.find()) {
            char code = Character.toLowerCase(legacy.group(1).charAt(0));
            String tag = CODES.get(code);
            boolean isColor = (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
            String replacement = code == 'r' ? "<reset>" : (isColor ? "<reset><" + tag + ">" : "<" + tag + ">");
            legacy.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        legacy.appendTail(out);
        return out.toString();
    }
}
