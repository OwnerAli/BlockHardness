package me.ogali.blockhardness.speed;

import me.ogali.blockhardness.BlockHardnessPlugin;
import me.ogali.blockhardness.config.BlockHardnessConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * Vanilla's mining speed maths, computed from the item a player is holding.
 * <p>
 * Numbers follow <a href="https://minecraft.wiki/w/Breaking">the wiki</a>:
 * {@code (tierSpeed + efficiencyBonus) * hasteMultiplier}, never below 1.0.
 * <p>
 * Deliberately does not read {@code BLOCK_BREAK_SPEED} or {@link org.bukkit.block.Block#getBreakSpeed(Player)}:
 * plugins freeze that attribute to 0 while a custom block is being mined, so it reports "infinitely slow".
 */
public final class ToolSpeed {

    /** Shears on cobweb and leaves. */
    public static final double SHEARS_ON_WEB_AND_LEAVES = 15.0;
    /** Shears on wool. */
    public static final double SHEARS_ON_WOOL = 2.0;

    private ToolSpeed() {
    }

    /**
     * How much faster this player breaks the given block than with a bare hand.
     *
     * @return 1.0 for a bare hand, a non-tool or the wrong tool; higher for a suitable tool.
     */
    public static double multiplierFor(Player player, Material blockMaterial) {
        if (player == null || blockMaterial == null) return 1.0;

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        double tierSpeed = tierSpeedFor(heldItem, blockMaterial);

        return compute(tierSpeed, efficiencyLevelOf(heldItem), hasteLevelOf(player));
    }

    /**
     * The tier speed of the held item for this block, before enchantments and effects:
     * 1.0 for a bare hand, a non-tool or a tool that does not suit the block.
     */
    public static double tierSpeedFor(ItemStack heldItem, Material blockMaterial) {
        if (heldItem == null || heldItem.getType() == Material.AIR) return 1.0;

        Material toolMaterial = heldItem.getType();
        if (toolMaterial == Material.SHEARS) return shearsSpeedFor(blockMaterial);

        double tierSpeed = tierSpeedFor(toolMaterial, toolSpeeds());
        if (tierSpeed <= 1.0) return 1.0;

        return isPreferredTool(blockMaterial, heldItem) ? tierSpeed : 1.0;
    }

    /**
     * {@code (tierSpeed + efficiencyBonus) * hasteMultiplier}, floored at 1.0.
     * Efficiency adds {@code level^2 + 1}; Haste / Conduit Power multiply by {@code 1 + 0.2 * level}.
     */
    public static double compute(double tierSpeed, int efficiencyLevel, int hasteLevel) {
        double speed = Math.max(1.0, tierSpeed);

        if (efficiencyLevel > 0) {
            speed += (efficiencyLevel * efficiencyLevel) + 1;
        }
        if (hasteLevel > 0) {
            speed *= 1 + (0.2 * hasteLevel);
        }
        return Math.max(1.0, speed);
    }

    /**
     * Matched by name prefix rather than enum constant, so tools from other plugins can be given a speed and
     * unknown or custom tools simply fall through to 1.0 — and no version-specific material is referenced.
     */
    public static double tierSpeedFor(Material toolMaterial, Map<String, Double> toolSpeeds) {
        if (toolMaterial == null || !isTool(toolMaterial)) return 1.0;

        String name = toolMaterial.name();
        for (Map.Entry<String, Double> entry : toolSpeeds.entrySet()) {
            if (name.startsWith(entry.getKey())) return entry.getValue();
        }
        return 1.0;
    }

    /** 15 on cobweb and leaves, 2 on wool, 1 on anything else. */
    public static double shearsSpeedFor(Material blockMaterial) {
        if (blockMaterial == null) return 1.0;

        String name = blockMaterial.name();
        if (name.equals("COBWEB") || name.endsWith("_LEAVES")) return SHEARS_ON_WEB_AND_LEAVES;
        if (name.endsWith("_WOOL")) return SHEARS_ON_WOOL;
        return 1.0;
    }

    /**
     * Vanilla only applies tool speed when the tool suits the block — without this a diamond <i>sword</i>
     * would mine stone 8x faster. Older servers without the API fall back to allowing it.
     */
    private static boolean isPreferredTool(Material blockMaterial, ItemStack heldItem) {
        try {
            return blockMaterial.createBlockData().isPreferredTool(heldItem);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static boolean isTool(Material toolMaterial) {
        String name = toolMaterial.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE") || name.endsWith("_SWORD");
    }

    private static int efficiencyLevelOf(ItemStack heldItem) {
        if (heldItem == null || !heldItem.hasItemMeta()) return 0;

        // Looked up through the registry: the deprecated Enchantment#getByName returns null on newer forks,
        // which silently drops the bonus instead of erroring.
        Enchantment efficiency = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("efficiency"));
        if (efficiency == null) return 0;

        return heldItem.getItemMeta().getEnchantLevel(efficiency);
    }

    private static int hasteLevelOf(Player player) {
        int haste = effectLevelOf(player, "haste");
        int conduitPower = effectLevelOf(player, "conduit_power");
        return Math.max(haste, conduitPower);
    }

    private static int effectLevelOf(Player player, String key) {
        PotionEffectType type = effectType(key);
        if (type == null) return 0;

        PotionEffect effect = player.getPotionEffect(type);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    /**
     * Looked up through the registry where the server has one (the effect registry is newer than the API this
     * plugin compiles against, hence {@link Bukkit#getRegistry}); the deprecated
     * {@link PotionEffectType#getByName} is only a fallback for servers without it, because it returns null on
     * newer forks, which silently drops the bonus instead of erroring.
     */
    @SuppressWarnings("deprecation")
    private static PotionEffectType effectType(String key) {
        try {
            Registry<PotionEffectType> registry = Bukkit.getRegistry(PotionEffectType.class);
            if (registry != null) {
                PotionEffectType type = registry.get(NamespacedKey.minecraft(key));
                if (type != null) return type;
            }
        } catch (Throwable ignored) {
            // No effect registry on this server; fall through.
        }

        try {
            return PotionEffectType.getByName(key);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Map<String, Double> toolSpeeds() {
        BlockHardnessPlugin plugin = BlockHardnessPlugin.instance;
        if (plugin == null || plugin.getBlockHardnessConfig() == null) {
            return BlockHardnessConfig.DEFAULT_TOOL_SPEEDS;
        }
        return plugin.getBlockHardnessConfig().getToolSpeeds();
    }

}
