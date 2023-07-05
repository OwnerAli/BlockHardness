package me.ogali.blockhardness.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;

public class CustomHardnessBlockBreakEvent extends BlockBreakEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return getHandlerList();
    }

    public CustomHardnessBlockBreakEvent(Block theBlock, Player player) {
        super(theBlock, player);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

}
