package me.ogali.blockhardness;

import me.ogali.blockhardness.listeners.BlockDigPacketListener;
import me.ogali.blockhardness.listeners.PlayerJoinListener;
import me.ogali.blockhardness.player.BreakPlayerRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockHardnessPlugin extends JavaPlugin {

    public static BlockHardnessPlugin instance;
    public BreakPlayerRegistry breakPlayerRegistry;

    @Override
    public void onEnable() {
        instance = this;
        breakPlayerRegistry = new BreakPlayerRegistry();
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockDigPacketListener(breakPlayerRegistry), this);
    }

    @Override
    public void onDisable() {
    }

    public BreakPlayerRegistry getBreakPlayerRegistry() {
        return breakPlayerRegistry;
    }

    public static BlockHardnessPlugin getInstance() {
        return instance;
    }

}
