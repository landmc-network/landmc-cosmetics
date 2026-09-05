package pl.landmc.cosmetics.api;

import pl.landmc.platform.messaging.message.NetworkMessage;

/**
 * A backend asking the shop what everybody is wearing.
 *
 * <p>Sent once when the backend starts. A broadcast only reaches servers that were running when
 * it was published, so a backend that restarts would show every player plain until they next
 * changed something - which for a cosmetic nobody changes often is effectively forever.
 *
 * <p>Carries nothing: the answer is the same for every asker, and who asked is already in the
 * envelope.
 */
public record CosmeticSnapshotRequest() implements NetworkMessage {

    public static final String TYPE = "cosmetics.snapshot.request";

    @Override
    public String type() {
        return TYPE;
    }
}
