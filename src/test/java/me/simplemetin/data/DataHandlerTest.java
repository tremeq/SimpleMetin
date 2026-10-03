package me.simplemetin.data;

import me.simplemetin.SimpleMetin;
import me.simplemetin.managers.BoostManager;
import me.simplemetin.managers.CrystalManager;
import me.simplemetin.managers.HologramManager;
import me.simplemetin.managers.PlayerStatsManager;
import me.simplemetin.managers.StatsManager;
import me.simplemetin.managers.CrystalManagerTest;
import me.simplemetin.testutil.TestSupport;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataHandlerTest {

    @TempDir
    Path dir;
    List<Entity> spawned;
    World world;

    record Env(SimpleMetin plugin, CrystalManager manager, DataHandler data) {
    }

    @BeforeEach
    void setUp() {
        TestSupport.resetServer();
        spawned = new ArrayList<>();
        world = TestSupport.world("world", spawned);
    }

    Env newEnv() {
        YamlConfiguration config = TestSupport.yaml(CrystalManagerTest.config());
        SimpleMetin plugin = TestSupport.plugin(dir.toFile(), config);
        var playerStats = mock(PlayerStatsManager.class);
        when(playerStats.getDamage(any())).thenReturn(10);
        var stats = new StatsManager(plugin);
        var boosts = new BoostManager(plugin);
        when(plugin.getPlayerStatsManager()).thenReturn(playerStats);
        when(plugin.getStatsManager()).thenReturn(stats);
        when(plugin.getBoostManager()).thenReturn(boosts);
        var data = new DataHandler(plugin);
        var manager = new CrystalManager(plugin, new HologramManager(plugin), data);
        when(plugin.getCrystalManager()).thenReturn(manager);
        when(plugin.getDataHandler()).thenReturn(data);
        return new Env(plugin, manager, data);
    }

    @Test
    @DisplayName("data.yml round-trip: position, HP, override name, destroyed state and respawn time")
    void roundTrip() {
        var env = newEnv();
        env.manager().spawnCrystal("alive", "test_respawn", new Location(world, 1.5, 70, -3.5), "&cBoss");
        env.manager().getCrystalById("alive").setCurrentHp(12);
        env.manager().spawnCrystal("dead", "test_respawn", new Location(world, 10, 64, 10), null);
        long respawnAt = System.currentTimeMillis() + 50_000;
        env.manager().markCrystalDestroyed("dead");
        env.manager().getCrystalById("dead").setRespawnTime(respawnAt);
        env.data().saveData();

        var env2 = newEnv();
        env2.data().loadData();

        var alive = env2.manager().getCrystalById("alive");
        assertNotNull(alive);
        assertEquals(12, alive.getCurrentHp());
        assertEquals("&cBoss", alive.getOverrideName());
        assertEquals(1.5, alive.getLocation().getX());
        assertEquals(70, alive.getLocation().getY());
        assertEquals(-3.5, alive.getLocation().getZ());
        assertFalse(alive.isDestroyed());

        var dead = env2.manager().getCrystalById("dead");
        assertNotNull(dead);
        assertTrue(dead.isDestroyed());
        assertEquals(respawnAt, dead.getRespawnTime());
        assertNull(dead.getEntity(), "no entity for a destroyed crystal after load");
    }

    @Test
    @DisplayName("old UUID IDs are renamed to <type>-<n> on load (state kept, data.yml rewritten), existing IDs are not reused")
    void uuidIdsMigrated() throws Exception {
        Files.writeString(dir.resolve("data.yml"), """
                crystals:
                  3f0c1c9e-1111-4c2a-9f00-123456789abc:
                    config-id: test_respawn
                    location: {world: world, x: 1.0, y: 64.0, z: 1.0}
                    current-hp: 12
                    is-destroyed: false
                    override-name: "&cOld"
                  test_respawn-1:
                    config-id: test_respawn
                    location: {world: world, x: 9.0, y: 64.0, z: 9.0}
                    current-hp: 30
                    is-destroyed: false
                    spawner: forest
                    expires-at: 99999999999999
                """);
        var env = newEnv();
        env.data().loadData();

        var kept = env.manager().getCrystalById("test_respawn-1");
        assertEquals(9.0, kept.getLocation().getX(), "proper ID untouched");
        assertEquals("forest", kept.getSpawnerId());
        assertEquals(99999999999999L, kept.getExpiresAt());

        var renamed = env.manager().getCrystalById("test_respawn-2");
        assertNotNull(renamed, "UUID entry got the next free ID");
        assertEquals(12, renamed.getCurrentHp());
        assertEquals("&cOld", renamed.getOverrideName());

        var saved = YamlConfiguration.loadConfiguration(new File(dir.toFile(), "data.yml"));
        assertEquals(java.util.Set.of("test_respawn-1", "test_respawn-2"), saved.getConfigurationSection("crystals").getKeys(false));
        assertEquals("forest", saved.getString("crystals.test_respawn-1.spawner"));
    }

    @Test
    @DisplayName("one-time crystal destroyed -> not saved anymore")
    void oneTimeNotSavedAfterDestroy() {
        var env = newEnv();
        env.manager().spawnCrystal("once", "test_once", new Location(world, 0, 64, 0), null);
        var steve = TestSupport.player("Steve");
        env.manager().handleDamage(env.manager().getCrystalById("once").getEntity(), steve);
        env.data().saveData();

        var saved = YamlConfiguration.loadConfiguration(new File(dir.toFile(), "data.yml"));
        assertFalse(saved.contains("crystals.once"));
    }

    @Test
    @DisplayName("crystal in a world not loaded at startup is kept in data.yml and loaded when the world appears")
    void missingWorldDoesNotLoseData() throws Exception {
        Files.writeString(dir.resolve("data.yml"), """
                crystals:
                  far:
                    config-id: test_respawn
                    location:
                      world: event_world
                      x: 1.0
                      y: 64.0
                      z: 1.0
                    current-hp: 10
                    is-destroyed: false
                """);
        var env = newEnv();
        env.data().loadData();   // event_world is not loaded (e.g. Multiverse enabled later)
        env.data().saveData();   // autosave / shutdown

        var saved = YamlConfiguration.loadConfiguration(new File(dir.toFile(), "data.yml"));
        assertTrue(saved.contains("crystals.far"), "crystal 'far' was wiped from data.yml");
        assertEquals("event_world", saved.getString("crystals.far.location.world"));
        assertEquals(10, saved.getInt("crystals.far.current-hp"));
        assertEquals(java.util.Set.of("far"), env.data().getPendingIds());

        // World loaded later (WorldLoadEvent -> retryPending)
        TestSupport.world("event_world", spawned);
        assertEquals(1, env.data().retryPending());
        var data = env.manager().getCrystalById("far");
        assertNotNull(data);
        assertEquals(10, data.getCurrentHp());
        assertTrue(env.data().getPendingIds().isEmpty());

        env.data().saveData();
        saved = YamlConfiguration.loadConfiguration(new File(dir.toFile(), "data.yml"));
        assertEquals(1, saved.getConfigurationSection("crystals").getKeys(false).size(), "saved exactly once");
    }

    @Test
    @DisplayName("crystal whose config-id was removed from config.yml is kept in data.yml and restored after reload")
    void missingConfigIdDoesNotLoseData() throws Exception {
        Files.writeString(dir.resolve("data.yml"), """
                crystals:
                  legacy:
                    config-id: removed_type
                    location:
                      world: world
                      x: 1.0
                      y: 64.0
                      z: 1.0
                    current-hp: 10
                    is-destroyed: false
                """);
        var env = newEnv();
        env.data().loadData();
        env.data().saveData();

        var saved = YamlConfiguration.loadConfiguration(new File(dir.toFile(), "data.yml"));
        assertTrue(saved.contains("crystals.legacy"), "crystal 'legacy' was wiped from data.yml");
        assertNull(env.manager().getCrystalById("legacy"));

        // Admin restores the type in config.yml and runs /metin reload
        env.plugin().getConfig().set("crystals.removed_type.type", "respawn");
        env.plugin().getConfig().set("crystals.removed_type.max-hp", 20);
        assertEquals(1, env.data().retryPending());
        assertEquals(10, env.manager().getCrystalById("legacy").getCurrentHp());
    }
}
