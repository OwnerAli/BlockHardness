package me.ogali.blockhardness.mining;

import me.ogali.blockhardness.BlockHardnessPlugin;
import me.ogali.blockhardness.config.BlockHardnessConfig;

/**
 * Per-call mining behaviour. BlockHardness owns the speed numbers, the caller owns the decision of whether
 * tool speed applies at all — one drop may want "always exactly 5 seconds", another "5 seconds, faster with
 * a better pick".
 *
 * @param applyToolSpeed   divide the break time by {@link me.ogali.blockhardness.speed.ToolSpeed}'s multiplier
 * @param dropVanillaBlock drop the block's vanilla drops when it breaks
 */
public record MiningOptions(boolean applyToolSpeed, boolean dropVanillaBlock) {

    /** For callers with no opinion: whatever this plugin's config says. */
    public static MiningOptions defaults() {
        return new MiningOptions(applyToolSpeedByDefault(), false);
    }

    /** As {@link #defaults()}, but choosing whether vanilla drops are dropped. */
    public static MiningOptions defaults(boolean dropVanillaBlock) {
        return new MiningOptions(applyToolSpeedByDefault(), dropVanillaBlock);
    }

    private static boolean applyToolSpeedByDefault() {
        BlockHardnessPlugin plugin = BlockHardnessPlugin.instance;
        if (plugin == null || plugin.getBlockHardnessConfig() == null) {
            return BlockHardnessConfig.DEFAULT_APPLY_TOOL_SPEED;
        }
        return plugin.getBlockHardnessConfig().isApplyToolSpeedByDefault();
    }

}
