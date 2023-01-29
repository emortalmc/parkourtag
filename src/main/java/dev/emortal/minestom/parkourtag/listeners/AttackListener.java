package dev.emortal.minestom.parkourtag.listeners;

import dev.emortal.minestom.parkourtag.ParkourTagGame;
import dev.emortal.minestom.parkourtag.utils.FireworkUtils;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import net.minestom.server.color.Color;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.item.firework.FireworkEffect;
import net.minestom.server.item.firework.FireworkEffectType;
import net.minestom.server.sound.SoundEvent;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class AttackListener {

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

    public static void registerListener(EventNode<InstanceEvent> eventNode, ParkourTagGame game) {
        Set<Player> taggers = game.getTaggers();

        eventNode.addListener(EntityAttackEvent.class, e -> {
            if (!game.canHitPlayers()) return;
            if (!taggers.contains(e.getEntity())) return;
            if (e.getEntity().getEntityType() != EntityType.PLAYER) return;
            if (e.getTarget().getEntityType() != EntityType.PLAYER) return;

            Player attacker = (Player) e.getEntity();
            Player target = (Player) e.getTarget();

            if (attacker.getGameMode() != GameMode.ADVENTURE) return;
            if (target.getGameMode() != GameMode.ADVENTURE) return;

            attacker.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());
            target.showTitle(
                    Title.title(
                            Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                            Component.empty(),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(500))
                    )
            );

            target.setGameMode(GameMode.SPECTATOR);
            target.setAutoViewable(false);
            target.setGlowing(false);

            game.getTaggers().remove(target);
            game.getGoons().remove(target);
            target.setTeam(ParkourTagGame.DEAD_TEAM);

            // Pick a random death message
            ThreadLocalRandom random = ThreadLocalRandom.current();
            game.getAudience().sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gray>" +
                            DEATH_MESSAGES.get(random.nextInt(DEATH_MESSAGES.size())),
                    Placeholder.component("victim", Component.text(target.getUsername(), NamedTextColor.RED)),
                    Placeholder.component("tagger", Component.text(attacker.getUsername(), NamedTextColor.WHITE))
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
            FireworkUtils.showFirework(game.getPlayers(), e.getInstance(), target.getPosition().add(0, 1.5, 0), List.of(randomColorEffect));

            // Check for win with new alive count
            game.checkPlayerCounts();
        });
    }

}
