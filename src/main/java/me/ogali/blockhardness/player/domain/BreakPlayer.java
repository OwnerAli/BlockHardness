package me.ogali.blockhardness.player.domain;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import me.ogali.blockhardness.events.CustomHardnessBlockBreakEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BreakPlayer {

    private final Player player;
    private Block currentBlockBeingBroken;
    private int currentBlockStage;
    private long lastDamageTime;
    private double timeBetweenEachIncrement;

    public BreakPlayer(Player player, Plugin plugin) {
        this.player = player;
        playerStopMiningPacketListener(plugin);
    }

    public Player getPlayer() {
        return player;
    }

    public void startMining(Block block, double secondsBlockShouldTakeToBreak) {
        if (!block.equals(currentBlockBeingBroken)) {
            startMiningNewBlock(block, secondsBlockShouldTakeToBreak);
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastDamage = currentTime - lastDamageTime;

        if (timeSinceLastDamage < timeBetweenEachIncrement) return;

        lastDamageTime = currentTime;
        if (currentBlockStage + 1 == 11) {
            breakBlock();
            return;
        }

        lastDamageTime = System.currentTimeMillis();
        sendBreakAnimation(currentBlockStage++);
    }

    private void startMiningNewBlock(Block block, double secondsBlockShouldTakeToBreak) {
        calculateTimeBetweenEachIncrement(secondsBlockShouldTakeToBreak);
        currentBlockStage = 0;
        currentBlockBeingBroken = block;
        lastDamageTime = System.currentTimeMillis();
        sendBreakAnimation(currentBlockStage++);
    }

    private void stopMiningAndResetAnimation() {
        resetBreakAnimation();
        stopMining();
    }

    private void stopMining() {
        currentBlockStage = 0;
        currentBlockBeingBroken = null;
    }

    private void breakBlock() {
        resetBreakAnimation();
        Bukkit.getPluginManager().callEvent(new CustomHardnessBlockBreakEvent(currentBlockBeingBroken, player));
        player.playSound(player, currentBlockBeingBroken.getBlockData().getSoundGroup().getBreakSound(), 1, 1);
        stopMining();
    }

    private void calculateTimeBetweenEachIncrement(double secondsBlockShouldTakeToBreak) {
        timeBetweenEachIncrement = secondsBlockShouldTakeToBreak * 1000 / 10;
    }

    private void playerStopMiningPacketListener(Plugin plugin) {
        ProtocolLibrary.getProtocolManager()
                .addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
                        PacketType.Play.Client.BLOCK_DIG) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        if (!event.getPlayer().equals(player)) return;
                        if (currentBlockBeingBroken == null) return;

                        PacketContainer packet = event.getPacket();
                        String value = packet.getModifier().readSafely(2).toString();

                        if (!value.equalsIgnoreCase("ABORT_DESTROY_BLOCK")) return;
                        stopMiningAndResetAnimation();
                    }
                });
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
