package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import me.simplemetin.data.DataHandler;
import me.simplemetin.events.MetinCrystalDestroyedEvent;
import me.simplemetin.models.CrystalType;
import me.simplemetin.testutil.TestSupport;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CrystalManagerTest {

    /** Shared test crystal definitions (also used by DataHandlerTest). */
    public static String config() {
        return CONFIG;
    }

    static final String CONFIG = """
            settings:
              hit-cooldown: 0.0
              bossbar-enabled: true
              bossbar-range: 30.0
            boosts:
              bossbar:
                enabled: false
            crystals:
              test_respawn:
                display-name: "&eTest"
                type: respawn
                max-hp: 30
                damage-per-hit: 1
                respawn-time: 60
                show-actionbar: true
                show-bossbar: true
                hologram:
                  enabled: true
                  range: 32.0
                  lines:
                    - "HP %hp%/%max_hp%"
                    - "ID %id%"
                hit-drops:
                  d1:
                    item: DIAMOND
                    amount: 2
                    chance: 100.0
                hit-commands:
                  c1:
                    command: "eco give %player% 10"
                    chance: 100.0
                death-drops:
                  d1:
                    item: EMERALD
                    amount: 3
                    chance: 100.0
                death-commands:
                  c1:
                    command: "eco give %player% 500"
                    chance: 100.0
                  c2:
                    command: "say %player% won"
                    chance: 100.0
                death-effects:
                  particle: FLAME
                  sound: ENTITY_GENERIC_EXPLODE
              test_once:
                display-name: "&dOnce"
                type: one-time
                max-hp: 10
                hologram:
                  enabled: true
                  lines: ["%hp%"]
              half_chance:
                type: respawn
                max-hp: 100000
                hit-drops:
                  d1:
                    item: GOLD_INGOT
                    amount: 1
                    chance: 50.0
            """;

    Server server;
    SimpleMetin plugin;
    List<Entity> spawned;
    World world;
    PlayerStatsManager playerStats;
    StatsManager statsManager;
    BoostManager boostManager;
    CrystalManager manager;
    Player steve;

    @BeforeEach
    void setUp(@TempDir Path dir) {
        server = TestSupport.resetServer();
        YamlConfiguration config = TestSupport.yaml(CONFIG);
        plugin = TestSupport.plugin(dir.toFile(), config);
        spawned = new ArrayList<>();
        world = TestSupport.world("world", spawned);

        playerStats = mock(PlayerStatsManager.class);
        when(playerStats.getDamage(any())).thenReturn(10);
        statsManager = new StatsManager(plugin);
        boostManager = new BoostManager(plugin);
        when(plugin.getPlayerStatsManager()).thenReturn(playerStats);
        when(plugin.getStatsManager()).thenReturn(statsManager);
        when(plugin.getBoostManager()).thenReturn(boostManager);

        manager = new CrystalManager(plugin, new HologramManager(plugin), new DataHandler(plugin));
        when(plugin.getCrystalManager()).thenReturn(manager);

        steve = TestSupport.player("Steve");
        // Commands "succeed" (an economy plugin is present); money is only counted for successful commands
        when(server.dispatchCommand(any(), anyString())).thenReturn(true);
    }

    Location loc() {
        return new Location(world, 0.5, 64, 0.5);
    }

    EnderCrystal crystalEntity(String id) {
        return manager.getCrystalById(id).getEntity();
    }

    void hit(String id, int times) {
        for (int i = 0; i < times; i++) {
            var data = manager.getCrystalById(id);
            if (data == null) return;
            manager.handleDamage(data.getEntity(), steve);
        }
    }

    @Test
    @DisplayName("spawn: EnderCrystal + scoreboard tags + one TextDisplay hologram + BossBar")
    void spawnCreatesEntityTagsHologramAndBossbar() {
        manager.spawnCrystal("c1", "test_respawn", loc(), null);

        var data = manager.getCrystalById("c1");
        assertNotNull(data);
        assertEquals(CrystalType.RESPAWN, data.getType());
        assertEquals(30, data.getMaxHp());
        assertEquals(30, data.getCurrentHp());

        var crystal = data.getEntity();
        assertTrue(crystal.getScoreboardTags().contains("simplemetin"));
        assertTrue(crystal.getScoreboardTags().contains("metin_test_respawn"));
        verify(crystal).setShowingBottom(false);

        assertTrue(TestSupport.armorStands(spawned).isEmpty(), "no ArmorStands any more");
        var holograms = TestSupport.holograms(spawned);
        assertEquals(1, holograms.size(), "one TextDisplay for all lines");
        assertEquals("HP 30/30\nID c1", TestSupport.plain(holograms.get(0).text()));
        verify(holograms.get(0)).setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        verify(holograms.get(0)).setViewRange((float) (32.0 / 64.0));

        verify(server).createBossBar(contains("Test"), any(), any(), any(org.bukkit.boss.BarFlag[].class));
        assertSame(data, manager.getCrystalByEntity(crystal.getUniqueId()));
    }

    @Test
    @DisplayName("hit: damage = player strength (PlayerStatsManager), damage-per-hit is ignored")
    void hitUsesPlayerStrength() {
        when(playerStats.getDamage(steve.getUniqueId())).thenReturn(7);
        manager.spawnCrystal("c1", "test_respawn", loc(), null);

        hit("c1", 1);

        assertEquals(23, manager.getCrystalById("c1").getCurrentHp());
        assertEquals(7, statsManager.getStats(steve.getUniqueId()).getTotalDamageDealt());
        assertEquals("HP 23/30\nID c1", TestSupport.plain(TestSupport.holograms(spawned).get(0).text()));
        verify(steve, atLeastOnce()).sendActionBar(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    @DisplayName("hit cooldown blocks a second hit within the window")
    void hitCooldownBlocksSecondHit() {
        plugin.getConfig().set("settings.hit-cooldown", 10.0);
        manager.spawnCrystal("c1", "test_respawn", loc(), null);

        hit("c1", 3);

        assertEquals(20, manager.getCrystalById("c1").getCurrentHp(), "only the first hit counts");
        verify(steve, atLeast(3)).sendActionBar(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    @DisplayName("hit: hit-drops go to inventory, hit-commands run from console, stats tracked")
    void hitDropsAndCommands() {
        manager.spawnCrystal("c1", "test_respawn", loc(), null);

        hit("c1", 1);

        verify(steve.getInventory()).addItem(any(ItemStack[].class));
        verify(server).dispatchCommand(any(), eq("eco give Steve 10"));
        var stats = statsManager.getStats(steve.getUniqueId());
        assertEquals(2, stats.getItemsReceived());
        assertEquals(10, stats.getMoneyEarned());
    }

    @Test
    @DisplayName("destroy (respawn): death drops/commands, event, entity+hologram removed, respawn timer set")
    void destroyRespawnCrystal() {
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        var entity = crystalEntity("c1");
        long before = System.currentTimeMillis();

        hit("c1", 3); // 3 x 10 dmg = 30 hp

        var data = manager.getCrystalById("c1");
        assertNotNull(data, "respawn crystal stays in data");
        assertTrue(data.isDestroyed());
        assertTrue(entity.isDead(), "entity removed");
        TestSupport.holograms(spawned).forEach(s -> assertTrue(s.isDead(), "hologram removed"));
        assertTrue(data.getRespawnTime() >= before + 60_000 && data.getRespawnTime() <= System.currentTimeMillis() + 60_000);

        verify(server).dispatchCommand(any(), eq("eco give Steve 500"));
        verify(server).dispatchCommand(any(), eq("say Steve won"));

        var captor = ArgumentCaptor.forClass(Event.class);
        verify(server.getPluginManager(), atLeastOnce()).callEvent(captor.capture());
        var event = captor.getAllValues().stream()
                .filter(MetinCrystalDestroyedEvent.class::isInstance)
                .map(MetinCrystalDestroyedEvent.class::cast)
                .findFirst().orElseThrow();
        assertSame(steve, event.getKiller());
        assertSame(data, event.getCrystal());

        var stats = statsManager.getStats(steve.getUniqueId());
        assertEquals(1, stats.getCrystalsDestroyed());
        assertEquals(1, stats.getCrystalTypeDestroyed("test_respawn"));
        assertEquals(30 + 500, stats.getMoneyEarned());
        assertEquals(3 * 2 + 3, stats.getItemsReceived());

        // Hitting a destroyed crystal does nothing
        manager.handleDamage(entity, steve);
        assertEquals(1, stats.getCrystalsDestroyed());
    }

    @Test
    @DisplayName("destroy (one-time): crystal removed permanently")
    void destroyOneTimeCrystal() {
        manager.spawnCrystal("c1", "test_once", loc(), null);
        hit("c1", 1);
        assertNull(manager.getCrystalById("c1"));
        assertTrue(manager.getAllCrystals().isEmpty());
    }

    @Test
    @DisplayName("update task: countdown hologram while destroyed, automatic respawn after respawn-time")
    void automaticRespawn() {
        manager.startUpdateTask();
        var runnable = ArgumentCaptor.forClass(Runnable.class);
        BukkitScheduler scheduler = server.getScheduler();
        verify(scheduler).runTaskTimer(any(Plugin.class), runnable.capture(), anyLong(), anyLong());

        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        hit("c1", 3);
        var data = manager.getCrystalById("c1");
        assertTrue(data.isDestroyed());

        runnable.getValue().run();
        var countdown = TestSupport.holograms(spawned).stream().filter(s -> !s.isDead()).toList();
        assertEquals(1, countdown.size());
        assertTrue(TestSupport.plain(countdown.get(0).text()).contains("Respawns in"), TestSupport.plain(countdown.get(0).text()));

        data.setRespawnTime(System.currentTimeMillis() - 1);
        runnable.getValue().run();

        assertFalse(data.isDestroyed());
        assertEquals(30, data.getCurrentHp());
        assertFalse(data.getEntity().isDead());
        assertTrue(countdown.get(0).isDead(), "countdown hologram removed on respawn");
        assertSame(data, manager.getCrystalByEntity(data.getEntity().getUniqueId()));
    }

    @Test
    @DisplayName("/metin respawn: manual respawn of a destroyed crystal")
    void manualRespawn() {
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        hit("c1", 3);
        manager.manualRespawn("c1");
        var data = manager.getCrystalById("c1");
        assertFalse(data.isDestroyed());
        assertEquals(30, data.getCurrentHp());
    }

    @Test
    @DisplayName("/metin remove: entity, hologram, bossbar and data removed")
    void removeCrystal() {
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        var entity = crystalEntity("c1");

        manager.removeCrystal("c1");

        assertNull(manager.getCrystalById("c1"));
        assertTrue(entity.isDead());
        TestSupport.holograms(spawned).forEach(s -> assertTrue(s.isDead()));
        assertNull(manager.getCrystalByEntity(entity.getUniqueId()));
    }

    @Test
    @DisplayName("BossBar: only players within bossbar-range see it")
    void bossBarRange() {
        manager.startUpdateTask();
        var runnable = ArgumentCaptor.forClass(Runnable.class);
        verify(server.getScheduler()).runTaskTimer(any(Plugin.class), runnable.capture(), anyLong(), anyLong());

        var bossBarCaptor = new ArrayList<BossBar>();
        when(server.createBossBar(any(), any(), any(), any(org.bukkit.boss.BarFlag[].class))).thenAnswer(inv -> {
            var bar = TestSupport.bossBar();
            bossBarCaptor.add(bar);
            return bar;
        });
        manager.spawnCrystal("c1", "test_respawn", loc(), null);

        Player near = TestSupport.player("Near");
        Player far = TestSupport.player("Far");
        when(near.getLocation()).thenReturn(new Location(world, 10, 64, 0));
        when(far.getLocation()).thenReturn(new Location(world, 100, 64, 0));
        when(world.getPlayers()).thenReturn(List.of(near, far));

        runnable.getValue().run();

        var bar = bossBarCaptor.get(0);
        assertTrue(bar.getPlayers().contains(near));
        assertFalse(bar.getPlayers().contains(far));

        when(near.getLocation()).thenReturn(new Location(world, 50, 64, 0));
        runnable.getValue().run();
        assertFalse(bar.getPlayers().contains(near), "removed after walking away");
    }

    @Test
    @DisplayName("boost: drop chance multiplied (50% x 2 = 100%)")
    void boostDoublesDropChance() {
        boostManager.activatePersonalBoost(steve, 2.0, 60);
        manager.spawnCrystal("c1", "half_chance", loc(), null);

        hit("c1", 40);

        verify(steve.getInventory(), times(40)).addItem(any(ItemStack[].class));
    }

    @Test
    @DisplayName("without boost: 50% drop chance is not always hit")
    void noBoostHalfChance() {
        manager.spawnCrystal("c1", "half_chance", loc(), null);
        hit("c1", 200);
        verify(steve.getInventory(), atMost(199)).addItem(any(ItemStack[].class));
        verify(steve.getInventory(), atLeast(1)).addItem(any(ItemStack[].class));
    }

    @Test
    @DisplayName("negative player strength (edited players.yml) never heals the crystal")
    void negativeStrengthMustNotHeal() {
        when(playerStats.getDamage(steve.getUniqueId())).thenReturn(-50);
        manager.spawnCrystal("c1", "test_respawn", loc(), null);

        hit("c1", 1);

        var data = manager.getCrystalById("c1");
        assertTrue(data.getCurrentHp() <= data.getMaxHp(),
                "HP " + data.getCurrentHp() + " exceeds max " + data.getMaxHp());
    }

    @Test
    @DisplayName("total-damage counts only damage actually dealt (no overkill)")
    void totalDamageCountsOverkill() {
        when(playerStats.getDamage(steve.getUniqueId())).thenReturn(1000);
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        hit("c1", 1);
        assertEquals(30, statsManager.getStats(steve.getUniqueId()).getTotalDamageDealt());
    }

    @Test
    @DisplayName("crystal type removed from config: crystal still takes hits (defaults, no drops) and admin is warned")
    void crystalWithMissingConfigIsUnhittable() {
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        plugin.getConfig().set("crystals.test_respawn", null);

        hit("c1", 1);

        assertEquals(20, manager.getCrystalById("c1").getCurrentHp(), "hit silently ignored");
        assertTrue(TestSupport.logged("missing in config.yml"));
        verify(server, never()).dispatchCommand(any(), anyString());
        hit("c1", 2);
        assertTrue(manager.getCrystalById("c1").isDestroyed(), "can still be destroyed");
    }

    @Test
    @DisplayName("/metin reload: new max-hp applied (full crystal stays full, damaged one is capped)")
    void reloadAppliesMaxHp() {
        manager.spawnCrystal("full", "test_respawn", loc(), null);
        manager.spawnCrystal("hurt", "test_respawn", new Location(world, 5, 64, 5), null);
        manager.getCrystalById("hurt").setCurrentHp(25);

        plugin.getConfig().set("crystals.test_respawn.max-hp", 20);
        manager.reload();

        assertEquals(20, manager.getCrystalById("full").getMaxHp());
        assertEquals(20, manager.getCrystalById("full").getCurrentHp());
        assertEquals(20, manager.getCrystalById("hurt").getCurrentHp());

        manager.getCrystalById("hurt").setCurrentHp(15);
        plugin.getConfig().set("crystals.test_respawn.max-hp", 100);
        manager.reload();
        assertEquals(100, manager.getCrystalById("full").getCurrentHp());
        assertEquals(15, manager.getCrystalById("hurt").getCurrentHp(), "damaged crystal keeps its HP");
        var liveTexts = TestSupport.liveHologramTexts(spawned);
        assertTrue(liveTexts.contains("HP 100/100\nID full") && liveTexts.contains("HP 15/100\nID hurt"), "holograms rebuilt: " + liveTexts);
        assertEquals(2, liveTexts.size(), "old holograms removed");
    }

    @Test
    @DisplayName("settings.actionbar-enabled=false suppresses actionbar messages")
    void actionBarDisabled() {
        plugin.getConfig().set("settings.actionbar-enabled", false);
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        hit("c1", 1);
        verify(steve, never()).sendActionBar(any(net.kyori.adventure.text.Component.class));
    }

    @Test
    @DisplayName("death particle: legacy name EXPLOSION_HUGE is still accepted")
    void legacyParticleAccepted() {
        plugin.getConfig().set("crystals.test_respawn.death-effects.particle", "EXPLOSION_HUGE");
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        hit("c1", 3);
        verify(world).spawnParticle(eq(org.bukkit.Particle.EXPLOSION_EMITTER), any(Location.class), anyInt(),
                anyDouble(), anyDouble(), anyDouble(), anyDouble());
        assertFalse(TestSupport.logged("Invalid particle"));
    }

    @Test
    @DisplayName("crystal + holograms are non-persistent (never saved with the chunk -> no duplicates after restart/crash)")
    void spawnedEntitiesShouldNotBePersistent() {
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        verify(crystalEntity("c1")).setPersistent(false);
        assertFalse(TestSupport.holograms(spawned).isEmpty());
        TestSupport.holograms(spawned).forEach(s -> {
            verify(s).setPersistent(false);
            assertTrue(s.getScoreboardTags().contains(HologramManager.HOLOGRAM_TAG));
        });
    }

    @Test
    @DisplayName("chunk not loaded: crystal registered, entity spawned once the chunk loads")
    void spawnDeferredUntilChunkLoad() {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        manager.spawnCrystal("c1", "test_respawn", new Location(world, 100.5, 64, 200.5), null);

        var data = manager.getCrystalById("c1");
        assertNotNull(data);
        assertNull(data.getEntity());
        assertTrue(spawned.isEmpty());

        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        manager.handleChunkLoad(TestSupport.chunk(world, 6, 12));
        var task = ArgumentCaptor.forClass(Runnable.class);
        verify(server.getScheduler()).runTask(any(Plugin.class), task.capture());
        task.getValue().run();

        assertNotNull(data.getEntity());
        assertEquals(1, spawned.stream().filter(e -> e instanceof EnderCrystal).count());
        assertEquals(1, TestSupport.holograms(spawned).size());
        assertSame(data, manager.getCrystalByEntity(data.getEntity().getUniqueId()));

        // A second load event for the same chunk does not spawn a duplicate
        manager.spawnMissingEntities("world", 6, 12);
        assertEquals(1, spawned.stream().filter(e -> e instanceof EnderCrystal).count());
    }

    @Test
    @DisplayName("chunk unload: entity and hologram removed, HP kept; reload spawns a fresh entity that can be hit")
    void chunkUnloadAndReload() {
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        hit("c1", 1);
        var old = crystalEntity("c1");

        manager.handleChunkUnload(TestSupport.chunk(world, 0, 0));

        var data = manager.getCrystalById("c1");
        assertTrue(old.isDead());
        assertNull(data.getEntity());
        assertNull(manager.getCrystalByEntity(old.getUniqueId()));
        TestSupport.holograms(spawned).forEach(s -> assertTrue(s.isDead()));
        assertEquals(20, data.getCurrentHp());

        manager.spawnMissingEntities("world", 0, 0);
        assertNotNull(data.getEntity());
        assertNotEquals(old.getUniqueId(), data.getEntity().getUniqueId());
        hit("c1", 1);
        assertEquals(10, data.getCurrentHp());
    }

    @Test
    @DisplayName("unload of another chunk does not touch the crystal")
    void otherChunkUnloadIgnored() {
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        manager.handleChunkUnload(TestSupport.chunk(world, 5, 5));
        assertFalse(crystalEntity("c1").isDead());
    }

    @Test
    @DisplayName("self-heal: entity removed without a damage event (/kill) is respawned by the update task")
    void selfHealAfterKill() {
        manager.startUpdateTask();
        var runnable = ArgumentCaptor.forClass(Runnable.class);
        verify(server.getScheduler()).runTaskTimer(any(Plugin.class), runnable.capture(), anyLong(), anyLong());
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        hit("c1", 1);
        var old = crystalEntity("c1");

        old.remove();   // /kill
        runnable.getValue().run();

        var fresh = crystalEntity("c1");
        assertNotSame(old, fresh);
        assertFalse(fresh.isDead());
        assertEquals(20, manager.getCrystalById("c1").getCurrentHp(), "HP not reset");
        assertEquals(1, TestSupport.holograms(spawned).stream().filter(s -> !s.isDead()).count(), "no duplicate hologram");
    }

    @Test
    @DisplayName("orphans: untracked tagged crystals/TextDisplays and all old ArmorStand holograms are removed, tracked and foreign entities kept")
    void removeOrphans() {
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        var tracked = crystalEntity("c1");
        var trackedHologram = TestSupport.holograms(spawned).get(0);
        var ghostDisplay = TestSupport.entity(org.bukkit.entity.TextDisplay.class);
        ghostDisplay.addScoreboardTag(HologramManager.HOLOGRAM_TAG);
        var foreignDisplay = TestSupport.entity(org.bukkit.entity.TextDisplay.class); // another plugin's hologram

        var ghost = TestSupport.entity(EnderCrystal.class);
        ghost.addScoreboardTag("simplemetin");
        var ghostStand = TestSupport.entity(ArmorStand.class);
        ghostStand.addScoreboardTag(HologramManager.HOLOGRAM_TAG);
        var vanillaCrystal = TestSupport.entity(EnderCrystal.class);
        var foreignStand = TestSupport.entity(ArmorStand.class);
        when(foreignStand.getLocation()).thenReturn(new Location(world, 50, 64, 50));
        when(foreignStand.isMarker()).thenReturn(true);
        when(foreignStand.isCustomNameVisible()).thenReturn(true);

        // Hologram from an older version: no tag, invisible marker right above the crystal
        var legacyStand = TestSupport.entity(ArmorStand.class);
        when(legacyStand.getLocation()).thenReturn(loc().add(0, 2.25, 0));
        when(legacyStand.isMarker()).thenReturn(true);
        when(legacyStand.isVisible()).thenReturn(false);
        when(legacyStand.isCustomNameVisible()).thenReturn(true);

        int removed = manager.removeOrphans(List.of(tracked, trackedHologram, ghost, ghostStand, ghostDisplay,
                vanillaCrystal, foreignStand, foreignDisplay, legacyStand));

        assertEquals(4, removed);
        assertTrue(ghostDisplay.isDead());
        assertFalse(foreignDisplay.isDead());
        assertTrue(ghost.isDead());
        assertTrue(ghostStand.isDead());
        assertTrue(legacyStand.isDead());
        assertFalse(tracked.isDead());
        assertFalse(trackedHologram.isDead());
        assertFalse(vanillaCrystal.isDead());
        assertFalse(foreignStand.isDead());
    }

    @Test
    @DisplayName("respawn countdown hologram waits for the chunk to be loaded")
    void countdownHologramOnlyInLoadedChunk() {
        manager.startUpdateTask();
        var runnable = ArgumentCaptor.forClass(Runnable.class);
        verify(server.getScheduler()).runTaskTimer(any(Plugin.class), runnable.capture(), anyLong(), anyLong());
        manager.spawnCrystal("c1", "test_respawn", loc(), null);
        hit("c1", 3);
        int holograms = TestSupport.holograms(spawned).size();

        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(false);
        runnable.getValue().run();
        assertEquals(holograms, TestSupport.holograms(spawned).size(), "nothing spawned into an unloaded chunk");

        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        runnable.getValue().run();
        assertEquals(holograms + 1, TestSupport.holograms(spawned).size());
    }
}
