package me.simplemetin.listeners;

import me.simplemetin.SimpleMetin;
import me.simplemetin.managers.CrystalManager;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class CrystalListener implements Listener {
    private final SimpleMetin plugin;
    private final CrystalManager crystalManager;

    public CrystalListener(SimpleMetin plugin, CrystalManager crystalManager) {
        this.plugin = plugin;
        this.crystalManager = crystalManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        var data = crystalManager.getCrystalByEntity(crystal.getUniqueId());
        if (data == null) return;

        event.setCancelled(true);
        crystalManager.handleDamage(crystal, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrystalExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;

        var data = crystalManager.getCrystalByEntity(crystal.getUniqueId());
        if (data == null) return;

        event.blockList().clear();
        event.setCancelled(true);
    }
}
