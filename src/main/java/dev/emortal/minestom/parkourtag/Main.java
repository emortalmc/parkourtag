package dev.emortal.minestom.parkourtag;

import com.github.stephengold.joltjni.Jolt;
import com.github.stephengold.joltjni.JoltPhysicsObject;
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
import net.minestom.server.extras.MojangAuth;

public final class Main {

    public static void main(String[] args) {
        LibraryInfo libInfo = new LibraryInfo(null, "joltjni", DirectoryPath.USER_DIR);
        NativeBinaryLoader loader = new NativeBinaryLoader(libInfo);
        NativeDynamicLibrary[] libraries = {
//                new NativeDynamicLibrary("linux/aarch64/com/github/stephengold", PlatformPredicate.LINUX_ARM_64),
//                new NativeDynamicLibrary("linux/armhf/com/github/stephengold", PlatformPredicate.LINUX_ARM_32),
                new NativeDynamicLibrary("linux/x86-64/com/github/stephengold", PlatformPredicate.LINUX_X86_64),
//                new NativeDynamicLibrary("osx/aarch64/com/github/stephengold", PlatformPredicate.MACOS_ARM_64),
//                new NativeDynamicLibrary("osx/x86-64/com/github/stephengold", PlatformPredicate.MACOS_X86_64),
                new NativeDynamicLibrary("windows/x86-64/com/github/stephengold", PlatformPredicate.WIN_X86_64)
        };
        loader.registerNativeLibraries(libraries).initPlatformLibrary();
        try {
            loader.loadLibrary(LoadingCriterion.CLEAN_EXTRACTION);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load a Jolt-JNI native library!");
        }

        //Jolt.setTraceAllocations(true); // to log Jolt-JNI heap allocations
        JoltPhysicsObject.startCleaner(); // to reclaim native memory
        Jolt.registerDefaultAllocator(); // tell Jolt Physics to use malloc/free
        Jolt.installDefaultAssertCallback();
        Jolt.installDefaultTraceCallback();
        boolean success = Jolt.newFactory();
        assert success;
        Jolt.registerTypes();

        MinestomGameServer server = MinestomGameServer.create((a) -> {
            MapManager mapManager = new MapManager();

            MojangAuth.init();

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