package me.ogali.blockhardness.config;

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
    public static final boolean DEFAULT_APPLY_TOOL_SPEED = false;
    public static final double DEFAULT_MINIMUM_BREAK_SECONDS = 0.1;

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

    private final JavaPlugin plugin;

    private Map<String, Double> toolSpeeds = DEFAULT_TOOL_SPEEDS;
    private boolean applyToolSpeedByDefault = DEFAULT_APPLY_TOOL_SPEED;
    private double minimumBreakSeconds = DEFAULT_MINIMUM_BREAK_SECONDS;

    public BlockHardnessConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        toolSpeeds = loadToolSpeeds(plugin.getConfig().getConfigurationSection("Tool-Speeds"));
        applyToolSpeedByDefault = plugin.getConfig()
                .getBoolean("Mining.apply-tool-speed-by-default", DEFAULT_APPLY_TOOL_SPEED);
        minimumBreakSeconds = plugin.getConfig()
                .getDouble("Mining.minimum-break-seconds", DEFAULT_MINIMUM_BREAK_SECONDS);

        if (minimumBreakSeconds <= 0) {
            minimumBreakSeconds = DEFAULT_MINIMUM_BREAK_SECONDS;
        }
    }

    /**
     * Tool name prefix (e.g. {@code DIAMOND_}) to speed, the built-in table merged with whatever the config adds
     * or overrides, so a partial section still leaves every vanilla tier working.
     */
    public Map<String, Double> getToolSpeeds() {
        return toolSpeeds;
    }

    public boolean isApplyToolSpeedByDefault() {
        return applyToolSpeedByDefault;
    }

    public double getMinimumBreakSeconds() {
        return minimumBreakSeconds;
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
