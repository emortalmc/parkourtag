package dev.emortal.minestom.parkourtag.blockhandler;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class SignHandler implements BlockHandler {

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(
                Tag.Boolean("is_waxed"),
                Tag.NBT("front_text"),
                Tag.NBT("back_text")
        );
    }

    @Override
    public @NotNull Key getKey() {
        return Key.key("minecraft:sign");
    }
}
