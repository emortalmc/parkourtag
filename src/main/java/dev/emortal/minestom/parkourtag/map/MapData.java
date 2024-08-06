package dev.emortal.minestom.parkourtag.map;

import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;

public record MapData(@NotNull Pos tagger, @NotNull Pos goon, @NotNull String name, @NotNull String[] credits) {
}
