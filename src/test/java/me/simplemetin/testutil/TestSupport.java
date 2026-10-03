package me.simplemetin.testutil;

import me.simplemetin.SimpleMetin;
import me.simplemetin.messages.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.TextDisplay;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BossBar;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Minimal fake Bukkit environment built from Mockito mocks.
 * Bukkit.setServer() can only be called once per JVM, so the server mock is shared and re-stubbed per test.
 */
public final class TestSupport {

    public static final Logger LOGGER = Logger.getLogger("SimpleMetinTest");
    public static final List<LogRecord> LOGS = Collections.synchronizedList(new ArrayList<>());
    private static final Map<UUID, String> NAMES = new ConcurrentHashMap<>();
    private static final Map<UUID, AtomicReference<Location>> SPAWN_LOCATIONS = new ConcurrentHashMap<>();
    private static Server server;

    static {
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(new Handler() {
            @Override public void publish(LogRecord record) { LOGS.add(record); }
            @Override public void flush() { }
            @Override public void close() { }
        });
    }

    private TestSupport() {
    }

    public static synchronized Server resetServer() {
        if (server == null) {
            server = mock(Server.class);
            when(server.getLogger()).thenReturn(LOGGER);
            try {
                Bukkit.setServer(server);
            } catch (RuntimeException ignored) {
                // The singleton is assigned first; only the version banner (ServerBuildInfo) fails without a real server.
            }
        }
        reset(server);
        LOGS.clear();
        NAMES.clear();

        when(server.getLogger()).thenReturn(LOGGER);
        when(server.getScheduler()).thenReturn(mock(BukkitScheduler.class));
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(server.getConsoleSender()).thenReturn(mock(ConsoleCommandSender.class));
        when(server.createBossBar(any(), any(), any(), any(BarFlag[].class))).thenAnswer(inv -> bossBar());
        when(server.getOfflinePlayer(any(UUID.class))).thenAnswer(inv -> offlinePlayer(inv.getArgument(0)));
        return server;
    }

    public static Server server() {
        return server;
    }

    public static boolean logged(String fragment) {
        synchronized (LOGS) {
            return LOGS.stream().anyMatch(r -> r.getMessage() != null && r.getMessage().contains(fragment));
        }
    }

    // ── Config ──────────────────────────────────────────────────

    public static YamlConfiguration yaml(String text) {
        var cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        return cfg;
    }

    public static YamlConfiguration bundledResource(String name) {
        var in = TestSupport.class.getClassLoader().getResourceAsStream(name);
        if (in == null) throw new IllegalStateException("Missing resource " + name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    // ── Plugin ──────────────────────────────────────────────────

    public static SimpleMetin plugin(File dataFolder, YamlConfiguration config) {
        SimpleMetin plugin = mock(SimpleMetin.class);
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(LOGGER);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getName()).thenReturn("SimpleMetin");
        // Real messages (bundled messages-en.yml / messages-pl.yml), so tests see the actual texts
        var messages = new MessageManager(plugin);
        messages.load();
        when(plugin.getMessages()).thenReturn(messages);
        return plugin;
    }

    // ── Adventure components ────────────────────────────────────

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Matches a Component whose plain text contains the fragment. */
    public static Component text(String fragment) {
        return argThat(c -> c != null && plain(c).contains(fragment));
    }

    /** All plain texts of Components sent to the receiver with sendMessage(Component). */
    public static List<String> sentMessages(org.bukkit.command.CommandSender receiver) {
        var captor = org.mockito.ArgumentCaptor.forClass(Component.class);
        verify(receiver, atLeast(0)).sendMessage(captor.capture());
        return captor.getAllValues().stream().map(TestSupport::plain).toList();
    }

    public static boolean wasSent(org.bukkit.command.CommandSender receiver, String fragment) {
        return sentMessages(receiver).stream().anyMatch(m -> m.contains(fragment));
    }

    // ── Players ─────────────────────────────────────────────────

    public static Player player(String name) {
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn(name);
        when(player.isOnline()).thenReturn(true);
        when(player.getPlayer()).thenReturn(player);

        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.addItem(any(ItemStack[].class))).thenAnswer(inv -> new HashMap<Integer, ItemStack>());
        when(player.getInventory()).thenReturn(inventory);

        NAMES.put(uuid, name);
        when(server.getPlayer(name)).thenReturn(player);
        when(server.getPlayerExact(name)).thenReturn(player);
        when(server.getPlayer(uuid)).thenReturn(player);
        return player;
    }

