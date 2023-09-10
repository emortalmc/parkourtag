package dev.emortal.minestom.parkourtag.map;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.hollowcube.polar.PolarLoader;
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class MapManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapManager.class);
    private static final Gson GSON = new Gson();

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

    private final Map<String, PreLoadedMap> preLoadedMaps;

    public MapManager() {
        MinecraftServer.getDimensionTypeManager().addDimension(DIMENSION_TYPE);

        Map<String, PreLoadedMap> maps = new HashMap<>();
        for (String mapName : ENABLED_MAPS) {
            Path mapPath = MAPS_PATH.resolve(mapName);
            Path polarPath = mapPath.resolve("map.polar");
            Path spawnsPath = mapPath.resolve("spawns.json");

            try {
                MapSpawns spawns = GSON.fromJson(new JsonReader(Files.newBufferedReader(spawnsPath)), MapSpawns.class);
                LOGGER.info("Loaded spawns for map {}: [{}]", mapName, spawns);

                PolarLoader polarLoader = new PolarLoader(polarPath);

                InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(DIMENSION_TYPE, polarLoader);
                instance.setTimeRate(0);
                instance.setTimeUpdate(null);

                // Do some preloading!
                for (int x = -CHUNK_LOADING_RADIUS; x < CHUNK_LOADING_RADIUS; x++) {
                    for (int z = -CHUNK_LOADING_RADIUS; z < CHUNK_LOADING_RADIUS; z++) {
                        instance.loadChunk(x, z);
                    }
                }

                maps.put(mapName, new PreLoadedMap(instance, spawns));
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }

        this.preLoadedMaps = Map.copyOf(maps);
    }

    public @NotNull LoadedMap getMap(@Nullable String id) {
        if (id == null) {
            return this.getRandomMap();
        }

        PreLoadedMap map = this.preLoadedMaps.get(id);
        if (map == null) {
            LOGGER.warn("Map {} not found, loading random map", id);
            return this.getRandomMap();
        }

        return map.load(id);
    }

    public @NotNull LoadedMap getRandomMap() {
        String randomMapId = ENABLED_MAPS.get(ThreadLocalRandom.current().nextInt(ENABLED_MAPS.size()));

        PreLoadedMap map = this.preLoadedMaps.get(randomMapId);
        return map.load(randomMapId);
    }

    private record PreLoadedMap(@NotNull InstanceContainer rootInstance, @NotNull MapSpawns spawns) {

        @NotNull LoadedMap load(@NotNull String mapId) {
            Instance shared = MinecraftServer.getInstanceManager().createSharedInstance(this.rootInstance());

            shared.setTag(MAP_ID_TAG, mapId);
            shared.setTimeRate(0);
            shared.setTimeUpdate(null);

            return new LoadedMap(shared, this.spawns());
        }
    }
}
