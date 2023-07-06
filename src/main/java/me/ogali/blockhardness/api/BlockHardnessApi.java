package me.ogali.blockhardness.api;

import me.ogali.blockhardness.player.BreakPlayerRegistry;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockHardnessApi {

    private final BreakPlayerRegistry breakPlayerRegistry;

    public BlockHardnessApi(BreakPlayerRegistry breakPlayerRegistry) {
        this.breakPlayerRegistry = breakPlayerRegistry;
    }

    public void startMining(Player player, Block block) {
        breakPlayerRegistry.getBreakPlayer(player)
                .ifPresent(breakPlayer -> breakPlayer.startMining(block));
    }

    public void stopMining(Player player, Block block) {
        breakPlayerRegistry.getBreakPlayer(player)
                .ifPresent(breakPlayer -> breakPlayer.startMining(block));
    }

}
