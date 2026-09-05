package pl.landmc.cosmetics.paper;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.landmc.cosmetics.api.CosmeticChangedMessage;
import pl.landmc.cosmetics.api.CosmeticEffect;
import pl.landmc.cosmetics.api.CosmeticMessages;
import pl.landmc.cosmetics.api.CosmeticSnapshot;
import pl.landmc.cosmetics.api.CosmeticSnapshotRequest;
import pl.landmc.cosmetics.paper.config.CosmeticsConfig;
import pl.landmc.cosmetics.paper.listener.CosmeticListener;
import pl.landmc.platform.api.ModuleLifecycle;
import pl.landmc.platform.config.ConfigPlaceholders;
import pl.landmc.platform.config.ConfigService;
import pl.landmc.platform.messaging.MessageBus;
import pl.landmc.platform.messaging.PlayerPresence;
import pl.landmc.platform.messaging.message.MessageTarget;
import pl.landmc.platform.messaging.redis.RedisMessageTransport;
import pl.landmc.platform.messaging.serialization.MessageRegistry;
import pl.landmc.platform.messaging.serialization.MessageSerializer;
import pl.landmc.platform.messaging.transport.LocalMessageTransport;
import pl.landmc.platform.messaging.transport.MessageTransport;
import pl.landmc.platform.paper.scheduler.MainThreadExecutor;

/**
 * The side of cosmetics that draws, and decides nothing.
 *
 * <p>There is no command here and no catalogue. What a cosmetic costs, who owns it and who is
 * wearing it are all the shop's, because the shop is where the diamonds are; this server is
 * told and it draws. A backend that could also decide would be a second opinion about what
 * somebody paid for.
 *
 * <p>Every message arrives on a messaging worker and every Bukkit call has to happen on the main
 * thread, so each handler hands the work over before touching a player.
 */
public final class LandCosmeticsPaperPlugin extends JavaPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("landmc-cosmetics");

    private final ModuleLifecycle lifecycle = new ModuleLifecycle(LOGGER);
    private final CosmeticState state = new CosmeticState();

    private CosmeticsConfig config;
    private ParticleRenderer particles;
    private GlowRenderer glow;
    private WingRenderer wings;
    private MessageBus bus;

    @Override
    public void onEnable() {
        ConfigService configs =
                new ConfigService(ConfigPlaceholders.forPlugin(this.getDataFolder().toPath()));
        this.config = configs.load(
                this.getDataFolder().toPath(), "config.yml", CosmeticsConfig.class);

        this.glow = new GlowRenderer(this, this.state);
        this.wings = new WingRenderer(this, this.state);
        this.particles = new ParticleRenderer(this, this.config, this.state);
        this.particles.start();

        Executor mainThread = new MainThreadExecutor(this);

        this.bus = this.createBus();
        this.registerHandlers(mainThread);
        this.lifecycle.register(this.bus).enableAll();

        this.getServer().getPluginManager().registerEvents(
                new CosmeticListener(this, this.state, this.glow, this.wings), this);

        this.requestSnapshot(mainThread);

        LOGGER.info(
                "LandMC Cosmetics (backend) ready ({}, czasteczki {}).",
                this.config.messaging.enabled
                        ? "Redis"
                        : "no messaging - nothing will be worn here",
                this.particles.isEnabled() ? "wlaczone" : "wylaczone");
    }

    @Override
    public void onDisable() {
        if (this.particles != null) {
            this.particles.stop();
        }

        // Before the bus goes: a player left glowing is a player the next plugin to touch that
        // scoreboard has to guess about.
        if (this.glow != null) {
            for (Player player : this.getServer().getOnlinePlayers()) {
                player.setGlowing(false);
                this.glow.forget(player);
            }
        }

        // An armour stand nobody takes away is an armour stand still standing there next week.
        if (this.wings != null) {
            this.wings.removeAll();
        }

        this.lifecycle.disableAll();
    }

    private void registerHandlers(Executor mainThread) {
        this.bus.subscribe(CosmeticChangedMessage.class, (message, context) -> {
            // Remembered off the main thread, so somebody who connects in the meantime is
            // already known to be wearing it; only the drawing is handed over.
            this.state.put(message.playerId(), message.effect());

            mainThread.execute(() -> {
                Player player = this.getServer().getPlayer(message.playerId());
                if (player == null) {
                    return;
                }

                switch (message.effect().kind()) {
                    case GLOW -> this.glow.apply(player);
                    case WING -> this.wings.apply(player);
                    // Particles need nothing here: the draw pass reads the state each frame.
                    case PARTICLE -> { }
                    default -> { }
                }
            });
        });
    }

    /**
     * Asks the shop what everybody is wearing.
     *
     * <p>A broadcast only reaches servers that were running when it was sent, so a backend that
     * restarts would show everybody plain until they next changed something - and a cosmetic is
     * exactly the thing nobody changes twice in an evening. Failure is a warning rather than a
     * refusal to start: an empty lobby is better than no lobby.
     */
    private void requestSnapshot(Executor mainThread) {
        if (!this.config.messaging.enabled) {
            return;
        }

        this.bus.request(MessageTarget.broadcast(), new CosmeticSnapshotRequest(), CosmeticSnapshot.class)
                .thenAccept(snapshot -> mainThread.execute(() -> {
                    List<Map.Entry<UUID, CosmeticEffect>> entries =
                            new ArrayList<>(snapshot.worn().size());
                    for (CosmeticSnapshot.Entry entry : snapshot.worn()) {
                        entries.add(new AbstractMap.SimpleEntry<>(
                                entry.playerId(), entry.effect()));
                    }
                    this.state.replaceAll(entries);

                    for (Player player : this.getServer().getOnlinePlayers()) {
                        this.glow.apply(player);
                        this.wings.apply(player);
                    }

                    LOGGER.info("Cosmetic state received: {} worn.", snapshot.worn().size());
                }))
                .exceptionally(throwable -> {
                    LOGGER.warn(
                            "Nobody answered the cosmetic snapshot request ({});"
                                    + " players will look plain until they change something.",
                            throwable.getMessage());
                    return null;
                });
    }

    private MessageBus createBus() {
        MessageRegistry registry = CosmeticMessages.register(new MessageRegistry());
        MessageSerializer serializer = new MessageSerializer(registry);
        String serverId = this.config.serverId;

        MessageTransport transport;
        if (this.config.messaging.enabled) {
            transport = new RedisMessageTransport(
                    this.config.messaging.redis, serverId, serializer, LOGGER);
        }
        else {
            LOGGER.warn("Messaging is disabled - this server will never learn who wears what.");
            transport = new LocalMessageTransport(serverId);
        }

        return MessageBus.builder(serverId, transport, serializer, LOGGER)
                .playerPresence(PlayerPresence.NONE)
                .build();
    }
}
