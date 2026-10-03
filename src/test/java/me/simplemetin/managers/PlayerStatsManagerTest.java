package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import me.simplemetin.commands.MetinAdminCommand;
import me.simplemetin.testutil.TestSupport;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerStatsManagerTest {

    @TempDir
    Path dir;
    SimpleMetin plugin;
    Player steve;

    @BeforeEach
    void setUp() {
        TestSupport.resetServer();
        plugin = TestSupport.plugin(dir.toFile(), new YamlConfiguration());
        steve = TestSupport.player("Steve");
    }

    YamlConfiguration file() {
        return YamlConfiguration.loadConfiguration(new File(dir.toFile(), "players.yml"));
    }

    void join(PlayerStatsManager m, Player p) {
        var e = mock(PlayerJoinEvent.class);
        when(e.getPlayer()).thenReturn(p);
        m.onJoin(e);
    }

    @Test
    @DisplayName("new player on join: strength 10, written to players.yml immediately")
    void newPlayerDefault() {
        var m = new PlayerStatsManager(plugin);
        join(m, steve);
        assertEquals(10, m.getDamage(steve.getUniqueId()));
        assertEquals(10, file().getInt(steve.getUniqueId() + ".damage"));
    }

    @Test
    @DisplayName("existing player is loaded from players.yml")
    void existingPlayerLoaded() throws Exception {
        Files.writeString(dir.resolve("players.yml"), steve.getUniqueId() + ":\n  damage: 42\n");
        var m = new PlayerStatsManager(plugin);
        join(m, steve);
        assertEquals(42, m.getDamage(steve.getUniqueId()));
    }

    @Test
    @DisplayName("set / add / reset are persisted immediately")
    void setAddResetPersist() {
        var m = new PlayerStatsManager(plugin);
        join(m, steve);
        m.setDamage(steve.getUniqueId(), 25);
        assertEquals(25, file().getInt(steve.getUniqueId() + ".damage"));
        m.addDamage(steve.getUniqueId(), 5);
        assertEquals(30, m.getDamage(steve.getUniqueId()));
        assertEquals(30, file().getInt(steve.getUniqueId() + ".damage"));
        m.resetDamage(steve.getUniqueId());
        assertEquals(10, file().getInt(steve.getUniqueId() + ".damage"));
    }

    @Test
    @DisplayName("quit persists and evicts from cache; offline lookup reads the file")
    void quitPersists() {
        var m = new PlayerStatsManager(plugin);
        join(m, steve);
        m.setDamage(steve.getUniqueId(), 77);
        var quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(steve);
        m.onQuit(quit);
        assertEquals(77, m.getDamage(steve.getUniqueId()));
        assertEquals(77, file().getInt(steve.getUniqueId() + ".damage"));
        assertEquals(10, m.getDamage(UUID.randomUUID()), "unknown player -> default");
    }

    @Test
    @DisplayName("leaderboard: top 10 sorted by strength desc")
    void leaderboard() throws Exception {
        var sb = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            sb.append(UUID.nameUUIDFromBytes(("p" + i).getBytes())).append(":\n  damage: ").append(i * 10).append('\n');
        }
        sb.append("not-a-uuid:\n  damage: 9999\n");
        Files.writeString(dir.resolve("players.yml"), sb.toString());

        var m = new PlayerStatsManager(plugin);
        var top = m.getCachedLeaderboard();
        assertEquals(10, top.size());
        assertEquals(120, top.get(0).value());
        assertEquals(30, top.get(9).value());
        for (int i = 1; i < top.size(); i++) {
            assertTrue(top.get(i - 1).value() >= top.get(i).value());
        }
    }

    @Test
    @DisplayName("/ma add|set|get|reset")
    void adminCommand() {
        var m = new PlayerStatsManager(plugin);
        when(plugin.getPlayerStatsManager()).thenReturn(m);
        join(m, steve);
        var cmd = new MetinAdminCommand(plugin);
        var sender = mock(CommandSender.class);
        when(sender.hasPermission("simplemetin.admin")).thenReturn(true);
        var c = mock(Command.class);

        cmd.onCommand(sender, c, "ma", new String[]{"add", "Steve", "5"});
        assertEquals(15, m.getDamage(steve.getUniqueId()));
        cmd.onCommand(sender, c, "ma", new String[]{"set", "Steve", "50"});
        assertEquals(50, m.getDamage(steve.getUniqueId()));
        cmd.onCommand(sender, c, "ma", new String[]{"set", "Steve", "abc"});
        assertEquals(50, m.getDamage(steve.getUniqueId()));
        cmd.onCommand(sender, c, "ma", new String[]{"get", "Steve"});
        verify(sender).sendMessage(TestSupport.text("strength: 50"));
        cmd.onCommand(sender, c, "ma", new String[]{"reset", "Steve"});
        assertEquals(10, m.getDamage(steve.getUniqueId()));

        var noPerm = mock(CommandSender.class);
        cmd.onCommand(noPerm, c, "ma", new String[]{"set", "Steve", "999"});
        assertEquals(10, m.getDamage(steve.getUniqueId()));
    }

    @Test
    @DisplayName("/ma set rejects 0 / negative strength, /ma add cannot go below 1")
    void adminCommandRejectsNonPositive() {
        var m = new PlayerStatsManager(plugin);
        when(plugin.getPlayerStatsManager()).thenReturn(m);
        join(m, steve);
        var sender = mock(CommandSender.class);
        when(sender.hasPermission("simplemetin.admin")).thenReturn(true);
        var cmd = new MetinAdminCommand(plugin);

        cmd.onCommand(sender, mock(Command.class), "ma", new String[]{"set", "Steve", "-5"});
        assertEquals(10, m.getDamage(steve.getUniqueId()));
        cmd.onCommand(sender, mock(Command.class), "ma", new String[]{"set", "Steve", "0"});
        assertEquals(10, m.getDamage(steve.getUniqueId()));
        verify(sender, times(2)).sendMessage(TestSupport.text("at least"));

        cmd.onCommand(sender, mock(Command.class), "ma", new String[]{"add", "Steve", "-100"});
        assertEquals(1, m.getDamage(steve.getUniqueId()));
        cmd.onCommand(sender, mock(Command.class), "ma", new String[]{"set", "Steve", String.valueOf(Integer.MAX_VALUE)});
        cmd.onCommand(sender, mock(Command.class), "ma", new String[]{"add", "Steve", "5"});
        assertEquals(Integer.MAX_VALUE, m.getDamage(steve.getUniqueId()), "no int overflow");
    }

    @Test
    @DisplayName("/ma works for known offline players, unknown names are rejected")
    void adminCommandOfflinePlayer() throws Exception {
        var offline = UUID.nameUUIDFromBytes("OfflineGuy".getBytes());
        Files.writeString(dir.resolve("players.yml"), offline + ":\n  damage: 15\n");
        var m = new PlayerStatsManager(plugin);
        when(plugin.getPlayerStatsManager()).thenReturn(m);
        var op = mock(org.bukkit.OfflinePlayer.class);
        when(op.getUniqueId()).thenReturn(offline);
        when(op.getName()).thenReturn("OfflineGuy");
        when(TestSupport.server().getOfflinePlayerIfCached("OfflineGuy")).thenReturn(op);
        var sender = mock(CommandSender.class);
        when(sender.hasPermission("simplemetin.admin")).thenReturn(true);
        var cmd = new MetinAdminCommand(plugin);

        cmd.onCommand(sender, mock(Command.class), "ma", new String[]{"get", "OfflineGuy"});
        verify(sender).sendMessage(TestSupport.text("strength: 15"));
        cmd.onCommand(sender, mock(Command.class), "ma", new String[]{"set", "OfflineGuy", "40"});
        assertEquals(40, m.getDamage(offline));
        assertEquals(40, file().getInt(offline + ".damage"));

        cmd.onCommand(sender, mock(Command.class), "ma", new String[]{"set", "Nobody", "40"});
        verify(sender).sendMessage(TestSupport.text("was not found"));
    }

    @Test
    @DisplayName("/ma matches names exactly (\"Ste\" is not \"Steve\")")
    void adminCommandExactName() {
        var m = new PlayerStatsManager(plugin);
        when(plugin.getPlayerStatsManager()).thenReturn(m);
        join(m, steve);
        when(TestSupport.server().getPlayer("Ste")).thenReturn(steve); // prefix match of Bukkit.getPlayer
        var sender = mock(CommandSender.class);
        when(sender.hasPermission("simplemetin.admin")).thenReturn(true);

        new MetinAdminCommand(plugin).onCommand(sender, mock(Command.class), "ma", new String[]{"set", "Ste", "99"});

        assertEquals(10, m.getDamage(steve.getUniqueId()));
    }
}
