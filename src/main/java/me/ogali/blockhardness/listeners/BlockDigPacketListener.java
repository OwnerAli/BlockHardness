package me.ogali.blockhardness.listeners;

import me.ogali.blockhardness.player.BreakPlayerRegistry;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

public class BlockDigPacketListener implements Listener {

    private final BreakPlayerRegistry breakPlayerRegistry;

    public BlockDigPacketListener(BreakPlayerRegistry breakPlayerRegistry) {
        this.breakPlayerRegistry = breakPlayerRegistry;
    }

    @EventHandler
    public void onBlockInteract(PlayerAnimationEvent event) {
        if (!event.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) return;
        Block playerTargetedBlock = event.getPlayer().getTargetBlockExact(5);

        if (playerTargetedBlock == null) return;
        if (playerTargetedBlock.getType() == Material.STONE) return;

        breakPlayerRegistry.getBreakPlayer(event.getPlayer())
                .ifPresent(breakPlayer -> breakPlayer.startMining(playerTargetedBlock));
    }

}
