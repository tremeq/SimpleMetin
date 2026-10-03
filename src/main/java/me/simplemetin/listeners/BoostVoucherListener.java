package me.simplemetin.listeners;

import me.simplemetin.SimpleMetin;
import me.simplemetin.managers.BoostManager;
import me.simplemetin.utils.TextUtils;
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
            plugin.getMessages().send(player, "boost-already-active");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Get voucher data
        double multiplier = VoucherUtils.getVoucherMultiplier(plugin, item);
        long duration = VoucherUtils.getVoucherDuration(plugin, item);

        // Activate boost
        plugin.getBoostManager().activatePersonalBoost(player, multiplier, duration);

        // Send message
        plugin.getMessages().send(player, "boost-activated",
                "multiplier", BoostManager.formatMultiplier(multiplier), "duration", TextUtils.formatDuration(duration));

        // Play sound
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        // Remove one voucher
        item.setAmount(item.getAmount() - 1);
    }


}
