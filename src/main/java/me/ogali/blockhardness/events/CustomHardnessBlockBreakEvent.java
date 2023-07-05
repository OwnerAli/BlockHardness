package me.ogali.blockhardness.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CustomHardnessBlockBreakEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Block block;
    private final Player player;

    public static HandlerList getHandlerList() {
        return getHandlerList();
    }

    public CustomHardnessBlockBreakEvent(Block theBlock, Player player) {
        this.block = theBlock;
        this.player = player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public Block getBlock() {
        return block;
    }

    public Player getPlayer() {
        return player;
    }

}
