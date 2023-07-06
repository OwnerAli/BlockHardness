package me.ogali.blockhardness.api;

import me.ogali.blockhardness.player.BreakPlayerRegistry;
import me.ogali.blockhardness.player.domain.BreakPlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BlockHardnessApi {

    private static final BlockHardnessApi instance = new BlockHardnessApi(new BreakPlayerRegistry());
    private final BreakPlayerRegistry breakPlayerRegistry;

    public BlockHardnessApi(BreakPlayerRegistry breakPlayerRegistry) {
        this.breakPlayerRegistry = breakPlayerRegistry;
    }

    public void createBreakPlayer(Player player, Plugin plugin) {
        breakPlayerRegistry.addBreakPlayer(new BreakPlayer(player, plugin));
    }

    public void startMining(Player player, Block block) {
        breakPlayerRegistry.getBreakPlayer(player)
                .ifPresent(breakPlayer -> breakPlayer.startMining(block));
    }

    public void stopMining(Player player, Block block) {
        breakPlayerRegistry.getBreakPlayer(player)
                .ifPresent(breakPlayer -> breakPlayer.startMining(block));
    }

    public static BlockHardnessApi getInstance() {
        return instance;
    }

    public BreakPlayerRegistry getBreakPlayerRegistry() {
        return breakPlayerRegistry;
    }

}
