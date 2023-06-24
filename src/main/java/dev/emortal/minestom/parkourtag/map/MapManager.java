package dev.emortal.minestom.parkourtag.map;

import net.hollowcube.polar.AnvilPolar;
import net.hollowcube.polar.ChunkSelector;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWorld;
import net.hollowcube.polar.PolarWriter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MapManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapManager.class);

    private static final DimensionType DIMENSION_TYPE = DimensionType.builder(NamespaceID.from("emortalmc:parkourtag"))
            .skylightEnabled(true)
//            .ambientLight(1.0f)
            .build();

    private static final List<String> ENABLED_MAPS = List.of(
            "city",
            "ruins"
    );
    private static final Path MAPS_PATH = Path.of("maps");
    public static final Tag<String> MAP_ID_TAG = Tag.String("mapId");

    private static final int CHUNK_LOADING_RADIUS = 3;

    private final Map<String, InstanceContainer> mapInstances;

    public MapManager() {
        MinecraftServer.getDimensionTypeManager().addDimension(DIMENSION_TYPE);

        Map<String, InstanceContainer> instances = new HashMap<>();

        for (String mapName : ENABLED_MAPS) {
            final Path polarPath = MAPS_PATH.resolve(mapName + ".polar");
            final Path anvilPath = MAPS_PATH.resolve(mapName);

            try {
                PolarLoader polarLoader;
                if (!Files.exists(polarPath)) { // File needs to be converted
                    PolarWorld world = AnvilPolar.anvilToPolar(anvilPath, ChunkSelector.radius(CHUNK_LOADING_RADIUS));
                    Files.write(polarPath, PolarWriter.write(world));
                    polarLoader = new PolarLoader(world);
                } else {
                    polarLoader = new PolarLoader(polarPath);
                }

                InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(DIMENSION_TYPE, polarLoader);
                instance.setTimeRate(0);
                instance.setTimeUpdate(null);

                // Do some preloading!
                for (int x = -CHUNK_LOADING_RADIUS; x < CHUNK_LOADING_RADIUS; x++) {
                    for (int z = -CHUNK_LOADING_RADIUS; z < CHUNK_LOADING_RADIUS; z++) {
                        instance.loadChunk(x, z);
                    }
                }

                instances.put(mapName, instance);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        this.mapInstances = Map.copyOf(instances);
    }

    public @NotNull Instance getMap(@Nullable String id) {
        if (id == null) return this.getRandomMap();

        final InstanceContainer instance = this.mapInstances.get(id);
        if (instance == null) {
            LOGGER.warn("Map {} not found, loading random map", id);
            return this.getRandomMap();
        }

        LOGGER.info("Creating instance for map {}", id);

        Instance shared = MinecraftServer.getInstanceManager().createSharedInstance(instance);
        shared.setTag(MAP_ID_TAG, id);

        return shared;
    }

    public @NotNull Instance getRandomMap() {
        final String randomMapId = ENABLED_MAPS.get(ThreadLocalRandom.current().nextInt(ENABLED_MAPS.size()));
        final InstanceContainer instance = this.mapInstances.get(randomMapId);

        Instance shared = MinecraftServer.getInstanceManager().createSharedInstance(instance);
        shared.setTag(MAP_ID_TAG, randomMapId);

        return shared;
    }
}
