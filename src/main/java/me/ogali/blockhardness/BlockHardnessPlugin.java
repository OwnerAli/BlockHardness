package me.ogali.blockhardness;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.ogali.blockhardness.config.BlockHardnessConfig;
import me.ogali.blockhardness.listeners.PlayerJoinListener;
import me.ogali.blockhardness.listeners.PlayerQuitListener;
import me.ogali.blockhardness.listeners.PlayerSwingListener;
import me.ogali.blockhardness.player.BreakPlayerRegistry;
import me.ogali.blockhardness.player.domain.BreakPlayer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockHardnessPlugin extends JavaPlugin {
    public static BlockHardnessPlugin instance;
    private BreakPlayerRegistry breakPlayerRegistry;
    private BlockHardnessConfig blockHardnessConfig;

    @Override
    public void onEnable() {
        instance = this;
        blockHardnessConfig = new BlockHardnessConfig(this);
        breakPlayerRegistry = new BreakPlayerRegistry();
        registerListeners();
        startCrackRefreshTask();
    }

    @Override
    public void onDisable() {
    }

    public BreakPlayerRegistry getBreakPlayerRegistry() {
        return breakPlayerRegistry;
    }

    public BlockHardnessConfig getBlockHardnessConfig() {
        return blockHardnessConfig;
    }

    /**
     * Keeps the cracks on blocks with saved progress alive. The client forgets block damage it has not been told
     * about for a while, and decay has to be seen as it happens, not only when the player comes back.
     */
    private void startCrackRefreshTask() {
        int refreshTicks = blockHardnessConfig.getCrackRefreshTicks();
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (BreakPlayer breakPlayer : breakPlayerRegistry.getBreakPlayers()) {
                breakPlayer.refreshSavedCracks();
            }
        }, refreshTicks, refreshTicks);
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerJoinListener(this), this);
        pluginManager.registerEvents(new PlayerQuitListener(this), this);
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
