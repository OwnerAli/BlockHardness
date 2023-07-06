package me.ogali.blockhardness.listeners;

import me.ogali.blockhardness.BlockHardnessPlugin;
import me.ogali.blockhardness.player.domain.BreakPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerJoinListener implements Listener {

    private final BlockHardnessPlugin main;

    public PlayerJoinListener(BlockHardnessPlugin main) {
        this.main = main;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        main.getBreakPlayerRegistry().addBreakPlayer(new BreakPlayer(event.getPlayer(), main));
        event.getPlayer().removePotionEffect(PotionEffectType.SLOW_DIGGING);
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, Integer.MAX_VALUE, -1, false,
                false, false));
    }

}
