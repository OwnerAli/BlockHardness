package me.ogali.blockhardness.player;

import me.ogali.blockhardness.player.domain.BreakPlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link BreakPlayer} of every online player, keyed by UUID.
 * <p>
 * Keyed rather than a set, because a rejoining player is a different {@link Player} object: a set would collect a
 * second entry per login and hand out the stale one. The lookup is also on the dig-packet path, several times a
 * second per digging player, so it should not be a scan.
 */
public class BreakPlayerRegistry {

    private final Map<UUID, BreakPlayer> breakPlayerMap = new ConcurrentHashMap<>();

    /** Replaces any entry left over for the same player. */
    public void addBreakPlayer(BreakPlayer breakPlayer) {
        breakPlayerMap.put(breakPlayer.getPlayer().getUniqueId(), breakPlayer);
    }

    public Optional<BreakPlayer> getBreakPlayer(Player player) {
        return player == null ? Optional.empty() : getBreakPlayer(player.getUniqueId());
    }

    public Optional<BreakPlayer> getBreakPlayer(UUID uuid) {
        return Optional.ofNullable(breakPlayerMap.get(uuid));
    }

    /**
     * Drops the player's mining state. Must happen when they leave, or the registry keeps their {@link Player}
     * — and through it their world and inventory — alive until the server restarts.
     */
    public Optional<BreakPlayer> removeBreakPlayer(Player player) {
        return player == null ? Optional.empty() : removeBreakPlayer(player.getUniqueId());
    }

    public Optional<BreakPlayer> removeBreakPlayer(UUID uuid) {
        return Optional.ofNullable(breakPlayerMap.remove(uuid));
    }

    public Collection<BreakPlayer> getBreakPlayers() {
        return Collections.unmodifiableCollection(breakPlayerMap.values());
    }

}
