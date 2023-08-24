package me.ogali.blockhardness.listeners;

import me.ogali.blockhardness.events.BlockHardnessBlockInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;

public class PlayerSwingListener implements Listener {

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (!event.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) return;

        Block playerTargetedBlock = event.getPlayer().getTargetBlockExact(5);

        if (playerTargetedBlock == null) return;
        if (playerTargetedBlock.getType() == Material.AIR) return;
        Bukkit.getPluginManager().callEvent(new BlockHardnessBlockInteractEvent(playerTargetedBlock, event.getPlayer()));
    }

}
