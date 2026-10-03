package me.simplemetin.utils;

import me.simplemetin.messages.MessageManager;
import me.simplemetin.testutil.TestSupport;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

class TextAndConfigTest {

    @TempDir
    Path dir;

    @BeforeEach
    void setUp() {
        TestSupport.resetServer();
    }

    // ── TextUtils ───────────────────────────────────────────────

    @Test
    @DisplayName("legacy & codes, hex &#RRGGBB and MiniMessage tags all parse (also mixed)")
    void parseFormats() {
        var legacy = TextUtils.parse("&eYellow &lbold");
        assertEquals("Yellow bold", TextUtils.plain(legacy));
        assertEquals(NamedTextColor.YELLOW, legacy.children().isEmpty() ? legacy.color() : findColor(legacy));

        var mini = TextUtils.parse("<gradient:gold:yellow>Boost</gradient> <#ff8800>hex");
        assertEquals("Boost hex", TextUtils.plain(mini));

        var mixed = TextUtils.parse("&c[!] <green>ok</green> &#00ff00x");
        assertEquals("[!] ok x", TextUtils.plain(mixed));
        assertTrue(TextUtils.legacy(mixed).contains("§c"), TextUtils.legacy(mixed));

        assertTrue(TextUtils.legacy("&#ff8800hex").contains("§x§f§f§8§8§0§0"), "hex kept for boss bars");
        assertEquals("", TextUtils.plain(TextUtils.parse("")));
    }

    @Test
    @DisplayName("a legacy color resets bold like in vanilla (&l&e -> not bold), item text is not italic")
    void legacySemantics() {
        assertEquals("<reset><yellow>x", TextUtils.legacyToMiniMessage("&ex"));
        assertEquals("<bold><reset><yellow>x", TextUtils.legacyToMiniMessage("&l&ex"));
        assertEquals("<reset><#00ff00>x", TextUtils.legacyToMiniMessage("&#00ff00x"));
        assertEquals("plain <b>mini</b>", TextUtils.legacyToMiniMessage("plain <b>mini</b>"));

        var item = TextUtils.parseItemText("&bName");
        assertEquals(TextDecoration.State.FALSE, item.decoration(TextDecoration.ITALIC));
    }

    @Test
    @DisplayName("formatDuration: 45s, 5m, 1h 5m")
    void duration() {
        assertEquals("45s", TextUtils.formatDuration(45));
        assertEquals("5m", TextUtils.formatDuration(330));
        assertEquals("1h 5m", TextUtils.formatDuration(3900));
    }

    private static TextColor findColor(net.kyori.adventure.text.Component c) {
        if (c.color() != null) return c.color();
        for (var child : c.children()) {
            var color = findColor(child);
            if (color != null) return color;
        }
        return null;
    }

    // ── ConfigUpdater ───────────────────────────────────────────

