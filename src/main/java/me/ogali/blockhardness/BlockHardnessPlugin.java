package me.ogali.blockhardness;

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

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
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
