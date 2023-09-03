package dev.emortal.minestom.parkourtag.listeners.parkourtag;

import dev.emortal.minestom.parkourtag.ParkourTagGame;
import dev.emortal.minestom.parkourtag.map.MapManager;
import dev.emortal.minestom.parkourtag.utils.NoTickEntity;
import dev.emortal.minestom.parkourtag.utils.Quaternion;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

public class ParkourTagTickListener {

    private static final Tag<Boolean> launchCooldownTag = Tag.Boolean("launchCooldown");

    public static void registerListener(EventNode<InstanceEvent> eventNode, ParkourTagGame game) {
        if (game.instance.getTag(MapManager.MAP_ID_TAG).equals("city")) {
            createRailParticle(3, 60, -16, game);
            createRailParticle(5, 61, -18, game);

            createRailTextDisplay(5.163, 62.5, -18.999, 1, game.instance);
            createRailTextDisplay(5.163, 63.25, -18.999, 2, game.instance);
            createRailTextDisplay(5.163, 64, -18.999, 3, game.instance);
        }

        eventNode.addListener(EntityTickEvent.class, e -> {
            if (e.getEntity().getEntityType() != EntityType.PLAYER) return;

            Player player = (Player) e.getEntity();

            if (player.getGameMode() != GameMode.ADVENTURE) return;

            Pos playerPos = player.getPosition();

            // Note block sound based on distance to tagger
            if (!game.isVictorying() && game.getTaggers().contains(player)) {
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

    private static void createRailParticle(double x, double y, double z, ParkourTagGame game) {
        double spinSpeed = 0.35;
        double spinScale = 0.9;
        game.instance.scheduler().buildTask(() -> {
            double tick = game.instance.getWorldAge();

            ParticlePacket packet = ParticleCreator.createParticlePacket(Particle.SNOWFLAKE, true, x + Math.sin(tick * spinSpeed) * spinScale, y, z + Math.cos(tick * spinSpeed) * spinScale, 0f, 0.5f, 0f, 1f, 0, null);

            game.sendGroupedPacket(packet);
        }).repeat(TaskSchedule.tick(1)).schedule();
    }

    private static void createRailTextDisplay(double x, double y, double z, int delay, Instance instance) {
        int onTicks = 7;

        Tag<Boolean> onTag = Tag.Boolean("on");
        Component on = Component.text(">", NamedTextColor.GOLD);
        Component off = Component.text(">", NamedTextColor.DARK_GRAY);

        Entity entity = new NoTickEntity(EntityType.TEXT_DISPLAY);
        TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
        meta.setText(off);
        meta.setBackgroundColor(0);
        meta.setScale(new Vec(7.5));
        Quaternion quaternion = new Quaternion(new Vec(1, 0, 0), Math.toRadians(90));
        meta.setLeftRotation(new float[] {(float) quaternion.getW(), (float) quaternion.getX(), (float) quaternion.getY(), (float) quaternion.getZ()});
        entity.setTag(onTag, false);

        instance.scheduler().buildTask(() -> {
            instance.scheduler().submitTask(() -> {
                System.out.println("toggled light");
                if (entity.getTag(onTag)) {
                    // turn off
                    meta.setText(off);
                    entity.setTag(onTag, false);
                    return TaskSchedule.tick(onTicks * 2);
                } else {
                    // turn on
                    meta.setText(on);
                    entity.setTag(onTag, true);
                    return TaskSchedule.tick(onTicks);
                }
            });
        }).delay(TaskSchedule.tick(onTicks * delay)).schedule();

        entity.setInstance(instance, new Pos(x, y, z, 180f, 0f));
    }

}
