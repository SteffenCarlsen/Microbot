package net.runelite.client.plugins.microbot.TaF.PitFallTrapHunter;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.hunter.HunterTrap;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pluginscheduler.api.SchedulablePlugin;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.AndCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.condition.logical.LogicalCondition;
import net.runelite.client.plugins.microbot.pluginscheduler.event.PluginScheduleEntrySoftStopEvent;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@PluginDescriptor(
        name = PluginDescriptor.TaFCat + "Pitfall Trap Hunter",
        description = "Automates hunting all creatures with Pitfall traps",
        tags = {"hunter", "Pitfall", "skilling", "xp", "loot", "TaF"},
        enabledByDefault = false
)
public class PitFallTrapHunterPlugin extends Plugin implements SchedulablePlugin {

    @Getter
    private final Map<WorldPoint, HunterTrap> traps = new HashMap<>();
    @Inject
    private Client client;
    @Inject
    private PitFallTrapHunterConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private PitFallTrapHunterOverlay fitFallTrapHunterOverlay;
    private PitFallTrapHunterScript script;
    private PitFallTrapInventoryHandlerScript looter;
    private WorldPoint lastTickLocalPlayerLocation;
    private Instant scriptStartTime;
    private LogicalCondition stopCondition = new AndCondition();

    @Provides
    PitFallTrapHunterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PitFallTrapHunterConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        log.info("Pitfall hunter plugin started!");
        scriptStartTime = Instant.now();
        overlayManager.add(fitFallTrapHunterOverlay);
        script = new PitFallTrapHunterScript();
        script.run(config, this);
        looter = new PitFallTrapInventoryHandlerScript();
        looter.run(config, script);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Pitfall hunter plugin stopped!");
        scriptStartTime = null;
        overlayManager.remove(fitFallTrapHunterOverlay);
        if (script != null) {
            script.shutdown();
            looter.shutdown();
        }
    }

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        final GameObject gameObject = event.getGameObject();
        final WorldPoint trapLocation = gameObject.getWorldLocation();
        final HunterTrap myTrap = traps.get(trapLocation);
        final Player localPlayer = client.getLocalPlayer();
        switch (gameObject.getId()) {
            /*
             * ------------------------------------------------------------------------------
             * Placing traps
             * ------------------------------------------------------------------------------
             */
            case ObjectID.HUNTING_PITFALL_INVIS_CATCHING: // 19229
            case ObjectID.HUNTING_PITFALL_INVIS_COLLPASED: // 19230
            case ObjectID.HUNTER_PITFALL_FULL_GRAAHK: // 19231
            case ObjectID.HUNTER_PITFALL_FULL_LARUPIA: // 19232
            case ObjectID.HUNTER_PITFALL_FULL_KYATT: // 19233
            case ObjectID.HUNTER_PITFALL_FULL_GRAAHK_180: // 19234
            case ObjectID.HUNTER_PITFALL_FULL_LARUPIA_180: // 19235
            case ObjectID.HUNTER_PITFALL_FULL_KYATT_180: // 19236
                if (myTrap != null) {
                    myTrap.setState(HunterTrap.State.FULL);
                    myTrap.resetTimer();
                }

                break;
            case ObjectID.HUNTING_DEADFALL_BOULDER: //Empty deadfall trap
                if (myTrap != null) {
                    myTrap.setState(HunterTrap.State.EMPTY);
                    myTrap.resetTimer();
                }

                break;
            // Pitfall trap transition
            case ObjectID.HUNTING_PITFALL_SPIKES_LARGE_SW:
            case ObjectID.HUNTING_PITFALL_SPIKES_LARGE_SE:
            case ObjectID.HUNTING_PITFALL_SPIKES_LARGE_NW:
            case ObjectID.HUNTING_PITFALL_SPIKES_LARGE_NE:
            case 51673: // Spiked pitfall trap for Sunlight and Moonlight antelopes
                if (myTrap != null) {
                    myTrap.setState(HunterTrap.State.TRANSITION);
                }
                break;

            // Pitfall trap fallover
            case ObjectID.HUNTING_PITFALL_WOODLAND_LARGE_SW:
            case ObjectID.HUNTING_PITFALL_WOODLAND_LARGE_SE:
            case ObjectID.HUNTING_PITFALL_WOODLAND_LARGE_NW:
            case ObjectID.HUNTING_PITFALL_WOODLAND_LARGE_NE:
            case ObjectID.HUNTING_PITFALL_DESERT_LARGE_SW:
            case ObjectID.HUNTING_PITFALL_DESERT_LARGE_SE:
            case ObjectID.HUNTING_PITFALL_DESERT_LARGE_NW:
            case ObjectID.HUNTING_PITFALL_DESERT_LARGE_NE:
            case ObjectID.HUNTING_PITFALL_POLAR_LARGE_SW:
            case ObjectID.HUNTING_PITFALL_POLAR_LARGE_SE:
            case ObjectID.HUNTING_PITFALL_POLAR_LARGE_NW:
            case ObjectID.HUNTING_PITFALL_POLAR_LARGE_NE:
            case ObjectID.HUNTING_PITFALL_1:
            case ObjectID.HUNTING_PITFALL_2:
            case ObjectID.HUNTING_PITFALL_3:
            case ObjectID.HUNTING_PITFALL_4:
            case ObjectID.HUNTING_PITFALL_5:
            case ObjectID.HUNTING_PITFALL_6:
            case ObjectID.HUNTING_PITFALL_7:
            case ObjectID.HUNTING_PITFALL_8:
            case ObjectID.HUNTING_PITFALL_9:
            case ObjectID.HUNTING_PITFALL_10:
            case ObjectID.HUNTING_PITFALL_11:
            case ObjectID.HUNTING_PITFALL_12:
            case ObjectID.HUNTING_PITFALL_13:
            case ObjectID.HUNTING_PITFALL_14:
            case ObjectID.HUNTING_PITFALL_15:
            case ObjectID.HUNTING_PITFALL_16:
            case ObjectID.HUNTING_PITFALL_1_NW:
            case ObjectID.HUNTING_PITFALL_2_NW:
            case ObjectID.HUNTING_PITFALL_3_NW:
            case ObjectID.HUNTING_PITFALL_4_NW:
            case ObjectID.HUNTING_PITFALL_5_NW:
            case ObjectID.HUNTING_PITFALL_6_NW:
            case ObjectID.HUNTING_PITFALL_7_NW:
            case ObjectID.HUNTING_PITFALL_8_NW:
            case ObjectID.HUNTING_PITFALL_9_NW:
            case ObjectID.HUNTING_PITFALL_10_NW:
            case ObjectID.HUNTING_PITFALL_11_NW:
            case ObjectID.HUNTING_PITFALL_12_NW:
            case ObjectID.HUNTING_PITFALL_13_NW:
            case ObjectID.HUNTING_PITFALL_14_NW:
            case ObjectID.HUNTING_PITFALL_15_NW:
            case ObjectID.HUNTING_PITFALL_16_NW:
            case ObjectID.HUNTING_PITFALL_1_NE:
            case ObjectID.HUNTING_PITFALL_2_NE:
            case ObjectID.HUNTING_PITFALL_3_NE:
            case ObjectID.HUNTING_PITFALL_4_NE:
            case ObjectID.HUNTING_PITFALL_5_NE:
            case ObjectID.HUNTING_PITFALL_6_NE:
            case ObjectID.HUNTING_PITFALL_7_NE:
            case ObjectID.HUNTING_PITFALL_8_NE:
            case ObjectID.HUNTING_PITFALL_9_NE:
            case ObjectID.HUNTING_PITFALL_10_NE:
            case ObjectID.HUNTING_PITFALL_11_NE:
            case ObjectID.HUNTING_PITFALL_12_NE:
            case ObjectID.HUNTING_PITFALL_13_NE:
            case ObjectID.HUNTING_PITFALL_14_NE:
            case ObjectID.HUNTING_PITFALL_15_NE:
            case ObjectID.HUNTING_PITFALL_16_NE:
            case ObjectID.HUNTING_PITFALL_1_SE:
            case ObjectID.HUNTING_PITFALL_2_SE:
            case ObjectID.HUNTING_PITFALL_3_SE:
            case ObjectID.HUNTING_PITFALL_4_SE:
            case ObjectID.HUNTING_PITFALL_5_SE:
            case ObjectID.HUNTING_PITFALL_6_SE:
            case ObjectID.HUNTING_PITFALL_7_SE:
            case ObjectID.HUNTING_PITFALL_8_SE:
            case ObjectID.HUNTING_PITFALL_9_SE:
            case ObjectID.HUNTING_PITFALL_10_SE:
            case ObjectID.HUNTING_PITFALL_11_SE:
            case ObjectID.HUNTING_PITFALL_12_SE:
            case ObjectID.HUNTING_PITFALL_13_SE:
            case ObjectID.HUNTING_PITFALL_14_SE:
            case ObjectID.HUNTING_PITFALL_15_SE:
            case ObjectID.HUNTING_PITFALL_16_SE:
            case ObjectID.HUNTING_PITFALL_1_SW:
            case ObjectID.HUNTING_PITFALL_2_SW:
            case ObjectID.HUNTING_PITFALL_3_SW:
            case ObjectID.HUNTING_PITFALL_4_SW:
            case ObjectID.HUNTING_PITFALL_5_SW:
            case ObjectID.HUNTING_PITFALL_6_SW:
            case ObjectID.HUNTING_PITFALL_7_SW:
            case ObjectID.HUNTING_PITFALL_8_SW:
            case ObjectID.HUNTING_PITFALL_9_SW:
            case ObjectID.HUNTING_PITFALL_10_SW:
            case ObjectID.HUNTING_PITFALL_11_SW:
            case ObjectID.HUNTING_PITFALL_12_SW:
            case ObjectID.HUNTING_PITFALL_13_SW:
            case ObjectID.HUNTING_PITFALL_14_SW:
            case ObjectID.HUNTING_PITFALL_15_SW:
            case ObjectID.HUNTING_PITFALL_16_SW:
                if (myTrap != null) {
                    myTrap.setState(HunterTrap.State.EMPTY);
                }
                break;

        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() == ChatMessageType.GAMEMESSAGE && event.getMessage().equalsIgnoreCase("oh dear, you are dead!")) {
            script.hasDied = true;
        }
        if (event.getType() == ChatMessageType.GAMEMESSAGE && event.getMessage().contains("You don't have enough inventory space. You need")) {
            script.forceBank = true;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        // Check if all traps are still there, and remove the ones that are not.
        Iterator<Map.Entry<WorldPoint, HunterTrap>> it = traps.entrySet().iterator();
        Tile[][][] tiles = client.getScene().getTiles();

        Instant expire = Instant.now().minus(HunterTrap.TRAP_TIME.multipliedBy(2));

        while (it.hasNext()) {
            Map.Entry<WorldPoint, HunterTrap> entry = it.next();
            HunterTrap trap = entry.getValue();
            WorldPoint world = entry.getKey();
            LocalPoint local = LocalPoint.fromWorld(client, world);

            // Not within the client's viewport
            if (local == null) {
                // Cull very old traps
                if (trap.getPlacedOn().isBefore(expire)) {
                    log.debug("Trap removed from personal trap collection due to timeout, {} left", traps.size());
                    it.remove();
                    continue;
                }
                continue;
            }

            Tile tile = tiles[world.getPlane()][local.getSceneX()][local.getSceneY()];
            GameObject[] objects = tile.getGameObjects();

            boolean containsBoulder = false;
            boolean containsAnything = false;
            boolean containsYoungTree = false;
            for (GameObject object : objects) {
                if (object != null) {
                    containsAnything = true;
                    if (object.getId() == ObjectID.HUNTING_DEADFALL_BOULDER || object.getId() == ObjectID.HUNTING_MONKEYTRAP_UNSET) {
                        containsBoulder = true;
                        break;
                    }

                    // Check for young trees (used while catching salamanders) in the tile.
                    // Otherwise, hunter timers will never disappear after a trap is dismantled
                    if (object.getId() == ObjectID.HUNTING_SAPLING_UP_ORANGE || object.getId() == ObjectID.HUNTING_SAPLING_UP_RED ||
                            object.getId() == ObjectID.HUNTING_SAPLING_UP_BLACK || object.getId() == ObjectID.HUNTING_SAPLING_UP_SWAMP ||
                            object.getId() == ObjectID.HUNTING_SAPLING_UP_MOUNTAIN || object.getId() == ObjectID.HUNTING_SAPLING_SETTING_MOUNTAIN) {
                        containsYoungTree = true;
                    }
                }
            }

            if (!containsAnything || containsYoungTree) {
                it.remove();
                log.debug("Trap removed from personal trap collection, {} left", traps.size());
            } else if (containsBoulder) // For traps like deadfalls. This is different because when the trap is gone, there is still a GameObject (boulder)
            {
                it.remove();
                log.debug("Special trap removed from personal trap collection, {} left", traps.size());

                // Case we have notifications enabled and the action was not manual, throw notification
                if (trap.getObjectId() == ObjectID.HUNTING_MONKEYTRAP_SET && !trap.getState().equals(HunterTrap.State.FULL) && !trap.getState().equals(HunterTrap.State.OPEN)) {
                    //notifier.notify(config.maniacalMonkeyNotify(), "The monkey escaped.");
                }
            }
        }

        lastTickLocalPlayerLocation = client.getLocalPlayer().getWorldLocation();
    }

    @Override
    public void onPluginScheduleEntrySoftStopEvent(PluginScheduleEntrySoftStopEvent event) {
        if (event.getPlugin() == this) {
            if (script != null) {
                Rs2Bank.walkToBank();
            }
            Microbot.stopPlugin(this);
        }
    }

    @Override
    public LogicalCondition getStopCondition() {
        // Create a new stop condition
        return this.stopCondition;
    }
}
