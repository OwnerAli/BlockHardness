package me.ogali.blockhardness;

import me.ogali.blockhardness.api.BlockHardnessApi;
import me.ogali.blockhardness.listeners.BlockDigPacketListener;
import me.ogali.blockhardness.listeners.PlayerJoinListener;
import me.ogali.blockhardness.player.BreakPlayerRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockHardnessPlugin extends JavaPlugin {

    public static BlockHardnessPlugin instance;
    private BreakPlayerRegistry breakPlayerRegistry;
    private BlockHardnessApi blockHardnessApi;

    @Override
    public void onEnable() {
        instance = this;
        breakPlayerRegistry = new BreakPlayerRegistry();
        blockHardnessApi = new BlockHardnessApi(breakPlayerRegistry);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockDigPacketListener(breakPlayerRegistry), this);
    }

    @Override
    public void onDisable() {
    }

    public void test() {
        Bukkit.getConsoleSender().sendMessage("TEST");
    }

    public BreakPlayerRegistry getBreakPlayerRegistry() {
        return breakPlayerRegistry;
    }

    public BlockHardnessApi getBlockHardnessApi() {
        return blockHardnessApi;
    }

    public static BlockHardnessPlugin getInstance() {
        return instance;
    }

}
