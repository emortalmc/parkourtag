package dev.emortal.minestom.parkourtag.physics.objects;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.joints.ConeJoint;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import dev.emortal.minestom.parkourtag.physics.MinecraftPhysicsHandler;
import dev.emortal.minestom.parkourtag.physics.PlayerDisplayPart;
import dev.emortal.minestom.parkourtag.utils.NoTickEntity;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RagdollPhysics implements MinecraftPhysicsObject {
    private static final PlayerSkin PLACEHOLDER_SKIN = new PlayerSkin("ewogICJ0aW1lc3RhbXAiIDogMTcyNDgwNjQxNDU1OCwKICAicHJvZmlsZUlkIiA6ICI3YmQ1YjQ1OTFlNmI0NzUzODI3NDFmYmQyZmU5YTRkNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJlbW9ydGFsZGV2IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Q1YmQ1MDViMTBkM2I2YWZjOGY3NTI1OGIwMWE3YzQwMjFjNjFkODFkMjA1M2I4MDg4ZWUyYjhjMTA0NDE4OTMiCiAgICB9LAogICAgIkNBUEUiIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2NkOWQ4MmFiMTdmZDkyMDIyZGJkNGE4NmNkZTRjMzgyYTc1NDBlMTE3ZmFlN2I5YTI4NTM2NTg1MDVhODA2MjUiCiAgICB9CiAgfQp9", "vuqasGHA0u5lehBFVjdYLgFaQvxrLpOS2MO0XH+ATrLhv68022JiTHGnXdz1QFN5kXWQoo4IOoTFC2fAxcPctSFjxL7WTnPsQlwHAZgOuWA0xbyS/m9/2Lmz+bLaL1+dSe6rWxI/j2gGjMgw7Ugy2RsBmg2yTHu4vIT081ntuRjFCn3UaOzRvgbYXGJDnMs3C5nVKKtvbvzJXBonxZHqznJeY7dnRxK2G9Hp0Uu++cPcJ2G3nJRbtAuh4jn3v4gDhA+hbLLzonebiqaE7BNcqjKHfShcbPoZlq4FWnJdFVDeQ8WIP7sseAqcW3iRT4noIU5AKOqFt54ejgh1INVK2TquiVIbIsGTbh6tDR1mmMxYa7v0WFINInK7uAZbOvxejfCVJIJUh0/SsP4Wxeg9g8NzutdyKtuMcE3i/2FebPBTm1Mys+zUK5lmKOnQfDkxv1Te9LhR59NOIxROWUTbwhwcLiDyAn3MbjQzvFB1E4uvm92a3u8MjpJvqb2U4OMTkeb4Bx9MNqLSxmhizbrKjBRexRUQKMRBQpEdE6o2oFVax55AKF5GUeQ87LQ1AOeYpvqYllt/ihbVk6aiNApAoVIviUZ01X/Q5h4hGxtnztLa0NatjY09hRRj5/7h/mwSuPRVD46rJUrirU5Doh9uTIQAVYaptwhFqJESw9Jb8OI=");

    public static final Vector3f TORSO_SIZE = new Vector3f(8.0f/16.0f, 12.0f/16.0f, 4.0f/16.0f);
    public static final Vector3f HEAD_SIZE = new Vector3f(8.0f/16.0f, 8.0f/16.0f, 8.0f/16.0f);
    public static final Vector3f LIMB_SIZE = new Vector3f(4.0f/16.0f, 12.0f/16.0f, 4.0f/16.0f);
    public static final double PLAYER_SIZE = 1.0;

    private final Vector3f size;
    private Entity entity;


    private final @NotNull MinecraftPhysicsHandler physicsHandler;
    private final @NotNull PhysicsRigidBody rigidBody;
    private final @Nullable PhysicsJoint joint;

    private final @NotNull PlayerSkin playerSkin;

    private final @NotNull PlayerDisplayPart part;

    public RagdollPhysics(MinecraftPhysicsHandler physicsHandler, Player spawner, @Nullable PhysicsRigidBody torso, PlayerDisplayPart part, Instance instance, Vector3f position, Quaternion quaternion, float mass) {
        this.size = part.getSize().mult(0.5f);
        this.physicsHandler = physicsHandler;
        this.part = part;

        PlayerSkin playerSkin1;
        playerSkin1 = spawner.getSkin();
        if (playerSkin1 == null) playerSkin1 = PLACEHOLDER_SKIN;
        this.playerSkin = playerSkin1;

        physicsHandler.addObject(this);

        BoxCollisionShape boxShape = new BoxCollisionShape(size.x, size.y, size.z);
        rigidBody = new PhysicsRigidBody(boxShape, mass);
        physicsHandler.getPhysicsSpace().addCollisionObject(rigidBody);
        rigidBody.setAngularDamping(0.1f);
        rigidBody.setLinearDamping(0.3f);
        rigidBody.setPhysicsRotation(quaternion);
        rigidBody.setPhysicsLocation(position);

        if (torso != null) {
            assert (part != PlayerDisplayPart.TORSO);

            // From torso
            Vector3f firstThing = switch (part) {
                case HEAD -> new Vector3f(0f, TORSO_SIZE.y / 2f, 0f);
                case RIGHT_ARM -> new Vector3f(TORSO_SIZE.x / 1.35f, TORSO_SIZE.y / 2f, 0f);
                case LEFT_ARM -> new Vector3f(-TORSO_SIZE.x / 1.35f, TORSO_SIZE.y / 2f, 0f);
                case RIGHT_LEG -> new Vector3f(0.13f, -TORSO_SIZE.y / 2f, 0f);
                case LEFT_LEG -> new Vector3f(-0.13f, -TORSO_SIZE.y / 2f, 0f);
                default -> throw new IllegalStateException("Unexpected value: " + part);
            };
            // From part
            Vector3f secondThing = switch (part) {
                case HEAD -> new Vector3f(0f, -HEAD_SIZE.y / 2f, 0f);
                case RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG -> new Vector3f(0f, LIMB_SIZE.y / 2f, 0f);
                default -> throw new IllegalStateException("Unexpected value: " + part);
            };

            joint = new ConeJoint(torso, rigidBody, firstThing.mult((float) PLAYER_SIZE), secondThing.mult((float) PLAYER_SIZE));
            physicsHandler.getPhysicsSpace().addJoint(joint);
        } else {
            joint = null;
        }

        entity = spawnEntity(instance);
    }

    private Entity spawnEntity(Instance instance) {
        Entity entity = new NoTickEntity(EntityType.ITEM_DISPLAY);

        entity.setBoundingBox(size.y, size.y, size.y);

        Transform transform = new Transform();
        rigidBody.getTransform(transform);

        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setWidth(2);
            meta.setHeight(2);
            meta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRD_PERSON_RIGHT_HAND);
            meta.setItemStack(ItemStack.builder(Material.PLAYER_HEAD)
                    .set(ItemComponent.PROFILE, new HeadProfile(this.playerSkin))
                    .customModelData(this.part.getCustomModelData()).build());
            meta.setScale(new Vec(PLAYER_SIZE));
            meta.setTranslation(new Vec(0, this.part.getYTranslation(), 0));

            meta.setLeftRotation(MinecraftPhysicsObject.toFloats(transform.getRotation()));
            meta.setPosRotInterpolationDuration(2);
            meta.setTransformationInterpolationDuration(2);
        });

        entity.setInstance(instance, MinecraftPhysicsObject.toVec(transform.getTranslation()));

        return entity;
    }

    @Override
    public void updateEntity() {
        if (entity == null) return;

        Transform transform = new Transform();
        rigidBody.getTransform(transform);

        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setNotifyAboutChanges(false);

            meta.setTransformationInterpolationStartDelta(0);

            // size not updated as it doesn't change
            meta.setLeftRotation(MinecraftPhysicsObject.toFloats(transform.getRotation()));
            meta.setNotifyAboutChanges(true);
        });

        entity.teleport(MinecraftPhysicsObject.toPos(transform.getTranslation()));
    }

    @Override
    public @NotNull PhysicsRigidBody getRigidBody() {
        return rigidBody;
    }
    @Override
    public @Nullable Entity getEntity() {
        return entity;
    }

    @Override
    public void destroy() {
        if (entity != null) entity.remove();
        entity = null;

        physicsHandler.getPhysicsSpace().removeCollisionObject(rigidBody);
        if (joint != null) physicsHandler.getPhysicsSpace().removeJoint(joint);
    }

}
