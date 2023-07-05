package me.ogali.blockhardness.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
import me.ogali.blockhardness.BlockHardnessPlugin;
import me.ogali.blockhardness.player.domain.BreakPlayer;
import me.ogali.blockhardness.runnables.BlockBreakRunnable;
import org.bukkit.Bukkit;
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
        main.breakPlayerRegistry.addBreakPlayer(new BreakPlayer(event.getPlayer()));
        event.getPlayer().removePotionEffect(PotionEffectType.SLOW_DIGGING);
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, Integer.MAX_VALUE, -1, false,
                false, false));
    }

}
