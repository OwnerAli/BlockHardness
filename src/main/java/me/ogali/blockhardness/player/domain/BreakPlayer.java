package me.ogali.blockhardness.player.domain;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import me.ogali.blockhardness.config.BlockHardnessConfig;
import me.ogali.blockhardness.events.CustomHardnessBlockBreakEvent;
import me.ogali.blockhardness.mining.MiningOptions;
import me.ogali.blockhardness.progress.BlockKey;
import me.ogali.blockhardness.progress.MiningProgressStore;
import me.ogali.blockhardness.speed.ToolSpeed;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BreakPlayer {

    /** The crack animation has ten stages; progress is tracked as a fraction so it can be saved and decayed. */
    private static final int BREAK_STAGES = 10;

    /** Cracks further away than this are not worth a packet; the client will not have the chunk in view. */
    private static final int CRACK_VISIBLE_RADIUS = 64;

    private final Player player;
    private final MiningProgressStore progressStore = new MiningProgressStore();

    /** Blocks this player is currently being shown a crack on, so they can be cleared again exactly once. */
    private final Set<BlockKey> shownCracks = ConcurrentHashMap.newKeySet();

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
            showCrack(BlockKey.of(block), stageOf(clamped));
            return;
        }

        progressStore.setProgress(block, clamped);
        if (showSavedCracks()) {
            showCrack(BlockKey.of(block), stageOf(clamped));
        }
    }

    /** Forgets a block's progress and takes its crack off the player's screen. */
    public void clearProgress(Block block) {
        progressStore.remove(block);
        if (block.equals(currentBlockBeingBroken)) {
            currentBlockBeingBroken = null;
            currentProgress = 0;
        }
        hideCrack(BlockKey.of(block));
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
        showCrack(BlockKey.of(block), stageOf(currentProgress));
    }

    /**
     * Stops the current dig. The progress is kept if the last call asked for it, and so is the crack - this runs
     * when the player releases the button, which is exactly when a saved block should stay visibly damaged.
     */
    public void stopMiningAndResetAnimation() {
        setAsideCurrentProgress(currentOptions);
        currentBlockBeingBroken = null;
        currentProgress = 0;
    }

    /**
     * Re-sends the cracks for blocks this player has saved progress on, and clears the ones whose progress has
     * decayed away. Called on a timer by the plugin: the client drops block damage it has not heard about for a
     * while, and decay has to be shown as it happens rather than only when the player returns.
     */
    public void refreshSavedCracks() {
        if (!showSavedCracks()) {
            clearAllCracksExceptCurrent();
            return;
        }

        Set<BlockKey> blockKeys = new HashSet<>(progressStore.trackedBlocks());
        blockKeys.addAll(shownCracks);

        for (BlockKey blockKey : blockKeys) {
            if (isCurrentBlock(blockKey)) continue;

            double progress = progressStore.getProgress(blockKey);
            if (progress <= 0) {
                hideCrack(blockKey);
                continue;
            }
            if (isWithinRange(blockKey)) {
                showCrack(blockKey, stageOf(progress));
            }
        }
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

    /**
     * Saves the current block's progress, leaving its crack on screen so the player can see what they half-mined.
     * Without saving, both the progress and the crack go.
     */
    private void setAsideCurrentProgress(MiningOptions options) {
        if (currentBlockBeingBroken == null) return;

        BlockKey blockKey = BlockKey.of(currentBlockBeingBroken);
        if (options.saveProgress() && currentProgress > 0) {
            progressStore.setProgress(blockKey, currentProgress);
            if (showSavedCracks()) {
                showCrack(blockKey, stageOf(currentProgress));
            } else {
                hideCrack(blockKey);
            }
            return;
        }

        progressStore.remove(blockKey);
        hideCrack(blockKey);
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

    private void breakBlock(boolean dropVanillaBlock) {
        Block block = currentBlockBeingBroken;
        BlockKey blockKey = BlockKey.of(block);

        hideCrack(blockKey);
        Bukkit.getPluginManager().callEvent(new CustomHardnessBlockBreakEvent(block, player));
        player.playSound(player, block.getBlockData().getSoundGroup().getBreakSound(), 1, 1);

        // A broken block keeps no progress, however the options were set.
        progressStore.remove(blockKey);
        block.setType(Material.AIR);

        if (dropVanillaBlock) {
            block.breakNaturally(player.getItemInUse());
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

    private boolean showSavedCracks() {
        return BlockHardnessConfig.current().isShowSavedCracks();
    }

    private boolean isCurrentBlock(BlockKey blockKey) {
        return currentBlockBeingBroken != null && blockKey.equals(BlockKey.of(currentBlockBeingBroken));
    }

    private boolean isWithinRange(BlockKey blockKey) {
        if (!player.getWorld().getUID().equals(blockKey.worldId())) return false;

        double dx = player.getLocation().getX() - blockKey.x();
        double dy = player.getLocation().getY() - blockKey.y();
        double dz = player.getLocation().getZ() - blockKey.z();

        return (dx * dx) + (dy * dy) + (dz * dz) <= CRACK_VISIBLE_RADIUS * CRACK_VISIBLE_RADIUS;
    }

    private void clearAllCracksExceptCurrent() {
        for (BlockKey blockKey : new HashSet<>(shownCracks)) {
            if (isCurrentBlock(blockKey)) continue;
            hideCrack(blockKey);
        }
    }

    private static int stageOf(double progress) {
        return Math.max(0, Math.min(BREAK_STAGES - 1, (int) (progress * BREAK_STAGES)));
    }

    private void showCrack(BlockKey blockKey, int stage) {
        shownCracks.add(blockKey);
        sendBreakAnimation(blockKey, stage);
    }

    private void hideCrack(BlockKey blockKey) {
        shownCracks.remove(blockKey);
        sendBreakAnimation(blockKey, -1);
    }

    private void sendBreakAnimation(BlockKey blockKey, int stage) {
        World world = Bukkit.getWorld(blockKey.worldId());
        if (world == null) return;

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer blockBreakPacket = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        blockBreakPacket.getIntegers().write(0, blockKey.animationId());
        blockBreakPacket.getBlockPositionModifier().write(0,
                new BlockPosition(blockKey.x(), blockKey.y(), blockKey.z()));
        blockBreakPacket.getIntegers().write(1, stage);

        try {
            protocolManager.sendServerPacket(player, blockBreakPacket);
        } catch (Exception ignored) {
            Bukkit.getConsoleSender().sendMessage("ERROR IN PACKET!");
        }
    }

}
