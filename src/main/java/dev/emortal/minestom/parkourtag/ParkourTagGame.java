package dev.emortal.minestom.parkourtag;

import com.google.common.collect.Sets;
import dev.emortal.api.kurushimi.KurushimiUtils;
import dev.emortal.minestom.core.Environment;
import dev.emortal.minestom.gamesdk.GameSdkModule;
import dev.emortal.minestom.gamesdk.config.GameCreationInfo;
import dev.emortal.minestom.gamesdk.game.Game;
import dev.emortal.minestom.parkourtag.listeners.AttackListener;
import dev.emortal.minestom.parkourtag.listeners.DoubleJumpListener;
import dev.emortal.minestom.parkourtag.listeners.TickListener;
import dev.emortal.tnt.TNTLoader;
import dev.emortal.tnt.source.FileTNTSource;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.AreaEffectCloudMeta;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static dev.emortal.minestom.parkourtag.ParkourTagModule.SPAWN_POSITION_MAP;

public class ParkourTagGame extends Game {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParkourTagGame.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Pos SPAWN_POINT = new Pos(0.5, 65.0, 0.5);

    public static final Team TAGGER_TEAM = MinecraftServer.getTeamManager().createBuilder("taggers")
            .teamColor(NamedTextColor.RED)
            .nameTagVisibility(TeamsPacket.NameTagVisibility.ALWAYS)
            .updateTeamPacket()
            .build();
    public static final Team GOONS_TEAM = MinecraftServer.getTeamManager().createBuilder("goons")
            .teamColor(NamedTextColor.GREEN)
            .nameTagVisibility(TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS)
            .updateTeamPacket()
            .build();
    public static final Team DEAD_TEAM = MinecraftServer.getTeamManager().createBuilder("dead")
            .teamColor(NamedTextColor.GRAY)
            .prefix(Component.text("☠ ", NamedTextColor.GRAY))
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .updateTeamPacket()
            .build();

    public static final int MIN_PLAYERS = 2;

    private final Set<Player> taggers = Sets.newConcurrentHashSet();
    private final Set<Player> goons = Sets.newConcurrentHashSet();

    public final @NotNull Instance instance;

    private boolean allowHitPlayers = false;

