package pl.landmc.cosmetics.paper;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import pl.landmc.cosmetics.api.CosmeticEffect;

/**
 * What everyone is wearing, as this server last heard it.
 *
 * <p>Keyed by player and then by family, because a player wears one of each at a time: putting
 * a new trail on replaces their trail and must not touch their glow. That is also why
 * {@link CosmeticEffect.Kind} is a closed set rather than a string - the map has one slot per
 * family by construction.
 *
 * <p>Holds ids rather than players. A message arrives for people who are not on this server -
 * that is the point of broadcasting it - and holding a {@code Player} for somebody who has
 * never connected here would be holding nothing at all.
 *
 * <p>Written from the messaging thread and read from the main thread, so both maps are
 * concurrent. Nothing here touches Bukkit.
 */
public final class CosmeticState {

    private final Map<UUID, Map<CosmeticEffect.Kind, CosmeticEffect>> worn =
            new ConcurrentHashMap<>();

    /**
     * Records what a player is wearing in one family.
     *
     * <p>Taking something off removes the entry rather than storing an empty one, so a player
     * wearing nothing costs nothing to remember - and the map of everybody wearing something
     * stays a map of everybody wearing something.
     */
    public void put(UUID playerId, CosmeticEffect effect) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(effect, "effect");

        if (!effect.isWorn()) {
            Map<CosmeticEffect.Kind, CosmeticEffect> byKind = this.worn.get(playerId);
            if (byKind == null) {
                return;
            }

            byKind.remove(effect.kind());
            if (byKind.isEmpty()) {
                this.worn.remove(playerId, byKind);
            }
            return;
        }

        this.worn
                .computeIfAbsent(playerId, id -> new EnumMap<>(CosmeticEffect.Kind.class))
                .put(effect.kind(), effect);
    }

    /** What this player wears in that family, or null. */
    public CosmeticEffect of(UUID playerId, CosmeticEffect.Kind kind) {
        Map<CosmeticEffect.Kind, CosmeticEffect> byKind = this.worn.get(playerId);
        return byKind == null ? null : byKind.get(kind);
    }

    /** Everybody wearing something, so a draw pass walks the few rather than the many. */
    public Iterable<Map.Entry<UUID, Map<CosmeticEffect.Kind, CosmeticEffect>>> everyone() {
        return this.worn.entrySet();
    }

    public boolean isEmpty() {
        return this.worn.isEmpty();
    }

    /** Replaces everything known, for the snapshot a restarting server asks for. */
    public void replaceAll(Iterable<Map.Entry<UUID, CosmeticEffect>> entries) {
        this.worn.clear();
        for (Map.Entry<UUID, CosmeticEffect> entry : entries) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    public void forget(UUID playerId) {
        this.worn.remove(playerId);
    }
}
