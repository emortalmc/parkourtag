package dev.emortal.minestom.parkourtag;

import dev.emortal.minestom.gamesdk.MinestomGameServer;
import dev.emortal.minestom.gamesdk.config.GameSdkConfig;
import dev.emortal.minestom.parkourtag.map.MapManager;

public final class Main {

    public static void main(String[] args) {
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