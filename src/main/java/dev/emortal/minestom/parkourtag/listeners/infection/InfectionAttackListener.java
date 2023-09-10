package dev.emortal.minestom.parkourtag.listeners.infection;

import dev.emortal.minestom.parkourtag.InfectionGame;
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
import net.minestom.server.network.packet.server.play.HitAnimationPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class InfectionAttackListener {

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

    private static final Tag<Long> HIT_COOLDOWN = Tag.Long("hitCooldown");

    public static void registerListener(EventNode<InstanceEvent> eventNode, InfectionGame game) {
        Set<Player> infected = game.getInfected();

        eventNode.addListener(EntityAttackEvent.class, e -> {
            if (!game.canHitPlayers()) return;
            if (e.getEntity().getEntityType() != EntityType.PLAYER) return;
            if (e.getTarget().getEntityType() != EntityType.PLAYER) return;

            Player attacker = (Player) e.getEntity();
            Player target = (Player) e.getTarget();

            if (attacker.getGameMode() != GameMode.ADVENTURE) return;
            if (target.getGameMode() != GameMode.ADVENTURE) return;

            if (game.isVictorying() && infected.contains(target)) { // Attacking tagger after victory
                if (!target.hasTag(HIT_COOLDOWN)) target.setTag(HIT_COOLDOWN, System.currentTimeMillis());
                if (target.getTag(HIT_COOLDOWN) > System.currentTimeMillis()) return;
                target.setTag(HIT_COOLDOWN, System.currentTimeMillis() + 500);

                target.takeKnockback(0.4f, Math.sin(Math.toRadians(attacker.getPosition().yaw())), -Math.cos(Math.toRadians(attacker.getPosition().yaw())));
                HitAnimationPacket hitAnimationPacket = new HitAnimationPacket(target.getEntityId(), attacker.getPosition().yaw());
                game.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_HURT, Sound.Source.PLAYER, 1f, 1f), target.getPosition());

                game.sendGroupedPacket(hitAnimationPacket);

                return;
            }

            if (infected.contains(target)) return;

            attacker.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());
            target.showTitle(
                    Title.title(
                            Component.text("INFECTED", NamedTextColor.GREEN, TextDecoration.BOLD),
                            Component.text("Infect all the goons!", NamedTextColor.GRAY),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(500))
                    )
            );

            game.getInfected().add(target);
            game.getGoons().remove(target);
            target.setGlowing(true);
            target.setTeam(InfectionGame.INFECTED_TEAM);

            // Pick a random death message
            ThreadLocalRandom random = ThreadLocalRandom.current();
            game.sendMessage(MiniMessage.miniMessage().deserialize(
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