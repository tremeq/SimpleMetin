package me.simplemetin.utils;

import me.simplemetin.models.DropCommand;
import me.simplemetin.models.DropItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Random;

public class DropUtils {
    private static final Random RANDOM = new Random();

    public static int processDrops(List<DropItem> drops, Location location, Player player, Plugin plugin) {
        if (drops == null || drops.isEmpty()) return 0;

        var world = location.getWorld();
        if (world == null) return 0;

        boolean hadFullInventory = false;
        int itemsReceived = 0;

        for (var drop : drops) {
            if (RANDOM.nextDouble() * 100 < drop.chance()) {
                var item = drop.createItemStack();
                itemsReceived += item.getAmount();

                var leftover = player.getInventory().addItem(item);

                if (!leftover.isEmpty()) {
                    hadFullInventory = true;
                    for (var leftoverItem : leftover.values()) {
                        world.dropItemNaturally(location, leftoverItem);
                    }
                }
            }
        }

        if (hadFullInventory) {
            String message = plugin.getConfig().getString("messages.inventory-full", "");
            if (message != null && !message.isEmpty()) {
                player.sendMessage(colorize(message));
            }
        }

        return itemsReceived;
    }

    private static String colorize(String text) {
        return text.replace('&', '§');
    }

    public static long executeCommands(List<DropCommand> commands, Player player) {
        if (commands == null || commands.isEmpty()) return 0;

        long moneyEarned = 0;

        for (var cmd : commands) {
            if (RANDOM.nextDouble() * 100 < cmd.chance()) {
                var command = cmd.command().replace("%player%", player.getName());

                // Try to extract money from eco give command
                if (command.toLowerCase().contains("eco give")) {
                    try {
                        String[] parts = command.split("\\s+");
                        for (int i = 0; i < parts.length; i++) {
                            if (parts[i].equalsIgnoreCase("give") && i + 2 < parts.length) {
                                moneyEarned += Long.parseLong(parts[i + 2]);
                                break;
                            }
                        }
                    } catch (NumberFormatException ignored) {
                        // Not a valid number, skip
                    }
                }

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }

        return moneyEarned;
    }
}
