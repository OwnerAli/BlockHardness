package me.ogali.blockhardness.mining;

import me.ogali.blockhardness.speed.SpeedOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningOptionsTest {

    @Test
    void theShorthandConstructorTurnsEverySourceOnOrOff() {
        MiningOptions on = new MiningOptions(true, false);
        assertEquals(SpeedOptions.all(), on.speedOptions());
        assertTrue(on.appliesToolSpeed());
        assertFalse(on.dropVanillaBlock());

        MiningOptions off = new MiningOptions(false, true);
        assertEquals(SpeedOptions.none(), off.speedOptions());
        assertFalse(off.appliesToolSpeed());
        assertTrue(off.dropVanillaBlock());
    }

    @Test
    void theBuilderExposesTheFinerToggles() {
        MiningOptions options = MiningOptions.builder()
                .toolTier(true)
                .potionEffects(true)
                .dropVanillaBlock(true)
                .build();

        assertTrue(options.speedOptions().applyToolTier());
        assertFalse(options.speedOptions().applyEnchantments());
        assertTrue(options.speedOptions().applyPotionEffects());
        assertTrue(options.dropVanillaBlock());
    }

    @Test
    void anUntouchedBuilderMatchesTheDefaults() {
        assertEquals(MiningOptions.defaults(), MiningOptions.builder().build());
    }

    @Test
    void readyMadeSpeedOptionsWin() {
        MiningOptions options = MiningOptions.builder()
                .toolTier(true)
                .speedOptions(SpeedOptions.none())
                .build();

        assertEquals(SpeedOptions.none(), options.speedOptions());
        assertFalse(options.appliesToolSpeed());
    }

    @Test
    void defaultsDoNotDropVanillaBlocks() {
        assertFalse(MiningOptions.defaults().dropVanillaBlock());
    }

}
