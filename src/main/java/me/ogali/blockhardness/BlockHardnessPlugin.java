package me.ogali.blockhardness;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.ogali.blockhardness.listeners.PlayerJoinListener;
import me.ogali.blockhardness.player.BreakPlayerRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockHardnessPlugin extends JavaPlugin {

    public static BlockHardnessPlugin instance;
    private BreakPlayerRegistry breakPlayerRegistry;

    @Override
    public void onEnable() {
        instance = this;
        breakPlayerRegistry = new BreakPlayerRegistry();
        registerListeners();
    }

    @Override
    public void onDisable() {
    }

    public BreakPlayerRegistry getBreakPlayerRegistry() {
        return breakPlayerRegistry;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
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
                                    if (breakPlayer.getCurrentBlockBeingBroken() == null) return;

                                    PacketContainer packet = event.getPacket();
                                    String value = packet.getModifier().readSafely(2).toString();

                                    if (!value.equalsIgnoreCase("ABORT_DESTROY_BLOCK")) return;
                                    breakPlayer.stopMiningAndResetAnimation();
                                });
                    }
                });
    }

}
