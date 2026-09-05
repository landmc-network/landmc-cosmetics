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
 *
 * <p>Each family can be switched off separately, because the answer differs by server: a lobby
 * wants all of it, and a server where people are fighting each other does not want a pet
 * wandering into a fight or a line of text hiding what is behind it.
 */
public class CosmeticsConfig extends OkaeriConfig {

    @Comment("Identyfikator tej instancji na szynie wiadomosci. Musi byc unikalny w sieci.")
    @CustomKey("server-id")
    public String serverId = "lobby-1";

    @Comment("")
    public ParticlesSection particles = new ParticlesSection();

    @Comment("")
    public WingsSection wings = new WingsSection();

    @Comment("")
    public StatusesSection statuses = new StatusesSection();

    @Comment("")
    public PetsSection pets = new PetsSection();

    @Comment("")
    public TitlesSection titles = new TitlesSection();

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

    public static class WingsSection extends OkaeriConfig {

        @Comment("Czy skrzydla sa zakladane na tym serwerze.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Gdzie siedzi model wzgledem miejsca, w ktorym jedzie na graczu.")
        @Comment("To jedyna rzecz tutaj, ktorej nie da sie ustalic inaczej niz patrzac:")
        @Comment("model zrobiony pod montaz innego pluginu siedzi tam, gdzie tamten go")
        @Comment("postawil, a w pliku modelu nie ma o tym ani slowa.")
        @Comment("Y w dol to wartosc ujemna, Z do tylu to wartosc ujemna.")
        @CustomKey("offset-x")
        public double offsetX = 0.0D;

        @CustomKey("offset-y")
        public double offsetY = -0.75D;

        @CustomKey("offset-z")
        public double offsetZ = -0.25D;

        @Comment("")
        @Comment("Skala modelu. 1.0 to rozmiar, w jakim zostal narysowany.")
        public double scale = 1.0D;
    }

    public static class StatusesSection extends OkaeriConfig {

        @Comment("Czy statusy sa pokazywane nad glowami na tym serwerze.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Ile nad punktem, w ktorym tekst jedzie na graczu. Domyslnie tuz nad nickiem;")
        @Comment("za nisko i status przykryje nick, za wysoko i odklei sie od gracza.")
        @CustomKey("offset-y")
        public double offsetY = 1.15D;

        @Comment("")
        @Comment("Skala tekstu. 1.0 to rozmiar nicku.")
        public double scale = 0.9D;

        @Comment("")
        @Comment("Przezroczystosc tla, 0-255. Zero to sam tekst bez ciemnego prostokata.")
        @CustomKey("background-opacity")
        public int backgroundOpacity = 90;

        @Comment("")
        @Comment("Z jakiej odleglosci widac status, jako mnoznik zasiegu domyslnego.")
        @CustomKey("view-range")
        public double viewRange = 0.6D;
    }

    public static class PetsSection extends OkaeriConfig {

        @Comment("Czy pupile sa przywolywane na tym serwerze. Na serwerze, gdzie sie walczy,")
        @Comment("zwierze latajace obok gracza przeszkadza bardziej, niz cieszy.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Gdzie pupil leci wzgledem gracza, w blokach: w bok, do tylu i w gore.")
        @CustomKey("side-offset")
        public double sideOffset = 0.9D;

        @CustomKey("back-offset")
        public double backOffset = 0.5D;

        public double height = 0.9D;

        @Comment("")
        @Comment("Jak szybko pupil dogania swoje miejsce, 0-1. Jeden to sztywne przyklejenie,")
        @Comment("mniej znaczy, ze zostaje z tylu na zakretach - i wtedy wyglada jak zwierze,")
        @Comment("a nie jak przyczepiony model.")
        public double smoothing = 0.35D;

        @Comment("")
        @Comment("Powyzej ilu blokow od swojego miejsca pupil jest przenoszony od razu,")
        @Comment("zamiast lecieć przez pol mapy. Teleport gracza to wlasnie ten przypadek.")
        @CustomKey("catch-up-distance")
        public double catchUpDistance = 12.0D;

        @Comment("")
        @Comment("Wysokosc i tempo bujania sie w miejscu. Zero wylacza.")
        @CustomKey("bob-height")
        public double bobHeight = 0.12D;

        @Comment("")
        @Comment("Czy pupil zostawia za soba czasteczki, i co ile tickow.")
        public boolean particles = true;

        @CustomKey("particle-interval-ticks")
        public long particleIntervalTicks = 4L;
    }

    public static class TitlesSection extends OkaeriConfig {

        @Comment("Czy tytuly sa doklejane do nicku na tym serwerze.")
        public boolean enabled = true;

        @Comment("")
        @Comment("Jak tytul jest opakowany. {TITLE} to sam tytul ze sklepu.")
        @Comment("Czyta to landmc-chat - ten plugin tylko oznacza gracza gotowym tekstem,")
        @Comment("zeby czat i tablista nie musialy wiedziec, ze dodatki w ogole istnieja.")
        public String format = "<dark_gray>[{TITLE}<dark_gray>] ";
    }

    public static class MessagingSection extends OkaeriConfig {

        @Comment("Bez tego ten serwer nie dowie sie, kto co nosi - dodatki nie zadzialaja tutaj.")
        public boolean enabled = true;

        public RedisConfig redis = new RedisConfig();
    }
}
