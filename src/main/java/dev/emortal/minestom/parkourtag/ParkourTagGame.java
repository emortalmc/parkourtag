package dev.emortal.minestom.parkourtag;

import com.google.common.collect.Sets;
import dev.emortal.minestom.core.Environment;
import dev.emortal.minestom.gamesdk.config.GameCreationInfo;
import dev.emortal.minestom.gamesdk.game.Game;
import dev.emortal.minestom.parkourtag.utils.FireworkUtils;
import dev.emortal.tnt.TNTLoader;
import dev.emortal.tnt.source.FileTNTSource;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.firework.FireworkEffect;
import net.minestom.server.item.firework.FireworkEffectType;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static dev.emortal.minestom.parkourtag.ParkourTagModule.SPAWN_POSITION_MAP;

public class ParkourTagGame extends Game {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParkourTagGame.class);
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Pos SPAWN_POINT = new Pos(0.5, 65.0, 0.5);

    private static final Team TAGGER_TEAM = MinecraftServer.getTeamManager().createBuilder("taggers")
            .teamColor(NamedTextColor.RED)
            .updateTeamPacket()
            .build();
    private static final Team GOONS_TEAM = MinecraftServer.getTeamManager().createBuilder("goons")
            .teamColor(NamedTextColor.GREEN)
            .nameTagVisibility(TeamsPacket.NameTagVisibility.HIDE_FOR_OTHER_TEAMS)
            .updateTeamPacket()
            .build();
    private static final Team DEAD_TEAM = MinecraftServer.getTeamManager().createBuilder("dead")
            .teamColor(NamedTextColor.GRAY)
            .prefix(Component.text("☠ ", NamedTextColor.GRAY))
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .updateTeamPacket()
            .build();


    public static final int MIN_PLAYERS = 2;

    private static final List<String> DEATH_MESSAGES = List.of(
            "<victim> was found by <tagger>",
            "<victim> failed the jump",
            "<tagger> was too fast for <victim>",
            "<victim> isn't very good at hide and seek",
            "<victim> didn't realise the game started",
            "<victim> was cornered by <tagger>",
            "<victim> walked into a wall",
            "<tagger> got too close to <victim>",
            "<victim> should play more Marathon",
            "<victim> skipped leg day",
            "<victim> failed <tagger>'s expectations"
    );


    private @NotNull Instance instance;
    private EventNode<InstanceEvent> eventNode;
    private @NotNull Set<Player> players = Sets.newConcurrentHashSet();
    private Audience audience = Audience.audience(players);

    private Task gameBeginTask;
    private BossBar bossBar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.PINK, BossBar.Overlay.PROGRESS);

    private @Nullable Task gameTimerTask;

    protected ParkourTagGame(@NotNull GameCreationInfo creationInfo, @NotNull EventNode<Event> parentEventNode) {
        super(creationInfo);
        EventNode<Event> gameEventNode = EventNode.all(UUID.randomUUID().toString());
        parentEventNode.addChild(gameEventNode);

        CompletableFuture<Void> worldLoadFuture = CompletableFuture.runAsync(() -> {
            this.instance = this.createInstance();
        });

        parentEventNode.addListener(PlayerLoginEvent.class, event -> {
            worldLoadFuture.join();

            Player player = event.getPlayer();
            if (!creationInfo.playerIds().contains(event.getPlayer().getUuid())) {
                player.kick("Unexpected join (" + Environment.getHostname() + ")");
                LOGGER.info("Unexpected join for player {}", player.getUuid());
                return;
            }

            player.setRespawnPoint(SPAWN_POINT);
            event.setSpawningInstance(this.instance);
            this.players.add(player);

            player.setAutoViewable(true);
            player.setTeam(null);
            player.setGlowing(false);
            player.setGameMode(GameMode.ADVENTURE);
            player.showBossBar(this.bossBar);
        });

        worldLoadFuture.join();

        this.eventNode = this.instance.eventNode();
        // world loading takes some time so players might have already joined
//        for (Player player : this.players) {
//            player.setInstance(this.instance, SPAWN_POINT);
//        }

        // TODO: event node needed here
        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, event -> {
            if (this.players.remove(event.getPlayer())) this.checkPlayerCounts();
        });

        this.gameBeginTask = this.instance.scheduler().submitTask(new Supplier<>() {
            int i = 10;

            @Override
            public TaskSchedule get() {
                if (i == 0) {
                    if (players.size() >= MIN_PLAYERS) {
                        start();
                    }

                    return TaskSchedule.stop();
                }

                bossBar.name(Component.text("Waiting for players (" + i + "s)"));

                i--;

                return TaskSchedule.seconds(1);
            }

        });
    }

    private Instance createInstance() {
        LOGGER.info("Loading instance for game");
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setTimeRate(0);
        instance.setTimeUpdate(null);

        LOGGER.info("Setting chunk loader");
        try {
            instance.setChunkLoader(new TNTLoader(new FileTNTSource(Path.of("city.tnt"))));
        } catch (IOException | NBTException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Returning instance for game");

//        int chunkRadius = 5;
//        for (int x = -chunkRadius; x < chunkRadius; x++) {
//            for (int y = -chunkRadius; y < chunkRadius; y++) {
//                instance.loadChunk(x, y);
//            }
//        }

        return instance;
    }

    private void start() {
        for (Player player : players) {
            player.hideBossBar(bossBar);
        }

        audience.playSound(Sound.sound(SoundEvent.BLOCK_PORTAL_TRIGGER, Sound.Source.MASTER, 0.45f, 1.27f));

        instance.scheduler().submitTask(new Supplier<>() {
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
                                beginGame(player);
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

    private void beginGame(Player tagger) {
        tagger.setTeam(TAGGER_TEAM);
        tagger.setGlowing(true);

        int goonsLeft = players.size() - 1;
        bossBar.name(
                Component.text()
                        .append(Component.text(goonsLeft, TextColor.fromHexString("#cdffc4"), TextDecoration.BOLD)) // assumes only one tagger
                        .append(Component.text(goonsLeft == 1 ? " goon remaining" : " goons remaining", TextColor.fromHexString("#8fff82")))
                        .build()
        );
        bossBar.color(BossBar.Color.GREEN);

        beginTimer();

        for (Player player : players) {
            player.showBossBar(bossBar);

            if (player.getUuid() == tagger.getUuid()) {
                player.teleport(SPAWN_POSITION_MAP.get("city").tagger.asPos());
                continue;
            }

            player.setTeam(GOONS_TEAM);

            player.teleport(SPAWN_POSITION_MAP.get("city").goon.asPos());
        }


        this.instance.eventNode().addListener(EntityAttackEvent.class, e -> {
            if (e.getEntity().getUuid() != tagger.getUuid()) return;
            if (e.getEntity().getEntityType() != EntityType.PLAYER) return;
            if (e.getTarget().getEntityType() != EntityType.PLAYER) return;

            Player attacker = (Player) e.getEntity();
            Player target = (Player) e.getTarget();

            if (attacker.getGameMode() != GameMode.ADVENTURE) return;
            if (target.getGameMode() != GameMode.ADVENTURE) return;

            attacker.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());
            target.showTitle(
                    Title.title(
                            Component.text("YOU DIED", NamedTextColor.RED),
                            Component.empty(),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(700), Duration.ofMillis(400))
                    )
            );

            target.setGameMode(GameMode.SPECTATOR);
            target.setAutoViewable(false);
            target.setGlowing(false);
            target.setTeam(DEAD_TEAM);

            // Pick a random death message
            ThreadLocalRandom random = ThreadLocalRandom.current();
            audience.sendMessage(MINI_MESSAGE.deserialize(
                    "<gray>" +
                            DEATH_MESSAGES.get(random.nextInt(DEATH_MESSAGES.size())),
                    Placeholder.component("victim", Component.text(target.getUsername(), NamedTextColor.RED)),
                    Placeholder.component("tagger", Component.text(tagger.getUsername(), NamedTextColor.WHITE))
            ));

            target.setVelocity(attacker.getPosition().direction().mul(15.0));

            // Firework death effect
            FireworkEffect randomColorEffect = new FireworkEffect(
                    false,
                    false,
                    FireworkEffectType.LARGE_BALL,
                    List.of(new Color(java.awt.Color.HSBtoRGB(random.nextFloat(), 1f, 1f))),
                    List.of()
            );
            FireworkUtils.showFirework(players, instance, target.getPosition().add(0, 1.5, 0), List.of(randomColorEffect));

            // Check for win with new alive count
            checkPlayerCounts();
        });
    }

    private void beginTimer() {
        this.gameTimerTask = instance.scheduler().submitTask(new Supplier<>() {
            final int startingSecondsLeft = 60; // TODO: Make this dynamic
            int secondsLeft = startingSecondsLeft;

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

                if (secondsLeft == startingSecondsLeft / 2) {
                    audience.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1f, 1.5f), Sound.Emitter.self());
                    audience.sendMessage(
                            Component.text()
                                    .append(Component.text(secondsLeft, NamedTextColor.WHITE))
                                    .append(Component.text(" seconds left!", NamedTextColor.GRAY))
                    );
                }

                bossBar.progress((float) secondsLeft / (float) startingSecondsLeft);

                secondsLeft--;
                return TaskSchedule.seconds(1);
            }
        });
    }

    private void checkPlayerCounts() {
        Set<Player> goons = getGoons();
        Set<Player> taggers = getTaggers();

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
                        .append(Component.text(goons.size(), TextColor.fromHexString("#cdffc4"), TextDecoration.BOLD)) // assumes only one tagger
                        .append(Component.text(goons.size() == 1 ? " goon remaining" : " goons remaining", TextColor.fromHexString("#8fff82")))
                        .build()
        );
    }

    private void victory(Set<Player> winners) {
        if (gameTimerTask != null) gameTimerTask.cancel();

        Title victoryTitle = Title.title(
                MINI_MESSAGE.deserialize("<gradient:#ffc570:gold><bold>VICTORY!"),
                Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(4))
        );
        Title defeatTitle = Title.title(
                MINI_MESSAGE.deserialize("<gradient:#ff474e:#ff0d0d><bold>DEFEAT!"),
                Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(4))
        );

        for (Player player : players) {
            player.hideBossBar(bossBar);

            if (winners.contains(player)) {
                player.showTitle(victoryTitle);
            } else {
                player.showTitle(defeatTitle);
            }
        }

        instance.scheduler().buildTask(this::destroy).delay(TaskSchedule.seconds(6)).schedule();
    }

    @Override
    public @NotNull Set<UUID> getPlayers() {
        Set<UUID> uuids = new HashSet<>();
        for (Player player : this.players) {
            uuids.add(player.getUuid());
        }
        return uuids;
    }

    public @NotNull Set<Player> getGoons() {
        Set<Player> goons = Sets.newConcurrentHashSet();
        for (Player player : this.players) {
            if (player.getTeam() == null) continue;
            if (player.getTeam().getTeamName().equals(GOONS_TEAM.getTeamName())) {
                goons.add(player);
            }
        }
        return goons;
    }

    public @NotNull Set<Player> getTaggers() {
        Set<Player> goons = Sets.newConcurrentHashSet();
        for (Player player : players) {
            if (player.getTeam().getTeamName().equals(TAGGER_TEAM.getTeamName())) {
                goons.add(player);
            }
        }
        return goons;
    }

    // TODO rework cancel system
    @Override
    public void cancel() {
        LOGGER.warn("Game cancelled");
        destroy();
    }

    public void destroy() {

        // TODO: Players should be removed here

        //MinecraftServer.getInstanceManager().unregisterInstance(instance);
        MinecraftServer.getBossBarManager().destroyBossBar(bossBar);
        this.gameBeginTask.cancel();
        if (this.gameTimerTask != null) this.gameTimerTask.cancel();
    }

    @Override
    public void fastStart() {
        gameBeginTask.cancel();
        start();
    }
}
