package net.runelite.client.plugins.microbot.TaF.DemonicGorillaKiller;

import net.runelite.api.HeadIcon;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.LootingParameters;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.reflection.Rs2Reflection;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;

// Script heavily inspired by Tormented Demon script
public class DemonicGorillaScript extends Script {

    public static final double VERSION = 1.0;
    public static final int DEMONIC_GORILLA_MAGIC_ATTACK = 7225;
    public static final int DEMONIC_GORILLA_MELEE_ATTACK = 7226;
    public static final int DEMONIC_GORILLA_RANGED_ATTACK = 7227;
    public static final int DEMONIC_GORILLA_AOE_ATTACK = 7228;
    public static final int DEMONIC_GORILLA_PRAYER_SWITCH = 7228;
    public static final int DEMONIC_GORILLA_DEFEND = 7224;
    // Behind rope in the entrance of the cave
    private static final WorldPoint SAFE_LOCATION = new WorldPoint(2465, 3494, 0);
    public static int killCount = 0;
    public static int currentTripKillCount = 0;
    public static Rs2PrayerEnum currentDefensivePrayer = null;
    public static Rs2NpcModel currentTarget = null;
    public static boolean lootAttempted = false;
    public static State BOT_STATUS = State.BANKING;
    public static TravelStep travelStep = TravelStep.GNOME_STRONGHOLD;
    public static int npcAnimationCount = 0;
    public static int lastAnimation = 0;
    public static int lastRealAnimation = 0;
    public static int lastAttackAnimation = 0;
    public static int gameTickCount = 0;
    public LocalPoint demonicGorillaRockPosition = null;
    public int demonicGorillaRockLifeCycle = -1;
    private boolean isRunning = false;
    private Rs2PrayerEnum currentOffensivePrayer = null;
    private HeadIcon currentOverheadIcon = null;
    private String lastChatMessage = "";
    private BankingStep bankingStep = BankingStep.BANK;
    private Instant outOfCombatTime = Instant.now();
    private WorldPoint lastLocation = new WorldPoint(0, 0, 0);
    // In some cases, the player may be stuck out of combat with an invalid target
    private int failedCount = 0;
    private int lastGameTick = 0;

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
        Rs2AntibanSettings.moveMouseOffScreen = false;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.moveMouseRandomlyChance = 0.04;
        Rs2Antiban.setActivityIntensity(EXTREME);
    }

    public boolean run(DemonicGorillaConfig config) {
        bankingStep = BankingStep.BANK;
        travelStep = TravelStep.GNOME_STRONGHOLD;
        Microbot.enableAutoRunOn = false;
        isRunning = true;
        disableAllPrayers();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) return;
                switch (BOT_STATUS) {
                    case BANKING:
                        handleBanking(config);
                        break;
                    case TRAVEL_TO_GORILLAS:
                        handleTravel(config);
                        break;
                    case FIGHTING:
                        handleFighting(config);
                        break;
                }
            } catch (Exception ex) {
                logOnceToChat("Error in main loop: " + ex.getMessage());
                System.out.println("Exception message: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleTravel(DemonicGorillaConfig config) {
        WorldPoint gnomeStrongholdLocation = new WorldPoint(2465, 3494, 0);
        WorldPoint crashSiteLocation = new WorldPoint(2435, 3519, 0);
        WorldPoint caveLocation = new WorldPoint(2026, 5610, 0);
        WorldPoint gorillaLocation = new WorldPoint(2103, 5655, 0);

        switch (travelStep) {
            case GNOME_STRONGHOLD:
                Microbot.status = "Teleporting to The Grand Tree";
                if (Rs2Inventory.interact("Royal seed pod", "Commune")) {
                    Rs2Player.waitForAnimation();
                    sleepUntil(() -> !Rs2Player.isAnimating());
                    sleepUntil(() -> Microbot.getClient().getLocalPlayer().getWorldLocation().distanceTo(gnomeStrongholdLocation) <= 5);
                    travelStep = TravelStep.TRAVEL_TO_OPENING;
                }
                break;
            case TRAVEL_TO_OPENING:
                Rs2Walker.walkTo(crashSiteLocation);
                sleepUntil(() -> Microbot.getClient().getLocalPlayer().getWorldLocation().distanceTo(crashSiteLocation) <= 5);
                travelStep = TravelStep.CRASH_SITE;
                break;
            case CRASH_SITE:
                Microbot.status = "Passing through opening to Crash Site";
                var interacted = Rs2GameObject.interact(28807, "Pass-through");
                if (interacted) {
                    Rs2Player.waitForAnimation();
                    sleepUntil(() -> !Rs2Player.isAnimating());
                    Microbot.status = "Walking to the cave entrance";
                    Rs2Walker.walkTo(caveLocation);
                    sleepUntil(() -> Microbot.getClient().getLocalPlayer().getWorldLocation().distanceTo(caveLocation) <= 5);
                    interacted = Rs2GameObject.interact(28686, "Enter");
                    if (interacted) {
                        Rs2Player.waitForAnimation();
                        sleepUntil(() -> !Rs2Player.isAnimating());
                    }
                    travelStep = TravelStep.IN_CAVE;
                }
                break;

            case IN_CAVE:
                currentDefensivePrayer = Rs2PrayerEnum.PROTECT_MAGIC;
                Rs2Prayer.toggle(currentDefensivePrayer, true);
                Microbot.status = "Walking to the gorillas";
                Rs2Walker.walkTo(gorillaLocation);
                sleepUntil(() -> Microbot.getClient().getLocalPlayer().getWorldLocation().distanceTo(gorillaLocation) <= 10);
                Microbot.status = "Arrived at Gorillas";
                BOT_STATUS = State.FIGHTING;
                travelStep = TravelStep.GNOME_STRONGHOLD;
                break;
        }
    }

    private void handleBanking(DemonicGorillaConfig config) {
        switch (bankingStep) {
            case BANK:
                Rs2Bank.walkToBank();
                sleepUntil(() -> Microbot.getClient().getLocalPlayer().getWorldLocation().distanceTo(Rs2Bank.getNearestBank().getWorldPoint()) <= 5);
                Microbot.status = "Opening bank...";
                Rs2Bank.openBank();
                sleepUntil(Rs2Bank::isOpen);
                Rs2Bank.depositAll();

                bankingStep = BankingStep.LOAD_INVENTORY;
                break;

            case LOAD_INVENTORY:
                Microbot.status = "Loading inventory and equipment setup...";
                Rs2InventorySetup inventorySetup = new Rs2InventorySetup("Demonic Gorilla", mainScheduledFuture);
                boolean equipmentLoaded = inventorySetup.loadEquipment();
                boolean inventoryLoaded = inventorySetup.loadInventory();

                if (equipmentLoaded && inventoryLoaded) {
                    var ate = false;
                    boolean ateFood = false;
                    boolean drankPrayerPot = false;
                    while (Rs2Player.getHealthPercentage() < 70 || ateFood || drankPrayerPot) {
                        ateFood = Rs2Player.eatAt(80);
                        drankPrayerPot = Rs2Player.drinkPrayerPotionAt(config.minEatPercent());
                        ate = true;
                        sleep(1200);
                    }
                    if (ate) {
                        logOnceToChat("Restocked");
                        inventorySetup.loadInventory();
                    }
                    Rs2Bank.closeBank();
                    bankingStep = BankingStep.BANK;
                    BOT_STATUS = State.TRAVEL_TO_GORILLAS;
                } else {
                    shutdown();
                }
                break;
        }
    }

    private void handleFighting(DemonicGorillaConfig config) {
        if (currentTarget == null || currentTarget.isDead()) {
            logOnceToChat("Target is null or dead");
            handleNewTargetAndLootOld(config);
        }

        if (shouldRetreat(config)) {
            retreatToSafety();
            return;
        }

        // Ensure currently selected target is also who we are attacking
        if (currentTarget != null) {
            var tempTarget = getTarget(true);
            if ((tempTarget != null && tempTarget.getIndex() != currentTarget.getIndex()) || currentTarget.isDead()) {
                logOnceToChat("Invalid target was selected, switching to correct enemy");
                currentTarget = tempTarget;
            }
        }
        if (!Rs2Player.isInCombat() && outOfCombatTime == null) {
            outOfCombatTime = Instant.now();
        } else {
            outOfCombatTime = null;
        }
        if (outOfCombatTime != null && Instant.now().isAfter(outOfCombatTime.plusSeconds(8))) {
            logOnceToChat("Out of combat for 30 seconds, forcing new target");
            currentTarget = getTarget(true);
        }

        attackGorilla(config);
        if (currentTarget == null) return;
        handleDemonicGorillaAttacks(config);
        if (currentTarget == null) return;
        handleGearSwitching(config);
        if (currentTarget == null) return;
        evaluateAndConsumePotions(config);

        if (config.enableOffensivePrayer()) {
            activateOffensivePrayer(config);
        }
    }

    private void retreatToSafety() {
        currentTarget = null;
        currentOverheadIcon = null;
        currentTripKillCount = 0;
        Microbot.pauseAllScripts = true;
        escapeTosafety();
        sleepUntil(() -> Microbot.getClient().getLocalPlayer().getWorldLocation().equals(SAFE_LOCATION), 5000);
        disableAllPrayers();
        Microbot.pauseAllScripts = false;
        BOT_STATUS = State.BANKING;
    }

    private void handleNewTargetAndLootOld(DemonicGorillaConfig config) {
        if (!lootAttempted) {
            Microbot.pauseAllScripts = true;
            sleep(2600);
            attemptLooting(config);
            lootAttempted = true;
            if (currentTarget != null && currentTarget.isDead()) {
                killCount++;
                currentTripKillCount++;
            }
            currentTarget = null;
            Microbot.pauseAllScripts = false;
        }

        currentTarget = getTarget();
        if (currentTarget != null) {
            try {
                currentOverheadIcon = Rs2Reflection.getHeadIcon(currentTarget);
                if (currentOverheadIcon == null) {
                    logOnceToChat("Failed to retrieve HeadIcon for target - NULL");
                    return;
                }
            } catch (Exception e) {
                logOnceToChat("Failed to retrieve HeadIcon for target - Exception");
                return;
            }

            switchGear(config, currentOverheadIcon);
            lootAttempted = false;
        } else {
            logOnceToChat("No target found for attack.");
        }
    }

    private void attackGorilla(DemonicGorillaConfig config) {
        if (currentTarget != null && !currentTarget.isDead()) {
            Rs2Player.eatAt(config.minEatPercent());
            Rs2Player.drinkPrayerPotionAt(config.minPrayerPercent());
            if (currentTarget != null) {
                if (!Rs2Player.isAnimating(1600)) {
                    if (currentTarget != null && !currentTarget.isDead()) {
                        if (config.enableAutoSpecialAttacks()) {
                            Rs2Combat.setSpecState(true, 500);
                        }
                        Rs2Npc.attack(currentTarget);
                    }
                }
            }
        } else {
            logOnceToChat("CurrentTarget is null or dead");
        }
    }

    private void handleDemonicGorillaAttacks(DemonicGorillaConfig config) {
        Rs2PrayerEnum newDefensivePrayer = null;
        if (currentTarget != null && !currentTarget.isDead()) {
            int currentAnimation = currentTarget.getAnimation();
            var location = currentTarget.getWorldLocation();

            // Handle premature prayer switching
            if (npcAnimationCount >= 4 && currentAnimation == -1 && gameTickCount != lastGameTick) {
                logOnceToChat("Demonic Gorilla attacked 3 times - Prematurely changing prayer");
                if (lastAttackAnimation == DEMONIC_GORILLA_MAGIC_ATTACK) {
                    if (lastLocation.distanceTo(location) > 1) {
                        newDefensivePrayer = Rs2PrayerEnum.PROTECT_MELEE;
                    } else {
                        newDefensivePrayer = Rs2PrayerEnum.PROTECT_RANGE;
                    }
                } else if (lastAttackAnimation == DEMONIC_GORILLA_RANGED_ATTACK) {
                    if (lastLocation.distanceTo(location) > 1) {
                        newDefensivePrayer = Rs2PrayerEnum.PROTECT_MELEE;
                    } else {
                        newDefensivePrayer = Rs2PrayerEnum.PROTECT_MAGIC;
                    }
                } else if (lastAttackAnimation == DEMONIC_GORILLA_MELEE_ATTACK) {
                    newDefensivePrayer = Rs2PrayerEnum.PROTECT_MAGIC;
                }
            }

            // Handle premature prayer switching data
            if (gameTickCount != lastGameTick) {
                if (lastRealAnimation != currentAnimation && currentAnimation != -1 && currentAnimation != DEMONIC_GORILLA_AOE_ATTACK && currentAnimation != DEMONIC_GORILLA_DEFEND) {
                    npcAnimationCount = 0;
                    lastRealAnimation = 0;
                }
                if (lastAnimation == -1 && currentAnimation != DEMONIC_GORILLA_AOE_ATTACK && currentAnimation != DEMONIC_GORILLA_DEFEND) {
                    lastRealAnimation = currentAnimation;
                }
                if (currentAnimation == lastRealAnimation) {
                    npcAnimationCount++;
                }
            }

            // Handle normal prayer switching
            if (currentAnimation == DEMONIC_GORILLA_MAGIC_ATTACK) {
                newDefensivePrayer = Rs2PrayerEnum.PROTECT_MAGIC;
            } else if (currentAnimation == DEMONIC_GORILLA_RANGED_ATTACK) {
                newDefensivePrayer = Rs2PrayerEnum.PROTECT_RANGE;
            } else if (currentAnimation == DEMONIC_GORILLA_MELEE_ATTACK) {
                newDefensivePrayer = Rs2PrayerEnum.PROTECT_MELEE;
            } else if (currentAnimation == DEMONIC_GORILLA_PRAYER_SWITCH) {
                logOnceToChat("Handling gear switch based on prayer switching");
                handleGearSwitching(config);
            }

            // Switch defensive prayer if needed
            if (newDefensivePrayer != null && newDefensivePrayer != currentDefensivePrayer) {
                switchDefensivePrayer(newDefensivePrayer);
            }

            // Handle AOE attack
            if (currentAnimation == DEMONIC_GORILLA_AOE_ATTACK && demonicGorillaRockPosition != null) {
                List<WorldPoint> dangerousWorldPoints = Rs2Tile.getDangerousGraphicsObjectTiles()
                        .stream()
                        .map(Pair::getKey)
                        .collect(Collectors.toList());
                dangerousWorldPoints.add(Microbot.getClient().getLocalPlayer().getWorldLocation());
                dangerousWorldPoints.add(currentTarget.getWorldLocation());
                dangerousWorldPoints.add(location);
                if (demonicGorillaRockPosition != null) {
                    dangerousWorldPoints.add(new WorldPoint(demonicGorillaRockPosition.getX(), demonicGorillaRockPosition.getY(), demonicGorillaRockPosition.getWorldView()));
                }
                final WorldPoint safeTile = findSafeTile(Rs2Player.getWorldLocation(), dangerousWorldPoints);
                if (safeTile != null) {
                    Rs2Walker.walkFastCanvas(safeTile);
                    logOnceToChat("Walking to safe tile");
                    sleep(1600);
                }
                demonicGorillaRockPosition = null;
                demonicGorillaRockLifeCycle = -1;
            }

            if (currentAnimation != -1) {
                lastAttackAnimation = currentAnimation;
                lastLocation = location;
            }
            lastAnimation = currentAnimation;
        } else {
            logOnceToChat("CurrentTarget is null or dead - HandleGorillaAttacks");
        }

        lastGameTick = gameTickCount;
    }

    private void handleGearSwitching(DemonicGorillaConfig config) {
        try {
            if (failedCount >= 3) {
                currentTarget = getTarget(true);
                logOnceToChat("Forcing new target");
            }
            HeadIcon newOverheadIcon = Rs2Reflection.getHeadIcon(currentTarget);
            if (newOverheadIcon == null) return;
            if (newOverheadIcon != currentOverheadIcon) {
                currentOverheadIcon = newOverheadIcon;
                if (!Rs2Inventory.isOpen()) {
                    Rs2Inventory.open();
                    sleepUntil(Rs2Inventory::isOpen, 1000);
                }
                switchGear(config, currentOverheadIcon);
                sleep(100);
                failedCount = 0;
            }
        } catch (Exception e) {
            logOnceToChat("Failed to retrieve HeadIcon for target.");
            failedCount++;
        }
    }

    private void escapeTosafety() {
        Rs2Inventory.interact("Royal seed pod", "Commune");
    }

    public Rs2NpcModel getTarget() {
        return getTarget(false);
    }

    public Rs2NpcModel getTarget(boolean force) {
        if (currentTarget != null && !currentTarget.isDead() && !force) {
            return currentTarget;
        }
        var playerLocation = Microbot.getClient().getLocalPlayer().getWorldLocation();

        var alreadyInteractingNpcs = Rs2Npc.getNpcsForPlayer("Demonic gorilla");
        if (!alreadyInteractingNpcs.isEmpty()) {
            return alreadyInteractingNpcs.stream()
                    .min(Comparator.comparingInt(npc -> npc.getWorldLocation().distanceTo(playerLocation))).get();
        }

        var demonicGorillaStream = Rs2Npc.getNpcs("Demonic gorilla");
        if (demonicGorillaStream == null) {
            logOnceToChat("No demonic gorilla found.");
            return null;
        }

        var player = Rs2Player.getLocalPlayer();
        String playerName = player.getName();
        List<Rs2NpcModel> demonicGorillas = demonicGorillaStream.collect(Collectors.toList());

        for (Rs2NpcModel demonicGorilla : demonicGorillas) {
            if (demonicGorilla != null) {
                var interacting = demonicGorilla.getInteracting();
                String interactingName = interacting != null ? interacting.getName() : "None";
                if (interacting != null && Objects.equals(interactingName, playerName)) {
                    return demonicGorilla;
                }
            }
        }

        logOnceToChat("Finding closest demonic gorilla.");
        return demonicGorillas.stream()
                .filter(npc -> npc != null && !npc.isDead() && !npc.isInteracting())
                .min(Comparator.comparingInt(npc -> npc.getWorldLocation().distanceTo(playerLocation))).stream().findFirst()
                .orElse(null);
    }

    private WorldPoint findSafeTile(WorldPoint playerLocation, List<WorldPoint> dangerousWorldPoints) {
        List<WorldPoint> nearbyTiles = List.of(
                new WorldPoint(playerLocation.getX() + 1, playerLocation.getY(), playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX() + 2, playerLocation.getY(), playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX() - 1, playerLocation.getY(), playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX() - 2, playerLocation.getY(), playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX(), playerLocation.getY() + 1, playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX(), playerLocation.getY() + 2, playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX(), playerLocation.getY() - 1, playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX(), playerLocation.getY() - 2, playerLocation.getPlane())
        );

        for (WorldPoint tile : nearbyTiles) {
            final LocalPoint location = LocalPoint.fromWorld(Microbot.getClient(), tile);
            if (!dangerousWorldPoints.contains(tile) && Rs2Tile.isWalkable(location)) {
                logOnceToChat("Found safe tile: " + tile);
                return tile;
            }
        }
        logOnceToChat("No safe tile found!");
        return null;
    }

    private void switchDefensivePrayer(Rs2PrayerEnum newDefensivePrayer) {
        if (currentDefensivePrayer != null) {
            Rs2Prayer.toggle(currentDefensivePrayer, false);
        }
        Rs2Prayer.toggle(newDefensivePrayer, true);
        currentDefensivePrayer = newDefensivePrayer;
    }

    private void activateOffensivePrayer(DemonicGorillaConfig config) {
        Rs2PrayerEnum newOffensivePrayer = null;
        if (config.useMagicStyle() && isGearEquipped(parseGear(config.magicGear()))) {
            newOffensivePrayer = Rs2PrayerEnum.AUGURY;
        } else if (config.useRangeStyle() && isGearEquipped(parseGear(config.rangeGear()))) {
            newOffensivePrayer = Rs2PrayerEnum.RIGOUR;
        } else if (config.useMeleeStyle() && isGearEquipped(parseGear(config.meleeGear()))) {
            newOffensivePrayer = Rs2PrayerEnum.PIETY;
        }

        if (newOffensivePrayer != null && newOffensivePrayer != currentOffensivePrayer) {
            switchOffensivePrayer(newOffensivePrayer);
            sleep(100);
        }
    }

    private void switchOffensivePrayer(Rs2PrayerEnum newOffensivePrayer) {
        if (currentOffensivePrayer != null) {
            Rs2Prayer.toggle(currentOffensivePrayer, false);
        }
        Rs2Prayer.toggle(newOffensivePrayer, true);
        currentOffensivePrayer = newOffensivePrayer;
    }

    private void switchGear(DemonicGorillaConfig config, HeadIcon combatNpcHeadIcon) {
        List<String> gearToEquip = new ArrayList<>();
        boolean useRange = config.useRangeStyle();
        boolean useMagic = config.useMagicStyle();
        boolean useMelee = config.useMeleeStyle();

        switch (combatNpcHeadIcon) {
            case RANGED:
                if (useMelee && useMagic) {
                    gearToEquip = Math.random() < 0.5 ? parseGear(config.meleeGear()) : parseGear(config.magicGear());
                } else if (useMelee) {
                    gearToEquip = parseGear(config.meleeGear());
                } else if (useMagic) {
                    gearToEquip = parseGear(config.magicGear());
                }
                break;

            case MAGIC:
                if (useRange && useMelee) {
                    gearToEquip = Math.random() < 0.5 ? parseGear(config.rangeGear()) : parseGear(config.meleeGear());
                } else if (useRange) {
                    gearToEquip = parseGear(config.rangeGear());
                } else if (useMelee) {
                    gearToEquip = parseGear(config.meleeGear());
                }
                break;

            case MELEE:
                if (useRange && useMagic) {
                    gearToEquip = Math.random() < 0.5 ? parseGear(config.rangeGear()) : parseGear(config.magicGear());
                } else if (useRange) {
                    gearToEquip = parseGear(config.rangeGear());
                } else if (useMagic) {
                    gearToEquip = parseGear(config.magicGear());
                }
                break;
        }

        if (!isGearEquipped(gearToEquip)) {
            equipGear(gearToEquip);
        }
    }

    private List<String> parseGear(String gearString) {
        return Arrays.asList(gearString.split(","));
    }

    private boolean isGearEquipped(List<String> gear) {
        return gear.stream().allMatch(Rs2Equipment::isWearing);
    }

    private void equipGear(List<String> gear) {
        for (String item : gear) {
            Rs2Inventory.wield(item);
            sleep(50);
        }
    }

    private boolean shouldRetreat(DemonicGorillaConfig config) {
        int currentHealth = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
        int currentPrayer = Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER);
        boolean noFood = Rs2Inventory.getInventoryFood().isEmpty();
        boolean noPrayerPotions = Rs2Inventory.items().stream()
                .noneMatch(item -> item != null && item.getName() != null && item.getName().toLowerCase().contains("prayer potion"));

        return (noFood && currentHealth <= config.healthThreshold()) || (noPrayerPotions && currentPrayer < 10);
    }

    public void disableAllPrayers() {
        Rs2Prayer.disableAllPrayers();
        currentDefensivePrayer = null;
        currentOffensivePrayer = null;
    }

    private void attemptLooting(DemonicGorillaConfig config) {
        Microbot.log("Checking loot..");
        var itemsOnValue = Rs2GroundItem.lootItemBasedOnValue(1500, 15);
        Microbot.log("Looted items: " + itemsOnValue);
        var counter = 0;
        while (itemsOnValue) {
            sleepUntil(() -> !Rs2Player.isAnimating());
            sleep(1200);
            itemsOnValue = Rs2GroundItem.lootItemBasedOnValue(1500, 15);
            logOnceToChat("Loot count: " + ++counter);
        }

        if (config.scatterAshes()) {
            lootAndScatterMalicious();
        }
    }

    private void lootAndScatterMalicious() {
        String ashesName = "Malicious ashes";

        if (!Rs2Inventory.isFull() && Rs2GroundItem.lootItemsBasedOnNames(new LootingParameters(10, 1, 1, 0, false, true, ashesName))) {
            sleepUntil(() -> Rs2Inventory.contains(ashesName), 2000);

            if (Rs2Inventory.contains(ashesName)) {
                Rs2Inventory.interact(ashesName, "Scatter");
                sleep(600); // Wait briefly for scattering action
            }
        }
    }

    private void evaluateAndConsumePotions(DemonicGorillaConfig config) {
        int threshold = config.boostedStatsThreshold();

        if (!isCombatPotionActive(config.combatPotionType(), threshold)) {
            consumeCombatPotion(config.combatPotionType());
        }

        if (!isRangingPotionActive(config.rangingPotionType(), threshold)) {
            consumeRangingPotion(config.rangingPotionType());
        }
    }

    private boolean isCombatPotionActive(DemonicGorillaConfig.CombatPotionType combatPotionType, int threshold) {
        switch (combatPotionType) {
            case SUPER_COMBAT:
                return Rs2Player.hasAttackActive(threshold) && Rs2Player.hasStrengthActive(threshold);
            case DIVINE_SUPER_COMBAT:
                return Rs2Player.hasDivineCombatActive();
            default:
                return true;
        }
    }

    private boolean isRangingPotionActive(DemonicGorillaConfig.RangingPotionType rangingPotionType, int threshold) {
        switch (rangingPotionType) {
            case RANGING:
                return Rs2Player.hasRangingPotionActive(threshold);
            case DIVINE_RANGING:
                return Rs2Player.hasDivineRangedActive();
            case BASTION:
                return Rs2Player.hasDivineBastionActive();
            default:
                return true;
        }
    }

    private void consumeCombatPotion(DemonicGorillaConfig.CombatPotionType combatPotionType) {
        String potion = null;
        switch (combatPotionType) {
            case SUPER_COMBAT:
                potion = "super combat";
                break;
            case DIVINE_SUPER_COMBAT:
                potion = "divine super combat";
                break;
            default:
                return;
        }
        consumePotion(potion);
    }

    private void consumeRangingPotion(DemonicGorillaConfig.RangingPotionType rangingPotionType) {
        String potion = null;
        switch (rangingPotionType) {
            case RANGING:
                potion = "ranging potion";
                break;
            case DIVINE_RANGING:
                potion = "divine ranging potion";
                break;
            case BASTION:
                potion = "bastion potion";
                break;
            default:
                return;
        }
        consumePotion(potion);
    }

    private void consumePotion(String keyword) {
        Rs2Inventory.getPotions().stream()
                .filter(potion -> potion.getName().toLowerCase().contains(keyword))
                .findFirst()
                .ifPresent(potion -> {
                    Rs2Inventory.interact(potion, "Drink");
                });
    }

    void logOnceToChat(String message) {
        if (!message.equals(lastChatMessage)) {
            Microbot.log(message);
            lastChatMessage = message;
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        isRunning = false;
        BOT_STATUS = State.BANKING;
        travelStep = TravelStep.GNOME_STRONGHOLD;
        bankingStep = BankingStep.BANK;
        currentTarget = null;
        killCount = 0;
        lootAttempted = false;  // Reset here
        currentDefensivePrayer = null;
        currentOffensivePrayer = null;
        currentOverheadIcon = null;
        disableAllPrayers();
        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }
        logOnceToChat("Shutting down Demonic Gorilla script");
    }

    public enum State {BANKING, TRAVEL_TO_GORILLAS, FIGHTING}

    public enum TravelStep {GNOME_STRONGHOLD, TRAVEL_TO_OPENING, CRASH_SITE, IN_CAVE, AT_GORILLAS}

    private enum BankingStep {BANK, LOAD_INVENTORY}
}
