package pl.landmc.cosmetics.paper.listener;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import pl.landmc.cosmetics.paper.GlowRenderer;
import pl.landmc.cosmetics.paper.PetRenderer;
import pl.landmc.cosmetics.paper.StatusRenderer;
import pl.landmc.cosmetics.paper.WingRenderer;

/**
 * Applies what a player was already wearing when they arrive, and clears up after them.
 *
 * <p>Only the glow, which is the one family nothing else looks after. Wings, statuses and pets
 * are put on by the heartbeat on its next pass, which is a tick away and already knows how to
 * notice that somebody is wearing something they have not got on - doing it here as well would
 * be the same work twice and a display spawned only to be replaced.
 *
 * <p>Delayed a tick past the join, and at the lowest priority so that runs last. Two things
 * have to have happened first: the player has to be fully in the world for a glow to stick, and
 * the lobby has to have given them its scoreboard - our teams go on the board they end up with,
 * not the one they had for a moment.
 *
 * <p>Leaving is not delayed and clears everything, including what the heartbeat put on. An
 * entity riding somebody who has disconnected is an entity standing in an empty lobby.
 */
public final class CosmeticListener implements Listener {

    private final Plugin plugin;
    private final GlowRenderer glow;
    private final WingRenderer wings;
    private final StatusRenderer statuses;
    private final PetRenderer pets;

    public CosmeticListener(
            Plugin plugin,
            GlowRenderer glow,
            WingRenderer wings,
            StatusRenderer statuses,
            PetRenderer pets) {

        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.glow = Objects.requireNonNull(glow, "glow");
        this.wings = Objects.requireNonNull(wings, "wings");
        this.statuses = Objects.requireNonNull(statuses, "statuses");
        this.pets = Objects.requireNonNull(pets, "pets");
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
        Player player = event.getPlayer();

        // Off the boards and out of the world, but not out of the state: what they wear is the
        // shop's to remember, and they may be walking to another server rather than leaving.
        this.glow.forget(player);
        this.wings.remove(player);
        this.statuses.remove(player);
        this.pets.remove(player);
    }
}
