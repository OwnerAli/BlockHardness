package me.ogali.blockhardness.progress;

import me.ogali.blockhardness.config.BlockHardnessConfig;

/**
 * The bounds on remembered mining progress. Nothing is persisted across a restart, but progress kept in memory
 * still has to be bounded: a player who chips at a thousand blocks should not leave a thousand entries behind.
 *
 * @param decayPerSecond    progress lost per second a block is not being mined; 0 never decays
 * @param forgetAfterSeconds how long a block is remembered without being mined; 0 remembers until evicted
 * @param maxTrackedBlocks  how many blocks one player may have progress on; 0 is unlimited. The least recently
 *                          mined block is dropped first.
 */
public record ProgressLimits(double decayPerSecond, double forgetAfterSeconds, int maxTrackedBlocks) {

    public static ProgressLimits defaults() {
        return new ProgressLimits(
                BlockHardnessConfig.DEFAULT_DECAY_PER_SECOND,
                BlockHardnessConfig.DEFAULT_FORGET_AFTER_SECONDS,
                BlockHardnessConfig.DEFAULT_MAX_TRACKED_BLOCKS);
    }

    /** Progress is kept exactly as left, until it is evicted for space. */
    public static ProgressLimits noDecay(int maxTrackedBlocks) {
        return new ProgressLimits(0, 0, maxTrackedBlocks);
    }

}
