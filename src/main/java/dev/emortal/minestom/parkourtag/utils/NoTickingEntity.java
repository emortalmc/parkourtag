package dev.emortal.minestom.parkourtag.utils;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class NoTickingEntity extends Entity {

    public NoTickingEntity(@NotNull EntityType entityType) {
        super(entityType);

        setNoGravity(true);
        hasPhysics = false;
    }

    @Override
    public void tick(long time) {

    }
}
