package pl.landmc.cosmetics.paper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import pl.landmc.cosmetics.api.CosmeticEffect;
import pl.landmc.cosmetics.paper.config.CosmeticsConfig;

/**
 * Wings, and anything else worn on the back.
 *
 * <p>Minecraft has no slot for this, so it is an entity riding the player. Which entity matters
 * more than it looks: an armour stand renders its hat wherever the game decides a passenger
 * sits, and that is around the player's eyes - wings on the face. A display can be moved
 * relative to where it rides, so the same mount can put them on the back.
 *
 * <p>That offset is configuration rather than a constant, because it is the one thing here that
 * can only be settled by looking at it: a model authored for another plugin's mounting sits
 * where that plugin put it, and nothing in the file says where that was.
 *
 * <p>The model travels as a material and a number because that is what the client is told - a
 * pack maps those onto a model and there is no way to name one from the server.
 *
 * <p>Riding something does not make an entity turn with it. A display keeps whatever yaw it was
 * spawned with, so a pair of wings that is never told otherwise points at one compass direction
 * for ever while its wearer walks circles around it. Turning them is therefore something the
 * heartbeat does, once a tick, for the few people wearing a pair.
 *
 * <p>The displays are removed when the wearer leaves, when they take the wings off and when the
 * plugin stops. An entity nobody cleans up is an entity still there next week.
 */
public final class WingRenderer {

    private final Plugin plugin;
    private final CosmeticsConfig config;
    private final CosmeticState state;

    /** What is riding each player, so it can be taken away again. */
    private final Map<UUID, UUID> worn = new HashMap<>();

    /**
     * The yaw each display was last turned to.
     *
     * <p>Kept so a standing player costs nothing: setting a rotation is a packet to everybody
     * who can see them, and somebody who has not turned does not need one every tick.
     */
    private final Map<UUID, Float> facing = new HashMap<>();

    public WingRenderer(Plugin plugin, CosmeticsConfig config, CosmeticState state) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.state = Objects.requireNonNull(state, "state");
    }

    public boolean isEnabled() {
        return this.config.wings.enabled;
    }

    /** Whether this player already has a pair on. */
    public boolean has(Player player) {
        return this.worn.containsKey(player.getUniqueId());
    }

    /** Puts on what this player wears, or takes off what they no longer do. Main thread only. */
    public void apply(Player player) {
        this.remove(player);

        if (!this.isEnabled()) {
            return;
        }

        CosmeticEffect effect = this.state.of(player.getUniqueId(), CosmeticEffect.Kind.WING);
        if (effect == null || !effect.isWorn()) {
            return;
        }

        ItemStack model = model(effect);
        if (model == null) {
            return;
        }

        CosmeticsConfig.WingsSection wings = this.config.wings;

        ItemDisplay display;
        try {
            display = player.getWorld().spawn(
                player.getLocation(), ItemDisplay.class, spawned -> {
                    spawned.setItemStack(model);
                    // Fixed, so the model turns with the player rather than swivelling to face
                    // whoever is looking at it - a pair of wings that always faces you is a
                    // sticker, not a pair of wings.
                    spawned.setBillboard(Display.Billboard.FIXED);
                    spawned.setTransformation(new Transformation(
                            new Vector3f(
                                    (float) wings.offsetX,
                                    (float) wings.offsetY,
                                    (float) wings.offsetZ),
                            new AxisAngle4f(),
                            new Vector3f((float) wings.scale, (float) wings.scale, (float) wings.scale),
                            new AxisAngle4f()));
                    // Smooths the ride: the client moves it over a tick instead of jumping it.
                    spawned.setTeleportDuration(1);
                    spawned.setPersistent(false);
                    spawned.setInvulnerable(true);
                    spawned.setSilent(true);
                });
        }
        catch (RuntimeException refused) {
            this.refuse(player);
            return;
        }

        player.addPassenger(display);
        this.worn.put(player.getUniqueId(), display.getUniqueId());
        this.facing.remove(player.getUniqueId());
    }

    /**
     * Remembers that a spawn was refused, so it is not attempted again every tick.
     *
     * <p>A plugin on this server can cancel a spawn - a lobby that allows no entities, a
     * protection that guards its region. The heartbeat notices a wearer with nothing on and
     * puts it back, so without this a refusal would be an exception twenty times a second for
     * as long as that player stays. A remembered nothing is still an answer, and the next
     * deliberate change puts the question again.
     */
    private void refuse(Player player) {
        this.worn.put(player.getUniqueId(), null);
        this.facing.remove(player.getUniqueId());
    }

    /**
     * Points one pair of wings the way its wearer is facing. Main thread only.
     *
     * <p>The body rather than the head: wings follow shoulders, and a player looking over their
     * shoulder should not take their wings with them.
     */
    public void tick(Player player) {
        UUID displayId = this.worn.get(player.getUniqueId());
        if (displayId == null) {
            return;
        }

        Entity display = this.plugin.getServer().getEntity(displayId);
        if (display == null) {
            // Gone without us. Forgetting it is what lets the heartbeat put a pair back on.
            this.worn.remove(player.getUniqueId());
            this.facing.remove(player.getUniqueId());
            return;
        }

        float yaw = player.getBodyYaw();
        Float last = this.facing.get(player.getUniqueId());
        // A degree is below what anybody can see, and above what a standing player's jitter
        // produces.
        if (last != null && Math.abs(angleBetween(last, yaw)) < 1.0F) {
            return;
        }

        this.facing.put(player.getUniqueId(), yaw);
        display.setRotation(yaw, 0.0F);
    }

    /** The shortest way round from one yaw to another, so 359 to 1 is two degrees and not 358. */
    private static float angleBetween(float from, float to) {
        float difference = (to - from) % 360.0F;
        if (difference > 180.0F) {
            difference -= 360.0F;
        }
        if (difference < -180.0F) {
            difference += 360.0F;
        }
        return difference;
    }

    /** Takes what is riding them away, if anything is. */
    public void remove(Player player) {
        UUID displayId = this.worn.remove(player.getUniqueId());
        this.facing.remove(player.getUniqueId());
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
        this.facing.clear();
    }

    /** The item the pack draws as this model, or null when it names something we do not have. */
    private static ItemStack model(CosmeticEffect effect) {
        Material material = Material.matchMaterial(effect.material().toUpperCase(Locale.ROOT));
        if (material == null || material.isAir()) {
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(effect.modelData());
        item.setItemMeta(meta);
        return item;
    }
}
