package me.simplemetin.listeners;

import me.simplemetin.SimpleMetin;
import me.simplemetin.managers.CrystalManager;
import me.simplemetin.models.CrystalData;
import me.simplemetin.models.CrystalType;
import me.simplemetin.testutil.TestSupport;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CrystalListenerTest {

    CrystalManager manager;
    CrystalListener listener;
    org.bukkit.configuration.file.YamlConfiguration config;
    EnderCrystal tracked;
    EnderCrystal untracked;
    Player player;

    @BeforeEach
    void setUp() {
        TestSupport.resetServer();
        manager = mock(CrystalManager.class);
        config = new org.bukkit.configuration.file.YamlConfiguration();
        listener = new CrystalListener(TestSupport.plugin(null, config), manager);
        tracked = TestSupport.entity(EnderCrystal.class);
        untracked = TestSupport.entity(EnderCrystal.class);
        player = TestSupport.player("Steve");
        var data = new CrystalData("c1", "x", null, CrystalType.RESPAWN, 10, null);
        when(manager.getCrystalByEntity(tracked.getUniqueId())).thenReturn(data);
    }

    EntityDamageByEntityEvent damageBy(org.bukkit.entity.Entity damager, org.bukkit.entity.Entity victim) {
        var event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(damager);
        return event;
    }

    @Test
    @DisplayName("player hit on a metin: vanilla damage cancelled, handleDamage called")
    void playerHitTracked() {
        var event = damageBy(player, tracked);
        listener.onCrystalDamage(event);
        verify(event).setCancelled(true);
        verify(manager).handleDamage(tracked, player);
    }

    @Test
    @DisplayName("normal (non-metin) EnderCrystal is left alone")
    void untrackedIgnored() {
        var event = damageBy(player, untracked);
        listener.onCrystalDamage(event);
        verify(event, never()).setCancelled(anyBoolean());
        verify(manager, never()).handleDamage(any(), any());
    }

    @Test
    @DisplayName("damage listener runs at HIGHEST priority")
    void priorityHighest() throws Exception {
        var handler = CrystalListener.class.getMethod("onCrystalDamage", EntityDamageEvent.class)
                .getAnnotation(EventHandler.class);
        assertEquals(EventPriority.HIGHEST, handler.priority());
    }

    @Test
    @DisplayName("explosion of a metin is cancelled and blocks are kept")
    void explosionCancelled() {
        var event = mock(EntityExplodeEvent.class);
        List<Block> blocks = new ArrayList<>(List.of(mock(Block.class)));
        when(event.getEntity()).thenReturn(tracked);
        when(event.blockList()).thenReturn(blocks);
        listener.onCrystalExplode(event);
        verify(event).setCancelled(true);
        assertTrue(blocks.isEmpty());
    }

    @Test
    @DisplayName("arrow shot by a player: cancelled, does not count as a hit")
    void arrowMustNotDestroyMetin() {
        var arrow = TestSupport.entity(Arrow.class);
        when(arrow.getShooter()).thenReturn(player);
        var event = damageBy(arrow, tracked);
        listener.onCrystalDamage(event);
        verify(event).setCancelled(true);
        verify(manager, never()).handleDamage(any(), any());
    }

    @Test
    @DisplayName("settings.projectile-hits: true -> player's arrow counts as that player's hit, arrow removed")
    void projectileHitsEnabled() {
        config.set("settings.projectile-hits", true);
        var arrow = TestSupport.entity(Arrow.class);
        when(arrow.getShooter()).thenReturn(player);
        var event = damageBy(arrow, tracked);

        listener.onCrystalDamage(event);

        verify(event).setCancelled(true);
        verify(manager).handleDamage(tracked, player);
        assertTrue(arrow.isDead(), "arrow does not bounce off to be picked up again");
    }

    @Test
    @DisplayName("settings.projectile-hits: true -> arrow from a non-player shooter (skeleton, dispenser) still ignored")
    void projectileFromNonPlayerIgnored() {
        config.set("settings.projectile-hits", true);
        var arrow = TestSupport.entity(Arrow.class);
        when(arrow.getShooter()).thenReturn(mock(org.bukkit.entity.Skeleton.class));
        var event = damageBy(arrow, tracked);

        listener.onCrystalDamage(event);

        verify(event).setCancelled(true);
        verify(manager, never()).handleDamage(any(), any());
    }

    @Test
    @DisplayName("TNT / other entities: cancelled")
    void tntMustNotDestroyMetin() {
        var tnt = TestSupport.entity(TNTPrimed.class);
        var event = damageBy(tnt, tracked);
        listener.onCrystalDamage(event);
        verify(event).setCancelled(true);
        verify(manager, never()).handleDamage(any(), any());
    }

    @Test
    @DisplayName("damage without a source entity (fire, block explosion, /damage generic): cancelled")
    void genericDamageCancelled() {
        var event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(tracked);
        listener.onCrystalDamage(event);
        verify(event).setCancelled(true);
        verify(manager, never()).handleDamage(any(), any());
    }

    @Test
    @DisplayName("damage to a normal EnderCrystal without a source entity is left alone")
    void genericDamageUntrackedIgnored() {
        var event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(untracked);
        listener.onCrystalDamage(event);
        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("chunk / entities / world load events are delegated")
    void lifecycleEventsDelegated() {
        var chunk = mock(org.bukkit.Chunk.class);
        var load = mock(org.bukkit.event.world.ChunkLoadEvent.class);
        when(load.getChunk()).thenReturn(chunk);
        listener.onChunkLoad(load);
        verify(manager).handleChunkLoad(chunk);

        var unload = mock(org.bukkit.event.world.ChunkUnloadEvent.class);
        when(unload.getChunk()).thenReturn(chunk);
        listener.onChunkUnload(unload);
        verify(manager).handleChunkUnload(chunk);

        var entitiesLoad = mock(org.bukkit.event.world.EntitiesLoadEvent.class);
        List<org.bukkit.entity.Entity> loaded = List.of(untracked);
        when(entitiesLoad.getEntities()).thenReturn(loaded);
        listener.onEntitiesLoad(entitiesLoad);
        verify(manager).removeOrphans(loaded);
    }
}
