package dev.emortal.minestom.parkourtag.physics;

import com.jme3.math.Vector3f;
import dev.emortal.minestom.parkourtag.physics.objects.RagdollPhysics;

public enum PlayerDisplayPart {
    HEAD(0, "phys_head", RagdollPhysics.HEAD_SIZE),
    RIGHT_ARM(-1024, "phys_right_arm", RagdollPhysics.LIMB_SIZE),
    LEFT_ARM(-2048, "phys_left_arm", RagdollPhysics.LIMB_SIZE),
    TORSO(-3072, "phys_torso", RagdollPhysics.TORSO_SIZE),
    RIGHT_LEG(-4096, "phys_right_leg", RagdollPhysics.LIMB_SIZE),
    LEFT_LEG(-5120, "phys_left_leg", RagdollPhysics.LIMB_SIZE);

    private final double yTranslation;
    private final String customModelData;
    private final Vector3f size;
    PlayerDisplayPart(double yTranslation, String customModelData, Vector3f size) {
        this.yTranslation = yTranslation;
        this.customModelData = customModelData;
        this.size = size;
    }

    public double getYTranslation() {
        return yTranslation;
    }

    public String getCustomModelData() {
        return customModelData;
    }

    public Vector3f getSize() {
        return size;
    }
}
