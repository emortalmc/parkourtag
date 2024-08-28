package dev.emortal.minestom.parkourtag.physics;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import dev.emortal.minestom.parkourtag.physics.objects.MinecraftPhysicsObject;
import dev.emortal.minestom.parkourtag.physics.objects.RagdollPhysics;
import dev.emortal.minestom.parkourtag.utils.PTQuaternion;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;

import java.util.concurrent.ThreadLocalRandom;

public class PlayerRagdoll {

    public static void spawnRagdollWithImpulse(MinecraftPhysicsHandler physicsHandler, Player player, Pos pos, Point impulse) {
        Instance instance = player.getInstance();

        pos = pos.add(0, 1.5, 0);

        float yaw = -pos.yaw() + 180;
        double yawRad = Math.toRadians(yaw);
        PTQuaternion yawQuat = new PTQuaternion(new Vec(0, 1, 0), yawRad);
        Quaternion yawQuat2 = new Quaternion((float) yawQuat.getX(), (float) yawQuat.getY(), (float) yawQuat.getZ(), (float) yawQuat.getW());

        MinecraftPhysicsObject torso = new RagdollPhysics(physicsHandler, player,null, PlayerDisplayPart.TORSO, instance, MinecraftPhysicsObject.toVector3(pos), yawQuat2, 1);
        MinecraftPhysicsObject head = new RagdollPhysics(physicsHandler, player, torso.getRigidBody(), PlayerDisplayPart.HEAD, instance, MinecraftPhysicsObject.toVector3(pos.add(new Vec(0, 0.62, 0).rotateAroundY(yawRad).mul(RagdollPhysics.PLAYER_SIZE))), yawQuat2, 1);
        MinecraftPhysicsObject rightArm = new RagdollPhysics(physicsHandler, player, torso.getRigidBody(), PlayerDisplayPart.RIGHT_ARM, instance, MinecraftPhysicsObject.toVector3(pos.add(new Vec(0.37, 0, 0).rotateAroundY(yawRad).mul(RagdollPhysics.PLAYER_SIZE))), yawQuat2, 1);
        MinecraftPhysicsObject leftArm = new RagdollPhysics(physicsHandler, player, torso.getRigidBody(), PlayerDisplayPart.LEFT_ARM, instance, MinecraftPhysicsObject.toVector3(pos.add(new Vec(-0.37, 0, 0).rotateAroundY(yawRad).mul(RagdollPhysics.PLAYER_SIZE))), yawQuat2, 1);
        MinecraftPhysicsObject rightLeg = new RagdollPhysics(physicsHandler, player, torso.getRigidBody(), PlayerDisplayPart.RIGHT_LEG, instance, MinecraftPhysicsObject.toVector3(pos.add(new Vec(0.13, -0.72, 0).rotateAroundY(yawRad).mul(RagdollPhysics.PLAYER_SIZE))), yawQuat2, 1);
        MinecraftPhysicsObject leftLeg = new RagdollPhysics(physicsHandler, player, torso.getRigidBody(), PlayerDisplayPart.LEFT_LEG, instance, MinecraftPhysicsObject.toVector3(pos.add(new Vec(-0.13, -0.72, 0).rotateAroundY(yawRad).mul(RagdollPhysics.PLAYER_SIZE))), yawQuat2, 1);

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        torso.getRigidBody().applyImpulse(MinecraftPhysicsObject.toVector3(impulse), new Vector3f(rand.nextFloat(-1f, 1f), rand.nextFloat(-1.5f, 1.5f), rand.nextFloat(-1f, 1f)));

        instance.scheduler().buildTask(() -> {
            torso.destroy();
            head.destroy();
            rightArm.destroy();
            leftLeg.destroy();
            rightLeg.destroy();
            leftArm.destroy();
        }).delay(TaskSchedule.tick(20 * 10)).schedule();

    }

}
