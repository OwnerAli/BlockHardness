package me.ogali.blockhardness.blocks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import lombok.Getter;
import me.ogali.blockhardness.events.CustomHardnessBlockBreakEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Getter
public class BlockWrapper implements Mineable {

    private final Block block;
    private final boolean dropVanillaBlock;
    private final double timeBetweenEachIncrement;
    private final double secondsBlockShouldTakeToBreak;
    private int currentBlockStage;
    private long lastDamageTime;

    public BlockWrapper(Block block, int currentBlockStage, boolean dropVanillaBlock, double timeBetweenEachIncrement, double secondsBlockShouldTakeToBreak) {
        this.block = block;
        this.currentBlockStage = currentBlockStage;
        this.dropVanillaBlock = dropVanillaBlock;
        this.timeBetweenEachIncrement = timeBetweenEachIncrement;
        this.secondsBlockShouldTakeToBreak = secondsBlockShouldTakeToBreak;
    }

    public BlockWrapper(Block block, boolean dropVanillaBlock, double secondsBlockShouldTakeToBreak) {
        this.block = block;
        this.dropVanillaBlock = dropVanillaBlock;
        this.timeBetweenEachIncrement = secondsBlockShouldTakeToBreak * 1000 / 10;
        this.secondsBlockShouldTakeToBreak = secondsBlockShouldTakeToBreak;
    }

    @Override
    public void mine(Player player) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastDamage = currentTime - lastDamageTime;

        if (timeSinceLastDamage < timeBetweenEachIncrement) return;

        lastDamageTime = currentTime;
        if (currentBlockStage + 1 == 11) {
            breakBlock(player);
            return;
        }

        lastDamageTime = System.currentTimeMillis();
        if (secondsBlockShouldTakeToBreak == 0.1) {
            breakBlock(player);
            return;
        }
        sendBreakAnimation(player, currentBlockStage++);
    }

    private void breakBlock(Player player) {
        resetBreakAnimation(player);

        CustomHardnessBlockBreakEvent event = new CustomHardnessBlockBreakEvent(block, player, this);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            stopMining();
            return;
        }
        player.playSound(player, block.getBlockData().getSoundGroup().getBreakSound(), 1, 1);

        if (dropVanillaBlock) {
            block.breakNaturally(player.getItemInUse());
            return;
        }
        block.setType(Material.AIR);
    }

    private void sendBreakAnimation(Player player, int stage) {
        if (block == null) return;
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer blockBreakPacket = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        blockBreakPacket.getIntegers().write(0, 0);
        blockBreakPacket.getBlockPositionModifier().write(0, new BlockPosition(block.getX(), block.getY(),
                block.getZ()));
        blockBreakPacket.getIntegers().write(1, stage);

        try {
            protocolManager.sendServerPacket(player, blockBreakPacket);
        } catch (Exception ignored) {
            Bukkit.getConsoleSender().sendMessage("ERROR IN PACKET!");
        }
    }

    private void resetBreakAnimation(Player player) {
        sendBreakAnimation(player, -1);
    }

    private void stopMining() {
        currentBlockStage = 0;
    }

}
