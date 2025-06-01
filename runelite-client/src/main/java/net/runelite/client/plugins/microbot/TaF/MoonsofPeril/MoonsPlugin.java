package net.runelite.client.plugins.microbot.TaF.MoonsofPeril;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.TaF.MoonsofPeril.enums.MoonsState;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.lang3.tuple.MutablePair;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

@PluginDescriptor(
        name = PluginDescriptor.TaFCat + "Moons of Peril",
        description = "Moons of Peril bot",
        tags = {"TaF", "Moons of Peril", "boss"},
        enabledByDefault = false
)
@Slf4j
public class MoonsPlugin extends Plugin {
    public static long lastBloodSpecialSpawnTick = 0;
    public static long globalTickCount = 0;
    public static List<MutablePair<WorldPoint, Long>> eclipseNpcs = new ArrayList<>();
    private static ScheduledExecutorService tileExecutor;
    @Inject
    public MoonsScript moonsScript;
    public List<MutablePair<WorldPoint, Integer>> dangerousGraphicsObjectTiles = new ArrayList<>();
    @Inject
    private MoonsConfig config;
    private Instant scriptStartTime;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private MoonsOverlay moonsOverlay;
    private ScheduledExecutorService scheduledExecutorService;

    @Provides
    MoonsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MoonsConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(moonsOverlay);
        }
        scriptStartTime = Instant.now();
        MoonsScript.moonsState = MoonsState.DEFAULT;
        scheduledExecutorService = Executors.newScheduledThreadPool(50);
        moonsScript.run(config);
        init();
        startJaguarTracking();
        dangerousGraphicsObjectTiles.clear();
        Rs2Tile.init();
    }

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    private final int TICK_DELAY = 2;
    private void startJaguarTracking() {
        Microbot.log("Starting Blood Jaguar tracking");
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            var bloodJaguarExist = Rs2Npc.getNpc(MoonsConstants.BLOOD_JAGUAR_NPC_ID);
            if (bloodJaguarExist != null) {
                long ticksSinceLastSpawn = globalTickCount - lastBloodSpecialSpawnTick;
                if (ticksSinceLastSpawn == TICK_DELAY) {
                    scheduledExecutorService.schedule(() -> {
                        Microbot.log("Started schedule");
                        NPC floorTileNPC = Rs2Npc.getNpc(MoonsConstants.PERILOUS_MOONS_SAFE_CIRCLE);
                        WorldPoint floorTileLocation = (floorTileNPC != null) ? floorTileNPC.getWorldLocation() : null;
                        if (floorTileLocation == null) {
                            return;
                        }
                        Rs2NpcModel bloodJaguar = MoonsHelpers.getClosestJaguar(floorTileLocation);
                        WorldPoint closestTile = MoonsScript.bloodMoonSafeCircles.stream()
                                .min(Comparator.comparingDouble(tile -> tile.distanceTo2D(floorTileLocation)))
                                .orElse(null);
                        if (closestTile != null && Rs2Player.getWorldLocation().distanceTo(closestTile) > 0 && bloodJaguar != null) {
                            Microbot.log("Globaltick count before: " + MoonsPlugin.globalTickCount);
                            var afterMove = MoonsPlugin.globalTickCount;
                            Rs2Walker.walkFastCanvas(closestTile);
                            // Record the current tick so we can wait exactly 1 tick
                            final long moveCompleteTick = afterMove + 1;
                            // Wait until exactly 1 tick has passed
                            sleepUntil(() -> MoonsPlugin.globalTickCount > moveCompleteTick, 1000);

                            // Now attack the jaguar if we're on the safe tile
                            if (!bloodJaguar.isDead() && Rs2Player.getWorldLocation().distanceTo(closestTile) <= 0) {
                                Rs2Npc.attack(bloodJaguar);
                                Microbot.log("Globaltick count attacked jaguar: " + MoonsPlugin.globalTickCount);
                            }
                            return;
                        }
                    }, 0, TimeUnit.MILLISECONDS);
                }
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    protected void shutDown() {
        scriptStartTime = null;
        moonsScript.shutdown();
        overlayManager.remove(moonsOverlay);
        tileExecutor.shutdown();
        dangerousGraphicsObjectTiles.clear();
    }

    protected void init() {
        if (tileExecutor == null) {
            tileExecutor = Executors.newSingleThreadScheduledExecutor();
            tileExecutor.scheduleWithFixedDelay(() -> {
                if (MoonsScript.moonsState == MoonsState.FIGHTING_ECLIPSE) {
                    eclipseNpcs.removeIf(eclipseNpc -> globalTickCount - eclipseNpc.getValue() > 12);
                } else {
                    eclipseNpcs.clear();
                }

                if (dangerousGraphicsObjectTiles.isEmpty()) {
                    return;
                }

                for (MutablePair<WorldPoint, Integer> dangerousTile : dangerousGraphicsObjectTiles) {
                    dangerousTile.setValue(dangerousTile.getValue() - 600);
                }
                // Remove expired tiles
                dangerousGraphicsObjectTiles.removeIf(x -> x.getValue() <= 0);

            }, 0, 600, TimeUnit.MILLISECONDS);
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        final var groundObject = event.getGameObject();
        if (groundObject.getId() == 51046) {
            final var tile = groundObject.getWorldLocation();
            dangerousGraphicsObjectTiles.add(new MutablePair<>(tile, (4) * 600));
            Rs2Tile.addDangerousGameObjectTile(groundObject, (4) * 600);

            // Record the tick when blood special spawns
            lastBloodSpecialSpawnTick = globalTickCount;
        }
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event) {
        final var graphicsObject = event.getGraphicsObject();
        final int id = graphicsObject.getId();
        if (id == 2771) {
            Rs2Tile.addDangerousGraphicsObjectTile(graphicsObject, 600 * 4);
        } else {
            Microbot.log("GraphicsObject with id " + id);
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        globalTickCount++;
        if (globalTickCount == Long.MAX_VALUE) {
            globalTickCount = 0;
        }
    }

    @Subscribe
    private void onNpcSpawned(NpcSpawned npcSpawned) {
        if (MoonsScript.moonsState == MoonsState.FIGHTING_ECLIPSE) {
            Microbot.log("Eclipse NPC spawned: " + npcSpawned.getNpc().getId() + " at " + npcSpawned.getNpc().getWorldLocation() + " tick: " + globalTickCount);
            var npc = npcSpawned.getNpc();
            if (npc.getId() == MoonsConstants.ECLIPSE_NPC_ID) {
                final var tile = npc.getWorldLocation();
                eclipseNpcs.add(new MutablePair<WorldPoint, Long>(tile, globalTickCount));
            }
        } else {
            eclipseNpcs.clear();
        }
    }
}
