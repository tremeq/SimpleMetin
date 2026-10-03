package me.simplemetin.utils;

import me.simplemetin.SimpleMetin;
import me.simplemetin.models.DropCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class DropUtils {
    private static final Random RANDOM = new Random();

    /**
     * Gives already rolled items to the player; whatever does not fit is dropped at the location.
     * Returns the number of items given (including dropped ones).
     */
    public static int giveItems(List<ItemStack> items, Location location, Player player, SimpleMetin plugin) {
        if (items == null || items.isEmpty()) return 0;

        var world = location.getWorld();
        boolean hadFullInventory = false;
        int itemsReceived = 0;

        for (var item : items) {
            itemsReceived += item.getAmount();
            var leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                hadFullInventory = true;
                if (world != null) {
                    for (var leftoverItem : leftover.values()) {
                        world.dropItemNaturally(location, leftoverItem);
                    }
                }
            }
        }

        if (hadFullInventory) {
            plugin.getMessages().send(player, "inventory-full");
        }
        return itemsReceived;
    }

    /** Rolls each command's chance and runs the successful ones; returns money from successful "eco give". */
    public static long executeCommands(List<DropCommand> commands, Player player) {
        if (commands == null || commands.isEmpty()) return 0;

        long moneyEarned = 0;
        for (var cmd : commands) {
            if (RANDOM.nextDouble() * 100 < cmd.chance()) {
                moneyEarned += runCommand(cmd.command(), player);
            }
        }
        return moneyEarned;
    }

    /** Runs already rolled commands; returns money from successful "eco give". */
    public static long runCommands(List<String> commands, Player player) {
        long moneyEarned = 0;
        for (var command : commands) {
            moneyEarned += runCommand(command, player);
        }
        return moneyEarned;
    }

    private static long runCommand(String rawCommand, Player player) {
        var command = rawCommand.replace("%player%", player.getName());
        // false = unknown command (e.g. no economy plugin) or it failed: nothing was paid out
        boolean executed = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        return executed ? parseEcoGiveAmount(command) : 0;
    }

    /** Amount from "eco give &lt;player&gt; &lt;amount&gt;", 0 for other commands or non-integer amounts. */
    private static long parseEcoGiveAmount(String command) {
        if (!command.toLowerCase().contains("eco give")) return 0;
        String[] parts = command.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("give") && i + 2 < parts.length) {
                try {
                    return Long.parseLong(parts[i + 2]);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
