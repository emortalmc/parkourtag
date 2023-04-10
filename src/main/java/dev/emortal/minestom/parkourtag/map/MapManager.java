package dev.emortal.minestom.parkourtag.map;

import dev.emortal.tnt.TNTLoader;
import dev.emortal.tnt.source.FileTNTSource;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MapManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapManager.class);

    private static final DimensionType DIMENSION_TYPE = DimensionType.builder(NamespaceID.from("emortalmc:parkourtag"))
            .skylightEnabled(true)
            .ambientLight(1.0f)
            .build();

    private static final List<String> ENABLED_MAPS = List.of(
            "city",
            "ruins"
    );
    private static final Path MAPS_PATH = Path.of("maps");

    private final Map<String, TNTLoader> mapLoaders;

    public MapManager() {
        MinecraftServer.getDimensionTypeManager().addDimension(DIMENSION_TYPE);

        Map<String, TNTLoader> chunkLoaders = new HashMap<>();

        for (String mapName : ENABLED_MAPS) {
            final Path tntPath = MAPS_PATH.resolve(mapName + ".tnt");

            try {
                final TNTLoader chunkLoader = new TNTLoader(new FileTNTSource(tntPath));

                chunkLoaders.put(mapName, chunkLoader);
            } catch (IOException | NBTException e) {
                throw new RuntimeException(e);
            }
        }

        this.mapLoaders = Map.copyOf(chunkLoaders);
    }

    public @NotNull Instance getMap(@Nullable String id) {
        if (id == null) return this.getRandomMap();

        final TNTLoader chunkLoader = this.mapLoaders.get(id);
        if (chunkLoader == null) {
            LOGGER.warn("Map {} not found, loading random map", id);
            return this.getRandomMap();
        }

        LOGGER.info("Creating instance for map {}", id);

        return MinecraftServer.getInstanceManager().createInstanceContainer(DIMENSION_TYPE, chunkLoader);
    }

    public @NotNull Instance getRandomMap() {
        final String randomMapId = ENABLED_MAPS.get(ThreadLocalRandom.current().nextInt(ENABLED_MAPS.size()));
        final TNTLoader chunkLoader = this.mapLoaders.get(randomMapId);

        return MinecraftServer.getInstanceManager()
                .createInstanceContainer(DIMENSION_TYPE, chunkLoader);
    }
}
