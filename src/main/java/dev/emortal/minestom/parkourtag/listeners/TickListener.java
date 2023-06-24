package dev.emortal.minestom.parkourtag.listeners;

import dev.emortal.minestom.parkourtag.ParkourTagGame;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

public class TickListener {

    private static final Tag<Boolean> launchCooldownTag = Tag.Boolean("launchCooldown");

    public static void registerListener(EventNode<InstanceEvent> eventNode, ParkourTagGame game) {
        eventNode.addListener(EntityTickEvent.class, e -> {
            if (e.getEntity().getEntityType() != EntityType.PLAYER) return;

            Player player = (Player) e.getEntity();

            if (player.getGameMode() != GameMode.ADVENTURE) return;

            Pos playerPos = player.getPosition();

            // Note block sound based on distance to tagger
            if (game.getTaggers().contains(player)) {
                for (Player goon : game.getGoons()) {
                    double distance = goon.getPosition().distanceSquared(playerPos);
                    if (distance > 25*25) continue;

                    if ((player.getAliveTicks() % Math.max((int)Math.round(Math.sqrt(distance) / 2), 2)) == 0L) {
                        goon.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_BASEDRUM, Sound.Source.MASTER, 1.5f, 1f), playerPos);
                    }
                }
            }

            // Rail launching logic
            if (
                    !player.hasTag(launchCooldownTag)
                    && player.getInstance().getBlock(playerPos, Block.Getter.Condition.TYPE).compare(Block.RAIL)
                    && player.getInstance().getBlock(playerPos.add(0, 1, 0), Block.Getter.Condition.TYPE).compare(Block.STRUCTURE_VOID)
            ) {
                player.playSound(Sound.sound(SoundEvent.ENTITY_BAT_TAKEOFF, Sound.Source.MASTER, 0.5f, 0.8f), Sound.Emitter.self());
                player.setVelocity(new Vec(0, 22.5, 0));
                player.setTag(launchCooldownTag, true);

                player.scheduler().buildTask(() -> player.removeTag(launchCooldownTag))
                        .delay(TaskSchedule.millis(1000))
                        .schedule();
            }
        });
    }

}
