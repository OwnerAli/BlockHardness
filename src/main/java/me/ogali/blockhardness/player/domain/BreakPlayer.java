package me.ogali.blockhardness.player.domain;

import lombok.Getter;
import me.ogali.blockhardness.blocks.BlockWrapper;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class BreakPlayer {

    private final Player player;
    private final List<BlockWrapper> blocksBeingMinedList = new ArrayList<>();

    public BreakPlayer(Player player) {
        this.player = player;
    }

    public void startMining(Block block, boolean dropVanillaBlock, double secondsBlockShouldTakeToBreak) {
        if (!isBlockBeingBroken(block)) {
            startMiningNewBlock(block, dropVanillaBlock, secondsBlockShouldTakeToBreak);
            return;
        }
        getBlockBeingBroken(block).ifPresent(blockWrapper -> blockWrapper.mine(player));
    }

    private void startMiningNewBlock(Block block, boolean dropVanillaBlock, double secondsBlockShouldTakeToBreak) {
        BlockWrapper blockWrapper = new BlockWrapper(block, dropVanillaBlock, secondsBlockShouldTakeToBreak);
        blocksBeingMinedList.add(blockWrapper);
        blockWrapper.mine(player);
    }

    private boolean isBlockBeingBroken(Block block) {
        return blocksBeingMinedList.stream().anyMatch(blockWrapper -> blockWrapper.getBlock().equals(block));
    }

    private Optional<BlockWrapper> getBlockBeingBroken(Block block) {
        return blocksBeingMinedList.stream()
                .filter(blockWrapper -> blockWrapper.getBlock().equals(block))
                .findFirst();
    }

}