    @Test
    @DisplayName("ConfigUpdater adds missing options with comments, keeps user values and user-owned crystal types")
    void configUpdater() throws Exception {
        var file = dir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), """
                # my comment
                settings:
                  hit-cooldown: 2.5  # mine
                crystals:
                  my_crystal:
                    max-hp: 5
                """);

        var added = ConfigUpdater.update(file, "config.yml", Set.of("crystals"));

        var updated = YamlConfiguration.loadConfiguration(file);
        assertEquals(2.5, updated.getDouble("settings.hit-cooldown"), "user value kept");
        assertTrue(updated.contains("settings.projectile-hits"), "new option added");
        assertEquals("en", updated.getString("language"));
        assertTrue(updated.contains("auto-spawn.max-alive-total"));
        assertEquals(Set.of("my_crystal"), updated.getConfigurationSection("crystals").getKeys(false),
                "default crystal types are not added to the admin's crystals");
        assertTrue(added.contains("language") && added.contains("auto-spawn"), added.toString());
        assertFalse(added.stream().anyMatch(p -> p.startsWith("auto-spawn.")), "children of new sections are not listed");

        var text = Files.readString(file.toPath());
        assertTrue(text.contains("# my comment"), "user comment kept");
        assertTrue(text.contains("Message file: en = messages-en.yml"), "comment of the new option copied:\n" + text);

        assertTrue(ConfigUpdater.update(file, "config.yml", Set.of("crystals")).isEmpty(), "second run adds nothing");
    }

    @Test
    @DisplayName("ConfigUpdater adds the whole user section when it is missing entirely")
    void configUpdaterMissingUserSection() throws Exception {
        var file = dir.resolve("config.yml").toFile();
        Files.writeString(file.toPath(), "settings:\n  hit-cooldown: 1.0\n");
        ConfigUpdater.update(file, "config.yml", Set.of("crystals"));
        var updated = YamlConfiguration.loadConfiguration(file);
        assertTrue(updated.contains("crystals.metin_common.max-hp"));
    }

    @Test
    void malformedConfigIsNeverOverwritten() throws Exception {
        var file = dir.resolve("config.yml");
        var broken = "settings:\n  hit-cooldown: [unfinished\n";
        Files.writeString(file, broken);
        assertThrows(java.io.IOException.class,
                () -> ConfigUpdater.update(file.toFile(), "config.yml", Set.of("crystals")));
        assertEquals(broken, Files.readString(file));
    }

    @Test
    void updaterDoesNotReplaceExistingScalarWithSection() throws Exception {
        var file = dir.resolve("config.yml");
        Files.writeString(file, "settings: false\ncrystals: {}\n");
        ConfigUpdater.update(file.toFile(), "config.yml", Set.of("crystals"));
        var updated = YamlConfiguration.loadConfiguration(file.toFile());
        assertEquals(false, updated.get("settings"));
        assertEquals("en", updated.getString("language"));
        assertTrue(updated.getConfigurationSection("crystals").getKeys(false).isEmpty());
    }

    // ── MessageManager ──────────────────────────────────────────

    @Test
    @DisplayName("messages: en by default, pl selectable, both files created; missing keys added to an old file")
    void languageFiles() throws Exception {
        var plugin = TestSupport.plugin(dir.toFile(), new YamlConfiguration());
        assertTrue(new File(dir.toFile(), "messages-en.yml").exists());
        assertTrue(new File(dir.toFile(), "messages-pl.yml").exists());
        assertEquals("&cYou don't have permission to use this command", plugin.getMessages().raw("no-permission"));

        // Old Polish file without new keys: they are added on the next load
        Files.writeString(dir.resolve("messages-pl.yml"), "no-permission: \"&cNIE!\"\n");
        plugin.getConfig().set("language", "pl");
        plugin.getMessages().load();
        assertEquals("&cNIE!", plugin.getMessages().raw("no-permission"), "admin's text kept");
        assertTrue(plugin.getMessages().raw("stats-none", "player", "X").contains("nie zaatakowal"), "added from the bundled pl file");
        assertTrue(Files.readString(dir.resolve("messages-pl.yml")).contains("spawner-spawned"));
    }

    @Test
    @DisplayName("messages: unknown language falls back to English; placeholders, lists and disabled ('') messages")
    void fallbackAndPlaceholders() {
        var config = new YamlConfiguration();
        config.set("language", "de");
        var plugin = TestSupport.plugin(dir.toFile(), config);
        var messages = plugin.getMessages();

        assertEquals("&cCrystal &enope&c not found", messages.raw("not-found", "id", "nope"));
        assertTrue(messages.raw("info").contains("\n"), "lists are joined with new lines");
        assertTrue(TestSupport.logged("messages-de.yml not found"));

        var player = TestSupport.player("Steve");
        messages.send(player, "not-found", "id", "x");
        verify(player).sendMessage(TestSupport.text("[SimpleMetin] Crystal x not found"));
    }

    @Test
    @DisplayName("messages from an old config.yml (section 'messages', boss bar titles, voucher texts) are moved to the messages file")
    void migrateFromConfig() throws Exception {
        var config = TestSupport.yaml("""
                messages:
                  prefix: "[OLD] "
                  removed: "gone %id%"
                boosts:
                  bossbar:
                    global:
                      title: "MY GLOBAL %multiplier%"
                  personal:
                    voucher:
                      name: "MY VOUCHER"
                """);
        var plugin = TestSupport.plugin(dir.toFile(), config);
        var messages = plugin.getMessages();

        assertEquals("gone c1", messages.raw("removed", "id", "c1"));
        assertEquals("[OLD] ", messages.raw("prefix"));
        assertEquals("MY GLOBAL 2.0", messages.raw("bossbar-boost-global", "multiplier", "2.0"));
        assertEquals("MY VOUCHER", messages.raw("voucher-name"));
        assertFalse(config.contains("messages"), "removed from config.yml");
        assertFalse(config.contains("boosts.bossbar.global.title"));
        assertTrue(Files.readString(dir.resolve(MessageManager.fileName("en"))).contains("gone %id%"));
    }
}
