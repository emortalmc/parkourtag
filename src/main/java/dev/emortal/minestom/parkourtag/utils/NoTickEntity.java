package dev.emortal.minestom.parkourtag.utils;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class NoTickEntity extends Entity {

    public NoTickEntity(@NotNull EntityType entityType) {
        super(entityType);

        hasCollision = false;
        setNoGravity(true);
        hasPhysics = false;
    }

    @Override
    public void tick(long time) {

    }
}
