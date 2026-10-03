package me.simplemetin.commands;

import me.simplemetin.SimpleMetin;
import me.simplemetin.data.DataHandler;
import me.simplemetin.managers.BoostManager;
import me.simplemetin.managers.CrystalManager;
import me.simplemetin.managers.CustomItemManager;
import me.simplemetin.managers.CrystalManagerTest;
import me.simplemetin.managers.HologramManager;
import me.simplemetin.managers.PlayerStatsManager;
import me.simplemetin.managers.SpawnerManager;
import me.simplemetin.managers.StatsManager;
import me.simplemetin.testutil.TestSupport;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import static me.simplemetin.testutil.TestSupport.text;
import static me.simplemetin.testutil.TestSupport.wasSent;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MetinCommandTest {

    Server server;
    SimpleMetin plugin;
    CrystalManager crystals;
    BoostManager boosts;
    StatsManager stats;
    MetinCommand cmd;
    Player admin;
    World world;
    List<Entity> spawned = new ArrayList<>();
    Command command = mock(Command.class);

    @BeforeEach
    void setUp(@TempDir Path dir) {
        server = TestSupport.resetServer();
        // An old-style "messages" section in config.yml: migrated into messages-en.yml and still used
        plugin = TestSupport.plugin(dir.toFile(), TestSupport.yaml(CrystalManagerTest.config()
                + "\nmessages:\n  prefix: \"\"\n  spawned: \"spawned %id%\"\n  removed: \"removed %id%\"\n"
                + "  not-found: \"nf %id%\"\n  global-boost-activated: \"GLOBAL %multiplier%x %duration%\"\n"));
        world = TestSupport.world("world", spawned);
        stats = new StatsManager(plugin);
        boosts = new BoostManager(plugin);
        var psm = mock(PlayerStatsManager.class);
        when(psm.getDamage(any())).thenReturn(10);
        when(plugin.getStatsManager()).thenReturn(stats);
        when(plugin.getBoostManager()).thenReturn(boosts);
        when(plugin.getPlayerStatsManager()).thenReturn(psm);
        var dataHandler = new DataHandler(plugin);
        crystals = new CrystalManager(plugin, new HologramManager(plugin), dataHandler);
        when(plugin.getCrystalManager()).thenReturn(crystals);
        when(plugin.getDataHandler()).thenReturn(dataHandler);
        var spawners = new SpawnerManager(plugin, crystals);
        spawners.load();
        when(plugin.getSpawnerManager()).thenReturn(spawners);
        cmd = new MetinCommand(plugin, crystals);

        admin = TestSupport.player("Admin");
        when(admin.hasPermission("simplemetin.admin")).thenReturn(true);
        when(admin.getLocation()).thenReturn(new Location(world, 5, 64, 5));
    }

    boolean run(CommandSender sender, String... args) {
        return cmd.onCommand(sender, command, "metin", args);
    }

    @Test
    void customItemGiveDropsOverflowInsteadOfLosingIt() {
        var items = mock(CustomItemManager.class);
        when(plugin.getCustomItemManager()).thenReturn(items);
        var item = mock(ItemStack.class);
        var amount = new java.util.concurrent.atomic.AtomicInteger(1);
        when(item.getAmount()).thenAnswer(inv -> amount.get());
        doAnswer(inv -> { amount.set(inv.getArgument(0)); return null; }).when(item).setAmount(anyInt());
        when(item.getMaxStackSize()).thenReturn(64);
        when(item.clone()).thenReturn(item);
        when(items.get("reward")).thenReturn(item);
        when(admin.getInventory().addItem(any(ItemStack[].class))).thenAnswer(inv -> {
            var leftover = new HashMap<Integer, ItemStack>();
            leftover.put(0, inv.getArgument(0, ItemStack.class).clone());
            return leftover;
        });

        run(admin, "item", "give", "reward", "Admin", "3");
        verify(world).dropItemNaturally(eq(admin.getLocation()), argThat(i -> i.getAmount() == 3));
        verify(admin).sendMessage(text("inventory is full"));
        assertTrue(wasSent(admin, "3"));
    }

    @Test
    void addDropRejectsInvalidChancesBeforeSaving() {
        var items = mock(CustomItemManager.class);
        when(plugin.getCustomItemManager()).thenReturn(items);
        clearInvocations(plugin);
        for (var chance : List.of("NaN", "Infinity", "-1", "101")) {
            run(admin, "adddrop", "test_respawn", "death", chance);
        }
        verifyNoInteractions(items);
        verify(plugin, never()).saveConfig();
        assertEquals(4, TestSupport.sentMessages(admin).stream().filter(m -> m.contains("Invalid number")).count());
    }

    @Test
    @DisplayName("/metin spawn <type> [id|auto] [name]: short IDs (common-1, common-2), custom ID, list and remove")
    void spawnListRemove() {
        run(admin, "spawn", "test_respawn");
        run(admin, "spawn", "test_respawn", "auto", "&c", "Big", "Boss");
        run(admin, "spawn", "test_respawn", "Boss");
        assertNotNull(crystals.getCrystalById("test_respawn-1"));
        assertEquals("&c Big Boss", crystals.getCrystalById("test_respawn-2").getOverrideName());
        assertNotNull(crystals.getCrystalById("boss"), "custom IDs are lower-cased");
        assertEquals(5, crystals.getCrystalById("boss").getLocation().getX());
        verify(admin).sendMessage(text("spawned test_respawn-1"));

        run(admin, "spawn", "test_respawn", "boss");
        verify(admin).sendMessage(text("already used"));
        run(admin, "spawn", "test_respawn", "bad id!");
        verify(admin).sendMessage(text("Invalid ID"));

        run(admin, "list");
        verify(admin, atLeastOnce()).sendMessage(text("[ACTIVE]"));

        run(admin, "remove", "test_respawn-1");
        assertNull(crystals.getCrystalById("test_respawn-1"));
        run(admin, "spawn", "test_respawn");
        assertNotNull(crystals.getCrystalById("test_respawn-1"), "free numbers are reused");
        run(admin, "remove", "nope");
        verify(admin).sendMessage(text("nf nope"));
    }

    @Test
    @DisplayName("ID numbers sort naturally in /metin list (common-2 before common-10)")
    void naturalIdOrder() {
        for (int i = 0; i < 10; i++) run(admin, "spawn", "test_once");
        var ids = crystals.getSortedCrystals().stream().map(d -> d.getId()).toList();
        assertEquals("test_once-2", ids.get(1));
        assertEquals("test_once-10", ids.get(9));
    }

    @Test
    @DisplayName("nearest: /metin remove nearest and /metin info nearest use the crystal next to the player")
    void nearest() {
        run(admin, "spawn", "test_respawn");
        run(admin, "info", "nearest");
        assertTrue(wasSent(admin, "Crystal test_respawn-1"), String.join(" | ", TestSupport.sentMessages(admin)));
        assertTrue(wasSent(admin, "HP: 30/30"));

        when(admin.getLocation()).thenReturn(new Location(world, 500, 64, 500));
        run(admin, "remove", "nearest");
        verify(admin).sendMessage(text("No crystal within"));

        when(admin.getLocation()).thenReturn(new Location(world, 6, 64, 6));
        run(admin, "remove", "nearest");
        assertTrue(crystals.getAllCrystals().isEmpty());
    }

    @Test
    @DisplayName("/metin spawn: unknown config-id and console sender are rejected")
    void spawnErrors() {
        run(admin, "spawn", "does_not_exist");
        assertTrue(crystals.getAllCrystals().isEmpty());
        verify(admin).sendMessage(text("does not exist"));
        var console = mock(CommandSender.class);
        when(console.hasPermission(anyString())).thenReturn(true);
        run(console, "spawn", "test_respawn");
        verify(console).sendMessage(text("Only players"));
    }

    @Test
    @DisplayName("/metin boost global|player")
    void boost() {
        run(admin, "boost", "global", "2", "120");
        assertNotNull(boosts.getGlobalBoost());
        assertEquals(2.0, boosts.getGlobalBoost().multiplier);
        verify(server).broadcast(text("GLOBAL 2.0x 2m"));

        var target = TestSupport.player("Target");
        run(admin, "boost", "player", "3", "60", "Target");
        assertEquals(3.0, boosts.getPersonalBoost(target.getUniqueId()).multiplier);
        verify(target).sendMessage(text("BOOST ACTIVATED"));

        run(admin, "boost", "global", "x", "60");
        verify(admin).sendMessage(text("Invalid number"));
    }

    @Test
    @DisplayName("/metin stats: own stats with simplemetin.stats, others need simplemetin.stats.others")
    void statsCommand() {
        var player = TestSupport.player("Gamer");
        when(player.hasPermission("simplemetin.stats")).thenReturn(true);
        stats.getOrCreateStats(player.getUniqueId()).addDamage(42);
        stats.getOrCreateStats(player.getUniqueId()).addCrystalDestroyed("test_respawn");

        run(player, "stats");
        assertTrue(wasSent(player, "Total Damage: 42 (#1)"), String.join(" | ", TestSupport.sentMessages(player)));
        assertTrue(wasSent(player, "test_respawn: 1"));
        assertTrue(wasSent(player, "Strength: 10"));

        run(player, "stats", "Admin");
        verify(player).sendMessage(text("don't have permission"));
        run(player, "stats", "Gamer");
        verify(player, atLeast(2)).sendMessage(text("Total Damage: 42"));

        run(admin, "stats", "Gamer");
        verify(admin).sendMessage(text("Total Damage: 42"));
        run(admin, "stats", "Admin");
        verify(admin).sendMessage(text("has not attacked any crystal yet"));
    }

    @Test
    @DisplayName("players without admin rights: /metin shows only allowed commands; admin commands are denied")
    void permissionFilteredHelp() {
        var player = TestSupport.player("Gamer");
        when(player.hasPermission("simplemetin.stats")).thenReturn(true);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));

        run(player);
        assertTrue(wasSent(player, "/metin stats"));
        assertFalse(wasSent(player, "/metin spawn"));
        assertEquals(List.of("stats"), cmd.onTabComplete(player, command, "metin", new String[]{""}));

        run(player, "spawn", "test_respawn");
        assertTrue(crystals.getAllCrystals().isEmpty());
        verify(player).sendMessage(text("don't have permission"));

        when(player.hasPermission("simplemetin.spawn")).thenReturn(true);
        run(player, "spawn", "test_respawn");
        assertEquals(1, crystals.getAllCrystals().size(), "single permission is enough");
    }

    @Test
    @DisplayName("/metin respawn rejects an active crystal, respawns a destroyed one")
    void respawnCommand() {
        run(admin, "spawn", "test_respawn");
        var data = crystals.getCrystalById("test_respawn-1");
        run(admin, "respawn", data.getId());
        verify(admin).sendMessage(text("is not destroyed"));
        crystals.markCrystalDestroyed(data.getId());
        run(admin, "respawn", data.getId());
        assertFalse(data.isDestroyed());
    }

    @Test
    @DisplayName("tab completion lists subcommands, types, IDs and nearest")
    void tabComplete() {
        run(admin, "spawn", "test_respawn");
        assertEquals(List.of("boost"), cmd.onTabComplete(admin, command, "metin", new String[]{"b"}));
        var types = cmd.onTabComplete(admin, command, "metin", new String[]{"spawn", ""});
        assertTrue(types.containsAll(List.of("test_respawn", "test_once")));
        var ids = cmd.onTabComplete(admin, command, "metin", new String[]{"remove", ""});
        assertTrue(ids.containsAll(List.of("nearest", "test_respawn-1")), ids.toString());
        assertEquals(List.of("auto", "test_respawn-2"), cmd.onTabComplete(admin, command, "metin", new String[]{"spawn", "test_respawn", ""}));
    }

    @Test
    @DisplayName("/metin spawner list shows the spawners from spawners.yml")
    void spawnerList() {
        run(admin, "spawner", "list");
        assertTrue(wasSent(admin, "wilderness [OFF] [random]"), String.join(" | ", TestSupport.sentMessages(admin)));
        assertTrue(wasSent(admin, "arena [OFF] [points]"));
    }
}
