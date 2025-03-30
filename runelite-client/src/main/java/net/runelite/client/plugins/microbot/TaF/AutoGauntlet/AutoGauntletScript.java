package net.runelite.client.plugins.microbot.TaF.AutoGauntlet;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.FishingSpot;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.TaF.AutoGauntlet.ResourceData.ResourceNodes;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static javax.swing.UIManager.put;
import static net.runelite.client.plugins.microbot.TaF.TzhaarVenatorBow.TzhaarVenatorBowScript.TravelStatus.TO_BANK;
import static net.runelite.client.plugins.microbot.TaF.TzhaarVenatorBow.TzhaarVenatorBowScript.TravelStatus.TO_TZHAAR;
import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;
import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.VERY_LOW;

public class AutoGauntletScript extends Script {
    public static final String VERSION = "1.0";
    public static State BOT_STATUS = State.TRAVELLING;
    public static int TotalLootValue = 0;
    private boolean isRunning;
    private Map<Integer, List<Integer>> connectedRoomsMap;

    {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings();
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.simulateFatigue = false;
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.dynamicActivity = true;
        Rs2AntibanSettings.profileSwitching = true;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.simulateMistakes = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.moveMouseRandomlyChance = 0.04;
        Rs2Antiban.setActivityIntensity(EXTREME);
        resourceNodes = new ResourceNodes();
        connectedRoomsMap = new HashMap<>();
    }

    public boolean run(AutoGauntletConfig config) {
        isRunning = true;
        BOT_STATUS = State.TRAVELLING;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) return;
                switch (BOT_STATUS) {
                    case PREPARING:
                        handlePreparing(config);
                        break;
                }
            } catch (Exception ex) {
                System.out.println("Exception message: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handlePreparing(AutoGauntletConfig config) {

    }


    private void Loot(AutoGauntletConfig config) {

    }



    private final List<Integer> NodeIds = List.of(35998, 35999);
    private final List<Integer> FishingSpots = List.of(35971);
    private final List<Integer> OreSpots = List.of(35967);
    private final List<Integer> CraftingSpots = List.of(35975);
    private final List<Integer> HerbSpots = List.of(35973);
    private final List<Integer> RootsSpots = List.of(35969);
    private final List<Integer> SingingBowl = List.of(35966);
    private final List<Integer> Range = List.of(35980);
    private final List<Integer> WaterPump = List.of(35981);

    private ResourceNodes resourceNodes = new ResourceNodes();

    protected Map<String, Integer> collectedResources = new HashMap<String, Integer>() {{
        put("Ores", 0);
        put("Roots", 0);
        put("Crafting", 0);
        put("Herbs", 0);
        put("Raw fish", 0);
    }};
    private static final int TILE_DISTANCE = 16;

    private static final int BOSS_ROOM = 25;

    public static final List<Integer> DEMI_ROOM_LIST = List.of(3, 4, 5, 15, 21, 22, 28, 29, 35, 45, 46, 47);

    private static final Set<Integer> RESOURCE_NODE_IDS = ImmutableSet.of(
            ObjectID.CRYSTAL_DEPOSIT,
            ObjectID.CORRUPT_DEPOSIT,
            ObjectID.PHREN_ROOTS,
            ObjectID.CORRUPT_PHREN_ROOTS,
            ObjectID.LINUM_TIRINUM,
            ObjectID.CORRUPT_LINUM_TIRINUM,
            ObjectID.GRYM_ROOT,
            ObjectID.CORRUPT_GRYM_ROOT,
            ObjectID.FISHING_SPOT_36068,
            ObjectID.CORRUPT_FISHING_SPOT
    );

    private static final Set<Integer> DEMI_BOSS_IDS = ImmutableSet.of(
            NpcID.CRYSTALLINE_BEAR,
            NpcID.CORRUPTED_BEAR,
            NpcID.CRYSTALLINE_DRAGON,
            NpcID.CORRUPTED_DRAGON,
            NpcID.CRYSTALLINE_DARK_BEAST,
            NpcID.CORRUPTED_DARK_BEAST
    );

    public void updateDemiBossLocations(WorldPoint player, NPC npc) {

    }

    public boolean isNewSession() {
        return false;
    }

    public void updateCurrentRoom(WorldPoint worldLocation) {

    }

    public void gameObjectSpawned(GameObject gameObject) {

    }

    public void gameObjectDespawned(GameObject gameObject) {

    }

    private void createStartingMaps()
    {
        Map<Integer, String> fileNameMap = new TreeMap<>();
        Map<Integer, List<Integer>> connectedRoomsMap = new TreeMap<>();

        for (int room = 1; room <= 49; room++)
        {
            List<Integer> connectedRoomsList = new ArrayList<>();

            switch (room)
            {
                case 1:
                    connectedRoomsList.addAll(Arrays.asList(2, 8));
                    break;
                case 7:
                    connectedRoomsList.addAll(Arrays.asList(6, 14));
                    break;
                case 43:
                    connectedRoomsList.addAll(Arrays.asList(36, 44));
                    break;
                case 49:
                    connectedRoomsList.addAll(Arrays.asList(42, 48));
                    break;
                case 8:
                case 15:
                case 22:
                case 29:
                case 36:
                    connectedRoomsList.addAll(Arrays.asList(room - 7, room + 1, room + 7));
                    break;
                case 14:
                case 21:
                case 28:
                case 35:
                case 42:
                    connectedRoomsList.addAll(Arrays.asList(room - 7, room - 1, room + 7));
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    connectedRoomsList.addAll(Arrays.asList(room - 1, room + 1, room + 7));
                    break;
                case 44:
                case 45:
                case 46:
                case 47:
                case 48:
                    connectedRoomsList.addAll(Arrays.asList(room - 7, room - 1, room + 1));
                    break;
                default:
                    connectedRoomsList.addAll(Arrays.asList(room - 7, room - 1, room + 1, room + 7));
                    break;
            }
            connectedRoomsMap.put(room, connectedRoomsList);
        }
        this.connectedRoomsMap = connectedRoomsMap;
    }

    public enum State {LOOTING, TRAVELLING, PREPARING, FIGHTING_BOSS}

}
