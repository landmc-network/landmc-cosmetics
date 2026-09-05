package pl.landmc.cosmetics.paper;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import pl.landmc.cosmetics.api.CosmeticEffect;
import pl.landmc.cosmetics.paper.config.CosmeticsConfig;

/**
 * Draws the trails.
 *
 * <p>One task for the whole server, walking the players who are wearing something. Not a task
 * per player: a lobby of two hundred would be two hundred repeating tasks doing the same
 * arithmetic, and the scheduler would spend more time waking them than they spend drawing.
 *
 * <p>The pattern is a function of time, not a stored position. A helix is where the angle has
 * got to, and the angle is the tick count - so nothing is remembered between frames, no state
 * goes stale when somebody leaves, and a player who logs out mid-turn takes nothing with them.
 *
 * <p>Every particle is sent to the players who can see it rather than to the world, because
 * spawning into a world sends it to everybody in render distance whether or not the effect is
 * for them, and because it is the only way to respect a viewing distance at all.
 */
public final class ParticleRenderer {

    /** A full turn, in radians, so a frame's angle wraps rather than growing without bound. */
    private static final double FULL_TURN = Math.PI * 2.0D;

    private final Plugin plugin;
    private final CosmeticsConfig config;
    private final CosmeticState state;

    private BukkitTask task;

    /** How far round the pattern has turned. Only ever read and written on the main thread. */
    private double angle;

    public ParticleRenderer(Plugin plugin, CosmeticsConfig config, CosmeticState state) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.state = Objects.requireNonNull(state, "state");
    }

    public boolean isEnabled() {
        return this.config.particles.enabled;
    }

    public void start() {
        if (!this.isEnabled() || this.task != null) {
            return;
        }

        long interval = Math.max(1L, this.config.particles.intervalTicks);
        this.task = this.plugin.getServer().getScheduler()
                .runTaskTimer(this.plugin, this::draw, interval, interval);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    private void draw() {
        if (this.state.isEmpty()) {
            return;
        }

        this.angle = (this.angle + 0.35D) % FULL_TURN;

        for (Map.Entry<UUID, Map<CosmeticEffect.Kind, CosmeticEffect>> entry
                : this.state.everyone()) {

            CosmeticEffect effect = entry.getValue().get(CosmeticEffect.Kind.PARTICLE);
            if (effect == null) {
                continue;
            }

            // Wearing it somewhere else on the network is the normal case, not a miss worth
            // logging: the state is broadcast to every server, and a player is only on one.
            Player player = this.plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                this.draw(player, effect);
            }
        }
    }

    private void draw(Player player, CosmeticEffect effect) {
        Particle particle = particle(effect.particle());
        if (particle == null) {
            return;
        }

        int points = Math.max(1, this.config.particles.pointsPerFrame);
        for (int point = 0; point < points; point++) {
            double step = this.angle + (FULL_TURN * point / points);
            this.show(player, particle, this.position(player, effect.pattern(), step));
        }
    }

    /**
     * Where one point of the pattern is, this frame.
     *
     * <p>Written as a switch over the pattern's name rather than a class each, because every
     * one of them is two lines of trigonometry and the family is small and closed. A pattern
     * this file has never heard of falls back to the ring, which is the one that looks like
     * something went wrong the least.
     */
    private Location position(Player player, String pattern, double step) {
        double radius = this.config.particles.radius;
        double height = this.config.particles.height;

        double x = Math.cos(step) * radius;
        double z = Math.sin(step) * radius;
        double y = switch (pattern.toUpperCase(Locale.ROOT)) {
            // Climbs as it turns, so the points make a spring rather than a circle.
            case "HELIX" -> (step / FULL_TURN) * height;
            // Sits at head height and stays there.
            case "HALO" -> height;
            // Sinks and rises, so the ring breathes.
            case "WAVE" -> (Math.sin(step * 2.0D) + 1.0D) * height / 2.0D;
            default -> height / 2.0D;
        };

        return player.getLocation().add(x, y, z);
    }

    /**
     * Sends one particle to the people near enough to see it.
     *
     * <p>Including the wearer: a cosmetic nobody can see is not much of a cosmetic, and the
     * person who paid for it is the one most likely to be looking.
     */
    private void show(Player wearer, Particle particle, Location location) {
        double distance = this.config.particles.viewDistance;
        double squared = distance * distance;

        for (Player viewer : this.plugin.getServer().getOnlinePlayers()) {
            if (!viewer.getWorld().equals(location.getWorld())) {
                continue;
            }
            if (viewer.getLocation().distanceSquared(location) > squared) {
                continue;
            }
            viewer.spawnParticle(particle, location, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** The named particle, or null when this server has never heard of it. */
    private static Particle particle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
