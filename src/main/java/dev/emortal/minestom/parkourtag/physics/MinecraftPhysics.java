package dev.emortal.minestom.parkourtag.physics;

import com.github.stephengold.joltjni.*;
import com.github.stephengold.joltjni.enumerate.EPhysicsUpdateError;
import dev.emortal.minestom.parkourtag.physics.objects.MinecraftPhysicsObject;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public class MinecraftPhysics {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftPhysics.class);

    public static final int objLayerMoving = 0;
    public static final int objLayerNonMoving = 1;

    private boolean paused = false;

    private @NotNull PhysicsSystem physicsSystem;
    private @NotNull TempAllocator tempAllocator;
    private @NotNull JobSystem jobSystem;

    private final @NotNull List<MinecraftPhysicsObject> objects = new CopyOnWriteArrayList<>();
    private final @NotNull Map<Long, MinecraftPhysicsObject> objectMap = new ConcurrentHashMap<>();
    private final Instance instance;

    public MinecraftPhysics(Instance instance) {
        this.instance = instance;

        instance.scheduler().submitTask(new Supplier<>() {
            boolean first = true;
            long lastRan = System.nanoTime();

            @Override
            public TaskSchedule get() {
                if (first) {
                    init();
                    first = false;
                }

                long diff = System.nanoTime() - lastRan;
                float deltaTime = diff / 1_000_000_000f;

                lastRan = System.nanoTime();
                if (paused) return TaskSchedule.tick(1);
                update(deltaTime);
//                update(0.05f);

                return TaskSchedule.tick(1);
            }
        });
    }

    private void init() {
        // https://github.com/stephengold/jolt-jni-docs/blob/master/java-apps/src/main/java/com/github/stephengold/sportjolt/javaapp/sample/console/HelloJoltJni.java

        // For simplicity, use a single broadphase layer:
        int numBpLayers = 1;
        int numObjLayers = 2;

        ObjectLayerPairFilterTable ovoFilter = new ObjectLayerPairFilterTable(numObjLayers);
        // Enable collisions between 2 moving bodies:
        ovoFilter.enableCollision(objLayerMoving, objLayerMoving);
        // Enable collisions between a moving body and a non-moving one:
        ovoFilter.enableCollision(objLayerMoving, objLayerNonMoving);
        // Disable collisions between 2 non-moving bodies:
        ovoFilter.disableCollision(objLayerNonMoving, objLayerNonMoving);

        // Map both object layers to broadphase layer 0:
        BroadPhaseLayerInterfaceTable layerMap = new BroadPhaseLayerInterfaceTable(numObjLayers, numBpLayers);
        layerMap.mapObjectToBroadPhaseLayer(objLayerMoving, 0);
        layerMap.mapObjectToBroadPhaseLayer(objLayerNonMoving, 0);

        // Rules for colliding object layers with broadphase layers:
        ObjectVsBroadPhaseLayerFilter ovbFilter = new ObjectVsBroadPhaseLayerFilterTable(layerMap, numBpLayers, ovoFilter, numObjLayers);

        physicsSystem = new PhysicsSystem();

        // Set high limits, even though this sample app uses only 2 bodies:
        int maxBodies = 5_000;
        int numBodyMutexes = 0; // 0 means "use the default number"
        int maxBodyPairs = 65_536;
        int maxContacts = 20_480;
        physicsSystem.init(maxBodies, numBodyMutexes, maxBodyPairs, maxContacts, layerMap, ovbFilter, ovoFilter);
        physicsSystem.optimizeBroadPhase();

        tempAllocator = new TempAllocatorMalloc();
        int numWorkerThreads = 1;
        LOGGER.info("Using {} worker threads for physics", numWorkerThreads);
        jobSystem = new JobSystemThreadPool(Jolt.cMaxPhysicsJobs, Jolt.cMaxPhysicsBarriers, numWorkerThreads); // use all available processors

        // Default: -9.81f
        // Minecraft: -31.36f
        physicsSystem.setGravity(new Vec3(0, -17f, 0));
    }

    private void update(float delta) {
        if (physicsSystem == null) return;

        int steps = 1;
        int errors = physicsSystem.update(delta, steps, tempAllocator, jobSystem);
        assert errors == EPhysicsUpdateError.None : errors;

        for (MinecraftPhysicsObject object : objects) {
            object.update();
        }
    }

    public @NotNull List<MinecraftPhysicsObject> getObjects() {
        return objects;
    }

    public void addObject(MinecraftPhysicsObject object) {
        objects.add(object);
        objectMap.put(object.getBody().va(), object);
    }
    public void removeObject(MinecraftPhysicsObject object) {
        objects.remove(object);
        objectMap.remove(object.getBody().va());
    }

    public void addConstraint(Constraint constraint) {
        physicsSystem.addConstraint(constraint);
        if (constraint instanceof TwoBodyConstraint twoBodyConstraint) {
            MinecraftPhysicsObject obj1 = getObjectByVa(twoBodyConstraint.getBody1().va());
            TwoBodyConstraintRef ref = twoBodyConstraint.toRef();
            if (obj1 != null) obj1.addRelatedConstraint(ref);
            MinecraftPhysicsObject obj2 = getObjectByVa(twoBodyConstraint.getBody2().va());
            if (obj2 != null) obj2.addRelatedConstraint(ref);
        }
    }

    public void removeConstraint(Constraint constraint) {
        physicsSystem.removeConstraint(constraint);
        if (constraint instanceof TwoBodyConstraint twoBodyConstraint) {
            TwoBodyConstraintRef ref = twoBodyConstraint.toRef();
            MinecraftPhysicsObject obj1 = getObjectByVa(twoBodyConstraint.getBody1().va());
            if (obj1 != null) obj1.removeRelatedConstraint(ref);
            MinecraftPhysicsObject obj2 = getObjectByVa(twoBodyConstraint.getBody2().va());
            if (obj2 != null) obj2.removeRelatedConstraint(ref);
        }
    }

    public @Nullable MinecraftPhysicsObject getObjectByVa(Long va) {
        return objectMap.get(va);
    }

    public void clear() {
        getPhysicsSystem().removeAllConstraints();
        getPhysicsSystem().destroyAllBodies();
        objects.clear();
        objectMap.clear();
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public Instance getInstance() {
        return instance;
    }

    public PhysicsSystem getPhysicsSystem() {
        return physicsSystem;
    }

    public BodyInterface getBodyInterface() {
        return getPhysicsSystem().getBodyInterface();
    }
}
