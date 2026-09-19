package me.ogali.blockhardness.progress;

import org.bukkit.block.Block;

import java.util.UUID;

/**
 * A block's identity, as a map key.
 * <p>
 * Stored instead of the {@link Block} itself so remembered progress holds nothing but a world id and three ints —
 * a Block keeps its world alive and is a fresh object on every lookup.
 *
 * @param worldId the world's UUID
 */
public record BlockKey(UUID worldId, int x, int y, int z) {

    public static BlockKey of(Block block) {
        return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

}
