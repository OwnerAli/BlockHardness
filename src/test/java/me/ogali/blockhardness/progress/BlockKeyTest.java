package me.ogali.blockhardness.progress;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockKeyTest {

    private static final UUID WORLD = UUID.randomUUID();

    @Test
    void theSameBlockAlwaysGetsTheSameAnimationId() {
        // The id is how a crack is cleared again later, so it has to be stable.
        assertEquals(new BlockKey(WORLD, 1, 2, 3).animationId(), new BlockKey(WORLD, 1, 2, 3).animationId());
    }

    @Test
    void animationIdsAreNegative() {
        // Real entities get positive ids; a collision would hijack that entity's crack.
        assertTrue(new BlockKey(WORLD, 0, 0, 0).animationId() < 0);
        assertTrue(new BlockKey(WORLD, -30_000_000, -64, 30_000_000).animationId() < 0);
        assertTrue(new BlockKey(WORLD, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE).animationId() < 0);
    }

    @Test
    void neighbouringBlocksGetDifferentAnimationIds() {
        // Blocks mined at once are usually neighbours, so these are the collisions that would show.
        Set<Integer> ids = new HashSet<>();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    ids.add(new BlockKey(WORLD, x, y, z).animationId());
                }
            }
        }

        assertEquals(16 * 16 * 16, ids.size());
    }

    @Test
    void theSameCoordinatesInAnotherWorldAreAnotherBlock() {
        BlockKey key = new BlockKey(WORLD, 1, 2, 3);
        BlockKey otherWorld = new BlockKey(UUID.randomUUID(), 1, 2, 3);

        assertNotEquals(key, otherWorld);
        assertNotEquals(key.animationId(), otherWorld.animationId());
    }

}
