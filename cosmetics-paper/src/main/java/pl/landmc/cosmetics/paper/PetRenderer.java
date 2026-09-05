package pl.landmc.cosmetics.paper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.plugin.Plugin;
import pl.landmc.cosmetics.api.CosmeticEffect;
import pl.landmc.cosmetics.paper.config.CosmeticsConfig;
import pl.landmc.platform.component.ComponentFormatter;

/**
 * The creature that follows a player about.
 *
 * <p>A real animal rather than a model on a display. The bee, the fox and the slime are already
 * in the game and everybody knows what they look like; drawing them again in a resource pack
 * would be work whose best possible outcome is that nobody notices it was done.
 *
 * <p>It is an animal with nothing switched on: no mind, no gravity, no sound, no way to be hurt
 * and nothing to collide with. What is left is a shape, moved by this class - which is the
 * point. An animal left to its own devices wanders into the lava, follows somebody else, or
 * stands still while its owner walks away.
 *
 * <p>Moved rather than ridden, because a passenger on a player sits on their head. The movement
 * is deliberately behind: it flies towards where it should be rather than appearing there, so
 * it swings wide on a corner and catches up on the straight, and that lag is the whole
 * difference between a pet and a lamp bolted to somebody's shoulder.
 */
public final class PetRenderer {

    /** How fast it bobs in place. Slow enough to read as breathing rather than as a wobble. */
    private static final double BOB_SPEED = 0.12D;

    private final Plugin plugin;
    private final CosmeticsConfig config;
    private final CosmeticState state;
    private final ComponentFormatter formatter;

    /** Whose pet is which entity, so it can be sent away again. */
    private final Map<UUID, UUID> summoned = new HashMap<>();

    public PetRenderer(
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
        return this.config.pets.enabled;
    }

    /** Whether this player already has one out. */
    public boolean has(Player player) {
        return this.summoned.containsKey(player.getUniqueId());
    }

    /** Calls out what this player has chosen, or sends away what they no longer have. */
    public void apply(Player player) {
        this.remove(player);

        if (!this.isEnabled()) {
            return;
        }

        CosmeticEffect effect = this.state.of(player.getUniqueId(), CosmeticEffect.Kind.PET);
        if (effect == null || !effect.isWorn()) {
            return;
        }

        Class<? extends LivingEntity> species = species(effect.entity());
        if (species == null) {
            return;
        }

        LivingEntity pet;
        try {
            pet = player.getWorld().spawn(
                this.target(player, 0), species, spawned -> {
                    spawned.setAI(false);
                    spawned.setSilent(true);
                    spawned.setInvulnerable(true);
                    spawned.setCollidable(false);
                    spawned.setGravity(false);
                    // Not written to the region file, and not tidied away when its owner walks
                    // out of the chunk - this class decides when it goes, and nothing else does.
                    spawned.setPersistent(false);
                    spawned.setRemoveWhenFarAway(false);

                    if (!effect.text().isBlank()) {
                        // The owner's name is the one thing the catalogue cannot write down,
                        // so it writes the hole and this fills it.
                        spawned.customName(this.formatter.format(
                                effect.text().replace("{PLAYER}", player.getName())));
                        spawned.setCustomNameVisible(true);
                    }

                    // A pet is small. The adult of most of these is the size of the player.
                    if (spawned instanceof Ageable ageable) {
                        ageable.setBaby();
                    }
                    if (spawned instanceof Slime slime) {
                        slime.setSize(1);
                    }
                });
        }
        catch (RuntimeException refused) {
            // Something here does not allow animals to be spawned. A remembered nothing, so
            // the heartbeat does not ask again every tick for as long as they are online.
            this.summoned.put(player.getUniqueId(), null);
            return;
        }

        this.summoned.put(player.getUniqueId(), pet.getUniqueId());
    }

    /** Sends away what is following them, if anything is. */
    public void remove(Player player) {
        UUID petId = this.summoned.remove(player.getUniqueId());
        if (petId == null) {
            return;
        }

        Entity pet = this.plugin.getServer().getEntity(petId);
        if (pet != null) {
            pet.remove();
        }
    }

    /** Sends away every one of them, for a shutdown or a reload. */
    public void removeAll() {
        for (UUID petId : this.summoned.values()) {
            if (petId == null) {
                continue;
            }

            Entity pet = this.plugin.getServer().getEntity(petId);
            if (pet != null) {
                pet.remove();
            }
        }
        this.summoned.clear();
    }

