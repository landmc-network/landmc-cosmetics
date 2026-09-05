package pl.landmc.cosmetics.paper.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import pl.landmc.platform.messaging.redis.RedisConfig;

/**
 * {@code config.yml} for the side that draws.
 *
 * <p>No catalogue here on purpose. What a cosmetic is called, what it costs and which particle
 * it draws are all decided where it is bought, and travel with the message - so this server
 * needs no list of cosmetics and cannot disagree with one. What is left is how often to draw
 * and how far, which are properties of this server rather than of any cosmetic.
 */
public class CosmeticsConfig extends OkaeriConfig {

    @Comment("Identyfikator tej instancji na szynie wiadomosci. Musi byc unikalny w sieci.")
    @CustomKey("server-id")
    public String serverId = "lobby-1";

    @Comment("")
    public ParticlesSection particles = new ParticlesSection();

    @Comment("")
    public MessagingSection messaging = new MessagingSection();

    public static class ParticlesSection extends OkaeriConfig {

        @Comment("Czy czasteczki sa rysowane. Wylaczone = gracze nadal je maja, tylko ich nie")
        @Comment("widac na tym serwerze - np. na serwerze gry, gdzie tylko przeszkadzaja.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Co ile tickow rysowana jest kolejna klatka. Nizej znaczy plynniej i drozej;")
        @Comment("dwa ticki to dziesiec klatek na sekunde, czego oko nie odroznia od gladkiego,")
        @Comment("a jest pieciokrotnie tansze niz co tick.")
        @CustomKey("interval-ticks")
        public long intervalTicks = 2L;

        @Comment("")
        @Comment("Ile punktow rysuje jeden gracz na klatke. To jest ta liczba, ktora mnozy sie")
        @Comment("przez liczbe graczy - przy stu noszacych czasteczki kazdy punkt wiecej to sto")
        @Comment("czasteczek na klatke.")
        @CustomKey("points-per-frame")
        public int pointsPerFrame = 3;

        @Comment("")
        @Comment("Promien i wysokosc wzoru wokol gracza, w blokach.")
        public double radius = 0.75D;
        public double height = 1.7D;

        @Comment("")
        @Comment("Z jakiej odleglosci widac czasteczki, w blokach. Czasteczka wyslana graczowi,")
        @Comment("ktory jej nie zobaczy, to pakiet wyslany po nic.")
        @CustomKey("view-distance")
        public double viewDistance = 32.0D;
    }

    public static class MessagingSection extends OkaeriConfig {

        @Comment("Bez tego ten serwer nie dowie sie, kto co nosi - dodatki nie zadzialaja tutaj.")
        public boolean enabled = true;

        public RedisConfig redis = new RedisConfig();
    }
}
