package me.ogali.blockhardness.speed;

import me.ogali.blockhardness.config.BlockHardnessConfig;

/**
 * Which parts of the mining speed maths apply. Each source is independent: a block may want to ignore the tool
 * tier entirely but still reward a Haste beacon, or the other way round.
 * <p>
 * Built through {@link #builder()}; anything left unset resolves from this plugin's config when the object is
 * built, so a caller only states the parts it has an opinion about, and new sources can be added later without
 * breaking existing call sites.
 */
public final class SpeedOptions {

    private final boolean applyToolTier;
    private final boolean applyEnchantments;
    private final boolean applyPotionEffects;

    private SpeedOptions(boolean applyToolTier, boolean applyEnchantments, boolean applyPotionEffects) {
        this.applyToolTier = applyToolTier;
        this.applyEnchantments = applyEnchantments;
        this.applyPotionEffects = applyPotionEffects;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Whatever this plugin's config says, for callers with no opinion. */
    public static SpeedOptions defaults() {
        return builder().build();
    }

    /** Every source on: a better tool, Efficiency and Haste all help. */
    public static SpeedOptions all() {
        return new SpeedOptions(true, true, true);
    }

    /** Every source off: the break time is exactly what the caller asked for. */
    public static SpeedOptions none() {
        return new SpeedOptions(false, false, false);
    }

    /** Does a better tool break the block faster? */
    public boolean applyToolTier() {
        return applyToolTier;
    }

    /** Does Efficiency help? */
    public boolean applyEnchantments() {
        return applyEnchantments;
    }

    /** Do Haste and Conduit Power help? */
    public boolean applyPotionEffects() {
        return applyPotionEffects;
    }

    /** True when at least one source is on, so the caller can skip the maths entirely. */
    public boolean anyApplied() {
        return applyToolTier || applyEnchantments || applyPotionEffects;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SpeedOptions speedOptions)) return false;
        return applyToolTier == speedOptions.applyToolTier
                && applyEnchantments == speedOptions.applyEnchantments
                && applyPotionEffects == speedOptions.applyPotionEffects;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(applyToolTier, applyEnchantments, applyPotionEffects);
    }

    @Override
    public String toString() {
        return "SpeedOptions[toolTier=" + applyToolTier
                + ", enchantments=" + applyEnchantments
                + ", potionEffects=" + applyPotionEffects + "]";
    }

    /**
     * Unset sources fall back to the config defaults, so {@code builder().potionEffects(true).build()} means
     * "Haste helps, and everything else is whatever the server owner configured".
     */
    public static final class Builder {

        private Boolean applyToolTier;
        private Boolean applyEnchantments;
        private Boolean applyPotionEffects;

        private Builder() {
        }

        public Builder toolTier(boolean applyToolTier) {
            this.applyToolTier = applyToolTier;
            return this;
        }

        public Builder enchantments(boolean applyEnchantments) {
            this.applyEnchantments = applyEnchantments;
            return this;
        }

        public Builder potionEffects(boolean applyPotionEffects) {
            this.applyPotionEffects = applyPotionEffects;
            return this;
        }

        /** Sets every source at once, overriding anything set before it. */
        public Builder all(boolean applyEverySource) {
            return toolTier(applyEverySource).enchantments(applyEverySource).potionEffects(applyEverySource);
        }

        public SpeedOptions build() {
            BlockHardnessConfig config = BlockHardnessConfig.current();
            return new SpeedOptions(
                    orDefault(applyToolTier, config.isApplyToolTierByDefault()),
                    orDefault(applyEnchantments, config.isApplyEnchantmentsByDefault()),
                    orDefault(applyPotionEffects, config.isApplyPotionEffectsByDefault()));
        }

        private static boolean orDefault(Boolean value, boolean fallback) {
            return value == null ? fallback : value;
        }

    }

}