    private final BossBar bossBar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.PINK, BossBar.Overlay.PROGRESS);

    private @Nullable Task gameTimerTask;

    protected ParkourTagGame(@NotNull GameCreationInfo creationInfo, @NotNull EventNode<Event> gameEventNode) {
        super(creationInfo);
        this.instance = this.createInstance();

        gameEventNode.addListener(PlayerDisconnectEvent.class, event -> {
            if (this.players.remove(event.getPlayer())) this.checkPlayerCounts();
        });
    }

    @Override
    public void onPlayerLogin(@NotNull PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (!getGameCreationInfo().playerIds().contains(player.getUuid())) {
            player.kick("Unexpected join (" + Environment.getHostname() + ")");
            LOGGER.info("Unexpected join for player {}", player.getUuid());
            return;
        }

        player.setRespawnPoint(SPAWN_POINT);
        event.setSpawningInstance(this.instance);
        this.players.add(player);

        player.setFlying(false);
        player.setAllowFlying(false);
        player.setAutoViewable(true);
        player.setTeam(null);
        player.setGlowing(false);
        player.setGameMode(GameMode.ADVENTURE);
        player.showBossBar(this.bossBar);
    }

    private Instance createInstance() {
        LOGGER.info("Loading instance for game");
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setTimeRate(0);
        instance.setTimeUpdate(null);

        try {
            instance.setChunkLoader(new TNTLoader(new FileTNTSource(Path.of("city.tnt"))));
        } catch (IOException | NBTException e) {
            throw new RuntimeException(e);
        }

        return instance;
    }

    public void start() {
        for (Player player : this.players) {
            player.hideBossBar(this.bossBar);
        }

        this.audience.playSound(Sound.sound(SoundEvent.BLOCK_PORTAL_TRIGGER, Sound.Source.MASTER, 0.45f, 1.27f));

        this.instance.scheduler().submitTask(new Supplier<>() {
            int i = 3;

            @Override
            public TaskSchedule get() {
                if (i == 0) {
                    pickTagger();
                    return TaskSchedule.stop();
                }

                audience.playSound(Sound.sound(Key.key("battle.countdown.begin"), Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());

                audience.showTitle(
                        Title.title(
                                Component.empty(),
                                Component.text(i, NamedTextColor.LIGHT_PURPLE),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
                        )
                );

                i--;
                return TaskSchedule.seconds(1);
            }
        });
    }

    private void pickTagger() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        this.instance.scheduler().submitTask(new Supplier<>() {
            int nameIter = 15;
            int offset = random.nextInt(players.size());

            @Override
            public TaskSchedule get() {
                Iterator<Player> iterator = players.iterator();
                int playerIndex = (nameIter + offset) % players.size();
                for (int playerIter = 0; playerIter < playerIndex; playerIter++) {
                    iterator.next();
                }
                Player player = iterator.next();

                if (nameIter == 0) {

                    audience.playSound(Sound.sound(SoundEvent.ENTITY_ENDER_DRAGON_GROWL, Sound.Source.MASTER, 0.8f, 1f), Sound.Emitter.self());

                    // fancy rainbow name animation
                    instance.scheduler().submitTask(new Supplier<>() {
                        int i = 0;

                        @Override
                        public TaskSchedule get() {
                            if (i >= 30) {
                                // finally begin the game!!
                                player.setTeam(TAGGER_TEAM);
                                taggers.add(player);

                                beginGame();
                                return TaskSchedule.stop();
                            }

                            audience.showTitle(
                                    Title.title(
                                            MINI_MESSAGE.deserialize("<rainbow:" + i + ">" + player.getUsername()),
                                            Component.text("is the tagger", NamedTextColor.GRAY),
                                            Title.Times.times(
                                                    Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(500)
                                            )
                                    )
                            );
                            i++;
                            return TaskSchedule.tick(2);
                        }
                    });

                    return TaskSchedule.stop();
                }

                audience.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1f, 1.5f));

                audience.showTitle(
                        Title.title(
                                Component.text(player.getUsername()),
                                Component.text("is the tagger", NamedTextColor.GRAY),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ofMillis(500))
                        )
                );

                nameIter--;

                return TaskSchedule.tick(Math.max(1, (int) ((17 - nameIter) / 1.2)));
            }
        });
    }

    private void beginGame() {
        Set<Player> taggers = getTaggers();

        int goonsLeft = this.players.size() - 1;
        this.bossBar.name(
                Component.text()
                        .append(Component.text(goonsLeft, TextColor.fromHexString("#cdffc4"), TextDecoration.BOLD)) // assumes only one tagger
                        .append(Component.text(goonsLeft == 1 ? " goon remaining" : " goons remaining", TextColor.fromHexString("#8fff82")))
                        .build()
        );
        this.bossBar.color(BossBar.Color.GREEN);

        beginTimer();

        var holderEntity = new Entity(EntityType.AREA_EFFECT_CLOUD);
        ((AreaEffectCloudMeta) holderEntity.getEntityMeta()).setRadius(0f);
        holderEntity.setInstance(instance, SPAWN_POSITION_MAP.get("city").tagger.asPos()).thenRun(() -> {
            for (Player tagger : taggers) {
                holderEntity.addPassenger(tagger);
                tagger.setGlowing(true);
                tagger.teleport(SPAWN_POSITION_MAP.get("city").tagger.asPos());
                tagger.updateViewerRule((entity) -> entity.getEntityId() == holderEntity.getEntityId());
            }

            instance.scheduler().buildTask(() -> {
                allowHitPlayers = true;

                audience.sendActionBar(Component.text("The tagger has been released!", NamedTextColor.GOLD));

                for (Player tagger : taggers) {
                    tagger.updateViewerRule((entity) -> true);
                }
                holderEntity.remove();
            }).delay(TaskSchedule.seconds(7)).schedule();
        });



        for (Player player : this.players) {
            player.showBossBar(this.bossBar);

            if (player.getTeam() == null) { // if player is not tagger
                player.teleport(SPAWN_POSITION_MAP.get("city").goon.asPos());
                player.setTeam(GOONS_TEAM);
                goons.add(player);
            }
        }

        EventNode<InstanceEvent> eventNode = instance.eventNode();
        AttackListener.registerListener(eventNode, this);
        TickListener.registerListener(eventNode, this);
        DoubleJumpListener.registerListener(eventNode, this);
    }

    private void beginTimer() {
        int playTime = 300 / (12 - players.size());
        int glowing = 15 + ((players.size() * 15) / 8);
        int doubleJump = glowing / 2;

        this.gameTimerTask = this.instance.scheduler().submitTask(new Supplier<>() {
            int secondsLeft = playTime;

            @Override
            public TaskSchedule get() {
                if (secondsLeft == 0) {
                    // Tagger has run out of time
                    victory(getGoons());

                    return TaskSchedule.stop();
                }

                if (secondsLeft <= 10) {
                    audience.playSound(Sound.sound(Key.key("minecraft:battle.showdown.count" + (secondsLeft % 2 + 1)), Sound.Source.MASTER, 0.7f, 1f), Sound.Emitter.self());
                    audience.showTitle(
                            Title.title(
                                    Component.empty(),
                                    Component.text(secondsLeft, TextColor.lerp(secondsLeft / 10f, NamedTextColor.RED, NamedTextColor.GREEN)),
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                            )
                    );
                }

                if (secondsLeft == doubleJump) {
                    audience.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1f, 1.5f), Sound.Emitter.self());
                    audience.showTitle(
                            Title.title(
                                    Component.empty(),
                                    Component.text("The tagger can now double jump!", NamedTextColor.GRAY),
                                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(2))
                            )
                    );

                    for (Player tagger : taggers) {
                        tagger.setAllowFlying(true);
                    }
                }
                if (secondsLeft == glowing) {
                    audience.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1f, 1.5f), Sound.Emitter.self());
                    audience.showTitle(
                            Title.title(
                                    Component.empty(),
                                    Component.text("Goons are now glowing!", NamedTextColor.GRAY),
                                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(2))
                            )
                    );

                    for (Player goon : goons) {
                        goon.setGlowing(true);
                    }
                }

                bossBar.progress((float) secondsLeft / (float) playTime);

                secondsLeft--;
                return TaskSchedule.seconds(1);
            }
        });
    }

    public void checkPlayerCounts() {
        if (goons.size() == 0) {
            victory(taggers);
            return;
        }
        if (taggers.size() == 0) {
            victory(goons);
            return;
        }

        bossBar.name(
                Component.text()
                        .append(Component.text(goons.size(), TextColor.fromHexString("#cdffc4"), TextDecoration.BOLD))
                        .append(Component.text(goons.size() == 1 ? " goon remaining" : " goons remaining", TextColor.fromHexString("#8fff82")))
                        .build()
        );
    }

    private void victory(Set<Player> winners) {
        allowHitPlayers = false;

        if (gameTimerTask != null) gameTimerTask.cancel();

        Title victoryTitle = Title.title(
                MINI_MESSAGE.deserialize("<gradient:#ffc570:gold><bold>VICTORY!"),
                Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        );
        Title defeatTitle = Title.title(
                MINI_MESSAGE.deserialize("<gradient:#ff474e:#ff0d0d><bold>DEFEAT!"),
                Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(3))
        );

        Sound defeatSound = Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.MASTER, 1f, 1f);
        Sound victorySound = Sound.sound(SoundEvent.BLOCK_BEACON_POWER_SELECT, Sound.Source.MASTER, 1f, 0.8f);

        for (Player player : players) {
            player.hideBossBar(bossBar);

            if (winners.contains(player)) {
                player.showTitle(victoryTitle);
                player.playSound(victorySound, Sound.Emitter.self());
            } else {
                player.showTitle(defeatTitle);
                player.playSound(defeatSound, Sound.Emitter.self());
            }
        }

        instance.scheduler().buildTask(this::sendBackToLobby)
                .delay(TaskSchedule.seconds(6))
                .schedule();
    }

    public @NotNull Set<Player> getGoons() {
        return goons;
    }

    public @NotNull Set<Player> getTaggers() {
        return taggers;
    }

    public boolean canHitPlayers() {
        return allowHitPlayers;
    }

    // TODO rework cancel system
    @Override
    public void cancel() {
        LOGGER.warn("Game cancelled");
        sendBackToLobby();
    }

    private void sendBackToLobby() {
        for (final Player player : players) {
            player.setTeam(null);
        }
        KurushimiUtils.sendToLobby(players, this::removeGame, this::removeGame);
    }

    private void removeGame() {
        GameSdkModule.getGameManager().removeGame(this);
        cleanUp();
    }

    private void cleanUp() {
        for (final Player player : this.players) {
            player.kick(Component.text("The game ended but we weren't able to connect you to a lobby. Please reconnect", NamedTextColor.RED));
        }
        MinecraftServer.getInstanceManager().unregisterInstance(this.instance);
        MinecraftServer.getBossBarManager().destroyBossBar(this.bossBar);
        if (this.gameTimerTask != null) this.gameTimerTask.cancel();
    }
}
