package me.ogali.blockhardness.player.domain;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import me.ogali.blockhardness.events.CustomHardnessBlockBreakEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BreakPlayer {

    private final Player player;
    private Block currentBlockBeingBroken;
    private int currentBlockStage;
    private long lastDamageTime;
    private double timeBetweenEachIncrement;

    public BreakPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getCurrentBlockBeingBroken() {
        return currentBlockBeingBroken;
    }

    public void startMining(Block block, double secondsBlockShouldTakeToBreak, boolean dropVanillaBlock) {
        if (!block.equals(currentBlockBeingBroken)) {
            startMiningNewBlock(block, secondsBlockShouldTakeToBreak);
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastDamage = currentTime - lastDamageTime;

        if (timeSinceLastDamage < timeBetweenEachIncrement) return;

        lastDamageTime = currentTime;
        if (currentBlockStage + 1 == 11) {
            breakBlock(dropVanillaBlock);
            return;
        }

        lastDamageTime = System.currentTimeMillis();
        if (secondsBlockShouldTakeToBreak == 0.1) {
            breakBlock(dropVanillaBlock);
            return;
        }
        sendBreakAnimation(currentBlockStage++);
    }

    public void stopMiningAndResetAnimation() {
        resetBreakAnimation();
        stopMining();
    }

    private void startMiningNewBlock(Block block, double secondsBlockShouldTakeToBreak) {
        calculateTimeBetweenEachIncrement(secondsBlockShouldTakeToBreak);
        currentBlockStage = 0;
        currentBlockBeingBroken = block;
        lastDamageTime = System.currentTimeMillis();
        sendBreakAnimation(currentBlockStage++);
    }

    private void stopMining() {
        currentBlockStage = 0;
        currentBlockBeingBroken = null;
    }

    private void breakBlock(boolean dropVanillaBlock) {
        resetBreakAnimation();

        CustomHardnessBlockBreakEvent event = new CustomHardnessBlockBreakEvent(currentBlockBeingBroken, player);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            Bukkit.getConsoleSender().sendMessage("CANCELLED");
            return;
        }
        player.playSound(player, currentBlockBeingBroken.getBlockData().getSoundGroup().getBreakSound(), 1, 1);
        currentBlockBeingBroken.setType(Material.AIR);

        if (dropVanillaBlock) {
            currentBlockBeingBroken.breakNaturally(player.getItemInUse());
        }

        stopMining();
    }

    private void calculateTimeBetweenEachIncrement(double secondsBlockShouldTakeToBreak) {
        timeBetweenEachIncrement = secondsBlockShouldTakeToBreak * 1000 / 10;
    }

    private void sendBreakAnimation(int stage) {
        if (currentBlockBeingBroken == null) return;
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer blockBreakPacket = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        blockBreakPacket.getIntegers().write(0, 0);
        blockBreakPacket.getBlockPositionModifier().write(0, new BlockPosition(currentBlockBeingBroken.getX(), currentBlockBeingBroken.getY(),
                currentBlockBeingBroken.getZ()));
        blockBreakPacket.getIntegers().write(1, stage);

        try {
            protocolManager.sendServerPacket(player, blockBreakPacket);
        } catch (Exception ignored) {
            Bukkit.getConsoleSender().sendMessage("ERROR IN PACKET!");
        }
    }

    private void resetBreakAnimation() {
        sendBreakAnimation(-1);
    }

}
