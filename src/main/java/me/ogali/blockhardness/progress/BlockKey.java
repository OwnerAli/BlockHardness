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

    /**
     * The id the crack animation is sent under. The client keys block damage by the id in the packet, so every
     * block needs its own: send them all under one id and only the newest block shows a crack.
     * <p>
     * Negative, to stay clear of the positive ids the server hands out to real entities, and derived from the
     * position so the same block always reuses its id and can be cleared later.
     */
    public int animationId() {
        int hash = (31 * (31 * x + y) + z) ^ worldId.hashCode();
        return hash == Integer.MIN_VALUE ? -1 : -Math.abs(hash);
    }

}
