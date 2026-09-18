package me.ogali.blockhardness.speed;

import me.ogali.blockhardness.config.BlockHardnessConfig;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolSpeedTest {

    private static final Map<String, Double> SPEEDS = BlockHardnessConfig.DEFAULT_TOOL_SPEEDS;
    private static final double DELTA = 1.0E-9;

    @Test
    void tierSpeedsMatchVanilla() {
        assertEquals(2.0, ToolSpeed.tierSpeedFor(Material.WOODEN_PICKAXE, SPEEDS), DELTA);
        assertEquals(4.0, ToolSpeed.tierSpeedFor(Material.STONE_PICKAXE, SPEEDS), DELTA);
        assertEquals(6.0, ToolSpeed.tierSpeedFor(Material.IRON_PICKAXE, SPEEDS), DELTA);
        assertEquals(8.0, ToolSpeed.tierSpeedFor(Material.DIAMOND_PICKAXE, SPEEDS), DELTA);
        assertEquals(9.0, ToolSpeed.tierSpeedFor(Material.NETHERITE_PICKAXE, SPEEDS), DELTA);
        assertEquals(12.0, ToolSpeed.tierSpeedFor(Material.GOLDEN_PICKAXE, SPEEDS), DELTA);
    }

    @Test
    void tierSpeedAppliesToEveryToolType() {
        assertEquals(8.0, ToolSpeed.tierSpeedFor(Material.DIAMOND_AXE, SPEEDS), DELTA);
        assertEquals(8.0, ToolSpeed.tierSpeedFor(Material.DIAMOND_SHOVEL, SPEEDS), DELTA);
        assertEquals(8.0, ToolSpeed.tierSpeedFor(Material.DIAMOND_HOE, SPEEDS), DELTA);
    }

    @Test
    void handAndNonToolsAreOne() {
        assertEquals(1.0, ToolSpeed.tierSpeedFor(Material.AIR, SPEEDS), DELTA);
        assertEquals(1.0, ToolSpeed.tierSpeedFor(Material.DIAMOND, SPEEDS), DELTA);
        assertEquals(1.0, ToolSpeed.tierSpeedFor(Material.DIAMOND_BLOCK, SPEEDS), DELTA);
        assertEquals(1.0, ToolSpeed.tierSpeedFor(Material.STICK, SPEEDS), DELTA);
    }

    @Test
    void unknownToolPrefixFallsThroughToOne() {
        assertEquals(1.0, ToolSpeed.tierSpeedFor(Material.SHEARS, SPEEDS), DELTA);
        assertEquals(1.0, ToolSpeed.tierSpeedFor(Material.TRIDENT, SPEEDS), DELTA);
    }

    @Test
    void copperPrefixIsConfigurableEvenIfTheServerHasNoCopperTools() {
        // Matched by name, so a copper pickaxe from another plugin still gets its tier speed.
        assertEquals(5.0, SPEEDS.get("COPPER_"), DELTA);
    }

    @Test
    void shearsAreFastOnWebAndLeaves() {
        assertEquals(15.0, ToolSpeed.shearsSpeedFor(Material.COBWEB), DELTA);
        assertEquals(15.0, ToolSpeed.shearsSpeedFor(Material.OAK_LEAVES), DELTA);
        assertEquals(15.0, ToolSpeed.shearsSpeedFor(Material.BIRCH_LEAVES), DELTA);
    }

    @Test
    void shearsAreSlightlyFastOnWool() {
        assertEquals(2.0, ToolSpeed.shearsSpeedFor(Material.WHITE_WOOL), DELTA);
        assertEquals(2.0, ToolSpeed.shearsSpeedFor(Material.BLACK_WOOL), DELTA);
    }

    @Test
    void shearsAreOrdinaryOnEverythingElse() {
        assertEquals(1.0, ToolSpeed.shearsSpeedFor(Material.STONE), DELTA);
        assertEquals(1.0, ToolSpeed.shearsSpeedFor(Material.DIRT), DELTA);
    }

    @Test
    void efficiencyAddsLevelSquaredPlusOne() {
        assertEquals(8.0 + 2, ToolSpeed.compute(8.0, 1, 0), DELTA);
        assertEquals(8.0 + 5, ToolSpeed.compute(8.0, 2, 0), DELTA);
        assertEquals(8.0 + 10, ToolSpeed.compute(8.0, 3, 0), DELTA);
        assertEquals(8.0 + 17, ToolSpeed.compute(8.0, 4, 0), DELTA);
        assertEquals(8.0 + 26, ToolSpeed.compute(8.0, 5, 0), DELTA);
    }

    @Test
    void hasteAddsTwentyPercentPerLevel() {
        assertEquals(8.0 * 1.2, ToolSpeed.compute(8.0, 0, 1), DELTA);
        assertEquals(8.0 * 1.4, ToolSpeed.compute(8.0, 0, 2), DELTA);
        assertEquals(1.0 * 1.6, ToolSpeed.compute(1.0, 0, 3), DELTA);
    }

    @Test
    void efficiencyIsAddedBeforeHasteMultiplies() {
        // (8 + 5) * 1.2, not 8 * 1.2 + 5
        assertEquals(13.0 * 1.2, ToolSpeed.compute(8.0, 2, 1), DELTA);
    }

    @Test
    void bareHandIsOne() {
        assertEquals(1.0, ToolSpeed.compute(1.0, 0, 0), DELTA);
    }

    @Test
    void neverGoesBelowOne() {
        assertEquals(1.0, ToolSpeed.compute(0.0, 0, 0), DELTA);
        assertEquals(1.0, ToolSpeed.compute(-5.0, 0, 0), DELTA);
        assertEquals(1.0, ToolSpeed.compute(0.5, 0, 0), DELTA);
    }

    @Test
    void negativeEnchantmentAndEffectLevelsAreIgnored() {
        assertEquals(8.0, ToolSpeed.compute(8.0, -1, -1), DELTA);
    }

}
