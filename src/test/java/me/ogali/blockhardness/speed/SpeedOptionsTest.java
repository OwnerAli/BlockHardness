package me.ogali.blockhardness.speed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeedOptionsTest {

    @Test
    void allTurnsEverySourceOn() {
        SpeedOptions options = SpeedOptions.all();

        assertTrue(options.applyToolTier());
        assertTrue(options.applyEnchantments());
        assertTrue(options.applyPotionEffects());
        assertTrue(options.anyApplied());
    }

    @Test
    void noneTurnsEverySourceOff() {
        SpeedOptions options = SpeedOptions.none();

        assertFalse(options.applyToolTier());
        assertFalse(options.applyEnchantments());
        assertFalse(options.applyPotionEffects());
        assertFalse(options.anyApplied());
    }

    @Test
    void sourcesAreIndependent() {
        // "Ignore the pickaxe, but a Haste beacon still helps."
        SpeedOptions options = SpeedOptions.builder()
                .toolTier(false)
                .enchantments(false)
                .potionEffects(true)
                .build();

        assertFalse(options.applyToolTier());
        assertFalse(options.applyEnchantments());
        assertTrue(options.applyPotionEffects());
        assertTrue(options.anyApplied());
    }

    @Test
    void unsetSourcesFallBackToTheDefaults() {
        // No plugin running, so the built-in defaults apply to everything the caller didn't state.
        SpeedOptions options = SpeedOptions.builder().potionEffects(true).build();

        assertFalse(options.applyToolTier());
        assertFalse(options.applyEnchantments());
        assertTrue(options.applyPotionEffects());
    }

    @Test
    void allOnTheBuilderOverridesWhatCameBefore() {
        SpeedOptions options = SpeedOptions.builder().toolTier(false).all(true).build();

        assertTrue(options.applyToolTier());
        assertTrue(options.applyEnchantments());
        assertTrue(options.applyPotionEffects());
    }

    @Test
    void equalOptionsAreEqual() {
        assertEquals(SpeedOptions.all(), SpeedOptions.builder().all(true).build());
        assertEquals(SpeedOptions.all().hashCode(), SpeedOptions.builder().all(true).build().hashCode());
    }

}
