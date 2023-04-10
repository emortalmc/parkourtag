package dev.emortal.minestom.parkourtag;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.emortal.minestom.core.module.Module;
import dev.emortal.minestom.core.module.ModuleData;
import dev.emortal.minestom.core.module.ModuleEnvironment;
import dev.emortal.minestom.gamesdk.GameSdkModule;
import dev.emortal.minestom.gamesdk.config.GameSdkConfig;
import dev.emortal.minestom.parkourtag.config.SpawnPositionJson;
import dev.emortal.minestom.parkourtag.map.MapManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@ModuleData(name = "parkourtag", softDependencies = {GameSdkModule.class}, required = true)
public class ParkourTagModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParkourTagModule.class);
    private static final Gson GSON = new Gson();

    public static Map<String, SpawnPositionJson> SPAWN_POSITION_MAP;

    protected ParkourTagModule(@NotNull ModuleEnvironment environment) {
        super(environment);
    }

    @Override
    public boolean onLoad() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("spawns.json");
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
        Type type = new TypeToken<HashMap<String, SpawnPositionJson>>(){}.getType();
        SPAWN_POSITION_MAP = GSON.fromJson(reader, type);

        MapManager mapManager = new MapManager();

        GameSdkModule.init(
                new GameSdkConfig.Builder()
                        .minPlayers(ParkourTagGame.MIN_PLAYERS)
                        .maxGames(10)
                        .gameSupplier((info, node) -> new ParkourTagGame(info, node, mapManager.getMap(info.mapId())))
                        .build()
        );

        return true;
    }

    @Override
    public void onUnload() {

    }
}