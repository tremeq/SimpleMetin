package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import me.simplemetin.testutil.TestSupport;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StatsManagerTest {

    @TempDir
    Path dir;
    SimpleMetin plugin;
    UUID alice;
    UUID bob;

    @BeforeEach
    void setUp() {
        TestSupport.resetServer();
        plugin = TestSupport.plugin(dir.toFile(), new YamlConfiguration());
        alice = TestSupport.registerName("Alice");
        bob = TestSupport.registerName("bob");
    }

    StatsManager filled() {
        var m = new StatsManager(plugin);
        var a = m.getOrCreateStats(alice);
        a.addDamage(100);
        a.addCrystalDestroyed("metin_common");
        a.addCrystalDestroyed("metin_common");
        a.addCrystalDestroyed("metin_rare");
        a.addItemsReceived(5);
        a.addMoneyEarned(1000);
        var b = m.getOrCreateStats(bob);
        b.addDamage(500);
        b.addCrystalDestroyed("metin_common");
        b.addItemsReceived(50);
        b.addMoneyEarned(10);
        return m;
    }

    @Test
    @DisplayName("stats.yml round-trip incl. crystal-types")
    void roundTrip() {
        filled().saveAllStats();
        var m = new StatsManager(plugin);
        var a = m.getStats(alice);
        assertNotNull(a);
        assertEquals(3, a.getCrystalsDestroyed());
        assertEquals(100, a.getTotalDamageDealt());
        assertEquals(5, a.getItemsReceived());
        assertEquals(1000, a.getMoneyEarned());
        assertEquals(2, a.getCrystalTypeDestroyed("metin_common"));
        assertEquals(1, a.getCrystalTypeDestroyed("metin_rare"));
    }

    @Test
    @DisplayName("top lists sorted per category")
    void tops() {
        var m = filled();
        assertEquals(alice, m.getTopCrystalsDestroyed(1).get(0).getPlayerUuid());
        assertEquals(bob, m.getTopDamage(1).get(0).getPlayerUuid());
        assertEquals(bob, m.getTopItemsReceived(1).get(0).getPlayerUuid());
        assertEquals(alice, m.getTopMoneyEarned(1).get(0).getPlayerUuid());
        assertEquals(2, m.getTopDamage(100).size());
    }

    @Test
    @DisplayName("cached TOP: reused within settings.top-cache-seconds, always fresh with 0")
    void cachedTop() {
        var m = filled();
        assertEquals(bob, m.getCachedTop("damage").get(0).getPlayerUuid());

        m.getOrCreateStats(alice).addDamage(10_000);
        assertEquals(bob, m.getCachedTop("damage").get(0).getPlayerUuid(), "cache reused (default 5s)");

        plugin.getConfig().set("settings.top-cache-seconds", 0);
        sleepQuietly(5);
        assertEquals(alice, m.getCachedTop("damage").get(0).getPlayerUuid(), "rebuilt when TTL expired");
        assertEquals(alice, m.getCachedTop("money").get(0).getPlayerUuid());
        assertTrue(m.getCachedTop("unknown").isEmpty());
    }

    static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("leaderboards.yml and statistics.yml are generated")
    void filesGenerated() {
        var m = filled();
        m.saveLeaderboards();
        m.saveReadableStatistics();

        var lb = YamlConfiguration.loadConfiguration(new File(dir.toFile(), "leaderboards.yml"));
        assertEquals("bob", lb.getString("top-damage.1.player"));
        assertEquals(500, lb.getLong("top-damage.1.value"));
        assertEquals("Alice", lb.getString("top-destroyed.1.player"));
        assertEquals(2, lb.getInt("info.total-players"));

        var st = YamlConfiguration.loadConfiguration(new File(dir.toFile(), "statistics.yml"));
        assertEquals(3, st.getInt("players.Alice.crystals-destroyed"));
        assertEquals(2, st.getInt("players.Alice.crystal-types.metin_common"));
        var names = st.getConfigurationSection("players").getKeys(false).stream().toList();
        assertEquals(java.util.List.of("Alice", "bob"), names, "sorted alphabetically (case-insensitive)");
    }
}
