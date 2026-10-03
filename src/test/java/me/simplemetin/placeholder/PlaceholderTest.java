package me.simplemetin.placeholder;

import me.simplemetin.SimpleMetin;
import me.simplemetin.managers.BoostManager;
import me.simplemetin.managers.PlayerStatsManager;
import me.simplemetin.managers.StatsManager;
import me.simplemetin.testutil.TestSupport;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlaceholderTest {

    @TempDir
    Path dir;
    SimpleMetin plugin;
    StatsManager stats;
    BoostManager boosts;
    MetinPlaceholder metin;
    Player steve;

    @BeforeEach
    void setUp() {
        TestSupport.resetServer();
        plugin = TestSupport.plugin(dir.toFile(), TestSupport.yaml("boosts:\n  bossbar:\n    enabled: false\n"));
        stats = new StatsManager(plugin);
        boosts = new BoostManager(plugin);
        when(plugin.getStatsManager()).thenReturn(stats);
        when(plugin.getBoostManager()).thenReturn(boosts);
        metin = new MetinPlaceholder(plugin);
        steve = TestSupport.player("Steve");

        var s = stats.getOrCreateStats(steve.getUniqueId());
        s.addDamage(123);
        s.addCrystalDestroyed("metin_common");
        s.addItemsReceived(4);
        s.addMoneyEarned(900);
        var alex = stats.getOrCreateStats(TestSupport.registerName("Alex"));
        alex.addDamage(999);
    }

    @Test
    @DisplayName("%metin_<stat>% and %metin_type_<id>%")
    void statPlaceholders() {
        assertEquals("1", metin.onRequest(steve, "crystals_destroyed"));
        assertEquals("123", metin.onRequest(steve, "total_damage"));
        assertEquals("4", metin.onRequest(steve, "items_received"));
        assertEquals("900", metin.onRequest(steve, "money_earned"));
        assertEquals("1", metin.onRequest(steve, "type_metin_common"));
        assertEquals("0", metin.onRequest(steve, "type_metin_rare"));
    }

    @Test
    @DisplayName("player without stats -> 0")
    void noStats() {
        var p = TestSupport.player("Newbie");
        assertEquals("0", metin.onRequest(p, "crystals_destroyed"));
        assertEquals("0", metin.onRequest(p, "type_metin_common"));
    }

    @Test
    @DisplayName("%metin_<cat>_top_N% / _name, out of range -> 0 / -")
    void topPlaceholders() {
        assertEquals("Alex", metin.onRequest(steve, "damage_top_1_name"));
        assertEquals("999", metin.onRequest(steve, "damage_top_1"));
        assertEquals("Steve", metin.onRequest(steve, "damage_top_2_name"));
        assertEquals("123", metin.onRequest(steve, "damage_top_2"));
        assertEquals("Steve", metin.onRequest(steve, "destroyed_top_1_name"));
        assertEquals("900", metin.onRequest(steve, "money_top_1"));
        assertEquals("4", metin.onRequest(steve, "items_top_1"));
        assertEquals("-", metin.onRequest(steve, "damage_top_50_name"));
        assertEquals("0", metin.onRequest(steve, "damage_top_50"));
    }

    @Test
    @DisplayName("boost placeholders")
    void boostPlaceholders() {
        assertEquals("false", metin.onRequest(steve, "boost_active"));
        assertEquals("1.0", metin.onRequest(steve, "global_boost_multiplier"));
        boosts.activateGlobalBoost(2.0, 3700);
        boosts.activatePersonalBoost(steve, 1.5, 90);
        assertEquals("true", metin.onRequest(steve, "boost_active"));
        assertEquals("2.5", metin.onRequest(steve, "boost_multiplier"), "additive: 1 + (2.0-1) + (1.5-1)");
        assertEquals("2.0", metin.onRequest(steve, "global_boost_multiplier").replace(',', '.'));
        assertEquals("1.5", metin.onRequest(steve, "personal_boost_multiplier").replace(',', '.'));
        assertTrue(metin.onRequest(steve, "global_boost_time").startsWith("1h 1m"));
        assertTrue(metin.onRequest(steve, "personal_boost_time").startsWith("1m"));

        OfflinePlayer offline = mock(OfflinePlayer.class);
        when(offline.getUniqueId()).thenReturn(UUID.randomUUID());
        assertEquals("1.0", metin.onRequest(offline, "boost_multiplier"));
    }

    @Test
    @DisplayName("%metin_damage_top_-1% / _top_0 return 0 / - instead of throwing")
    void negativeTopPosition() {
        assertEquals("0", assertDoesNotThrow(() -> metin.onRequest(steve, "damage_top_-1")));
        assertEquals("-", metin.onRequest(steve, "damage_top_0_name"));
    }

    @Test
    @DisplayName("TOP placeholders also work without a player (global holograms)")
    void topWithoutPlayer() {
        assertEquals("Alex", metin.onRequest(null, "damage_top_1_name"));
        assertEquals("", metin.onRequest(null, "total_damage"));
    }

    @Test
    @DisplayName("boost placeholders use a dot regardless of the system locale (\"2.0\", not \"2,0\")")
    void boostPlaceholderLocale() {
        var previous = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("pl-PL"));
            boosts.activateGlobalBoost(2.0, 60);
            assertEquals("2.0", metin.onRequest(steve, "global_boost_multiplier"));
        } finally {
            java.util.Locale.setDefault(previous);
        }
    }

    @Test
    @DisplayName("%simplemetin_damage% and %simplemetin_top_name_N% / _value_N")
    void simplemetinPlaceholders() throws Exception {
        var a = TestSupport.registerName("Alpha");
        var b = TestSupport.registerName("Beta");
        Files.writeString(dir.resolve("players.yml"), a + ":\n  damage: 50\n" + b + ":\n  damage: 80\n");
        var psm = new PlayerStatsManager(plugin);
        when(plugin.getPlayerStatsManager()).thenReturn(psm);
        var provider = new PlaceholderProvider(plugin);

        assertEquals("simplemetin", provider.getIdentifier());
        assertEquals("metin", metin.getIdentifier());
        assertEquals("10", provider.onRequest(steve, "damage"));
        var alpha = mock(OfflinePlayer.class);
        when(alpha.getUniqueId()).thenReturn(a);
        assertEquals("50", provider.onRequest(alpha, "damage"));
        assertEquals("Beta", provider.onRequest(steve, "top_name_1"));
        assertEquals("80", provider.onRequest(steve, "top_value_1"));
        assertEquals("Alpha", provider.onRequest(steve, "top_name_2"));
        assertEquals("-", provider.onRequest(steve, "top_name_3"));
        assertEquals("0", provider.onRequest(steve, "top_value_3"));
        assertNull(provider.onRequest(steve, "unknown"));
    }
}
