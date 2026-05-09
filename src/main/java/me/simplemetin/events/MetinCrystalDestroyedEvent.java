package me.simplemetin.events;

import me.simplemetin.models.CrystalData;
import me.simplemetin.models.DropItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MetinCrystalDestroyedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CrystalData crystal;
    private final Player killer;
    private final List<DropItem> drops;

    public MetinCrystalDestroyedEvent(CrystalData crystal, Player killer, List<DropItem> drops) {
        this.crystal = crystal;
        this.killer = killer;
        this.drops = drops;
    }

    public CrystalData getCrystal() {
        return crystal;
    }

    public Player getKiller() {
        return killer;
    }

    public List<DropItem> getDrops() {
        return drops;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
