package me.simplemetin.messages;

import me.simplemetin.SimpleMetin;
import me.simplemetin.utils.ConfigUpdater;
import me.simplemetin.utils.TextUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Messages from messages-&lt;language&gt;.yml (config.yml: language). Missing keys fall back to the bundled
 * file of that language, then to English, so a message is never blank because of an old file.
 */
public class MessageManager {

    public static final List<String> BUNDLED_LANGUAGES = List.of("en", "pl");
    public static final String DEFAULT_LANGUAGE = "en";

    /** Texts that used to live in config.yml, moved to the messages file on the first start of this version. */
    private static final Map<String, String> LEGACY_CONFIG_PATHS = Map.of(
            "boosts.bossbar.personal.title", "bossbar-boost-personal",
            "boosts.bossbar.global.title", "bossbar-boost-global",
            "boosts.personal.voucher.name", "voucher-name",
            "boosts.personal.voucher.lore", "voucher-lore"
    );

    private final SimpleMetin plugin;
    private YamlConfiguration messages = new YamlConfiguration();
    private String language = DEFAULT_LANGUAGE;

    public MessageManager(SimpleMetin plugin) {
        this.plugin = plugin;
    }

    /** Creates/updates the bundled language files, migrates old config.yml texts and loads the chosen language. */
    public void load() {
        language = plugin.getConfig().getString("language", DEFAULT_LANGUAGE).toLowerCase();
        var folder = plugin.getDataFolder();

        if (folder != null) {
            for (var lang : BUNDLED_LANGUAGES) {
                var file = new File(folder, fileName(lang));
                try {
                    if (!ConfigUpdater.saveIfMissing(fileName(lang), file)) {
                        var added = ConfigUpdater.update(file, fileName(lang), Set.of());
                        if (!added.isEmpty()) {
                            plugin.getLogger().info("Added " + added.size() + " new message(s) to " + file.getName() + ": " + String.join(", ", added));
                        }
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not update " + file.getName() + ": " + e.getMessage());
                }
            }
            migrateFromConfig(new File(folder, fileName(language)));
        }

        var bundled = BUNDLED_LANGUAGES.contains(language) ? language : DEFAULT_LANGUAGE;
        var defaults = ConfigUpdater.loadResource(fileName(bundled));
        if (!bundled.equals(DEFAULT_LANGUAGE)) {
            defaults.setDefaults(ConfigUpdater.loadResource(fileName(DEFAULT_LANGUAGE)));
        }

        var file = folder != null ? new File(folder, fileName(language)) : null;
        if (file != null && file.exists()) {
            messages = YamlConfiguration.loadConfiguration(file);
        } else {
            if (file != null) {
                plugin.getLogger().warning("Language file " + file.getName() + " not found, using " + fileName(bundled));
            }
            messages = new YamlConfiguration();
        }
        messages.setDefaults(defaults);
    }

    /**
     * Older versions kept messages in config.yml (section "messages" and a few boost/voucher texts).
     * They are copied into the active language file - keeping the admin's customisations - and removed from config.yml.
     */
    private void migrateFromConfig(File target) {
        var config = plugin.getConfig();
        var moved = new ArrayList<String>();
        var targetConfig = YamlConfiguration.loadConfiguration(target);

        var oldMessages = config.getConfigurationSection("messages");
        if (oldMessages != null) {
            for (var key : oldMessages.getKeys(false)) {
                targetConfig.set(key, oldMessages.get(key));
                moved.add(key);
            }
            config.set("messages", null);
        }
        for (var entry : LEGACY_CONFIG_PATHS.entrySet()) {
            if (config.contains(entry.getKey(), true)) {
                targetConfig.set(entry.getValue(), config.get(entry.getKey()));
                config.set(entry.getKey(), null);
                moved.add(entry.getValue());
            }
        }
        if (moved.isEmpty()) return;

        try {
            targetConfig.save(target);
            plugin.saveConfig();
            plugin.getLogger().info("Moved " + moved.size() + " message(s) from config.yml to " + target.getName());
        } catch (IOException e) {
            plugin.getLogger().warning("Could not move messages to " + target.getName() + ": " + e.getMessage());
        }
    }

    public static String fileName(String lang) {
        return "messages-" + lang + ".yml";
    }

    public String getLanguage() {
        return language;
    }

    // ── Raw access ──────────────────────────────────────────────

    /** Raw text with placeholders replaced; lists are joined with new lines. Empty string = disabled. */
    public String raw(String key, Object... placeholders) {
        String text;
        if (messages.isList(key)) {
            text = String.join("\n", messages.getStringList(key));
        } else {
            // getString(key) - unlike getString(key, def) - falls back to the bundled defaults
            text = messages.getString(key);
        }
        return replace(text == null ? "" : text, placeholders);
    }

    public List<String> rawList(String key, Object... placeholders) {
        var single = messages.getString(key);
        var lines = messages.isList(key) ? messages.getStringList(key) : List.of(single == null ? "" : single);
        return lines.stream().map(line -> replace(line, placeholders)).toList();
    }

    public boolean isDisabled(String key) {
        return raw(key).isEmpty();
    }

    /** Formatted component (legacy + MiniMessage), without prefix. */
    public Component get(String key, Object... placeholders) {
        return TextUtils.parse(raw(key, placeholders));
    }

    /** Section-sign string for string-only APIs (Bukkit boss bars). */
    public String legacy(String key, Object... placeholders) {
        return TextUtils.legacy(get(key, placeholders));
    }

    // ── Sending ─────────────────────────────────────────────────

    /** Sends the message with the prefix; disabled ("") messages are skipped. */
    public void send(CommandSender sender, String key, Object... placeholders) {
        var text = raw(key, placeholders);
        if (text.isEmpty()) return;
        sender.sendMessage(TextUtils.parse(raw("prefix") + text));
    }

    /** Sends the message without the prefix (help, lists, multi-line blocks). */
    public void sendRaw(CommandSender sender, String key, Object... placeholders) {
        var text = raw(key, placeholders);
        if (text.isEmpty()) return;
        sender.sendMessage(TextUtils.parse(text));
    }

    public void actionBar(Player player, String key, Object... placeholders) {
        var text = raw(key, placeholders);
        if (text.isEmpty()) return;
        player.sendActionBar(TextUtils.parse(text));
    }

    /** Broadcasts to all players and the console, with the prefix. */
    public void broadcast(String key, Object... placeholders) {
        var text = raw(key, placeholders);
        if (text.isEmpty()) return;
        Bukkit.broadcast(TextUtils.parse(raw("prefix") + text));
    }

    /** Replaces %name% placeholders given as alternating name/value pairs. */
    static String replace(String text, Object... placeholders) {
        if (text == null) return "";
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace("%" + placeholders[i] + "%", String.valueOf(placeholders[i + 1]));
        }
        return text;
    }
}
