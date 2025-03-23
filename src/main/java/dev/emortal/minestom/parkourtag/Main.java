package dev.emortal.minestom.parkourtag;

import com.jme3.system.NativeLibraryLoader;
import dev.emortal.minestom.gamesdk.MinestomGameServer;
import dev.emortal.minestom.gamesdk.config.GameSdkConfig;
import dev.emortal.minestom.parkourtag.commands.CreditsCommand;
import dev.emortal.minestom.parkourtag.map.MapManager;
import net.minestom.server.MinecraftServer;

import java.io.File;

public final class Main {

    public static void main(String[] args) {
        NativeLibraryLoader.loadLibbulletjme(true, new File("natives/"), "Release", "Sp");

        MinestomGameServer server = MinestomGameServer.create((a) -> {
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