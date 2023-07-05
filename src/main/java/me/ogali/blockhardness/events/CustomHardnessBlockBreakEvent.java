package me.ogali.blockhardness.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;

public class CustomHardnessBlockBreakEvent extends BlockBreakEvent {

    private static final HandlerList handlers = new HandlerList();

    public CustomHardnessBlockBreakEvent(Block theBlock, Player player) {
        super(theBlock, player);
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
