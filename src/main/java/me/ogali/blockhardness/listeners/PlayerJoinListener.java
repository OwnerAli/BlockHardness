package me.ogali.blockhardness.listeners;

import me.ogali.blockhardness.BlockHardnessPlugin;
import me.ogali.blockhardness.player.domain.BreakPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final BlockHardnessPlugin main;

    public PlayerJoinListener(BlockHardnessPlugin main) {
        this.main = main;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        main.getBreakPlayerRegistry().addBreakPlayer(new BreakPlayer(event.getPlayer()));
    }

}
