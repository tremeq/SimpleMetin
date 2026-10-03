package me.simplemetin.listeners;

import me.simplemetin.SimpleMetin;
import me.simplemetin.managers.CrystalManager;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class CrystalListener implements Listener {
    private final SimpleMetin plugin;
    private final CrystalManager crystalManager;

    public CrystalListener(SimpleMetin plugin, CrystalManager crystalManager) {
        this.plugin = plugin;
        this.crystalManager = crystalManager;
    }

    /**
     * Handles every damage source (EntityDamageByEntityEvent is a subclass), so arrows, explosions, other
     * crystals or /damage can no longer kill the vanilla entity. A direct player hit counts as a metin hit;
     * projectiles shot by a player count only with settings.projectile-hits: true.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrystalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;

        var data = crystalManager.getCrystalByEntity(crystal.getUniqueId());
        if (data == null) return;

        event.setCancelled(true);

        if (!(event instanceof EntityDamageByEntityEvent byEntity)) return;

        if (byEntity.getDamager() instanceof Player player) {
            crystalManager.handleDamage(crystal, player);
        } else if (byEntity.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter
                && plugin.getConfig().getBoolean("settings.projectile-hits", false)) {
            crystalManager.handleDamage(crystal, shooter);
            // The hit is cancelled, so the arrow would otherwise bounce off and could be picked up again
            projectile.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrystalExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) return;

        var data = crystalManager.getCrystalByEntity(crystal.getUniqueId());
        if (data == null) return;

        event.blockList().clear();
        event.setCancelled(true);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        crystalManager.handleChunkLoad(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        crystalManager.handleChunkUnload(event.getChunk());
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        crystalManager.removeOrphans(event.getEntities());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.getDataHandler().retryPending();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        crystalManager.clearCooldown(event.getPlayer().getUniqueId());
    }
}
