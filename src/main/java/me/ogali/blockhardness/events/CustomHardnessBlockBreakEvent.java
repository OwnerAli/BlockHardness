package me.ogali.blockhardness.events;

import lombok.Getter;
import me.ogali.blockhardness.blocks.BlockWrapper;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class CustomHardnessBlockBreakEvent extends BlockBreakEvent {

    private static final HandlerList handlers = new HandlerList();

    private final BlockWrapper blockWrapper;

    public CustomHardnessBlockBreakEvent(Block theBlock, Player player, BlockWrapper blockWrapper) {
        super(theBlock, player);
        this.blockWrapper = blockWrapper;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }

}
