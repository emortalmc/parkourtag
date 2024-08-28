package dev.emortal.minestom.parkourtag.physics.objects;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MinecraftPhysicsObject {

    PhysicsRigidBody getRigidBody();

    void updateEntity();

    @Nullable Entity getEntity();

    void destroy();

    static @NotNull Vector3f toVector3(Point vec) {
        return new Vector3f((float)vec.x(), (float)vec.y(), (float)vec.z());
    }
    static float[] toFloats(Quaternion rotation) {
        return new float[] { rotation.getX(), rotation.getY(), rotation.getZ(), rotation.getW() };
    }

    static @NotNull Vec toVec(Vector3f vector3) {
        return new Vec(vector3.x, vector3.y, vector3.z);
    }
    static @NotNull Pos toPos(Vector3f vector3) {
        return new Pos(vector3.x, vector3.y, vector3.z);
    }

}
