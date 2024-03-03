package me.ogali.blockhardness;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import lombok.Getter;
import me.ogali.blockhardness.listeners.CustomHardnessBlockBreakListener;
import me.ogali.blockhardness.listeners.PlayerJoinListener;
import me.ogali.blockhardness.listeners.PlayerSwingListener;
import me.ogali.blockhardness.player.BreakPlayerRegistry;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class BlockHardnessPlugin extends JavaPlugin {

    public static BlockHardnessPlugin instance;
    private BreakPlayerRegistry breakPlayerRegistry;

    @Override
    public void onEnable() {
        instance = this;
        breakPlayerRegistry = new BreakPlayerRegistry();
        registerListeners();
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new CustomHardnessBlockBreakListener(breakPlayerRegistry), this);
        pluginManager.registerEvents(new PlayerJoinListener(this), this);
        pluginManager.registerEvents(new PlayerSwingListener(), this);
        registryBreakResetPacketListener();
    }

    private void registryBreakResetPacketListener() {
        ProtocolLibrary.getProtocolManager()
                .addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL,
                        PacketType.Play.Client.BLOCK_DIG) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        breakPlayerRegistry.getBreakPlayer(event.getPlayer())
                                .ifPresent(breakPlayer -> {
                                    if (!event.getPlayer().equals(breakPlayer.getPlayer())) return;
                                    if (breakPlayer.getBlocksBeingMinedList().isEmpty()) return;

                                    PacketContainer packet = event.getPacket();
                                    String value = packet.getModifier().readSafely(2).toString();

                                    if (!value.equalsIgnoreCase("ABORT_DESTROY_BLOCK")) return;
                                });
                    }
                });
    }

}
