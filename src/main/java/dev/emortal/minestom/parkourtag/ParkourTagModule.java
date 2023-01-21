package dev.emortal.minestom.parkourtag;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.emortal.minestom.core.module.Module;
import dev.emortal.minestom.core.module.ModuleData;
import dev.emortal.minestom.core.module.ModuleEnvironment;
import dev.emortal.minestom.gamesdk.GameSdkModule;
import dev.emortal.minestom.gamesdk.config.GameCreationInfo;
import dev.emortal.minestom.gamesdk.config.GameSdkConfig;
import dev.emortal.minestom.parkourtag.config.SpawnPositionJson;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.instance.Instance;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

        MojangAuth.init();

//        Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer(DimensionType.OVERWORLD, null);
//
//        this.eventNode.addListener(PlayerLoginEvent.class, event -> event.setSpawningInstance(instance));

        GameSdkModule.init(
                new GameSdkConfig.Builder()
                        .minPlayers(ParkourTagGame.MIN_PLAYERS)
                        .maxGames(10)
                        .gameSupplier(info -> new ParkourTagGame(info, super.eventNode))
                        .build()
        );

//        ParkourTagGame ptg = new ParkourTagGame(
//                new GameCreationInfo(Set.of(
//                        UUID.fromString("7bd5b459-1e6b-4753-8274-1fbd2fe9a4d5"),
//                        UUID.fromString("70cdb3bf-8a7a-4861-af4e-a3ce5070ceb9")
//                ), Instant.now())
//        );
//        GameSdkModule.getGameManager().addGame(ptg);

        this.eventNode.addListener(ItemDropEvent.class, e -> e.setCancelled(true));
        this.eventNode.addListener(InventoryPreClickEvent.class, e -> e.setCancelled(true));
        this.eventNode.addListener(PlayerSwapItemEvent.class, e -> e.setCancelled(true));

        return true;
    }

    @Override
    public void onUnload() {

    }
}