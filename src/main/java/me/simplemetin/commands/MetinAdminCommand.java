package me.simplemetin.commands;

import me.simplemetin.SimpleMetin;
import me.simplemetin.managers.PlayerStatsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetinAdminCommand implements CommandExecutor, TabCompleter {

    private final SimpleMetin plugin;

    public MetinAdminCommand(SimpleMetin plugin) {
        this.plugin = plugin;
    }

    private record Target(UUID uuid, String name) {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, String[] args) {
        if (!MetinCommand.has(sender, "simplemetin.strength")) {
            plugin.getMessages().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> handleAdd(sender, args);
            case "set" -> handleSet(sender, args);
            case "get" -> handleGet(sender, args);
            case "reset" -> handleReset(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    // ── add <player> <value> ────────────────────────────────────

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender, "/ma add <player> <value>");
            return;
        }

        Target target = resolveTarget(sender, args[1]);
        if (target == null) return;

        Integer value = parseInt(sender, args[2]);
        if (value == null) return;

        plugin.getPlayerStatsManager().addDamage(target.uuid(), value);
        int newValue = plugin.getPlayerStatsManager().getDamage(target.uuid());
        boolean capped = newValue == PlayerStatsManager.MIN_DAMAGE && value < 0;
        plugin.getMessages().send(sender, capped ? "strength-added-capped" : "strength-added", "value", value,
                "player", target.name(), "strength", newValue, "min", PlayerStatsManager.MIN_DAMAGE);
    }

    // ── set <player> <value> ────────────────────────────────────

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender, "/ma set <player> <value>");
            return;
        }

        Target target = resolveTarget(sender, args[1]);
        if (target == null) return;

        Integer value = parseInt(sender, args[2]);
        if (value == null) return;
        if (value < PlayerStatsManager.MIN_DAMAGE) {
            plugin.getMessages().send(sender, "strength-too-low", "min", PlayerStatsManager.MIN_DAMAGE);
            return;
        }

        plugin.getPlayerStatsManager().setDamage(target.uuid(), value);
        plugin.getMessages().send(sender, "strength-set", "player", target.name(), "strength", value);
    }

    // ── get <player> ────────────────────────────────────────────

    private void handleGet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/ma get <player>");
            return;
        }

        Target target = resolveTarget(sender, args[1]);
        if (target == null) return;

        int value = plugin.getPlayerStatsManager().getDamage(target.uuid());
        plugin.getMessages().send(sender, "strength-get", "player", target.name(), "strength", value);
    }

    // ── reset <player> ──────────────────────────────────────────

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender, "/ma reset <player>");
            return;
        }

        Target target = resolveTarget(sender, args[1]);
        if (target == null) return;

        plugin.getPlayerStatsManager().resetDamage(target.uuid());
        plugin.getMessages().send(sender, "strength-reset", "player", target.name(), "strength", PlayerStatsManager.DEFAULT_DAMAGE);
    }

    // ── Help & Utilities ────────────────────────────────────────

    /**
     * Online player by exact name, otherwise a known offline player (has an entry in players.yml).
     * Exact matching: Bukkit.getPlayer(name) would match "Ste" to "Steve".
     */
    private Target resolveTarget(CommandSender sender, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return new Target(online.getUniqueId(), online.getName());
        }

        var offline = Bukkit.getOfflinePlayerIfCached(name);
        if (offline != null && plugin.getPlayerStatsManager().isKnown(offline.getUniqueId())) {
            return new Target(offline.getUniqueId(), offline.getName() != null ? offline.getName() : name);
        }

        plugin.getMessages().send(sender, "player-never-joined", "player", name);
        return null;
    }

    private Integer parseInt(CommandSender sender, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            plugin.getMessages().send(sender, "invalid-number", "value", raw);
            return null;
        }
    }

    private void sendUsage(CommandSender sender) {
        plugin.getMessages().sendRaw(sender, "strength-help", "default", PlayerStatsManager.DEFAULT_DAMAGE);
    }

    private void usage(CommandSender sender, String usage) {
        plugin.getMessages().send(sender, "usage", "usage", usage);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String[] args) {
        if (!MetinCommand.has(sender, "simplemetin.strength")) return List.of();

        if (args.length == 1) {
            return Stream.of("add", "set", "get", "reset")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return null; // Bukkit returns online player names
        }

        return List.of();
    }

}
