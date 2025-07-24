package dev.emortal.minestom.parkourtag.map;

import dev.emortal.minestom.parkourtag.physics.MinecraftPhysics;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public record LoadedMap(@NotNull Instance instance, @NotNull MapData mapData, @NotNull MinecraftPhysics physics) {
}
