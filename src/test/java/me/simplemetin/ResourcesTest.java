package me.simplemetin;

import me.simplemetin.testutil.TestSupport;
import me.simplemetin.utils.ParticleUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Validates the bundled plugin.yml and config.yml against the Paper 1.21 API. */
class ResourcesTest {

    final YamlConfiguration config = TestSupport.bundledResource("config.yml");
    final YamlConfiguration pluginYml = TestSupport.bundledResource("plugin.yml");

    @Test
    @DisplayName("plugin.yml: main class, commands, alias /ma, permission, softdepend")
    void pluginYml() {
        assertEquals(SimpleMetin.class.getName(), pluginYml.getString("main"));
        assertEquals("1.21", pluginYml.getString("api-version"));
        assertEquals("1.1.0", pluginYml.getString("version"), "filtered from pom.xml");
        assertTrue(pluginYml.isConfigurationSection("commands.metin"));
        assertTrue(pluginYml.isConfigurationSection("commands.metinadmin"));
        assertTrue(pluginYml.getStringList("commands.metinadmin.aliases").contains("ma"));
        assertNull(pluginYml.getString("commands.metin.permission"), "/metin is open; subcommands check their own permission");
        assertEquals("simplemetin.strength", pluginYml.getString("commands.metinadmin.permission"));
        assertEquals("true", pluginYml.getString("permissions.simplemetin.stats.default"), "players see their own stats");
        assertEquals("op", pluginYml.getString("permissions.simplemetin.stats.others.default"));
        var children = pluginYml.getConfigurationSection("permissions.simplemetin.admin.children").getKeys(false);
        var declared = pluginYml.getConfigurationSection("permissions").getKeys(false);
        for (var child : children) assertTrue(declared.contains(child), "child permission declared: " + child);
        assertEquals("op", pluginYml.getString("permissions.simplemetin.admin.default"));
        assertTrue(pluginYml.getStringList("softdepend").contains("PlaceholderAPI"));
    }

    @Test
    @DisplayName("config.yml: settings defaults match README")
    void settingsDefaults() {
        assertEquals(300, config.getInt("settings.save-interval"));
        assertEquals(20, config.getInt("settings.hologram-update-interval"));
        assertTrue(config.getBoolean("settings.bossbar-enabled"));
        assertEquals(30.0, config.getDouble("settings.bossbar-range"));
        assertEquals(1.0, config.getDouble("settings.hit-cooldown"));
        assertEquals(6000, config.getInt("settings.leaderboard-update-interval"));
    }

    @Test
    @DisplayName("config.yml: crystal types, HP, drop materials, sounds, boss bar enums are valid")
    void crystalDefinitionsValid() {
        var crystals = config.getConfigurationSection("crystals");
        assertNotNull(crystals);
        List<String> problems = new ArrayList<>();
        for (var id : crystals.getKeys(false)) {
            var c = crystals.getConfigurationSection(id);
            var type = c.getString("type");
            if (!"respawn".equals(type) && !"one-time".equals(type)) problems.add(id + ": bad type " + type);
            if (c.getInt("max-hp") <= 0) problems.add(id + ": max-hp <= 0");
            for (var section : List.of("hit-drops", "death-drops")) {
                var drops = c.getConfigurationSection(section);
                if (drops == null) continue;
                for (var key : drops.getKeys(false)) {
                    var item = drops.getString(key + ".item");
                    if (Material.getMaterial(item) == null) problems.add(id + "." + section + "." + key + ": bad item " + item);
                }
            }
            var sound = c.getString("death-effects.sound");
            if (sound != null) {
                try {
                    Sound.valueOf(sound);
                } catch (IllegalArgumentException e) {
                    problems.add(id + ": bad sound " + sound);
                }
            }
        }
        for (var bar : List.of("personal", "global")) {
            BarColor.valueOf(config.getString("boosts.bossbar." + bar + ".color"));
            BarStyle.valueOf(config.getString("boosts.bossbar." + bar + ".style"));
        }
        assertNotNull(Material.getMaterial(config.getString("boosts.personal.voucher.material")));
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    @DisplayName("config.yml: death particles use current (1.20.5+) names")
    void deathParticlesValid() {
        ConfigurationSection crystals = config.getConfigurationSection("crystals");
        List<String> problems = new ArrayList<>();
        for (var id : crystals.getKeys(false)) {
            var particle = crystals.getString(id + ".death-effects.particle");
            if (particle == null) continue;
            try {
                Particle.valueOf(particle);
            } catch (IllegalArgumentException e) {
                problems.add(id + ": " + particle);
            }
        }
        assertTrue(problems.isEmpty(), "invalid particles: " + problems);
    }

    @Test
    @DisplayName("ParticleUtils: current names, pre-1.20.5 names and lowercase resolve; unknown -> null")
    void particleResolve() {
        assertEquals(Particle.EXPLOSION_EMITTER, ParticleUtils.resolve("EXPLOSION_EMITTER"));
        assertEquals(Particle.EXPLOSION_EMITTER, ParticleUtils.resolve("EXPLOSION_HUGE"));
        assertEquals(Particle.LARGE_SMOKE, ParticleUtils.resolve("smoke_large"));
        assertEquals(Particle.DRAGON_BREATH, ParticleUtils.resolve("DRAGON_BREATH"));
        assertNull(ParticleUtils.resolve("NOT_A_PARTICLE"));
        assertNull(ParticleUtils.resolve(null));
    }

    @Test
    @DisplayName("config.yml has no texts any more; messages-en.yml and messages-pl.yml contain the same keys")
    void messagesFiles() {
        assertFalse(config.contains("messages"), "texts moved to messages-*.yml");
        assertFalse(config.contains("boosts.bossbar.personal.title"));
        assertFalse(config.contains("boosts.personal.voucher.name"));
        assertEquals("en", config.getString("language"));
        assertTrue(config.contains("settings.actionbar-enabled"));

        var en = TestSupport.bundledResource("messages-en.yml");
        var pl = TestSupport.bundledResource("messages-pl.yml");
        assertEquals(en.getKeys(true), pl.getKeys(true), "both languages define the same keys");
        assertTrue(en.contains("boost-expired") && en.contains("global-boost-expired") && en.contains("spawner-spawned"));
    }
}
