package me.ogali.blockhardness.player.domain;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import me.ogali.blockhardness.config.BlockHardnessConfig;
import me.ogali.blockhardness.events.CustomHardnessBlockBreakEvent;
import me.ogali.blockhardness.mining.MiningOptions;
import me.ogali.blockhardness.progress.MiningProgressStore;
import me.ogali.blockhardness.speed.ToolSpeed;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BreakPlayer {

    /** The crack animation has ten stages; progress is tracked as a fraction so it can be saved and decayed. */
    private static final int BREAK_STAGES = 10;

    private final Player player;
    private final MiningProgressStore progressStore = new MiningProgressStore();

    private Block currentBlockBeingBroken;
    private double currentProgress;
    private long lastDamageTime;
    private MiningOptions currentOptions = MiningOptions.defaults();

    public BreakPlayer(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getCurrentBlockBeingBroken() {
        return currentBlockBeingBroken;
    }

    /**
     * This player's remembered progress on blocks they are not currently mining. Held here, so it is freed when
     * they log out.
     */
    public MiningProgressStore getProgressStore() {
        return progressStore;
    }

    /**
     * How far this player has got on a block, from 0.0 to just under 1.0 - the live value for the block they are
     * mining, the remembered (and decayed) value for any other.
     */
    public double getProgress(Block block) {
        if (block.equals(currentBlockBeingBroken)) return currentProgress;
        return progressStore.getProgress(block);
    }

    /** Sets how far along a block is, whether or not it is the one being mined. 1.0 does not break it. */
    public void setProgress(Block block, double progress) {
        double clamped = Math.max(0, Math.min(1.0, progress));
        if (block.equals(currentBlockBeingBroken)) {
            currentProgress = clamped;
            sendBreakAnimation(stageOf(clamped));
            return;
        }
        progressStore.setProgress(block, clamped);
    }

    /** Forgets a block's progress, resetting the animation if it is the one being mined. */
    public void clearProgress(Block block) {
        progressStore.remove(block);
        if (block.equals(currentBlockBeingBroken)) {
            stopMiningAndResetAnimation();
        }
    }

    /**
     * Kept for callers written against the old signature: no tool speed, just a fixed time.
     */
    public void startMining(Block block, double secondsBlockShouldTakeToBreak, boolean dropVanillaBlock) {
        startMining(block, secondsBlockShouldTakeToBreak, new MiningOptions(false, dropVanillaBlock));
    }

    /**
     * @param secondsBlockShouldTakeToBreak base break time, before any tool speed the options ask for
     * @param options                       which speed sources apply, whether progress is remembered and whether
     *                                      vanilla drops are dropped; null falls back to
     *                                      {@link MiningOptions#defaults()}
     */
    public void startMining(Block block, double secondsBlockShouldTakeToBreak, MiningOptions options) {
        MiningOptions miningOptions = options == null ? MiningOptions.defaults() : options;
        double secondsToBreak = applyToolSpeed(block, secondsBlockShouldTakeToBreak, miningOptions);
        long currentTime = System.currentTimeMillis();

        if (!block.equals(currentBlockBeingBroken)) {
            switchToBlock(block, miningOptions, currentTime);
        }

        currentOptions = miningOptions;

        if (secondsToBreak <= minimumBreakSeconds()) {
            breakBlock(miningOptions.dropVanillaBlock());
            return;
        }

        currentProgress += progressSince(currentTime, secondsToBreak);
        lastDamageTime = currentTime;

        if (currentProgress >= 1.0) {
            breakBlock(miningOptions.dropVanillaBlock());
            return;
        }
        sendBreakAnimation(stageOf(currentProgress));
    }

    /**
     * Stops the current dig, keeping the progress if the last call asked for it - this is what runs when the
     * player releases the button, so it is the usual way progress gets remembered.
     */
    public void stopMiningAndResetAnimation() {
        resetBreakAnimation();
        stopMining();
    }

    /**
     * Puts the previous block's progress away (or throws it out) and picks up whatever is remembered for the new
     * one, so returning to a half-mined block resumes rather than starting over.
     */
    private void switchToBlock(Block block, MiningOptions options, long currentTime) {
        setAsideCurrentProgress(options);

        currentBlockBeingBroken = block;
        currentProgress = options.saveProgress() ? progressStore.getProgress(block) : 0;
        lastDamageTime = currentTime;

        if (options.saveProgress()) {
            // It is being mined again, so it is no longer idle progress waiting to decay.
            progressStore.remove(block);
        }
    }

    private void setAsideCurrentProgress(MiningOptions options) {
        if (currentBlockBeingBroken == null) return;

        if (options.saveProgress() && currentProgress > 0) {
            progressStore.setProgress(currentBlockBeingBroken, currentProgress);
        } else {
            progressStore.remove(currentBlockBeingBroken);
        }
        resetBreakAnimation();
    }

    /**
     * Credits the time since the last swing, but never more than one stage's worth at once: a player who swings
     * once a second should not bank a second of mining in a single packet.
     */
    private double progressSince(long currentTime, double secondsToBreak) {
        double millisToBreak = secondsToBreak * 1000;
        double elapsed = Math.max(0, currentTime - lastDamageTime);

        return Math.min(elapsed, millisToBreak / BREAK_STAGES) / millisToBreak;
    }

    private void stopMining() {
        setAsideCurrentProgress(currentOptions);
        currentBlockBeingBroken = null;
        currentProgress = 0;
    }

    private void breakBlock(boolean dropVanillaBlock) {
        resetBreakAnimation();
        Bukkit.getPluginManager().callEvent(new CustomHardnessBlockBreakEvent(currentBlockBeingBroken, player));
        player.playSound(player, currentBlockBeingBroken.getBlockData().getSoundGroup().getBreakSound(), 1, 1);

        // A broken block keeps no progress, however the options were set.
        progressStore.remove(currentBlockBeingBroken);
        currentBlockBeingBroken.setType(Material.AIR);

        if (dropVanillaBlock) {
            currentBlockBeingBroken.breakNaturally(player.getItemInUse());
        }

        currentBlockBeingBroken = null;
        currentProgress = 0;
    }

    private double applyToolSpeed(Block block, double secondsBlockShouldTakeToBreak, MiningOptions options) {
        if (!options.appliesToolSpeed()) return secondsBlockShouldTakeToBreak;

        double multiplier = ToolSpeed.multiplierFor(player, block.getType(), options.speedOptions());
        return Math.max(minimumBreakSeconds(), secondsBlockShouldTakeToBreak / multiplier);
    }

    private double minimumBreakSeconds() {
        return BlockHardnessConfig.current().getMinimumBreakSeconds();
    }

    private static int stageOf(double progress) {
        return Math.max(0, Math.min(BREAK_STAGES - 1, (int) (progress * BREAK_STAGES)));
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
