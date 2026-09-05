package pl.landmc.cosmetics.paper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import pl.landmc.cosmetics.api.CosmeticEffect;

/**
 * Wings, and anything else worn on the back.
 *
 * <p>Minecraft has no slot for this. What it has is an armour stand, which renders whatever is
 * on its head, and passengers, which follow and turn with whatever they are riding - so a pair
 * of wings is an invisible armour stand sitting on the player with the model as its hat. Every
 * server that has wings does some version of this; the alternative is a client mod.
 *
 * <p>The model is a material and a number rather than a name, because that is what a client is
 * told: the pack maps custom model data onto a model, and the server has no way to name one.
 *
 * <p>The stands are ours and are removed when the wearer leaves, when they take the wings off
 * and when the plugin stops. An armour stand nobody cleans up is an armour stand that is still
 * standing there next week.
 */
public final class WingRenderer {

    private final Plugin plugin;
    private final CosmeticState state;

    /** The stand riding each player, so it can be taken away again. */
    private final Map<UUID, UUID> stands = new HashMap<>();

    public WingRenderer(Plugin plugin, CosmeticState state) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.state = Objects.requireNonNull(state, "state");
    }

    /** Puts on what this player wears, or takes off what they no longer do. Main thread only. */
    public void apply(Player player) {
        this.remove(player);

        CosmeticEffect effect = this.state.of(player.getUniqueId(), CosmeticEffect.Kind.WING);
        if (effect == null || !effect.isWorn()) {
            return;
        }

        ItemStack model = model(effect);
        if (model == null) {
            return;
        }

        ArmorStand stand = player.getWorld().spawn(
                player.getLocation(), ArmorStand.class, spawned -> {
                    // Invisible and weightless, and it must not be a marker: a marker armour
                    // stand has no equipment rendered, which is the one thing this is for.
                    spawned.setVisible(false);
                    spawned.setGravity(false);
                    spawned.setBasePlate(false);
                    spawned.setArms(false);
                    spawned.setSmall(true);
                    spawned.setInvulnerable(true);
                    spawned.setSilent(true);
                    spawned.setPersistent(false);
                    // Nothing may pick it up, push it, or find it with a selector meant for mobs.
                    spawned.setCollidable(false);
                    spawned.setCanTick(false);
                    spawned.getEquipment().setHelmet(model);
                });

        player.addPassenger(stand);
        this.stands.put(player.getUniqueId(), stand.getUniqueId());
    }

    /** Takes the stand away, if there is one. */
    public void remove(Player player) {
        UUID standId = this.stands.remove(player.getUniqueId());
        if (standId == null) {
            return;
        }

        org.bukkit.entity.Entity stand = this.plugin.getServer().getEntity(standId);
        if (stand != null) {
            stand.remove();
        }
    }

    /** Clears every stand, for a shutdown or a reload. */
    public void removeAll() {
        for (UUID standId : this.stands.values()) {
            org.bukkit.entity.Entity stand = this.plugin.getServer().getEntity(standId);
            if (stand != null) {
                stand.remove();
            }
        }
        this.stands.clear();
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
