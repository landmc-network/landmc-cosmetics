package pl.landmc.cosmetics.paper;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import pl.landmc.cosmetics.api.CosmeticEffect;

/**
 * Makes players glow, in a colour.
 *
 * <p>Minecraft has no "glow in this colour". It has a glowing flag on the entity and it draws
 * the outline in the colour of the team that entity belongs to - so a coloured glow is a team
 * per colour, and somebody put in it.
 *
 * <p>Which is where this gets awkward, and the awkwardness is worth writing down. Teams belong
 * to a scoreboard, and the lobby gives every player their own so that its sidebar can differ per
 * person. A team therefore has to exist on the board of everybody who might look, not once on a
 * board shared by all - so the work here is per viewer, and has to happen again whenever a
 * viewer's board is replaced.
 *
 * <p>The teams are named with a prefix of ours and hold nothing but glowing players, so nothing
 * else on those boards is disturbed - the lobby's own line teams are untouched.
 */
public final class GlowRenderer {

    /** Distinctive enough that no other plugin's team is mistaken for one of ours. */
    private static final String TEAM_PREFIX = "landmc_glow_";

    private final Plugin plugin;
    private final CosmeticState state;

    public GlowRenderer(Plugin plugin, CosmeticState state) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.state = Objects.requireNonNull(state, "state");
    }

    /**
     * Applies one player's glow, and puts them on everybody's board in the right colour.
     *
     * <p>Main thread only.
     */
    public void apply(Player player) {
        CosmeticEffect effect = this.state.of(player.getUniqueId(), CosmeticEffect.Kind.GLOW);
        boolean glowing = effect != null && effect.isWorn();

        player.setGlowing(glowing);

        for (Player viewer : this.plugin.getServer().getOnlinePlayers()) {
            this.place(viewer.getScoreboard(), player, glowing ? effect.colour() : null);
        }
    }

    /**
     * Brings one viewer's board up to date with everybody's glow.
     *
     * <p>Called when a viewer arrives, and again whenever something else hands them a new
     * board - a scoreboard that was replaced has none of our teams on it, and the glow it was
     * showing turns white without this.
     */
    public void syncFor(Player viewer) {
        Scoreboard board = viewer.getScoreboard();

        for (Map.Entry<UUID, Map<CosmeticEffect.Kind, CosmeticEffect>> entry
                : this.state.everyone()) {

            CosmeticEffect effect = entry.getValue().get(CosmeticEffect.Kind.GLOW);
            if (effect == null) {
                continue;
            }

            Player glowing = this.plugin.getServer().getPlayer(entry.getKey());
            if (glowing != null && glowing.isOnline()) {
                this.place(board, glowing, effect.colour());
            }
        }
    }

    /** Takes a player off every board, for a disconnect. */
    public void forget(Player player) {
        for (Player viewer : this.plugin.getServer().getOnlinePlayers()) {
            this.place(viewer.getScoreboard(), player, null);
        }
    }

    /**
     * Puts a player in the team for a colour, or takes them out of ours entirely.
     *
     * <p>Removed from the others first, because a player in two teams is in whichever the
     * client heard about last - which is a glow that changes colour depending on when somebody
     * logged in.
     */
    private void place(Scoreboard board, Player player, String colour) {
        for (Team team : board.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX) && team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }

        if (colour == null) {
            return;
        }

        ChatColor parsed = parse(colour);
        if (parsed == null) {
            return;
        }

        String name = TEAM_PREFIX + parsed.name().toLowerCase(Locale.ROOT);
        Team team = board.getTeam(name);
        if (team == null) {
            team = board.registerNewTeam(name);
            team.setColor(parsed);
        }
        team.addEntry(player.getName());
    }

    /** The named colour, or null when it is not one - a typo should not colour anybody. */
    private static ChatColor parse(String colour) {
        try {
            ChatColor parsed = ChatColor.valueOf(colour.toUpperCase(Locale.ROOT));
            return parsed.isColor() ? parsed : null;
        }
        catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
