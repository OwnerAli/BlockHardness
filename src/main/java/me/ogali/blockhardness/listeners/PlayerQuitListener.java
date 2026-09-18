package me.ogali.blockhardness.listeners;

import me.ogali.blockhardness.BlockHardnessPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final BlockHardnessPlugin main;

    public PlayerQuitListener(BlockHardnessPlugin main) {
        this.main = main;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        main.getBreakPlayerRegistry().removeBreakPlayer(event.getPlayer());
    }

}