    public static UUID registerName(String name) {
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        NAMES.put(uuid, name);
        return uuid;
    }

    private static OfflinePlayer offlinePlayer(UUID uuid) {
        OfflinePlayer p = mock(OfflinePlayer.class);
        when(p.getUniqueId()).thenReturn(uuid);
        when(p.getName()).thenReturn(NAMES.get(uuid));
        return p;
    }

    // ── World & entities ────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static World world(String name, List<Entity> spawned) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(name);
        when(world.getPlayers()).thenReturn(new ArrayList<>());
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.getChunkAtAsync(anyInt(), anyInt())).thenAnswer(inv ->
                java.util.concurrent.CompletableFuture.completedFuture(chunk(world, inv.getArgument(0), inv.getArgument(1))));
        when(world.spawn(any(Location.class), any(Class.class), any(Consumer.class))).thenAnswer(inv -> {
            Class<? extends Entity> type = inv.getArgument(1);
            Entity entity = entity(type);
            SPAWN_LOCATIONS.get(entity.getUniqueId()).set(((Location) inv.getArgument(0)).clone());
            ((Consumer) inv.getArgument(2)).accept(entity);
            spawned.add(entity);
            return entity;
        });
        when(server.getWorld(name)).thenReturn(world);
        return world;
    }

    public static <T extends Entity> T entity(Class<T> type) {
        T entity = mock(type);
        UUID uuid = UUID.randomUUID();
        AtomicBoolean dead = new AtomicBoolean(false);
        Set<String> tags = new HashSet<>();
        AtomicReference<String> customName = new AtomicReference<>();
        AtomicReference<Location> location = new AtomicReference<>();
        SPAWN_LOCATIONS.put(uuid, location);

        when(entity.getUniqueId()).thenReturn(uuid);
        when(entity.isDead()).thenAnswer(i -> dead.get());
        when(entity.isValid()).thenAnswer(i -> !dead.get());
        doAnswer(i -> { dead.set(true); return null; }).when(entity).remove();
        when(entity.addScoreboardTag(anyString())).thenAnswer(i -> tags.add(i.getArgument(0)));
        when(entity.getScoreboardTags()).thenReturn(tags);
        doAnswer(i -> { customName.set(i.getArgument(0)); return null; }).when(entity).setCustomName(any());
        when(entity.getCustomName()).thenAnswer(i -> customName.get());
        when(entity.getLocation()).thenAnswer(i -> location.get() != null ? location.get().clone() : null);
        if (entity instanceof TextDisplay display) {
            AtomicReference<Component> text = new AtomicReference<>(Component.empty());
            doAnswer(i -> { text.set(i.getArgument(0)); return null; }).when(display).text(any(Component.class));
            when(display.text()).thenAnswer(i -> text.get());
        }
        return entity;
    }

    public static org.bukkit.Chunk chunk(World world, int x, int z) {
        var chunk = mock(org.bukkit.Chunk.class);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(x);
        when(chunk.getZ()).thenReturn(z);
        return chunk;
    }

    public static List<ArmorStand> armorStands(List<Entity> spawned) {
        return spawned.stream().filter(e -> e instanceof ArmorStand).map(e -> (ArmorStand) e).toList();
    }

    public static List<TextDisplay> holograms(List<Entity> spawned) {
        return spawned.stream().filter(e -> e instanceof TextDisplay).map(e -> (TextDisplay) e).toList();
    }

    /** Plain text of the live holograms (one TextDisplay per crystal). */
    public static List<String> liveHologramTexts(List<Entity> spawned) {
        return holograms(spawned).stream().filter(h -> !h.isDead()).map(h -> plain(h.text())).toList();
    }

    // ── Boss bars ───────────────────────────────────────────────

    public static BossBar bossBar() {
        BossBar bar = mock(BossBar.class);
        List<Player> players = new ArrayList<>();
        doAnswer(i -> { players.add(i.getArgument(0)); return null; }).when(bar).addPlayer(any());
        doAnswer(i -> { players.remove((Player) i.getArgument(0)); return null; }).when(bar).removePlayer(any());
        doAnswer(i -> { players.clear(); return null; }).when(bar).removeAll();
        when(bar.getPlayers()).thenAnswer(i -> new ArrayList<>(players));
        return bar;
    }
}
