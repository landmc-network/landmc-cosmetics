package pl.landmc.cosmetics.api;

import pl.landmc.platform.messaging.serialization.MessageRegistry;

/**
 * Registers the cosmetic message types on a bus.
 *
 * <p>Both sides call this, which is the point of the shared module: a type registered under one
 * name where it is sent and another where it is read is a message that silently never arrives.
 */
public final class CosmeticMessages {

    private CosmeticMessages() {
    }

    public static MessageRegistry register(MessageRegistry registry) {
        return registry
                .register(CosmeticChangedMessage.TYPE, CosmeticChangedMessage.class)
                .register(CosmeticSnapshotRequest.TYPE, CosmeticSnapshotRequest.class)
                .register(CosmeticSnapshot.TYPE, CosmeticSnapshot.class);
    }
}
