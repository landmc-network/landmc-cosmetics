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
 * <p>Most of them are blank for any given cosmetic, which is the price of one record instead of
 * a hierarchy of them: a sealed interface per family would serialise the same bytes and cost a
 * type to add every time somebody adds a way to be shown off.
 *
 * @param kind which family it belongs to, and therefore which of the fields below mean anything
 * @param particle the particle to draw, for {@link Kind#PARTICLE} and the trail behind a pet
 * @param pattern how to arrange it - a helix, a ring, a cloud - and, for
 *     {@link Kind#CHEST}, which choreography a crate opens with
 * @param colour the glow colour, for {@link Kind#GLOW}, as a named Minecraft colour
 * @param material the item a worn model is carried on, for {@link Kind#WING}
 * @param modelData which model of that item to draw, for {@link Kind#WING}
 * @param entity what a pet is, for {@link Kind#PET}, as a Minecraft entity type
 * @param text the words shown, for {@link Kind#STATUS} and for a pet's name
 */
public record CosmeticEffect(
        Kind kind,
        String particle,
        String pattern,
        String colour,
        String material,
        int modelData,
        String entity,
        String text) {

    public CosmeticEffect {
        Objects.requireNonNull(kind, "kind");
        particle = particle == null ? "" : particle;
        pattern = pattern == null ? "" : pattern;
        colour = colour == null ? "" : colour;
        material = material == null ? "" : material;
        entity = entity == null ? "" : entity;
        text = text == null ? "" : text;
    }

    /** Nothing worn. Sent when a player takes a cosmetic off, so the backend clears it. */
    public static CosmeticEffect none(Kind kind) {
        return new CosmeticEffect(kind, "", "", "", "", 0, "", "");
    }

    public static CosmeticEffect particle(String particle, String pattern) {
        return new CosmeticEffect(Kind.PARTICLE, particle, pattern, "", "", 0, "", "");
    }

    public static CosmeticEffect glow(String colour) {
        return new CosmeticEffect(Kind.GLOW, "", "", colour, "", 0, "", "");
    }

    /**
     * A model worn on the back.
     *
     * <p>An item and a number rather than a name of a model, because that is what a client is
     * actually told: the pack maps a material and a custom model value onto a model, and this
     * side of the network has no way to name one directly.
     */
    public static CosmeticEffect wing(String material, int modelData) {
        return new CosmeticEffect(Kind.WING, "", "", "", material, modelData, "", "");
    }

    /**
     * The choreography a magic chest opens with.
     *
     * <p>A name and nothing else, which is the one place this module steps away from sending
     * the whole effect. A trail is three strings and a backend that has never heard of it can
     * still draw it; an opening is a minute of movement, sound and timing that no record of
     * loose fields would describe without becoming a scripting language. So the skyblock server
     * owns what each one looks like and this carries only which one was chosen - the shop still
     * holds the single catalogue of what exists and what it costs.
     *
     * @param style the identifier the drawing server knows the choreography by
     */
    public static CosmeticEffect chest(String style) {
        return new CosmeticEffect(Kind.CHEST, "", style, "", "", 0, "", "");
    }

    /** A line of text floating above the player's head. */
    public static CosmeticEffect status(String text) {
        return new CosmeticEffect(Kind.STATUS, "", "", "", "", 0, "", text);
    }

    /**
     * A small creature that follows the player about.
     *
     * <p>A real animal rather than a floating model, because the animals are already in the
     * game and a pack would have to draw a bee that everybody already knows the look of.
     */
    public static CosmeticEffect pet(String entity, String text, String particle) {
        return new CosmeticEffect(Kind.PET, particle, "", "", "", 0, entity, text);
    }

    /** Whether this is a cosmetic at all, or the absence of one. */
    public boolean isWorn() {
        return switch (this.kind) {
            case PARTICLE -> !this.particle.isBlank();
            case GLOW -> !this.colour.isBlank();
            case WING -> !this.material.isBlank() && this.modelData > 0;
            case STATUS -> !this.text.isBlank();
            case PET -> !this.entity.isBlank();
            case CHEST -> !this.pattern.isBlank();
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
        GLOW,

        /** A model carried behind the player. */
        WING,

        /** A line of text above the player's head. */
        STATUS,

        /** A creature following the player around. */
        PET,

        /**
         * The way a magic chest opens for this player.
         *
         * <p>Not drawn on the player and not visible in a lobby, unlike the rest, but worn in
         * the sense that matters here: one at a time, bought with diamonds, replaced by putting
         * on another. Only the server with the chests on it acts on this one.
         */
        CHEST
    }
}
