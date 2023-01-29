package dev.emortal.minestom.parkourtag.listeners;

import dev.emortal.minestom.parkourtag.ParkourTagGame;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerStartFlyingEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;

import java.util.function.Supplier;

public class DoubleJumpListener {

    public static void registerListener(EventNode<InstanceEvent> eventNode, ParkourTagGame game) {
        eventNode.addListener(PlayerStartFlyingEvent.class, e -> {
            Pos playerPos = e.getPlayer().getPosition();

            e.getPlayer().setAllowFlying(false);
            e.getPlayer().setFlying(false);
            e.getPlayer().setVelocity(playerPos.direction().mul(20.0));

            game.getAudience().playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.MASTER, 1f, 1.5f), playerPos.x(), playerPos.y(), playerPos.z());

            e.getInstance().scheduler().submitTask(new Supplier<>() {
                int secondsLeft = 3;

                @Override
                public TaskSchedule get() {
                    if (secondsLeft == 0) {
                        e.getPlayer().setAllowFlying(true);
                        e.getPlayer().sendActionBar(Component.text("You can now double jump!", NamedTextColor.GREEN));
                        return TaskSchedule.stop();
                    }

                    e.getPlayer().sendActionBar(
                            Component.text()
                                    .append(Component.text("Double jump on cooldown for ", TextColor.color(200, 200, 200)))
                                    .append(Component.text(secondsLeft + "s", NamedTextColor.WHITE, TextDecoration.BOLD))
                                    .build()
                    );

                    secondsLeft--;

                    return TaskSchedule.seconds(1);
                }
            });
        });
    }

}
