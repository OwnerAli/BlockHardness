package me.ogali.blockhardness.config;

import me.ogali.blockhardness.progress.ProgressLimits;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds everything configurable about mining: the per-tier tool speeds and the mining defaults.
 * The built-in defaults mirror vanilla, so a missing or partial config still works.
 */
public class BlockHardnessConfig {
    public static final Map<String, Double> DEFAULT_TOOL_SPEEDS;
    public static final boolean DEFAULT_APPLY_TOOL_TIER = false;
    public static final boolean DEFAULT_APPLY_ENCHANTMENTS = false;
    public static final boolean DEFAULT_APPLY_POTION_EFFECTS = false;
    public static final double DEFAULT_MINIMUM_BREAK_SECONDS = 0.1;
    public static final boolean DEFAULT_SAVE_PROGRESS = false;
    public static final double DEFAULT_DECAY_PER_SECOND = 0.1;
    public static final double DEFAULT_FORGET_AFTER_SECONDS = 60;
    public static final int DEFAULT_MAX_TRACKED_BLOCKS = 16;
    public static final boolean DEFAULT_SHOW_SAVED_CRACKS = true;
    public static final int DEFAULT_CRACK_REFRESH_TICKS = 100;

    static {
        Map<String, Double> defaults = new LinkedHashMap<>();
        defaults.put("WOODEN_", 2.0);
        defaults.put("STONE_", 4.0);
        defaults.put("COPPER_", 5.0);
        defaults.put("IRON_", 6.0);
        defaults.put("DIAMOND_", 8.0);
        defaults.put("NETHERITE_", 9.0);
        defaults.put("GOLDEN_", 12.0);
        DEFAULT_TOOL_SPEEDS = Collections.unmodifiableMap(defaults);
    }

    /**
     * The config the rest of the plugin reads. Starts as the built-in defaults so the maths is usable — and
     * testable — without a server, and is replaced when the plugin enables.
     */
    private static BlockHardnessConfig current = new BlockHardnessConfig();

    private final JavaPlugin plugin;

    private Map<String, Double> toolSpeeds = DEFAULT_TOOL_SPEEDS;
    private boolean applyToolTierByDefault = DEFAULT_APPLY_TOOL_TIER;
    private boolean applyEnchantmentsByDefault = DEFAULT_APPLY_ENCHANTMENTS;
    private boolean applyPotionEffectsByDefault = DEFAULT_APPLY_POTION_EFFECTS;
    private double minimumBreakSeconds = DEFAULT_MINIMUM_BREAK_SECONDS;
    private boolean saveProgressByDefault = DEFAULT_SAVE_PROGRESS;
    private ProgressLimits progressLimits = ProgressLimits.defaults();
    private boolean showSavedCracks = DEFAULT_SHOW_SAVED_CRACKS;
    private int crackRefreshTicks = DEFAULT_CRACK_REFRESH_TICKS;

    private BlockHardnessConfig() {
        this.plugin = null;
    }

    public BlockHardnessConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
        current = this;
    }

    /** The live config, or the built-in defaults when no plugin has enabled. Never null. */
    public static BlockHardnessConfig current() {
        return current;
    }

    public void reload() {
        if (plugin == null) return;

        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        toolSpeeds = loadToolSpeeds(plugin.getConfig().getConfigurationSection("Tool-Speeds"));
        // The single switch still works as the fallback for every source, so a config written against the
        // original one-toggle design keeps behaving the same.
        boolean applyToolSpeed = plugin.getConfig().getBoolean("Mining.apply-tool-speed-by-default", false);

        applyToolTierByDefault = plugin.getConfig()
                .getBoolean("Mining.Tool-Speed-Defaults.tool-tier", applyToolSpeed);
        applyEnchantmentsByDefault = plugin.getConfig()
                .getBoolean("Mining.Tool-Speed-Defaults.enchantments", applyToolSpeed);
        applyPotionEffectsByDefault = plugin.getConfig()
                .getBoolean("Mining.Tool-Speed-Defaults.potion-effects", applyToolSpeed);
        minimumBreakSeconds = plugin.getConfig()
                .getDouble("Mining.minimum-break-seconds", DEFAULT_MINIMUM_BREAK_SECONDS);

        if (minimumBreakSeconds <= 0) {
            minimumBreakSeconds = DEFAULT_MINIMUM_BREAK_SECONDS;
        }

        saveProgressByDefault = plugin.getConfig()
                .getBoolean("Mining.Progress.save-by-default", DEFAULT_SAVE_PROGRESS);
        progressLimits = new ProgressLimits(
                Math.max(0, plugin.getConfig()
                        .getDouble("Mining.Progress.decay-per-second", DEFAULT_DECAY_PER_SECOND)),
                Math.max(0, plugin.getConfig()
                        .getDouble("Mining.Progress.forget-after-seconds", DEFAULT_FORGET_AFTER_SECONDS)),
                Math.max(0, plugin.getConfig()
                        .getInt("Mining.Progress.max-tracked-blocks-per-player", DEFAULT_MAX_TRACKED_BLOCKS)));

        showSavedCracks = plugin.getConfig()
                .getBoolean("Mining.Progress.show-saved-cracks", DEFAULT_SHOW_SAVED_CRACKS);
        crackRefreshTicks = Math.max(1, plugin.getConfig()
                .getInt("Mining.Progress.crack-refresh-ticks", DEFAULT_CRACK_REFRESH_TICKS));
    }

    /**
     * Tool name prefix (e.g. {@code DIAMOND_}) to speed, the built-in table merged with whatever the config adds
     * or overrides, so a partial section still leaves every vanilla tier working.
     */
    public Map<String, Double> getToolSpeeds() {
        return toolSpeeds;
    }

    /** Does a better tool break the block faster, when the caller doesn't say? */
    public boolean isApplyToolTierByDefault() {
        return applyToolTierByDefault;
    }

    /** Does Efficiency help, when the caller doesn't say? */
    public boolean isApplyEnchantmentsByDefault() {
        return applyEnchantmentsByDefault;
    }

    /** Do Haste and Conduit Power help, when the caller doesn't say? */
    public boolean isApplyPotionEffectsByDefault() {
        return applyPotionEffectsByDefault;
    }

    public double getMinimumBreakSeconds() {
        return minimumBreakSeconds;
    }

    /** Is progress on a block remembered when the player mines something else, when the caller doesn't say? */
    public boolean isSaveProgressByDefault() {
        return saveProgressByDefault;
    }

    /** Do blocks with saved progress keep their cracks while the player is not mining them? */
    public boolean isShowSavedCracks() {
        return showSavedCracks;
    }

    /** How often saved cracks are re-sent, in ticks. */
    public int getCrackRefreshTicks() {
        return crackRefreshTicks;
    }

    /** How fast remembered progress decays, and how much of it is kept. */
    public ProgressLimits getProgressLimits() {
        return progressLimits;
    }

    private Map<String, Double> loadToolSpeeds(ConfigurationSection section) {
        Map<String, Double> speeds = new LinkedHashMap<>(DEFAULT_TOOL_SPEEDS);
        if (section == null) return Collections.unmodifiableMap(speeds);

        for (String key : section.getKeys(false)) {
            if (!section.isDouble(key) && !section.isInt(key)) continue;
            speeds.put(key.toUpperCase(), section.getDouble(key));
        }
        return Collections.unmodifiableMap(speeds);
    }

}
