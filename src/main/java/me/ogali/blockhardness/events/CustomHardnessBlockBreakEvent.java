package me.ogali.blockhardness.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CustomHardnessBlockBreakEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final Block block;

    public CustomHardnessBlockBreakEvent(Block theBlock, Player player) {
        this.player = player;
        this.block = theBlock;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
