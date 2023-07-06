package me.ogali.blockhardness.player.domain;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import me.ogali.blockhardness.BlockHardnessPlugin;
import me.ogali.blockhardness.events.CustomHardnessBlockBreakEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BreakPlayer {

    private final Player player;
    private Block currentBlockBeingBroken;
    private int currentBlockStage;
    private long lastDamageTime;
    private long timeBetweenEachIncrement;

    public BreakPlayer(Player player, BlockHardnessPlugin blockHardnessPlugin) {
        this.player = player;
        playerStopMiningPacketListener(blockHardnessPlugin);
        calculateTimeBetweenEachIncrement();
    }

    public Player getPlayer() {
        return player;
    }

    public void startMining(Block block) {
        if (!block.equals(currentBlockBeingBroken)) {
            startMiningNewBlock(block);
            return;
        }
        if (!((System.currentTimeMillis() - lastDamageTime) / 1000f >= timeBetweenEachIncrement)) return;
        if (currentBlockStage + 1 == 11) breakBlock();
        lastDamageTime = System.currentTimeMillis();
        sendBreakAnimation(currentBlockStage++);
    }

    private void startMiningNewBlock(Block block) {
        currentBlockStage = 0;
        currentBlockBeingBroken = block;
        lastDamageTime = System.currentTimeMillis();
        sendBreakAnimation(currentBlockStage++);
    }

    public void stopMining() {
        resetBreakAnimation();
        currentBlockStage = 0;
        currentBlockBeingBroken = null;
    }

    private void breakBlock() {
        player.playSound(player, Sound.BLOCK_STONE_BREAK, 1, 1);
        currentBlockBeingBroken.setType(Material.STONE);
        Bukkit.getPluginManager().callEvent(new CustomHardnessBlockBreakEvent(currentBlockBeingBroken, player));
        stopMining();
    }

    private void calculateTimeBetweenEachIncrement() {
        timeBetweenEachIncrement = 5 / 10;
    }

    private void playerStopMiningPacketListener(BlockHardnessPlugin blockHardnessPlugin) {
        ProtocolLibrary.getProtocolManager()
                .addPacketListener(new PacketAdapter(blockHardnessPlugin, ListenerPriority.NORMAL,
                        PacketType.Play.Client.BLOCK_DIG) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        if (currentBlockBeingBroken == null) return;

                        PacketContainer packet = event.getPacket();
                        String value = packet.getModifier().readSafely(2).toString();

                        if (!value.equalsIgnoreCase("ABORT_DESTROY_BLOCK")) return;
                        stopMining();
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
