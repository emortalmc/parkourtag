package dev.emortal.minestom.parkourtag;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.emortal.minestom.gamesdk.MinestomGameServer;
import dev.emortal.minestom.gamesdk.config.GameSdkConfig;
import dev.emortal.minestom.parkourtag.config.SpawnPositionJson;
import dev.emortal.minestom.parkourtag.map.MapManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final Gson GSON = new Gson();

    public static Map<String, SpawnPositionJson> SPAWN_POSITION_MAP;

    public static void main(String[] args) {
        InputStream is = Main.class.getClassLoader().getResourceAsStream("spawns.json");
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
        Type type = new TypeToken<HashMap<String, SpawnPositionJson>>(){}.getType();
        SPAWN_POSITION_MAP = GSON.fromJson(reader, type);

        MinestomGameServer.create(() -> {
            MapManager mapManager = new MapManager();
            return GameSdkConfig.builder()
                    .minPlayers(ParkourTagGame.MIN_PLAYERS)
                    .maxGames(10)
                    .gameCreator(info -> new ParkourTagGame(info, mapManager.getMap(info.mapId())))
                    .build();
        });
    }
}