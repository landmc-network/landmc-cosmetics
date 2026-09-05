package pl.landmc.cosmetics.api;

import java.util.Objects;

/**
 * What a cosmetic actually does, as opposed to what it is called and what it costs.
 *
 * <p>The effect travels with the message rather than being looked up on the backend, and that
 * is the decision this whole module is arranged around. The alternative is a catalogue on each
 * side - prices and names on the proxy, particles and colours on the backend - and then adding
 * one cosmetic means editing two files that have to agree about an identifier nobody validates.
 * Sending the effect means there is one catalogue, in one place, and a backend that has never
 * heard of a cosmetic can still draw it.
 *
 * <p>The fields are deliberately loose - names of things, not enums. A backend knows what
 * {@code FLAME} and {@code SPIRAL} mean; this module would only be a third place to add a
 * particle to.
 *
 * @param kind which family it belongs to, and therefore which of the fields below mean anything
 * @param particle the particle to draw, for {@link Kind#PARTICLE}
 * @param pattern how to arrange it - a helix, a ring, a cloud
 * @param colour the glow colour, for {@link Kind#GLOW}, as a named Minecraft colour
 */
public record CosmeticEffect(Kind kind, String particle, String pattern, String colour) {

    public CosmeticEffect {
        Objects.requireNonNull(kind, "kind");
        particle = particle == null ? "" : particle;
        pattern = pattern == null ? "" : pattern;
        colour = colour == null ? "" : colour;
    }

    /** Nothing worn. Sent when a player takes a cosmetic off, so the backend clears it. */
    public static CosmeticEffect none(Kind kind) {
        return new CosmeticEffect(kind, "", "", "");
    }

    public static CosmeticEffect particle(String particle, String pattern) {
        return new CosmeticEffect(Kind.PARTICLE, particle, pattern, "");
    }

    public static CosmeticEffect glow(String colour) {
        return new CosmeticEffect(Kind.GLOW, "", "", colour);
    }

    /** Whether this is a cosmetic at all, or the absence of one. */
    public boolean isWorn() {
        return switch (this.kind) {
            case PARTICLE -> !this.particle.isBlank();
            case GLOW -> !this.colour.isBlank();
        };
    }

    /**
     * The families of cosmetic.
     *
     * <p>One per thing a player can wear at the same time, which is why they are a closed set
     * rather than free text: a player has one glow and one trail, and the backend has to be
     * able to replace the one without disturbing the other.
     */
    public enum Kind {

        /** Particles that follow the player around. */
        PARTICLE,

        /** The outline the player is drawn with, in a colour. */
        GLOW
    }
}
