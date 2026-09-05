package pl.landmc.cosmetics.paper;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import pl.landmc.cosmetics.api.CosmeticEffect;
import pl.landmc.cosmetics.paper.config.CosmeticsConfig;

/**
 * The words a player wears beside their name.
 *
 * <p>Nothing is drawn here, because a name is not drawn here: it is written by the chat and by
 * the tab list, and both of those belong to {@code landmc-chat}. What this does is mark the
 * player with the finished text, the way {@code landmc-vanish} marks a hidden one - a key
 * anybody can read and nobody has to depend on a plugin for.
 *
 * <p>That is the whole reason it is metadata rather than a service interface. A server with no
 * cosmetics plugin has players with no mark, the chat finds nothing there and writes the name
 * without a title, and neither side had to know the other exists. A hard dependency would mean
 * a chat plugin that refuses to load without a shop.
 *
 * <p>The wrapping brackets are applied here rather than in the chat, so the catalogue holds a
 * title and not a piece of layout, and so a server can change how titles are set off from the
 * name without the shop hearing about it.
 */
public final class TitleApplier {

    /**
     * What the mark is called.
     *
     * <p>Public because it is the contract: this is the name {@code landmc-chat} looks for, and
     * the two have no other connection.
     */
    public static final String METADATA_KEY = "cosmetic-title";

    private final Plugin plugin;
    private final CosmeticsConfig config;
    private final CosmeticState state;

    public TitleApplier(Plugin plugin, CosmeticsConfig config, CosmeticState state) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.state = Objects.requireNonNull(state, "state");
    }

    public boolean isEnabled() {
        return this.config.titles.enabled;
    }

    /** Marks the player with what they wear, or takes the mark off. Main thread only. */
    public void apply(Player player) {
        this.remove(player);

        if (!this.isEnabled()) {
            return;
        }

        CosmeticEffect effect = this.state.of(player.getUniqueId(), CosmeticEffect.Kind.TITLE);
        if (effect == null || !effect.isWorn()) {
            return;
        }

        String written = this.config.titles.format.replace("{TITLE}", effect.text());
        player.setMetadata(METADATA_KEY, new FixedMetadataValue(this.plugin, written));
    }

    /**
     * Takes our own mark off, and only ours.
     *
     * <p>Metadata is keyed by plugin as well as by name, so this cannot disturb a mark another
     * plugin happens to have put under the same key.
     */
    public void remove(Player player) {
        player.removeMetadata(METADATA_KEY, this.plugin);
    }

    /** Takes every one of them off, for a shutdown or a reload. */
    public void removeAll(Iterable<? extends Player> players) {
        for (Player player : players) {
            this.remove(player);
        }
    }
}
