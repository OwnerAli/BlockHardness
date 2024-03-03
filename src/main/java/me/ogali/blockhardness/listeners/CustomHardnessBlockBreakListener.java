package me.ogali.blockhardness.listeners;

import me.ogali.blockhardness.events.CustomHardnessBlockBreakEvent;
import me.ogali.blockhardness.player.BreakPlayerRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CustomHardnessBlockBreakListener implements Listener {

    private final BreakPlayerRegistry breakPlayerRegistry;

    public CustomHardnessBlockBreakListener(BreakPlayerRegistry breakPlayerRegistry) {
        this.breakPlayerRegistry = breakPlayerRegistry;
    }

    @EventHandler
    public void onBreak(CustomHardnessBlockBreakEvent event) {
        breakPlayerRegistry.getBreakPlayer(event.getPlayer())
                .ifPresent(breakPlayer -> breakPlayer.getBlocksBeingMinedList().remove(event.getBlockWrapper()));
    }

}