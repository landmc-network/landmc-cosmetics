package pl.landmc.cosmetics.paper;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import pl.landmc.cosmetics.api.CosmeticEffect;

/**
 * The one repeating job the worn cosmetics share.
 *
 * <p>Three families need something done every tick - wings have to be turned, pets have to be
 * flown along, and all of them have to be taken off somebody who has just gone invisible - and
 * three tasks would walk the same list of people three times to do it. This walks it once.
 *
 * <p>The list is the people wearing something, not the people online. That is the difference
 * between a job whose cost follows the number of cosmetics in use and one whose cost follows
 * the size of the lobby.
 *
 * <p>It also puts things back. Every renderer forgets a display or an animal that disappeared
 * without being asked to - an unloaded world, another plugin sweeping entities, a staff member
 * coming out of vanish - and this notices the gap on the next pass and fills it. Nothing else
 * has to detect any of those cases, which is why none of them are handled anywhere else.
 */
public final class CosmeticHeartbeat {

    /**
     * The mark {@code landmc-vanish} leaves on a hidden player.
     *
     * <p>A key rather than an import, the same way the chat reads it: hiding a player hides the
     * player and nothing else, so a vanished administrator would otherwise be a pair of wings
     * and a bee flying about on their own.
     */
    private static final String VANISHED = "vanished";

    private final Plugin plugin;
    private final CosmeticState state;
    private final WingRenderer wings;
    private final StatusRenderer statuses;
    private final PetRenderer pets;

    private BukkitTask task;

    public CosmeticHeartbeat(
            Plugin plugin,
            CosmeticState state,
            WingRenderer wings,
            StatusRenderer statuses,
            PetRenderer pets) {

        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.state = Objects.requireNonNull(state, "state");
        this.wings = Objects.requireNonNull(wings, "wings");
        this.statuses = Objects.requireNonNull(statuses, "statuses");
        this.pets = Objects.requireNonNull(pets, "pets");
    }

    public void start() {
        if (this.task != null) {
            return;
        }
        this.task = this.plugin.getServer().getScheduler()
                .runTaskTimer(this.plugin, this::beat, 1L, 1L);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    private void beat() {
        if (this.state.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, Map<CosmeticEffect.Kind, CosmeticEffect>> entry
                : this.state.everyone()) {

            // Wearing it somewhere else on the network is the normal case, not a miss worth
            // logging: the state is broadcast to every server, and a player is only on one.
            Player player = this.plugin.getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }

            if (player.hasMetadata(VANISHED)) {
                this.hide(player);
                continue;
            }

            Map<CosmeticEffect.Kind, CosmeticEffect> worn = entry.getValue();

            if (this.wings.isEnabled() && worn.containsKey(CosmeticEffect.Kind.WING)) {
                if (this.wings.has(player)) {
                    this.wings.tick(player);
                }
                else {
                    this.wings.apply(player);
                }
            }

            if (this.statuses.isEnabled()
                    && worn.containsKey(CosmeticEffect.Kind.STATUS)
                    && !this.statuses.has(player)) {

                this.statuses.apply(player);
            }

            if (this.pets.isEnabled() && worn.containsKey(CosmeticEffect.Kind.PET)) {
                if (this.pets.has(player)) {
                    this.pets.tick(player);
                }
                else {
                    this.pets.apply(player);
                }
            }
        }
    }

    /**
     * Takes everything visible off somebody who has just gone invisible.
     *
     * <p>Nothing is remembered about what was taken off, because nothing needs to be: the state
     * still says what they wear, and the next pass after they come back puts it all on again.
     */
    private void hide(Player player) {
        if (this.wings.has(player)) {
            this.wings.remove(player);
        }
        if (this.statuses.has(player)) {
            this.statuses.remove(player);
        }
        if (this.pets.has(player)) {
            this.pets.remove(player);
        }
    }
}
