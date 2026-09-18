package me.ogali.blockhardness.progress;

import me.ogali.blockhardness.config.BlockHardnessConfig;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * One player's mining progress on blocks they are not currently mining, as a fraction from 0.0 to 1.0.
 * <p>
 * Progress decays with time and is forgotten once it reaches zero, so walking away from a half-mined block
 * eventually costs the player that work. Everything lives in memory and dies with the player's
 * {@link me.ogali.blockhardness.player.domain.BreakPlayer}, so nothing survives a restart or a logout.
 * <p>
 * Limits are read through a supplier rather than captured, so a config reload takes effect immediately.
 */
public final class MiningProgressStore {

    private final Supplier<ProgressLimits> limits;
    private final LongSupplier clock;
    private final Map<BlockKey, Entry> entries;

    public MiningProgressStore() {
        this(() -> BlockHardnessConfig.current().getProgressLimits(), System::currentTimeMillis);
    }

    public MiningProgressStore(ProgressLimits limits) {
        this(() -> limits, System::currentTimeMillis);
    }

    /**
     * @param clock supplies the current time in milliseconds; injectable so decay can be tested without waiting
     */
    public MiningProgressStore(Supplier<ProgressLimits> limits, LongSupplier clock) {
        this.limits = limits;
        this.clock = clock;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<BlockKey, Entry> eldest) {
                int maxTrackedBlocks = limits.get().maxTrackedBlocks();
                return maxTrackedBlocks > 0 && size() > maxTrackedBlocks;
            }
        };
    }

    /**
     * How far this player had got on the block, after applying decay; 0.0 when nothing is remembered.
     * Entries that have decayed away or expired are dropped as they are read.
     */
    public synchronized double getProgress(BlockKey blockKey) {
        Entry entry = entries.get(blockKey);
        if (entry == null) return 0;

        ProgressLimits progressLimits = limits.get();
        double secondsSince = Math.max(0, clock.getAsLong() - entry.updatedAt) / 1000.0;

        if (progressLimits.forgetAfterSeconds() > 0 && secondsSince >= progressLimits.forgetAfterSeconds()) {
            entries.remove(blockKey);
            return 0;
        }

        double decayed = entry.progress - (progressLimits.decayPerSecond() * secondsSince);
        if (decayed <= 0) {
            entries.remove(blockKey);
            return 0;
        }
        return decayed;
    }

    public double getProgress(Block block) {
        return getProgress(BlockKey.of(block));
    }

    /**
     * Remembers progress on a block. Values at or below 0 forget it instead; values above 1.0 are clamped, since
     * a full bar is a broken block, not remembered progress.
     */
    public synchronized void setProgress(BlockKey blockKey, double progress) {
        if (progress <= 0) {
            entries.remove(blockKey);
            return;
        }
        entries.put(blockKey, new Entry(Math.min(1.0, progress), clock.getAsLong()));
        trimToLimit();
    }

    public void setProgress(Block block, double progress) {
        setProgress(BlockKey.of(block), progress);
    }

    public synchronized void remove(BlockKey blockKey) {
        entries.remove(blockKey);
    }

    public void remove(Block block) {
        remove(BlockKey.of(block));
    }

    /** Forgets every block this player had progress on. */
    public synchronized void clear() {
        entries.clear();
    }

    /** How many blocks are remembered, including any that have decayed away but not yet been read. */
    public synchronized int size() {
        return entries.size();
    }

    /** A snapshot of the remembered blocks, most recently mined last. */
    public synchronized Set<BlockKey> trackedBlocks() {
        return Collections.unmodifiableSet(new LinkedHashMap<>(entries).keySet());
    }

    /**
     * The map itself only evicts one entry per insertion, which leaves it over the limit for a while after a
     * config reload lowers the limit. This brings it back down in one go.
     */
    private void trimToLimit() {
        int maxTrackedBlocks = limits.get().maxTrackedBlocks();
        if (maxTrackedBlocks <= 0) return;

        Iterator<BlockKey> leastRecentFirst = entries.keySet().iterator();
        while (entries.size() > maxTrackedBlocks && leastRecentFirst.hasNext()) {
            leastRecentFirst.next();
            leastRecentFirst.remove();
        }
    }

    private record Entry(double progress, long updatedAt) {
    }

}
