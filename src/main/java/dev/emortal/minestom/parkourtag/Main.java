package dev.emortal.minestom.parkourtag;

import dev.emortal.minestom.gamesdk.MinestomGameServer;
import dev.emortal.minestom.gamesdk.config.GameSdkConfig;
import dev.emortal.minestom.parkourtag.commands.CreditsCommand;
import dev.emortal.minestom.parkourtag.map.MapManager;
import net.minestom.server.MinecraftServer;

public final class Main {

    public static void main(String[] args) {
        MinestomGameServer server = MinestomGameServer.create(() -> {
            MapManager mapManager = new MapManager();

            // TODO: Fix signs
//            MinecraftServer.getBlockManager().registerHandler("minecraft:sign", SignHandler::new);
//            for (Block value : Block.values()) {
//                if (value.name().endsWith("sign")) MinecraftServer.getBlockManager().registerHandler(value.namespace(), SignHandler::new);
//            }

            return GameSdkConfig.builder()
                    .minPlayers(ParkourTagGame.MIN_PLAYERS)
                    .gameCreator(info -> new ParkourTagGame(info, mapManager.getMap(info.mapId())))
                    .build();
        });

        MinecraftServer.getCommandManager().register(new CreditsCommand(server.getGameProvider()));
    }
}