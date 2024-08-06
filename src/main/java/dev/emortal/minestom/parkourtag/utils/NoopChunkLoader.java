package dev.emortal.minestom.parkourtag.utils;

import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.IChunkLoader;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class NoopChunkLoader implements IChunkLoader {
    @Override
    public @NotNull CompletableFuture<@Nullable Chunk> loadChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull CompletableFuture<Void> saveChunk(@NotNull Chunk chunk) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean supportsParallelSaving() {
        return true;
    }

    @Override
    public boolean supportsParallelLoading() {
        return true;
    }
}
