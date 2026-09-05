package pl.landmc.cosmetics.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pl.landmc.platform.messaging.message.NetworkMessage;

/**
 * Everything currently being worn, as the shop sees it.
 *
 * <p>The answer to a {@link CosmeticSnapshotRequest}. A list rather than a map because the wire
 * format is JSON and a map keyed by a player and a family serialises into keys nobody wants to
 * read; {@link #entries()} is what a backend walks.
 *
 * <p>A player appears once per family they are wearing something from, so somebody with a trail
 * and a glow is two entries. That is the same shape the changes arrive in, which means a
 * backend applies a snapshot with the code it already has for one change.
 */
public record CosmeticSnapshot(List<Entry> worn) implements NetworkMessage {

    public static final String TYPE = "cosmetics.snapshot";

    public CosmeticSnapshot {
        worn = worn == null ? List.of() : List.copyOf(worn);
    }

    public static CosmeticSnapshot empty() {
        return new CosmeticSnapshot(List.of());
    }

    @Override
    public String type() {
        return TYPE;
    }

    /** The snapshot as the changes it is equivalent to. */
    public List<CosmeticChangedMessage> entries() {
        List<CosmeticChangedMessage> changes = new ArrayList<>(this.worn.size());
        for (Entry entry : this.worn) {
            changes.add(new CosmeticChangedMessage(entry.playerId(), entry.effect()));
        }
        return changes;
    }

    public record Entry(UUID playerId, CosmeticEffect effect) {

        public Entry {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(effect, "effect");
        }
    }
}
