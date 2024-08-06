package dev.emortal.minestom.parkourtag.map;

import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public record LoadedMap(@NotNull Instance instance, @NotNull MapData mapData) {
}
