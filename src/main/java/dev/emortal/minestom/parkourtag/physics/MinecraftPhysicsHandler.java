package dev.emortal.minestom.parkourtag.physics;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.math.Vector3f;
import dev.emortal.minestom.parkourtag.physics.objects.MinecraftPhysicsObject;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MinecraftPhysicsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftPhysicsHandler.class);

    private @Nullable PhysicsSpace physicsSpace;

    private final @NotNull List<MinecraftPhysicsObject> objects = new CopyOnWriteArrayList<>();

    public MinecraftPhysicsHandler(@NotNull Instance instance, @NotNull List<PhysicsCollisionObject> chunkMeshes) {

        // Wrap in scheduler to avoid "WARNING: invoked from wrong thread"
        instance.scheduleNextTick((i) -> {
            System.out.println("Physics space no longer null");
            this.physicsSpace = new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);

            // Default: -9.81f
            // Minecraft: -31.36f
            this.physicsSpace.setGravity(new Vector3f(0, -17f, 0));

            for (PhysicsCollisionObject chunkMesh : chunkMeshes) {
                this.physicsSpace.add(chunkMesh);
            }
        });

        instance.scheduler().buildTask(new Runnable() {
            long lastRan = System.nanoTime();
            @Override
            public void run() {
                long diff = System.nanoTime() - lastRan;
                float deltaTime = diff / 1_000_000_000f;

                lastRan = System.nanoTime();
                update(deltaTime);
            }
        }).delay(TaskSchedule.tick(1)).repeat(TaskSchedule.tick(1)).schedule();
    }

    public void update(float delta) {
        if (physicsSpace == null) return;

        physicsSpace.update(delta);
        for (MinecraftPhysicsObject object : objects) {
            object.updateEntity();
        }
    }

    public void cleanup() {
        if (physicsSpace == null) return;
        physicsSpace.destroy();
        physicsSpace = null;
    }

    public void addObject(MinecraftPhysicsObject object) {
        objects.add(object);
    }

    public PhysicsSpace getPhysicsSpace() {
        return physicsSpace;
    }
}
