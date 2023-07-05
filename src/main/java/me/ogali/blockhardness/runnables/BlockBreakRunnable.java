package me.ogali.blockhardness.runnables;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import me.ogali.blockhardness.BlockHardnessPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BlockBreakRunnable extends BukkitRunnable {

    private final Player player;

    public BlockBreakRunnable(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
    }

}
