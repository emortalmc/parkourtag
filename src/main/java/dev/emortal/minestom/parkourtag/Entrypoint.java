package dev.emortal.minestom.parkourtag;

import dev.emortal.minestom.core.MinestomServer;
import dev.emortal.minestom.gamesdk.GameSdkModule;

public class Entrypoint {
    public static void main(String[] args) {
        new MinestomServer.Builder()
                .commonModules()
                .module(GameSdkModule.class, GameSdkModule::new)
                .module(ParkourTagModule.class, ParkourTagModule::new)
                .build();
    }
}
