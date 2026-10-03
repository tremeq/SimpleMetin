package me.simplemetin.commands;

import me.simplemetin.SimpleMetin;
import me.simplemetin.managers.BoostManager;
import me.simplemetin.managers.CrystalManager;
import me.simplemetin.managers.CustomItemManager;
import me.simplemetin.managers.SpawnerManager;
import me.simplemetin.messages.MessageManager;
import me.simplemetin.models.CrystalData;
import me.simplemetin.models.IntRange;
import me.simplemetin.utils.TextUtils;
import me.simplemetin.utils.DropUtils;
import me.simplemetin.utils.VoucherUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetinCommand implements CommandExecutor, TabCompleter {

    public static final String ADMIN = "simplemetin.admin";

    /** Subcommand -> permission; the order is the order of /metin help and tab completion. */
    private static final LinkedHashMap<String, String> PERMISSIONS = new LinkedHashMap<>();

    static {
        PERMISSIONS.put("spawn", "simplemetin.spawn");
        PERMISSIONS.put("remove", "simplemetin.remove");
        PERMISSIONS.put("list", "simplemetin.list");
        PERMISSIONS.put("info", "simplemetin.info");
        PERMISSIONS.put("tp", "simplemetin.tp");
        PERMISSIONS.put("respawn", "simplemetin.respawn");
        PERMISSIONS.put("reload", "simplemetin.reload");
        PERMISSIONS.put("stats", "simplemetin.stats");
        PERMISSIONS.put("givevoucher", "simplemetin.voucher");
        PERMISSIONS.put("boost", "simplemetin.boost");
        PERMISSIONS.put("updateleaderboard", "simplemetin.leaderboard");
        PERMISSIONS.put("spawner", "simplemetin.spawner");
        PERMISSIONS.put("item", "simplemetin.items");
        PERMISSIONS.put("adddrop", "simplemetin.items");
    }

    private final SimpleMetin plugin;
    private final CrystalManager crystalManager;

    public MetinCommand(SimpleMetin plugin, CrystalManager crystalManager) {
        this.plugin = plugin;
        this.crystalManager = crystalManager;
    }

    private MessageManager msg() {
        return plugin.getMessages();
    }

    /** simplemetin.admin grants everything, also when a permission plugin does not resolve the children. */
    static boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission(ADMIN);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        var sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("refreshleaderboard")) sub = "updateleaderboard";
        var permission = PERMISSIONS.get(sub);
        if (permission == null) {
            sendHelp(sender);
            return true;
        }
        if (!has(sender, permission)) {
            msg().send(sender, "no-permission");
            return true;
        }

        switch (sub) {
            case "spawn" -> handleSpawn(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "tp" -> handleTeleport(sender, args);
            case "respawn" -> handleRespawn(sender, args);
            case "reload" -> handleReload(sender);
            case "stats" -> handleStats(sender, args);
            case "givevoucher" -> handleGiveVoucher(sender, args);
            case "boost" -> handleBoost(sender, args);
            case "updateleaderboard" -> handleUpdateLeaderboard(sender);
            case "spawner" -> handleSpawner(sender, args);
            case "item" -> handleItem(sender, args);
            case "adddrop" -> handleAddDrop(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void usage(CommandSender sender, String usage) {
        msg().send(sender, "usage", "usage", usage);
    }

    // ── Crystals ────────────────────────────────────────────────

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg().send(sender, "player-only");
            return;
        }
        if (args.length < 2) {
            usage(sender, "/metin spawn <type> [id|auto] [name]");
            return;
        }

        var configId = args[1];
        if (plugin.getConfig().getConfigurationSection("crystals." + configId) == null) {
            msg().send(sender, "spawn-unknown-type", "type", configId);
            return;
        }

        String id;
        if (args.length > 2 && !args[2].equalsIgnoreCase("auto")) {
            id = args[2].toLowerCase(Locale.ROOT);
            if (!CrystalManager.isValidId(id) || id.equals("nearest") || id.equals("auto")) {
                msg().send(sender, "spawn-invalid-id", "id", args[2]);
                return;
            }
            if (crystalManager.isIdTaken(id)) {
                msg().send(sender, "spawn-id-taken", "id", id);
                return;
            }
        } else {
            id = crystalManager.generateId(configId);
        }

        var overrideName = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;
        crystalManager.spawnCrystal(id, configId, player.getLocation(), overrideName);
        msg().send(sender, "spawned", "id", id, "type", configId);
    }

    /** Crystal by ID, or the nearest one for "nearest" (players only). Sends the error message itself. */
    private CrystalData resolveCrystal(CommandSender sender, String arg) {
        if (arg.equalsIgnoreCase("nearest")) {
            if (!(sender instanceof Player player)) {
                msg().send(sender, "player-only");
                return null;
            }
            double range = plugin.getConfig().getDouble("settings.nearest-range", 10.0);
            var data = crystalManager.getNearest(player.getLocation(), range);
            if (data == null) {
                msg().send(sender, "no-crystal-nearby", "range", formatNumber(range));
            }
            return data;
        }
        var data = crystalManager.getCrystalById(arg);
        if (data == null) {
            msg().send(sender, "not-found", "id", arg);
        }
        return data;
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/metin remove <id|nearest>");
            return;
        }
        var data = resolveCrystal(sender, args[1]);
        if (data == null) return;

        crystalManager.removeCrystal(data.getId());
        msg().send(sender, "removed", "id", data.getId());
    }

    private void handleList(CommandSender sender) {
        var crystals = crystalManager.getSortedCrystals();
        var pending = plugin.getDataHandler().getPendingIds();

        if (crystals.isEmpty() && pending.isEmpty()) {
            msg().send(sender, "list-empty");
            return;
        }

        msg().sendRaw(sender, "list-header", "count", crystals.size() + pending.size());
        for (var data : crystals) {
            var l = data.getLocation();
            msg().sendRaw(sender, "list-entry", "id", data.getId(), "status", status(data), "type", data.getConfigId(),
                    "hp", data.getCurrentHp(), "max_hp", data.getMaxHp(), "world", data.getWorldName(),
                    "x", l.getBlockX(), "y", l.getBlockY(), "z", l.getBlockZ(),
                    "spawner", data.getSpawnerId() != null ? data.getSpawnerId() : msg().raw("none"));
        }
        for (var id : pending) {
            msg().sendRaw(sender, "list-entry-pending", "id", id, "status", msg().raw("status-pending"));
        }
    }

    private String status(CrystalData data) {
        return msg().raw(data.isDestroyed() ? "status-destroyed" : "status-active");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        var data = resolveCrystal(sender, args.length > 1 ? args[1] : "nearest");
        if (data == null) return;

        long now = System.currentTimeMillis();
        var none = msg().raw("none");
        var l = data.getLocation();
        var respawn = data.isDestroyed() && data.getRespawnTime() > now
                ? SpawnerManager.formatTime((data.getRespawnTime() - now) / 1000) : none;
        var expires = data.getExpiresAt() > now ? SpawnerManager.formatTime((data.getExpiresAt() - now) / 1000) : none;
        var perHit = crystalManager.getDamagePerHit(data);
        var damage = perHit > 0 ? msg().raw("damage-fixed", "damage", perHit) : msg().raw("damage-strength");

        msg().sendRaw(sender, "info", "id", data.getId(), "type", data.getConfigId(),
                "display_name", crystalManager.getDisplayName(data), "status", status(data),
                "hp", data.getCurrentHp(), "max_hp", data.getMaxHp(), "world", data.getWorldName(),
                "x", l.getBlockX(), "y", l.getBlockY(), "z", l.getBlockZ(), "respawn", respawn,
                "spawner", data.getSpawnerId() != null ? data.getSpawnerId() : none, "expires", expires,
                "damage", damage);
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg().send(sender, "player-only");
            return;
        }
        if (args.length < 2) {
            usage(sender, "/metin tp <id>");
            return;
        }
        var data = resolveCrystal(sender, args[1]);
        if (data == null) return;

        var world = data.getWorldName() != null ? Bukkit.getWorld(data.getWorldName()) : null;
        if (world == null) {
            msg().send(sender, "not-found", "id", data.getId());
            return;
        }
        // Next to the crystal, facing it
        var target = data.getLocation().clone().add(2.5, 0, 0.5);
        target.setWorld(world);
        target.setDirection(data.getLocation().toVector().subtract(target.toVector()));
        player.teleportAsync(target);
        msg().send(sender, "teleported", "id", data.getId());
    }

    private void handleRespawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/metin respawn <id|nearest>");
            return;
        }
        var data = resolveCrystal(sender, args[1]);
        if (data == null) return;

        if (!data.isDestroyed()) {
            msg().send(sender, "not-destroyed", "id", data.getId());
            return;
        }
        crystalManager.manualRespawn(data.getId());
        msg().send(sender, "respawned", "id", data.getId());
    }

    private void handleReload(CommandSender sender) {
        int restored = plugin.reloadAll();
        if (restored > 0) {
            msg().send(sender, "reload-pending-loaded", "count", restored);
        }
        msg().send(sender, "reloaded");
    }

    // ── Statistics ──────────────────────────────────────────────

    private void handleStats(CommandSender sender, String[] args) {
        UUID uuid;
        String name;
        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                usage(sender, "/metin stats <player>");
                return;
            }
            uuid = player.getUniqueId();
            name = player.getName();
        } else {
            if (!has(sender, "simplemetin.stats.others")
                    && !(sender instanceof Player self && self.getName().equalsIgnoreCase(args[1]))) {
                msg().send(sender, "no-permission");
                return;
            }
            Player online = Bukkit.getPlayerExact(args[1]);
            OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayerIfCached(args[1]);
            if (target == null) {
                msg().send(sender, "player-not-found", "player", args[1]);
                return;
            }
            uuid = target.getUniqueId();
            name = target.getName() != null ? target.getName() : args[1];
        }

        var statsManager = plugin.getStatsManager();
        var stats = statsManager.getStats(uuid);
        if (stats == null) {
            msg().send(sender, "stats-none", "player", name);
            return;
        }

        msg().sendRaw(sender, "stats", "player", name,
                "destroyed", stats.getCrystalsDestroyed(), "damage", stats.getTotalDamageDealt(),
                "items", stats.getItemsReceived(), "money", stats.getMoneyEarned(),
                "strength", plugin.getPlayerStatsManager().getDamage(uuid),
                "rank_destroyed", rank("destroyed", uuid), "rank_damage", rank("damage", uuid),
                "rank_items", rank("items", uuid), "rank_money", rank("money", uuid));

        var types = stats.getAllCrystalTypes();
        if (!types.isEmpty()) {
            msg().sendRaw(sender, "stats-types-header");
            types.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> msg().sendRaw(sender, "stats-type-line", "type", e.getKey(), "count", e.getValue()));
        }
    }

    private String rank(String category, UUID uuid) {
        var top = plugin.getStatsManager().getCachedTop(category);
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).getPlayerUuid().equals(uuid)) return String.valueOf(i + 1);
        }
        return msg().raw("none");
    }

    // ── Boosts ──────────────────────────────────────────────────

    private void handleGiveVoucher(CommandSender sender, String[] args) {
        if (args.length < 4) {
            usage(sender, "/metin givevoucher <player> <multiplier> <duration_seconds>");
            return;
        }

        var targetPlayer = Bukkit.getPlayerExact(args[1]);
        if (targetPlayer == null) {
            msg().send(sender, "player-not-found", "player", args[1]);
            return;
        }

        double multiplier;
        long duration;
        try {
            multiplier = Double.parseDouble(args[2]);
            duration = Long.parseLong(args[3]);
        } catch (NumberFormatException e) {
            msg().send(sender, "invalid-number", "value", args[2] + " " + args[3]);
            return;
        }

        var voucher = VoucherUtils.createBoostVoucher(plugin, multiplier, duration);
        targetPlayer.getInventory().addItem(voucher);

        msg().send(sender, "voucher-given", "player", targetPlayer.getName(),
                "multiplier", BoostManager.formatMultiplier(multiplier), "duration", TextUtils.formatDuration(duration));
        msg().send(targetPlayer, "voucher-received");
    }

    private void handleBoost(CommandSender sender, String[] args) {
        if (args.length < 4) {
            usage(sender, "/metin boost <global|player> <multiplier> <duration_seconds> [player]");
            return;
        }

        String type = args[1].toLowerCase(Locale.ROOT);
        double multiplier;
        long duration;
        try {
            multiplier = Double.parseDouble(args[2]);
            duration = Long.parseLong(args[3]);
        } catch (NumberFormatException e) {
            msg().send(sender, "invalid-number", "value", args[2] + " " + args[3]);
            return;
        }
        var multiplierText = BoostManager.formatMultiplier(multiplier);
        var durationText = TextUtils.formatDuration(duration);

        if (type.equals("global")) {
            plugin.getBoostManager().activateGlobalBoost(multiplier, duration);
            msg().broadcast("global-boost-activated", "multiplier", multiplierText, "duration", durationText);
        } else if (type.equals("player")) {
            if (args.length < 5) {
                usage(sender, "/metin boost player <multiplier> <duration> <player>");
                return;
            }
            var target = Bukkit.getPlayerExact(args[4]);
            if (target == null) {
                msg().send(sender, "player-not-found", "player", args[4]);
                return;
            }
            boolean replaced = plugin.getBoostManager().getPersonalBoost(target.getUniqueId()) != null;
            plugin.getBoostManager().activatePersonalBoost(target, multiplier, duration);
            msg().send(sender, replaced ? "boost-player-replaced" : "boost-player-activated",
                    "multiplier", multiplierText, "player", target.getName(), "duration", durationText);
            msg().send(target, "boost-activated", "multiplier", multiplierText, "duration", durationText);
        } else {
            msg().send(sender, "boost-invalid-type");
        }
    }

    private void handleUpdateLeaderboard(CommandSender sender) {
        msg().send(sender, "leaderboard-updating");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getStatsManager().saveLeaderboards();
            plugin.getStatsManager().saveReadableStatistics();
            Bukkit.getScheduler().runTask(plugin, () -> msg().send(sender, "leaderboard-updated"));
        });
    }

    // ── Spawners ────────────────────────────────────────────────

    private void handleSpawner(CommandSender sender, String[] args) {
        var spawners = plugin.getSpawnerManager();
        var action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";

        if (action.equals("list")) {
            msg().sendRaw(sender, "spawner-list-header");
            long now = System.currentTimeMillis();
            for (var sp : spawners.getSpawners()) {
                msg().sendRaw(sender, "spawner-list-entry", "spawner", sp.getId(),
                        "state", msg().raw(sp.isEnabled() ? "spawner-state-enabled" : "spawner-state-disabled"),
                        "mode", sp.getMode().name().toLowerCase(Locale.ROOT),
                        "alive", crystalManager.countSpawnerCrystals(sp.getId()), "max", sp.getMaxAlive(),
                        "next", sp.isEnabled() ? SpawnerManager.formatTime(Math.max(0, (sp.getNextSpawnAt() - now) / 1000)) : msg().raw("none"));
            }
            return;
        }

        if (args.length < 3 && !action.equals("point")) {
            usage(sender, "/metin spawner <list|spawn <spawner>|point <add|remove|list> <spawner> [#]>");
            return;
        }

        switch (action) {
            case "spawn" -> {
                var sp = spawners.getSpawner(args[2]);
                if (sp == null) {
                    msg().send(sender, "spawner-not-found", "spawner", args[2]);
                    return;
                }
                msg().send(sender, "spawner-force-searching", "spawner", sp.getId());
                spawners.spawnFrom(sp, null, data -> {
                    if (data == null) {
                        msg().send(sender, "spawner-force-failed", "spawner", sp.getId());
                    } else {
                        msg().send(sender, "spawner-forced", "spawner", sp.getId(), "id", data.getId());
                    }
                });
            }
            case "point" -> handleSpawnerPoint(sender, args);
            default -> usage(sender, "/metin spawner <list|spawn|point>");
        }
    }

    private void handleSpawnerPoint(CommandSender sender, String[] args) {
        if (args.length < 4) {
            usage(sender, "/metin spawner point <add|remove|list> <spawner> [#]");
            return;
        }
        var spawners = plugin.getSpawnerManager();
        var sp = spawners.getSpawner(args[3]);
        if (sp == null) {
            msg().send(sender, "spawner-not-found", "spawner", args[3]);
            return;
        }

        try {
            switch (args[2].toLowerCase(Locale.ROOT)) {
                case "add" -> {
                    if (!(sender instanceof Player player)) {
                        msg().send(sender, "player-only");
                        return;
                    }
                    int index = spawners.addPoint(sp, player.getLocation());
                    msg().send(sender, "spawner-point-added", "index", index, "spawner", sp.getId());
                }
                case "remove" -> {
                    if (args.length < 5) {
                        usage(sender, "/metin spawner point remove <spawner> <#>");
                        return;
                    }
                    int index;
                    try {
                        index = Integer.parseInt(args[4].replace("#", ""));
                    } catch (NumberFormatException e) {
                        msg().send(sender, "invalid-number", "value", args[4]);
                        return;
                    }
                    if (spawners.removePoint(sp, index)) {
                        msg().send(sender, "spawner-point-removed", "index", index, "spawner", sp.getId());
                    } else {
                        msg().send(sender, "spawner-point-invalid", "index", index, "spawner", sp.getId());
                    }
                }
                case "list" -> {
                    if (sp.getPoints().isEmpty()) {
                        msg().send(sender, "spawner-point-none", "spawner", sp.getId());
                        return;
                    }
                    msg().sendRaw(sender, "spawner-point-header", "spawner", sp.getId());
                    var points = sp.getPoints();
                    for (int i = 0; i < points.size(); i++) {
                        var parts = points.get(i).trim().split("\\s+");
                        msg().sendRaw(sender, "spawner-point-entry", "index", i + 1,
                                "world", parts.length > 0 ? parts[0] : "?", "x", parts.length > 1 ? parts[1] : "?",
                                "y", parts.length > 2 ? parts[2] : "?", "z", parts.length > 3 ? parts[3] : "?");
                    }
                }
                default -> usage(sender, "/metin spawner point <add|remove|list> <spawner> [#]");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save spawners.yml: " + e.getMessage());
        }
    }

    // ── Custom items & drops ────────────────────────────────────

    private void handleItem(CommandSender sender, String[] args) {
        var items = plugin.getCustomItemManager();
        var action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";

        try {
            switch (action) {
                case "list" -> {
                    var keys = items.getKeys();
                    if (keys.isEmpty()) {
                        msg().send(sender, "items-list-empty");
                        return;
                    }
                    msg().sendRaw(sender, "items-list-header", "count", keys.size());
                    for (var key : keys) {
                        var item = items.get(key);
                        msg().sendRaw(sender, "items-list-entry", "key", key, "material", item != null ? item.getType().name() : "?");
                    }
                }
                case "save" -> {
                    if (!(sender instanceof Player player)) {
                        msg().send(sender, "player-only");
                        return;
                    }
                    if (args.length < 3 || !CustomItemManager.isValidKey(args[2].toLowerCase(Locale.ROOT))) {
                        usage(sender, "/metin item save <key: a-z 0-9 _ ->");
                        return;
                    }
                    var hand = player.getInventory().getItemInMainHand();
                    if (hand.getType() == Material.AIR) {
                        msg().send(sender, "item-hand-empty");
                        return;
                    }
                    var key = args[2].toLowerCase(Locale.ROOT);
                    items.save(key, hand);
                    msg().send(sender, "item-saved", "key", key);
                }
                case "remove" -> {
                    if (args.length < 3) {
                        usage(sender, "/metin item remove <key>");
                        return;
                    }
                    if (items.remove(args[2].toLowerCase(Locale.ROOT))) {
                        msg().send(sender, "item-removed", "key", args[2]);
                    } else {
                        msg().send(sender, "item-not-found", "key", args[2]);
                    }
                }
                case "give" -> {
                    if (args.length < 3) {
                        usage(sender, "/metin item give <key> [player] [amount]");
                        return;
                    }
                    var item = items.get(args[2].toLowerCase(Locale.ROOT));
                    if (item == null) {
                        msg().send(sender, "item-not-found", "key", args[2]);
                        return;
                    }
                    Player target = args.length > 3 ? Bukkit.getPlayerExact(args[3]) : (sender instanceof Player p ? p : null);
                    if (target == null) {
                        msg().send(sender, "player-not-found", "player", args.length > 3 ? args[3] : "-");
                        return;
                    }
                    int amount = args.length > 4 ? parseIntOr(args[4], 1) : 1;
                    item.setAmount(Math.max(1, Math.min(item.getMaxStackSize(), amount)));
                    int given = DropUtils.giveItems(List.of(item), target.getLocation(), target, plugin);
                    msg().send(sender, "item-given", "amount", given, "key", args[2], "player", target.getName());
                }
                default -> usage(sender, "/metin item <save|give|remove|list>");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save items.yml: " + e.getMessage());
        }
    }

    /** /metin adddrop &lt;type&gt; &lt;hit|death&gt; &lt;chance&gt; [amount] [key] - saves the held item and adds it to the drops. */
    private void handleAddDrop(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            msg().send(sender, "player-only");
            return;
        }
        if (args.length < 4) {
            usage(sender, "/metin adddrop <type> <hit|death> <chance> [amount] [key]");
            return;
        }
        var type = args[1];
        if (plugin.getConfig().getConfigurationSection("crystals." + type) == null) {
            msg().send(sender, "spawn-unknown-type", "type", type);
            return;
        }
        var section = args[2].toLowerCase(Locale.ROOT);
        if (!section.equals("hit") && !section.equals("death")) {
            msg().send(sender, "drop-invalid-section");
            return;
        }
        double chance;
        try {
            chance = Double.parseDouble(args[3]);
            if (!Double.isFinite(chance) || chance < 0 || chance > 100) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            msg().send(sender, "invalid-number", "value", args[3]);
            return;
        }
        var amount = args.length > 4 ? IntRange.parse(args[4], null) : IntRange.of(1);
        if (amount == null || amount.min() < 1) {
            msg().send(sender, "invalid-number", "value", args[4]);
            return;
        }
        var hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            msg().send(sender, "item-hand-empty");
            return;
        }

        var items = plugin.getCustomItemManager();
        var key = args.length > 5 ? args[5].toLowerCase(Locale.ROOT)
                : items.nextFreeKey(CrystalManager.shortTypeName(type) + "_" + section);
        if (!CustomItemManager.isValidKey(key)) {
            usage(sender, "/metin adddrop <type> <hit|death> <chance> [amount] [key: a-z 0-9 _ ->]");
            return;
        }

        try {
            items.save(key, hand);
            var path = "crystals." + type + "." + section + "-drops." + key;
            plugin.getConfig().set(path + ".custom-item", key);
            plugin.getConfig().set(path + ".amount", amount.min() == amount.max() ? (Object) amount.min() : amount.toString());
            plugin.getConfig().set(path + ".chance", chance);
            plugin.saveConfig();
            crystalManager.clearDropCache();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save the drop: " + e.getMessage());
            return;
        }
        msg().send(sender, "drop-added", "key", key, "type", type, "section", section,
                "chance", formatNumber(chance), "amount", amount.toString());
    }

    // ── Help & completion ───────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        var lines = new ArrayList<String>();
        for (var entry : PERMISSIONS.entrySet()) {
            if (!has(sender, entry.getValue())) continue;
            switch (entry.getKey()) {
                case "stats" -> {
                    lines.add("help-stats");
                    if (has(sender, "simplemetin.stats.others")) lines.add("help-stats-others");
                }
                case "givevoucher" -> lines.add("help-givevoucher");
                case "updateleaderboard" -> lines.add("help-leaderboard");
                case "item" -> lines.add("help-items");
                default -> lines.add("help-" + entry.getKey());
            }
        }
        if (lines.isEmpty()) {
            msg().send(sender, "no-permission");
            return;
        }
        msg().sendRaw(sender, "help-header");
        lines.forEach(key -> msg().sendRaw(sender, key));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return filter(PERMISSIONS.entrySet().stream()
                    .filter(e -> has(sender, e.getValue()))
                    .map(Map.Entry::getKey), args[0]);
        }

        var sub = args[0].toLowerCase(Locale.ROOT);
        var permission = PERMISSIONS.get(sub);
        if (permission == null || !has(sender, permission)) return List.of();

        if (args.length == 2) {
            return switch (sub) {
                case "spawn", "adddrop" -> filter(crystalTypes(), args[1]);
                case "remove", "respawn", "info", "tp" -> filter(Stream.concat(Stream.of("nearest"),
                        crystalManager.getSortedCrystals().stream().map(CrystalData::getId)), args[1]);
                case "stats" -> has(sender, "simplemetin.stats.others") ? null : List.of();
                case "givevoucher" -> null;
                case "boost" -> filter(Stream.of("global", "player"), args[1]);
                case "spawner" -> filter(Stream.of("list", "spawn", "point"), args[1]);
                case "item" -> filter(Stream.of("save", "give", "remove", "list"), args[1]);
                default -> List.of();
            };
        }

        if (args.length == 3) {
            return switch (sub) {
                case "spawn" -> filter(Stream.of("auto", crystalManager.generateId(args[1])), args[2]);
                case "adddrop" -> filter(Stream.of("hit", "death"), args[2]);
                case "spawner" -> args[1].equalsIgnoreCase("spawn") ? filter(spawnerIds(), args[2])
                        : args[1].equalsIgnoreCase("point") ? filter(Stream.of("add", "remove", "list"), args[2]) : List.of();
                case "item" -> args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("remove")
                        ? filter(plugin.getCustomItemManager().getKeys().stream(), args[2]) : List.of();
                default -> List.of();
            };
        }

        if (args.length == 4 && sub.equals("spawner") && args[1].equalsIgnoreCase("point")) {
            return filter(spawnerIds(), args[3]);
        }
        if (args.length == 4 && sub.equals("item") && args[1].equalsIgnoreCase("give")) {
            return null; // online player names
        }
        if (args.length == 5 && sub.equals("boost") && args[1].equalsIgnoreCase("player")) {
            return null;
        }
        return List.of();
    }

    private Stream<String> crystalTypes() {
        var section = plugin.getConfig().getConfigurationSection("crystals");
        return section == null ? Stream.empty() : section.getKeys(false).stream();
    }

    private Stream<String> spawnerIds() {
        return plugin.getSpawnerManager().getSpawners().stream().map(SpawnerManager.Spawner::getId);
    }

    private static List<String> filter(Stream<String> options, String prefix) {
        var lower = prefix.toLowerCase(Locale.ROOT);
        return options.filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).collect(Collectors.toList());
    }

    private static int parseIntOr(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
