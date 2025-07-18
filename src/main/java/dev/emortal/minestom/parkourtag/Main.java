package dev.emortal.minestom.parkourtag;

import dev.emortal.minestom.gamesdk.MinestomGameServer;
import dev.emortal.minestom.gamesdk.config.GameSdkConfig;
import dev.emortal.minestom.parkourtag.commands.CreditsCommand;
import dev.emortal.minestom.parkourtag.map.MapManager;
import electrostatic4j.snaploader.LibraryInfo;
import electrostatic4j.snaploader.LoadingCriterion;
import electrostatic4j.snaploader.NativeBinaryLoader;
import electrostatic4j.snaploader.filesystem.DirectoryPath;
import electrostatic4j.snaploader.platform.NativeDynamicLibrary;
import electrostatic4j.snaploader.platform.util.PlatformPredicate;
import net.minestom.server.MinecraftServer;

public final class Main {

    public static void main(String[] args) throws Exception {
        LibraryInfo libInfo = new LibraryInfo(
                new DirectoryPath("linux/x86-64/com/github/stephengold"),
                "bulletjme", DirectoryPath.USER_DIR);
        NativeBinaryLoader loader = new NativeBinaryLoader(libInfo);
        NativeDynamicLibrary[] libraries = new NativeDynamicLibrary[]{
//                new NativeDynamicLibrary("native/linux/arm64", PlatformPredicate.LINUX_ARM_64),
//                new NativeDynamicLibrary("native/linux/arm32", PlatformPredicate.LINUX_ARM_32),
                new NativeDynamicLibrary("native/linux/x86_64", PlatformPredicate.LINUX_X86_64),
//                new NativeDynamicLibrary("native/osx/arm64", PlatformPredicate.MACOS_ARM_64),
//                new NativeDynamicLibrary("native/osx/x86_64", PlatformPredicate.MACOS_X86_64),
                new NativeDynamicLibrary("native/windows/x86_64", PlatformPredicate.WIN_X86_64)
        };
        loader.registerNativeLibraries(libraries).initPlatformLibrary();
        loader.loadLibrary(LoadingCriterion.INCREMENTAL_LOADING);

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