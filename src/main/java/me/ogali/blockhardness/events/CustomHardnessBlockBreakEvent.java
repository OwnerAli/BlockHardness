package me.ogali.blockhardness.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;

public class CustomHardnessBlockBreakEvent extends BlockBreakEvent {

    private static final HandlerList handlersList = new HandlerList();

    public CustomHardnessBlockBreakEvent(Block theBlock, Player player) {
        super(theBlock, player);
    }

    public static HandlerList getHandlerList() {
        return getHandlerList();
    }

    @Override
    public HandlerList getHandlers() {
        return handlersList;
    }

}
