package dev.emortal.minestom.parkourtag.physics;

import com.jme3.math.Vector3f;
import dev.emortal.minestom.parkourtag.physics.objects.RagdollPhysics;

public enum PlayerDisplayPart {
    HEAD(0, 9, RagdollPhysics.HEAD_SIZE),
    RIGHT_ARM(-1024, 12, RagdollPhysics.LIMB_SIZE),
    LEFT_ARM(-2048, 11, RagdollPhysics.LIMB_SIZE),
    TORSO(-3072, 4, RagdollPhysics.TORSO_SIZE),
    RIGHT_LEG(-4096, 12, RagdollPhysics.LIMB_SIZE),
    LEFT_LEG(-5120, 13, RagdollPhysics.LIMB_SIZE);

    private final double yTranslation;
    private final int customModelData;
    private final Vector3f size;
    PlayerDisplayPart(double yTranslation, int customModelData, Vector3f size) {
        this.yTranslation = yTranslation;
        this.customModelData = customModelData;
        this.size = size;
    }

    public double getYTranslation() {
        return yTranslation;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public Vector3f getSize() {
        return size;
    }
}
