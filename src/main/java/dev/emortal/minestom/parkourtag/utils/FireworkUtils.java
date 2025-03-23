package dev.emortal.minestom.parkourtag.utils;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.projectile.FireworkRocketMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.FireworkExplosion;
import net.minestom.server.item.component.FireworkList;
import net.minestom.server.network.packet.server.play.EntityStatusPacket;
import net.minestom.server.utils.PacketSendingUtils;

import java.util.List;
import java.util.Set;

public class FireworkUtils {

    public static void showFirework(Set<Player> players, Instance instance, Pos position, List<FireworkExplosion> effects) {
        ItemStack fwItem = ItemStack.builder(Material.FIREWORK_ROCKET).set(ItemComponent.FIREWORKS, new FireworkList((byte) 0, effects)).build();

        Entity firework = new Entity(EntityType.FIREWORK_ROCKET);
        ((FireworkRocketMeta) firework.getEntityMeta()).setFireworkInfo(fwItem);
        firework.setNoGravity(true);
        firework.setInstance(instance, position);

        // Immediately explode
        PacketSendingUtils.sendGroupedPacket(players, new EntityStatusPacket(firework.getEntityId(), (byte) 17));

        firework.remove();
    }

}
