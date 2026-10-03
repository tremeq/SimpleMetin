package me.simplemetin.events;

import me.simplemetin.models.CrystalData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Fired after a crystal was destroyed and its death rewards were given. */
public class MetinCrystalDestroyedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CrystalData crystal;
    private final Player killer;
    private final List<ItemStack> drops;
    private final List<String> commands;

    public MetinCrystalDestroyedEvent(CrystalData crystal, Player killer, List<ItemStack> drops, List<String> commands) {
        this.crystal = crystal;
        this.killer = killer;
        this.drops = List.copyOf(drops);
        this.commands = List.copyOf(commands);
    }

    public CrystalData getCrystal() {
        return crystal;
    }

    public Player getKiller() {
        return killer;
    }

    /** Items the killer actually received from the death drops/pools (copies). */
    public List<ItemStack> getDrops() {
        return drops.stream().map(ItemStack::clone).toList();
    }

    /** Commands that were run for the killer (with %player% already replaced). */
    public List<String> getCommands() {
        return commands;
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
