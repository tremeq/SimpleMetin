package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import me.simplemetin.data.DataHandler;
import me.simplemetin.models.CrystalData;
import me.simplemetin.testutil.TestSupport;
import org.bukkit.HeightMap;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SpawnerManagerTest {

    @TempDir
    Path dir;
    Server server;
    SimpleMetin plugin;
    World world;
    List<Entity> spawned;
    CrystalManager crystals;
    SpawnerManager spawners;
    Player steve;

    @BeforeEach
    void setUp() throws Exception {
        server = TestSupport.resetServer();
        when(server.isPrimaryThread()).thenReturn(true);
        var config = TestSupport.yaml(CrystalManagerTest.config() + """
                auto-spawn:
                  enabled: true
                  max-alive-total: 0
                  min-players-online: 1
                """);
        plugin = TestSupport.plugin(dir.toFile(), config);
        spawned = new ArrayList<>();
        world = TestSupport.world("world", spawned);
        var stats = new StatsManager(plugin);
        var boosts = new BoostManager(plugin);
        var psm = mock(PlayerStatsManager.class);
        when(psm.getDamage(any())).thenReturn(1000);
        when(plugin.getStatsManager()).thenReturn(stats);
        when(plugin.getBoostManager()).thenReturn(boosts);
        when(plugin.getPlayerStatsManager()).thenReturn(psm);
        var data = new DataHandler(plugin);
        crystals = new CrystalManager(plugin, new HologramManager(plugin), data);
        when(plugin.getCrystalManager()).thenReturn(crystals);
        when(plugin.getDataHandler()).thenReturn(data);
        spawners = new SpawnerManager(plugin, crystals);
        when(plugin.getSpawnerManager()).thenReturn(spawners);

        steve = TestSupport.player("Steve");
        doReturn(List.of(steve)).when(server).getOnlinePlayers();

        // Flat ground at y=63: solid block, air above
        when(world.getHighestBlockYAt(anyInt(), anyInt(), eq(HeightMap.MOTION_BLOCKING_NO_LEAVES))).thenReturn(63);
        var ground = mock(Block.class);
        when(ground.isSolid()).thenReturn(true);
        var air = mock(Block.class);
        when(air.isPassable()).thenReturn(true);
        when(world.getBlockAt(anyInt(), eq(63), anyInt())).thenReturn(ground);
        when(world.getBlockAt(anyInt(), intThat(y -> y > 63), anyInt())).thenReturn(air);
    }

    void loadSpawners(String yaml) throws Exception {
        Files.writeString(dir.resolve("spawners.yml"), yaml);
        spawners.load();
    }

    static final String RANDOM_SPAWNER = """
            spawners:
              forest:
                enabled: true
                mode: random
                world: world
                area: {shape: circle, center-x: 100, center-z: 100, radius: 20, min-y: 0, max-y: 200}
                min-distance: 0
                types: {test_once: 1}
                interval: 60
                max-alive: 2
                lifetime: 30
                spawn-on-start: true
                announce: {spawn: true, despawn: true, destroy: true, warning: 0, range: 0, sound: ""}
            """;

    @Test
    @DisplayName("default spawners.yml is created with two disabled examples")
    void defaultFile() {
        spawners.load();
        assertTrue(Files.exists(dir.resolve("spawners.yml")));
        assertEquals(2, spawners.getSpawners().size());
        assertTrue(spawners.getSpawners().stream().noneMatch(SpawnerManager.Spawner::isEnabled));
    }

    @Test
    @DisplayName("random mode: crystal on safe ground inside the area, short ID, lifetime, announcement")
    void randomSpawn() throws Exception {
        loadSpawners(RANDOM_SPAWNER);
        spawners.tick();

        assertEquals(1, crystals.countSpawnerCrystals("forest"));
        var data = crystals.getAllCrystals().iterator().next();
        assertEquals("test_once-1", data.getId(), "only the metin_ prefix is dropped");
        assertEquals("forest", data.getSpawnerId());
        var l = data.getLocation();
        assertEquals(64, l.getY(), "on top of the ground block");
        assertTrue(Math.hypot(l.getX() - 100, l.getZ() - 100) <= 21, "inside the circle: " + l);
        assertTrue(data.getExpiresAt() > System.currentTimeMillis() + 25_000);
        verify(steve).sendMessage(TestSupport.text("appeared at"));

        // Next attempt only after the interval
        spawners.tick();
        assertEquals(1, crystals.countSpawnerCrystals("forest"));
    }

    @Test
    @DisplayName("limits: max-alive per spawner and auto-spawn.max-alive-total, min-players-online, global switch")
    void limits() throws Exception {
        loadSpawners(RANDOM_SPAWNER.replace("max-alive: 2", "max-alive: 1"));
        var sp = spawners.getSpawner("forest");
        spawners.tick();
        assertEquals(1, crystals.countSpawnerCrystals("forest"));

        sp.nextSpawnAt = 0;
        spawners.tick();
        assertEquals(1, crystals.countSpawnerCrystals("forest"), "max-alive reached");

        loadSpawners(RANDOM_SPAWNER);
        plugin.getConfig().set("auto-spawn.max-alive-total", 1);
        spawners.getSpawner("forest").nextSpawnAt = 0;
        spawners.tick();
        assertEquals(1, crystals.countSpawnerCrystals(null), "global limit");

        plugin.getConfig().set("auto-spawn.max-alive-total", 0);
        doReturn(List.of()).when(server).getOnlinePlayers();
        spawners.getSpawner("forest").nextSpawnAt = 0;
        spawners.tick();
        assertEquals(1, crystals.countSpawnerCrystals(null), "nobody online");

        doReturn(List.of(steve)).when(server).getOnlinePlayers();
        plugin.getConfig().set("auto-spawn.enabled", false);
        spawners.getSpawner("forest").nextSpawnAt = 0;
        spawners.tick();
        assertEquals(1, crystals.countSpawnerCrystals(null), "auto-spawn disabled");
    }

    @Test
    @DisplayName("unsafe ground (water, no air above, outside y range) is skipped; no location -> nothing spawned")
    void unsafeGround() throws Exception {
        loadSpawners(RANDOM_SPAWNER);
        var water = mock(Block.class);
        when(water.isLiquid()).thenReturn(true);
        when(world.getBlockAt(anyInt(), eq(63), anyInt())).thenReturn(water);

        spawners.tick();

        assertEquals(0, crystals.countSpawnerCrystals(null));
        assertTrue(TestSupport.logged("could not find a valid location"));
    }

    @Test
    @DisplayName("lifetime over: crystal vanishes with an announcement; destroyed spawner crystal does not respawn in place")
    void lifetimeAndDestroy() throws Exception {
        loadSpawners(RANDOM_SPAWNER);
        spawners.tick();
        var data = crystals.getAllCrystals().iterator().next();

        data.setExpiresAt(System.currentTimeMillis() - 1);
        spawners.tick();
        assertNull(crystals.getCrystalById(data.getId()));
        verify(steve).sendMessage(TestSupport.text("has vanished"));

        spawners.getSpawner("forest").nextSpawnAt = 0;
        spawners.tick();
        var second = crystals.getAllCrystals().iterator().next();
        crystals.handleDamage(second.getEntity(), steve);   // strength 1000 -> destroyed
        assertNull(crystals.getCrystalById(second.getId()), "removed, the spawner places a new one later");
        verify(steve).sendMessage(TestSupport.text("Steve destroyed"));
    }

    @Test
    @DisplayName("warning is announced before the spawn with the planned crystal type")
    void warning() throws Exception {
        loadSpawners(RANDOM_SPAWNER.replace("warning: 0", "warning: 60").replace("spawn-on-start: true", "spawn-on-start: false"));
        var sp = spawners.getSpawner("forest");
        sp.nextSpawnAt = System.currentTimeMillis() + 30_000;
        spawners.tick();
        verify(steve).sendMessage(TestSupport.text("Once will appear in"));
        assertEquals(0, crystals.countSpawnerCrystals(null));
        spawners.tick();
        verify(steve, times(1)).sendMessage(TestSupport.text("will appear in"));
    }

    @Test
    @DisplayName("points mode: point add/remove saved to spawners.yml, crystal on a free point, forced spawn")
    void pointsMode() throws Exception {
        loadSpawners("""
                spawners:
                  arena:
                    enabled: true
                    mode: points
                    points: []
                    types: {test_respawn: 1}
                    interval: 600
                    max-alive: 5
                    lifetime: 0
                    announce: {spawn: false}
                """);
        var sp = spawners.getSpawner("arena");
        assertEquals(1, spawners.addPoint(sp, new Location(world, 10.3, 70, -5.7)));
        assertEquals(2, spawners.addPoint(sp, new Location(world, 30, 70, 30)));
        var saved = YamlConfiguration.loadConfiguration(dir.resolve("spawners.yml").toFile());
        assertEquals(List.of("world 10.5 70.0 -5.5", "world 30.5 70.0 30.5"), saved.getStringList("spawners.arena.points"));

        var result = new AtomicReference<CrystalData>();
        spawners.spawnFrom(sp, null, result::set);
        spawners.spawnFrom(sp, null, d -> {});
        spawners.spawnFrom(sp, null, d -> result.set(d));
        assertNull(result.get(), "both points occupied -> no location");
        assertEquals(2, crystals.countSpawnerCrystals("arena"));
        assertEquals(0, crystals.getCrystalById("test_respawn-1").getExpiresAt(), "lifetime 0 = never vanishes");

        assertTrue(spawners.removePoint(sp, 1));
        assertFalse(spawners.removePoint(sp, 5));
        assertEquals(1, sp.getPoints().size());
    }

    @Test
    @DisplayName("spawner state survives /metin reload (timers kept), invalid spawners are skipped")
    void reloadKeepsTimers() throws Exception {
        loadSpawners(RANDOM_SPAWNER.replace("spawn-on-start: true", "spawn-on-start: false"));
        long next = spawners.getSpawner("forest").getNextSpawnAt();
        spawners.load();
        assertEquals(next, spawners.getSpawner("forest").getNextSpawnAt());

        loadSpawners("spawners:\n  empty:\n    types: {}\n");
        assertNull(spawners.getSpawner("empty"));
        assertTrue(TestSupport.logged("has no crystal types"));
    }

    @Test
    void delayedSpawnsRespectGlobalLimit() throws Exception {
        var yaml = TestSupport.yaml(RANDOM_SPAWNER);
        yaml.createSection("spawners.second", yaml.getConfigurationSection("spawners.forest").getValues(true));
        spawners.loadFrom(yaml);
        plugin.getConfig().set("auto-spawn.max-alive-total", 1);
        var first = new CompletableFuture<Chunk>();
        var second = new CompletableFuture<Chunk>();
        when(world.getChunkAtAsync(anyInt(), anyInt())).thenReturn(first, second);

        spawners.tick();
        assertEquals(0, crystals.countSpawnerCrystals(null));
        first.complete(mock(Chunk.class));
        second.complete(mock(Chunk.class));
        assertEquals(1, crystals.countSpawnerCrystals(null), "limit checked when chunks finish loading");
    }

    @Test
    void reloadCancelsPendingSpawn() throws Exception {
        loadSpawners(RANDOM_SPAWNER);
        var pending = new CompletableFuture<Chunk>();
        when(world.getChunkAtAsync(anyInt(), anyInt())).thenReturn(pending);
        spawners.tick();
        loadSpawners(RANDOM_SPAWNER.replace("enabled: true", "enabled: false"));
        pending.complete(mock(Chunk.class));
        assertEquals(0, crystals.countSpawnerCrystals(null));
    }

    @Test
    void shutdownCancelsPendingSpawn() throws Exception {
        loadSpawners(RANDOM_SPAWNER);
        var pending = new CompletableFuture<Chunk>();
        when(world.getChunkAtAsync(anyInt(), anyInt())).thenReturn(pending);
        spawners.tick();
        spawners.shutdown();
        pending.complete(mock(Chunk.class));
        assertEquals(0, crystals.countSpawnerCrystals(null));
        verify(world, never()).getHighestBlockYAt(anyInt(), anyInt(), any(HeightMap.class));
    }

    @Test
    void delayedSpawnRechecksOnlinePlayers() throws Exception {
        loadSpawners(RANDOM_SPAWNER);
        var pending = new CompletableFuture<Chunk>();
        when(world.getChunkAtAsync(anyInt(), anyInt())).thenReturn(pending);
        spawners.tick();
        doReturn(List.of()).when(server).getOnlinePlayers();
        pending.complete(mock(Chunk.class));
        assertEquals(0, crystals.countSpawnerCrystals(null));
    }

    @Test
    void chunkCompletionFromWorkerSchedulesWorldAccessOnMainThread() throws Exception {
        loadSpawners(RANDOM_SPAWNER);
        when(server.isPrimaryThread()).thenReturn(false);
        spawners.tick();
        assertEquals(0, crystals.countSpawnerCrystals(null));
        verify(world, never()).getHighestBlockYAt(anyInt(), anyInt(), any(HeightMap.class));
        var task = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(server.getScheduler()).runTask(eq(plugin), task.capture());
        when(server.isPrimaryThread()).thenReturn(true);
        task.getValue().run();
        assertEquals(1, crystals.countSpawnerCrystals(null));
    }

    @Test
    void forcedSpawnStillIgnoresAutomaticLimits() throws Exception {
        loadSpawners(RANDOM_SPAWNER.replace("max-alive: 2", "max-alive: 0"));
        plugin.getConfig().set("auto-spawn.enabled", false);
        doReturn(List.of()).when(server).getOnlinePlayers();
        var result = new AtomicReference<CrystalData>();
        spawners.spawnFrom(spawners.getSpawner("forest"), null, result::set);
        assertNotNull(result.get());
        assertEquals(1, crystals.countSpawnerCrystals(null));
    }
}
