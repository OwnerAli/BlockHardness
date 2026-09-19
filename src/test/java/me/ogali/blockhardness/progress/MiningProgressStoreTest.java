package me.ogali.blockhardness.progress;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiningProgressStoreTest {

    private static final UUID WORLD = UUID.randomUUID();
    private static final BlockKey BLOCK = new BlockKey(WORLD, 1, 2, 3);
    private static final BlockKey OTHER_BLOCK = new BlockKey(WORLD, 4, 5, 6);
    private static final double DELTA = 1.0E-9;

    private final AtomicLong now = new AtomicLong(1_000_000);

    private MiningProgressStore storeWith(ProgressLimits limits) {
        return new MiningProgressStore(() -> limits, now::get);
    }

    private void advanceSeconds(double seconds) {
        now.addAndGet((long) (seconds * 1000));
    }

    @Test
    void anUntouchedBlockHasNoProgress() {
        assertEquals(0, storeWith(ProgressLimits.defaults()).getProgress(BLOCK), DELTA);
    }

    @Test
    void progressIsKeptWhenNothingDecays() {
        MiningProgressStore store = storeWith(ProgressLimits.noDecay(16));
        store.setProgress(BLOCK, 0.6);

        advanceSeconds(3600);

        assertEquals(0.6, store.getProgress(BLOCK), DELTA);
    }

    @Test
    void progressDecaysWithTime() {
        MiningProgressStore store = storeWith(new ProgressLimits(0.1, 0, 16));
        store.setProgress(BLOCK, 0.8);

        advanceSeconds(3);

        assertEquals(0.5, store.getProgress(BLOCK), DELTA);
    }

    @Test
    void decayedAwayProgressIsForgotten() {
        MiningProgressStore store = storeWith(new ProgressLimits(0.1, 0, 16));
        store.setProgress(BLOCK, 0.5);

        advanceSeconds(5);

        assertEquals(0, store.getProgress(BLOCK), DELTA);
        assertEquals(0, store.size(), "the entry should be dropped, not left at zero");
    }

    @Test
    void progressIsForgottenAfterTheDeadlineEvenWithoutDecay() {
        MiningProgressStore store = storeWith(new ProgressLimits(0, 60, 16));
        store.setProgress(BLOCK, 0.9);

        advanceSeconds(59);
        assertEquals(0.9, store.getProgress(BLOCK), DELTA);

        advanceSeconds(1);
        assertEquals(0, store.getProgress(BLOCK), DELTA);
        assertEquals(0, store.size());
    }

    @Test
    void writingProgressRestartsTheClock() {
        MiningProgressStore store = storeWith(new ProgressLimits(0.1, 0, 16));
        store.setProgress(BLOCK, 0.5);

        advanceSeconds(3);
        store.setProgress(BLOCK, 0.7);
        advanceSeconds(3);

        assertEquals(0.4, store.getProgress(BLOCK), DELTA);
    }

    @Test
    void progressIsClampedToAFullBar() {
        MiningProgressStore store = storeWith(ProgressLimits.noDecay(16));
        store.setProgress(BLOCK, 5.0);

        assertEquals(1.0, store.getProgress(BLOCK), DELTA);
    }

    @Test
    void settingZeroOrLessForgetsTheBlock() {
        MiningProgressStore store = storeWith(ProgressLimits.noDecay(16));
        store.setProgress(BLOCK, 0.5);

        store.setProgress(BLOCK, 0);

        assertEquals(0, store.size());
    }

    @Test
    void blocksAreTrackedSeparately() {
        MiningProgressStore store = storeWith(ProgressLimits.noDecay(16));
        store.setProgress(BLOCK, 0.3);
        store.setProgress(OTHER_BLOCK, 0.7);

        assertEquals(0.3, store.getProgress(BLOCK), DELTA);
        assertEquals(0.7, store.getProgress(OTHER_BLOCK), DELTA);
    }

    @Test
    void theSameCoordinatesInAnotherWorldAreAnotherBlock() {
        MiningProgressStore store = storeWith(ProgressLimits.noDecay(16));
        store.setProgress(BLOCK, 0.3);

        assertEquals(0, store.getProgress(new BlockKey(UUID.randomUUID(), 1, 2, 3)), DELTA);
    }

    @Test
    void theLimitDropsTheLeastRecentlyMinedBlock() {
        MiningProgressStore store = storeWith(ProgressLimits.noDecay(2));
        store.setProgress(new BlockKey(WORLD, 0, 0, 0), 0.5);
        store.setProgress(new BlockKey(WORLD, 0, 0, 1), 0.5);
        store.setProgress(new BlockKey(WORLD, 0, 0, 2), 0.5);

        assertEquals(2, store.size());
        assertEquals(0, store.getProgress(new BlockKey(WORLD, 0, 0, 0)), DELTA);
        assertEquals(0.5, store.getProgress(new BlockKey(WORLD, 0, 0, 2)), DELTA);
    }

    @Test
    void readingABlockKeepsItFromBeingEvictedFirst() {
        MiningProgressStore store = storeWith(ProgressLimits.noDecay(2));
        BlockKey first = new BlockKey(WORLD, 0, 0, 0);
        BlockKey second = new BlockKey(WORLD, 0, 0, 1);
        store.setProgress(first, 0.5);
        store.setProgress(second, 0.5);

        store.getProgress(first);
        store.setProgress(new BlockKey(WORLD, 0, 0, 2), 0.5);

        assertEquals(0.5, store.getProgress(first), DELTA, "the block read most recently should survive");
        assertEquals(0, store.getProgress(second), DELTA);
    }

    @Test
    void zeroMeansNoLimit() {
        MiningProgressStore store = storeWith(new ProgressLimits(0, 0, 0));
        for (int z = 0; z < 500; z++) {
            store.setProgress(new BlockKey(WORLD, 0, 0, z), 0.5);
        }

        assertEquals(500, store.size());
    }

    @Test
    void aChangedLimitAppliesToTheNextWrite() {
        // Limits come from a supplier, so a config reload is picked up without rebuilding the store.
        ProgressLimits[] limits = {ProgressLimits.noDecay(16)};
        MiningProgressStore store = new MiningProgressStore(() -> limits[0], now::get);
        store.setProgress(new BlockKey(WORLD, 0, 0, 0), 0.5);
        store.setProgress(new BlockKey(WORLD, 0, 0, 1), 0.5);

        limits[0] = ProgressLimits.noDecay(1);
        store.setProgress(new BlockKey(WORLD, 0, 0, 2), 0.5);

        assertEquals(1, store.size());
    }

    @Test
    void removingAndClearingForgetProgress() {
        MiningProgressStore store = storeWith(ProgressLimits.noDecay(16));
        store.setProgress(BLOCK, 0.5);
        store.setProgress(OTHER_BLOCK, 0.5);

        store.remove(BLOCK);
        assertEquals(0, store.getProgress(BLOCK), DELTA);
        assertEquals(1, store.size());

        store.clear();
        assertEquals(0, store.size());
    }

    @Test
    void trackedBlocksReportsWhatIsHeld() {
        MiningProgressStore store = storeWith(ProgressLimits.noDecay(16));
        store.setProgress(BLOCK, 0.5);

        assertTrue(store.trackedBlocks().contains(BLOCK));
        assertFalse(store.trackedBlocks().contains(OTHER_BLOCK));
    }

    @Test
    void theDefaultLimitsAreTheConfiguredDefaults() {
        ProgressLimits defaults = ProgressLimits.defaults();

        assertEquals(0.1, defaults.decayPerSecond(), DELTA);
        assertEquals(60, defaults.forgetAfterSeconds(), DELTA);
        assertEquals(16, defaults.maxTrackedBlocks());
    }

}
