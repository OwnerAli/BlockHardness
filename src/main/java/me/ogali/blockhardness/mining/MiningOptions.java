package me.ogali.blockhardness.mining;

import me.ogali.blockhardness.config.BlockHardnessConfig;
import me.ogali.blockhardness.speed.SpeedOptions;

import java.util.Objects;

/**
 * Per-call mining behaviour. BlockHardness owns the speed numbers, the caller owns the decision of whether they
 * apply at all — one drop may want "always exactly 5 seconds", another "5 seconds, faster with a better pick".
 * <p>
 * Built through {@link #builder()}; anything left unset resolves from this plugin's config when the object is
 * built, so a caller only states the parts it has an opinion about, and options added later cannot break an
 * existing call site.
 */
public final class MiningOptions {

    private final SpeedOptions speedOptions;
    private final boolean dropVanillaBlock;
    private final boolean saveProgress;

    private MiningOptions(SpeedOptions speedOptions, boolean dropVanillaBlock, boolean saveProgress) {
        this.speedOptions = speedOptions;
        this.dropVanillaBlock = dropVanillaBlock;
        this.saveProgress = saveProgress;
    }

    /**
     * Shorthand for the common case: tool speed either fully on or fully off.
     *
     * @param applyToolSpeed   every speed source, or none of them
     * @param dropVanillaBlock drop the block's vanilla drops when it breaks
     */
    public MiningOptions(boolean applyToolSpeed, boolean dropVanillaBlock) {
        this(applyToolSpeed ? SpeedOptions.all() : SpeedOptions.none(), dropVanillaBlock,
                BlockHardnessConfig.current().isSaveProgressByDefault());
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Whatever this plugin's config says, for callers with no opinion. */
    public static MiningOptions defaults() {
        return builder().build();
    }

    /** Which parts of the speed maths apply; never null. */
    public SpeedOptions speedOptions() {
        return speedOptions;
    }

    /** Drop the block's vanilla drops when it breaks. */
    public boolean dropVanillaBlock() {
        return dropVanillaBlock;
    }

    /**
     * Keep this block's progress when the player mines something else, so coming back resumes where they left
     * off rather than starting over. Progress decays and is forgotten per the config's limits.
     */
    public boolean saveProgress() {
        return saveProgress;
    }

    /** True when at least one speed source is on. */
    public boolean appliesToolSpeed() {
        return speedOptions.anyApplied();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MiningOptions miningOptions)) return false;
        return dropVanillaBlock == miningOptions.dropVanillaBlock
                && saveProgress == miningOptions.saveProgress
                && speedOptions.equals(miningOptions.speedOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(speedOptions, dropVanillaBlock, saveProgress);
    }

    @Override
    public String toString() {
        return "MiningOptions[" + speedOptions
                + ", dropVanillaBlock=" + dropVanillaBlock
                + ", saveProgress=" + saveProgress + "]";
    }

    public static final class Builder {

        private final SpeedOptions.Builder speedOptionsBuilder = SpeedOptions.builder();
        private SpeedOptions speedOptions;
        private Boolean dropVanillaBlock;
        private Boolean saveProgress;

        private Builder() {
        }

        /** Sets every speed source at once; the finer toggles below override it. */
        public Builder toolSpeed(boolean applyToolSpeed) {
            speedOptionsBuilder.all(applyToolSpeed);
            return this;
        }

        /** Uses a ready-made {@link SpeedOptions}, ignoring the finer toggles on this builder. */
        public Builder speedOptions(SpeedOptions speedOptions) {
            this.speedOptions = speedOptions;
            return this;
        }

        /** Does a better tool break the block faster? */
        public Builder toolTier(boolean applyToolTier) {
            speedOptionsBuilder.toolTier(applyToolTier);
            return this;
        }

        /** Does Efficiency help? */
        public Builder enchantments(boolean applyEnchantments) {
            speedOptionsBuilder.enchantments(applyEnchantments);
            return this;
        }

        /** Do Haste and Conduit Power help? */
        public Builder potionEffects(boolean applyPotionEffects) {
            speedOptionsBuilder.potionEffects(applyPotionEffects);
            return this;
        }

        public Builder dropVanillaBlock(boolean dropVanillaBlock) {
            this.dropVanillaBlock = dropVanillaBlock;
            return this;
        }

        /** Remember this block's progress when the player mines something else. */
        public Builder saveProgress(boolean saveProgress) {
            this.saveProgress = saveProgress;
            return this;
        }

        public MiningOptions build() {
            return new MiningOptions(
                    speedOptions == null ? speedOptionsBuilder.build() : speedOptions,
                    dropVanillaBlock != null && dropVanillaBlock,
                    saveProgress == null
                            ? BlockHardnessConfig.current().isSaveProgressByDefault()
                            : saveProgress);
        }

    }

}
