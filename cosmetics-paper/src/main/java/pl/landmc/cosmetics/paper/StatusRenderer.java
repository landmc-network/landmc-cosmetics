package pl.landmc.cosmetics.paper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.landmc.cosmetics.api.CosmeticEffect;
import pl.landmc.cosmetics.paper.config.CosmeticsConfig;
import pl.landmc.platform.component.ComponentFormatter;

/**
 * A line of text over the player's head, above their name.
 *
 * <p>A text display riding the player rather than a moved one: the game already carries
 * passengers about, and a status that is teleported every tick is a teleport per wearer per
 * tick to arrive at the place the player was going to take it anyway.
 *
 * <p>How high it sits is configuration for the same reason the wings' offset is - where a
 * passenger is mounted is not a number written down anywhere, and the only way to settle it is
 * to stand next to somebody wearing one. Too low hides the name, too high comes unstuck from
 * the head.
 *
 * <p>Its view range is deliberately shorter than the default. A status is a thing you read
 * about the person you are standing next to; a lobby where two hundred of them are legible
 * across the map is a lobby made of text.
 */
public final class StatusRenderer {

    private final Plugin plugin;
    private final CosmeticsConfig config;
    private final CosmeticState state;
    private final ComponentFormatter formatter;

    /** What is riding each player, so it can be taken away again. */
    private final Map<UUID, UUID> worn = new HashMap<>();

    public StatusRenderer(
            Plugin plugin,
            CosmeticsConfig config,
            CosmeticState state,
            ComponentFormatter formatter) {

        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.state = Objects.requireNonNull(state, "state");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    public boolean isEnabled() {
        return this.config.statuses.enabled;
    }

    /** Whether this player already has one floating over them. */
    public boolean has(Player player) {
        return this.worn.containsKey(player.getUniqueId());
    }

    /** Shows what this player wears, or takes down what they no longer do. Main thread only. */
    public void apply(Player player) {
        this.remove(player);

        if (!this.isEnabled()) {
            return;
        }

        CosmeticEffect effect = this.state.of(player.getUniqueId(), CosmeticEffect.Kind.STATUS);
        if (effect == null || !effect.isWorn()) {
            return;
        }

        CosmeticsConfig.StatusesSection statuses = this.config.statuses;
        float scale = (float) statuses.scale;

        TextDisplay display;
        try {
            display = player.getWorld().spawn(
                player.getLocation(), TextDisplay.class, spawned -> {
                    spawned.text(this.formatter.format(effect.text()));
                    // Turns to whoever is reading it, like the name it sits above. A fixed one
                    // would be an edge-on sliver to everybody standing to the side.
                    spawned.setBillboard(Display.Billboard.CENTER);
                    spawned.setAlignment(TextDisplay.TextAlignment.CENTER);
                    spawned.setBackgroundColor(background(statuses.backgroundOpacity));
                    spawned.setShadowed(false);
                    spawned.setSeeThrough(false);
                    spawned.setViewRange((float) statuses.viewRange);
                    spawned.setTransformation(new Transformation(
                            new Vector3f(0.0F, (float) statuses.offsetY, 0.0F),
                            new AxisAngle4f(),
                            new Vector3f(scale, scale, scale),
                            new AxisAngle4f()));
                    spawned.setTeleportDuration(1);
                    spawned.setPersistent(false);
                    spawned.setInvulnerable(true);
                    spawned.setSilent(true);
                });
        }
        catch (RuntimeException refused) {
            // A remembered nothing, so the heartbeat does not ask again every tick. See
            // WingRenderer, which is refused in exactly the same circumstances.
            this.worn.put(player.getUniqueId(), null);
            return;
        }

        player.addPassenger(display);
        this.worn.put(player.getUniqueId(), display.getUniqueId());
    }

    /** Takes down what is over them, if anything is. */
    public void remove(Player player) {
        UUID displayId = this.worn.remove(player.getUniqueId());
        if (displayId == null) {
            return;
        }

        Entity display = this.plugin.getServer().getEntity(displayId);
        if (display != null) {
            display.remove();
        }
    }

    /** Clears every one of them, for a shutdown or a reload. */
    public void removeAll() {
        for (UUID displayId : this.worn.values()) {
            if (displayId == null) {
                continue;
            }

            Entity display = this.plugin.getServer().getEntity(displayId);
            if (display != null) {
                display.remove();
            }
        }
        this.worn.clear();
    }

    /**
     * The panel the text sits on.
     *
     * <p>Fully transparent means no panel at all rather than a black one, which is the point of
     * setting nought: some statuses are meant to read as floating words.
     */
    private static Color background(int opacity) {
        return Color.fromARGB(Math.clamp(opacity, 0, 255), 0, 0, 0);
    }
}
