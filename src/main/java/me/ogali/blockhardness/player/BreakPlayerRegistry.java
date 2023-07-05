package me.ogali.blockhardness.player;

import me.ogali.blockhardness.player.domain.BreakPlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class BreakPlayerRegistry {

    private final Set<BreakPlayer> breakPlayerSet = new HashSet<>();

    public void addBreakPlayer(BreakPlayer breakPlayer) {
        breakPlayerSet.add(breakPlayer);
    }

    public Optional<BreakPlayer> getBreakPlayer(Player player) {
        return breakPlayerSet
                .stream()
                .filter(breakPlayer -> breakPlayer.getPlayer().equals(player))
                .findFirst();
    }

}
