package pl.landmc.cosmetics.paper.listener;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import pl.landmc.cosmetics.paper.CosmeticState;
import pl.landmc.cosmetics.paper.GlowRenderer;

/**
 * Applies what a player was already wearing when they arrive, and clears up after them.
 *
 * <p>Delayed a tick past the join, and at the lowest priority so that runs last. Two things
 * have to have happened first: the player has to be fully in the world for a glow to stick, and
 * the lobby has to have given them its scoreboard - our teams go on the board they end up with,
 * not the one they had for a moment.
 */
public final class CosmeticListener implements Listener {

    private final Plugin plugin;
    private final CosmeticState state;
    private final GlowRenderer glow;

    public CosmeticListener(Plugin plugin, CosmeticState state, GlowRenderer glow) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.state = Objects.requireNonNull(state, "state");
        this.glow = Objects.requireNonNull(glow, "glow");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        player.getScheduler().runDelayed(this.plugin, task -> {
            // What they wear, on everybody's board.
            this.glow.apply(player);
            // And everybody else's, on theirs.
            this.glow.syncFor(player);
        }, null, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        // Off the boards, but not out of the state: what they wear is the shop's to remember,
        // and they may be walking to another server rather than leaving the network.
        this.glow.forget(event.getPlayer());
    }
}
