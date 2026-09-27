package nl.oxod.nekoclient.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Loader-specific handling of the {@code minecraft:register} networking payload.
 *
 * <p>The payload class itself is an implementation detail of a specific mod loader, so shared
 * code routes register-payload inspection through this interface. Loaders without an equivalent
 * payload use {@link #UNSUPPORTED}, which makes the caller drop the packet instead of crashing.
 */
public interface RegistrationPayloadAdapter {
    RegistrationPayloadAdapter UNSUPPORTED = new RegistrationPayloadAdapter() {
        @Override
        public boolean isRegistrationPayload(CustomPacketPayload payload) {
            return false;
        }

        @Override
        public List<Identifier> channelsOf(CustomPacketPayload payload) {
            return List.of();
        }

        @Override
        public Optional<CustomPacketPayload> rebuild(CustomPacketPayload original, List<Identifier> kept) {
            return Optional.empty();
        }
    };

    boolean isRegistrationPayload(CustomPacketPayload payload);

    List<Identifier> channelsOf(CustomPacketPayload payload);

    Optional<CustomPacketPayload> rebuild(CustomPacketPayload original, List<Identifier> kept);
}
