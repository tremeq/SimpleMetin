package me.simplemetin.listeners;

import me.simplemetin.SimpleMetin;
import me.simplemetin.utils.VoucherUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class BoostVoucherListener implements Listener {
    private final SimpleMetin plugin;

    public BoostVoucherListener(SimpleMetin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVoucherUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        var item = event.getItem();
        if (item == null) return;

        if (!VoucherUtils.isBoostVoucher(plugin, item)) return;

        event.setCancelled(true);

        var player = event.getPlayer();

        // Check if already has boost
        if (plugin.getBoostManager().getPersonalBoost(player.getUniqueId()) != null) {
            String message = plugin.getConfig().getString("messages.boost-already-active", "&cYou already have an active boost!");
            if (message != null && !message.isEmpty()) {
                player.sendMessage(colorize(message));
            }
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Get voucher data
        double multiplier = VoucherUtils.getVoucherMultiplier(plugin, item);
        long duration = VoucherUtils.getVoucherDuration(plugin, item);

        // Activate boost
        plugin.getBoostManager().activatePersonalBoost(player, multiplier, duration);

        // Send message
        String message = plugin.getConfig().getString("messages.boost-activated", "&a&lBOOST ACTIVATED!");
        if (message != null && !message.isEmpty()) {
            message = message
                    .replace("%multiplier%", String.format("%.1f", multiplier))
                    .replace("%duration%", formatDuration(duration));
            player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix", "") + message));
        }

        // Play sound
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        // Remove one voucher
        item.setAmount(item.getAmount() - 1);
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm", minutes);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private String colorize(String text) {
        return text.replace('&', '§');
    }
}
