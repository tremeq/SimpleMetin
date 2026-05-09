package me.simplemetin.commands;

import me.simplemetin.SimpleMetin;
import me.simplemetin.managers.CrystalManager;
import me.simplemetin.utils.VoucherUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MetinCommand implements CommandExecutor, TabCompleter {
    private final SimpleMetin plugin;
    private final CrystalManager crystalManager;

    public MetinCommand(SimpleMetin plugin, CrystalManager crystalManager) {
        this.plugin = plugin;
        this.crystalManager = crystalManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("simplemetin.admin")) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission");
            if (message != null && !message.isEmpty()) {
                String prefix = plugin.getConfig().getString("messages.prefix", "");
                sender.sendMessage(colorize(prefix + message));
            }
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "respawn" -> handleRespawn(sender, args);
            case "reload" -> handleReload(sender);
            case "givepreview" -> handleGivePreview(sender, args);
            case "stats" -> handleStats(sender, args);
            case "givevoucher" -> handleGiveVoucher(sender, args);
            case "boost" -> handleBoost(sender, args);
            case "updateleaderboard", "refreshleaderboard" -> handleUpdateLeaderboard(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cOnly players can use this command"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /metin spawn <config-id> [name]"));
            return;
        }

        var configId = args[1];
        var config = plugin.getConfig().getConfigurationSection("crystals." + configId);

        if (config == null) {
            sender.sendMessage(colorize("&cCrystal config '" + configId + "' not found"));
            return;
        }

        var id = UUID.randomUUID().toString();
        var location = player.getLocation();
        var overrideName = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;

        crystalManager.spawnCrystal(id, configId, location, overrideName);

        sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix", "") +
                plugin.getConfig().getString("messages.spawned", "&aSpawned crystal")
                        .replace("%id%", id)));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /metin remove <id>"));
            return;
        }

        var id = args[1];
        var data = crystalManager.getCrystalById(id);

        if (data == null) {
            sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix", "") +
                    plugin.getConfig().getString("messages.not-found", "&cCrystal not found")
                            .replace("%id%", id)));
            return;
        }

        crystalManager.removeCrystal(id);
        sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix", "") +
                plugin.getConfig().getString("messages.removed", "&cRemoved crystal")
                        .replace("%id%", id)));
    }

    private void handleList(CommandSender sender) {
        var crystals = crystalManager.getAllCrystals();

        if (crystals.isEmpty()) {
            sender.sendMessage(colorize("&7No active crystals"));
            return;
        }

        sender.sendMessage(colorize("&e&lActive Crystals:"));
        for (var data : crystals) {
            var status = data.isDestroyed() ? "&c[DESTROYED]" : "&a[ACTIVE]";
            sender.sendMessage(colorize("&7- &e" + data.getId() + " " + status + " &7Type: &f" +
                    data.getConfigId() + " &7HP: &c" + data.getCurrentHp() + "&7/&c" + data.getMaxHp()));
        }
    }

    private void handleRespawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /metin respawn <id>"));
            return;
        }

        var id = args[1];
        var data = crystalManager.getCrystalById(id);

        if (data == null) {
            sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix", "") +
                    plugin.getConfig().getString("messages.not-found", "&cCrystal not found")
                            .replace("%id%", id)));
            return;
        }

        if (!data.isDestroyed()) {
            sender.sendMessage(colorize("&cCrystal is not destroyed"));
            return;
        }

        crystalManager.manualRespawn(id);
        sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix", "") +
                plugin.getConfig().getString("messages.respawned", "&aRespawned crystal")
                        .replace("%id%", id)));
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(colorize(plugin.getConfig().getString("messages.prefix", "") +
                plugin.getConfig().getString("messages.reloaded", "&aReloaded configuration")));
    }

    private void handleGivePreview(CommandSender sender, String[] args) {
        sender.sendMessage(colorize("&e[Preview] &7This would simulate drops for the specified crystal and player"));
        sender.sendMessage(colorize("&7Feature not yet implemented"));
    }

    private void handleStats(CommandSender sender, String[] args) {
        Player target;
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cUsage: /metin stats <player>"));
                return;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(colorize("&cPlayer not found!"));
                return;
            }
        }

        var stats = plugin.getStatsManager().getStats(target.getUniqueId());
        if (stats == null) {
            sender.sendMessage(colorize("&cNo stats found for " + target.getName()));
            return;
        }

        sender.sendMessage(colorize("&e&l" + target.getName() + "'s Stats:"));
        sender.sendMessage(colorize("&7Crystals Destroyed: &e" + stats.getCrystalsDestroyed()));
        sender.sendMessage(colorize("&7Total Damage: &e" + stats.getTotalDamageDealt()));
        sender.sendMessage(colorize("&7Items Received: &e" + stats.getItemsReceived()));
        sender.sendMessage(colorize("&7Money Earned: &e$" + stats.getMoneyEarned()));
    }

    private void handleGiveVoucher(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(colorize("&cUsage: /metin givevoucher <player> <multiplier> <duration_seconds>"));
            return;
        }

        var targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sender.sendMessage(colorize("&cPlayer not found!"));
            return;
        }

        try {
            double multiplier = Double.parseDouble(args[2]);
            long duration = Long.parseLong(args[3]);

            var voucher = VoucherUtils.createBoostVoucher(plugin, multiplier, duration);
            targetPlayer.getInventory().addItem(voucher);

            sender.sendMessage(colorize("&aGave boost voucher to " + targetPlayer.getName() +
                    " (&e" + multiplier + "x &afor &e" + duration + "s&a)"));
            targetPlayer.sendMessage(colorize("&aYou received a boost voucher!"));
        } catch (NumberFormatException e) {
            sender.sendMessage(colorize("&cInvalid number format!"));
        }
    }

    private void handleBoost(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(colorize("&cUsage: /metin boost <global|player> <multiplier> <duration_seconds> [player]"));
            return;
        }

        String type = args[1].toLowerCase();
        try {
            double multiplier = Double.parseDouble(args[2]);
            long duration = Long.parseLong(args[3]);

            if (type.equals("global")) {
                plugin.getBoostManager().activateGlobalBoost(multiplier, duration);
                Bukkit.broadcastMessage(colorize(plugin.getConfig().getString("messages.prefix", "") +
                        plugin.getConfig().getString("messages.global-boost-activated", "")
                                .replace("%multiplier%", String.format("%.1f", multiplier))
                                .replace("%duration%", formatDuration(duration))));
            } else if (type.equals("player")) {
                if (args.length < 5) {
                    sender.sendMessage(colorize("&cUsage: /metin boost player <multiplier> <duration> <player>"));
                    return;
                }
                var target = Bukkit.getPlayer(args[4]);
                if (target == null) {
                    sender.sendMessage(colorize("&cPlayer not found!"));
                    return;
                }
                plugin.getBoostManager().activatePersonalBoost(target, multiplier, duration);
                sender.sendMessage(colorize("&aActivated &e" + multiplier + "x &aboost for &e" + target.getName()));
            } else {
                sender.sendMessage(colorize("&cInvalid type! Use 'global' or 'player'"));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(colorize("&cInvalid number format!"));
        }
    }

    private void handleUpdateLeaderboard(CommandSender sender) {
        sender.sendMessage(colorize("&eUpdating statistics files..."));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getStatsManager().saveLeaderboards();
            plugin.getStatsManager().saveReadableStatistics();
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(colorize("&aStatistics updated successfully!"));
                sender.sendMessage(colorize("&7Check &eleaderboards.yml &7and &estatistics.yml &7in the plugin folder."));
            });
        });
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&e&lSimpleMetin Commands:"));
        sender.sendMessage(colorize("&7/metin spawn <config-id> [name] &f- Spawn a crystal"));
        sender.sendMessage(colorize("&7/metin remove <id> &f- Remove a crystal"));
        sender.sendMessage(colorize("&7/metin list &f- List all crystals"));
        sender.sendMessage(colorize("&7/metin respawn <id> &f- Manually respawn a crystal"));
        sender.sendMessage(colorize("&7/metin reload &f- Reload configuration"));
        sender.sendMessage(colorize("&7/metin stats [player] &f- View player stats"));
        sender.sendMessage(colorize("&7/metin givevoucher <player> <mult> <dur> &f- Give boost voucher"));
        sender.sendMessage(colorize("&7/metin boost <global|player> <mult> <dur> [player] &f- Activate boost"));
        sender.sendMessage(colorize("&7/metin updateleaderboard &f- Manually update leaderboards.yml"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!sender.hasPermission("simplemetin.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return Arrays.asList("spawn", "remove", "list", "respawn", "reload", "givepreview",
                    "stats", "givevoucher", "boost", "updateleaderboard")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spawn")) {
                var crystalsSection = plugin.getConfig().getConfigurationSection("crystals");
                if (crystalsSection != null) {
                    return new ArrayList<>(crystalsSection.getKeys(false));
                }
            } else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("respawn")) {
                return crystalManager.getAllCrystals().stream()
                        .map(data -> data.getId())
                        .collect(Collectors.toList());
            }
        }

        return List.of();
    }

    private String colorize(String text) {
        return text.replace('&', '§');
    }
}
