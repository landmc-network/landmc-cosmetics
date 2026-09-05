package pl.landmc.cosmetics.api;

import java.util.Objects;
import java.util.UUID;
import pl.landmc.platform.messaging.message.NetworkMessage;

/**
 * One player put a cosmetic on, or took it off.
 *
 * <p>Broadcast rather than sent to the server they are standing on, for the same reason the
 * vanish state is: a broadcast reaches every backend before the player does. Somebody who
 * changes their trail and then walks to another server should arrive already wearing it, not
 * arrive plain and light up a moment later.
 *
 * @param playerId whose cosmetic changed
 * @param effect what they are wearing now - see {@link CosmeticEffect#isWorn()} for taking one
 *     off, which is a change like any other and travels the same way
 */
public record CosmeticChangedMessage(UUID playerId, CosmeticEffect effect)
        implements NetworkMessage {

    public static final String TYPE = "cosmetics.changed";

    public CosmeticChangedMessage {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(effect, "effect");
    }

    @Override
    public String type() {
        return TYPE;
    }
}