    /**
     * Moves one pet a frame's worth towards where it belongs. Main thread only.
     *
     * <p>Called for the people wearing one rather than for everybody online, which is what
     * makes a per-tick job affordable at all.
     */
    public void tick(Player player) {
        UUID petId = this.summoned.get(player.getUniqueId());
        if (petId == null) {
            return;
        }

        Entity pet = this.plugin.getServer().getEntity(petId);
        if (pet == null || pet.isDead()) {
            // Gone without us: an unloaded world, a plugin that clears entities. Forgetting it
            // is what lets the heartbeat call it back on the next pass.
            this.summoned.remove(player.getUniqueId());
            return;
        }

        int tick = this.plugin.getServer().getCurrentTick();
        Location target = this.target(player, tick);
        Location current = pet.getLocation();

        CosmeticsConfig.PetsSection pets = this.config.pets;
        double catchUp = pets.catchUpDistance;

        // A different world or half a map away is a teleport rather than a journey: the owner
        // used a portal or a command, and a pet flying there in a straight line would cross
        // everything in between.
        if (!Objects.equals(current.getWorld(), target.getWorld())
                || current.distanceSquared(target) > catchUp * catchUp) {

            pet.teleport(target);
        }
        else {
            double smoothing = Math.clamp(pets.smoothing, 0.05D, 1.0D);
            Location next = current.clone();
            next.add(
                    (target.getX() - current.getX()) * smoothing,
                    (target.getY() - current.getY()) * smoothing,
                    (target.getZ() - current.getZ()) * smoothing);
            next.setYaw(target.getYaw());
            next.setPitch(0.0F);
            pet.teleport(next);
        }

        this.trail(player, pet, tick);
    }

    /** The particles it leaves behind, if it has any and this is one of their frames. */
    private void trail(Player owner, Entity pet, int tick) {
        CosmeticsConfig.PetsSection pets = this.config.pets;
        if (!pets.particles) {
            return;
        }

        long interval = Math.max(1L, pets.particleIntervalTicks);
        if (tick % interval != 0) {
            return;
        }

        CosmeticEffect effect = this.state.of(owner.getUniqueId(), CosmeticEffect.Kind.PET);
        if (effect == null || effect.particle().isBlank()) {
            return;
        }

        Particle particle = particle(effect.particle());
        if (particle == null) {
            return;
        }

        pet.getWorld().spawnParticle(particle, pet.getLocation(), 2, 0.1D, 0.1D, 0.1D, 0.0D);
    }

    /**
     * Where the pet belongs this frame: off the owner's shoulder, a little behind, bobbing.
     *
     * <p>Off the body rather than on it, and worked out from the way the owner is facing rather
     * than from a fixed compass direction, so it stays off the same shoulder when they turn.
     */
    private Location target(Player player, int tick) {
        CosmeticsConfig.PetsSection pets = this.config.pets;

        Location anchor = player.getLocation();
        float yaw = player.getBodyYaw();
        double radians = Math.toRadians(yaw);

        // Minecraft's yaw: nought looks along positive Z, and the owner's right hand is a
        // quarter turn from there.
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double rightX = -forwardZ;
        double rightZ = forwardX;

        double bob = pets.bobHeight == 0.0D
                ? 0.0D
                : Math.sin(tick * BOB_SPEED) * pets.bobHeight;

        return new Location(
                anchor.getWorld(),
                anchor.getX() + rightX * pets.sideOffset - forwardX * pets.backOffset,
                anchor.getY() + pets.height + bob,
                anchor.getZ() + rightZ * pets.sideOffset - forwardZ * pets.backOffset,
                yaw,
                0.0F);
    }

    /**
     * The animal a name asks for, or null when this build has never heard of it.
     *
     * <p>Living things only, and only ones that can be spawned: the catalogue is a text file
     * somewhere else, and a typo in it must not take the server down.
     */
    private static Class<? extends LivingEntity> species(String name) {
        EntityType type;
        try {
            type = EntityType.valueOf(name.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException unknown) {
            return null;
        }

        Class<? extends Entity> species = type.getEntityClass();
        if (species == null
                || !type.isSpawnable()
                || !LivingEntity.class.isAssignableFrom(species)) {

            return null;
        }
        return species.asSubclass(LivingEntity.class);
    }

    private static Particle particle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
