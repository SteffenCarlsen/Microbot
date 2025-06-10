package net.runelite.client.plugins.microbot.TaF.MoonsofPeril;

import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.TaF.MoonsofPeril.enums.BossToKill;
import net.runelite.client.plugins.microbot.TaF.MoonsofPeril.enums.MoonsState;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.combat.AttackTypeHelpers.AttackType;
import net.runelite.client.plugins.microbot.util.combat.Rs2AttackStyles;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.EXTREME;

public class MoonsScript extends Script {
    private static final int bigFishingNet = 305;
    private static final int vialOfWater = 227;
    private static final int moonlightGrub = 29078;
    private static final int moonlightGrubPaste = 29079;
    private static final int pestleAndMortar = 233;
    private static final int EAT_COOLDOWN_MS = 2000;
    private static final int PRAYER_COOLDOWN_MS = 2000;
    public static final List<WorldPoint> bloodMoonSafeCircles = MoonsHelpers.generateSafeCircles(1392, 9632, 0);
    private static final List<WorldPoint> eclipseSafeCircles = MoonsHelpers.generateSafeCircles(1488, 9632, 0);
    private static final List<WorldPoint> blueMoonSafeCircles = MoonsHelpers.generateSafeCircles(1440, 9680, 0);
    public static MoonsState moonsState = MoonsState.DEFAULT;
    public static MoonsState previousMoonsState = MoonsState.DEFAULT;
    public static boolean moveToBloodTile = false;
    private static long lastEatTime = -1;
    private long lastPrayerTime = -1;
    private int attempts = 0;
    public int chestsLooted = 0;

    {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings(true);
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

    private void preStateHandling(MoonsConfig config) {
        WorldPoint topLeft = new WorldPoint(1400, 9640, 0);
        WorldPoint bottomRight = new WorldPoint(1384, 9624, 0);

        Rs2Camera.setZoom(128);

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        var bossToKill = getBossToKill(config);
        if (bossToKill == null) {
            moonsState = MoonsState.GOING_TO_LOOT;
            return;
        }

        if (playerLocation.getX() <= topLeft.getX() && playerLocation.getX() >= bottomRight.getX() && playerLocation.getY() <= topLeft.getY() && playerLocation.getY() >= bottomRight.getY()) {
            if (moonsState != MoonsState.FIGHTING_BLOOD_MOON) {
                moonsState = MoonsState.FIGHTING_BLOOD_MOON;
            }
        } else if (!IsFighting() && MoonsHelpers.needsRestock(config)) {
            if (moonsState != MoonsState.GETTING_SUPPLIES && moonsState != MoonsState.GOING_TO_COOKER) {
                moonsState = MoonsState.GOING_TO_COOKER;
            }
        } else if (!IsFighting() && !MoonsHelpers.needsRestock(config)) {
            if (bossToKill == null) {
                moonsState = MoonsState.GOING_TO_LOOT;
                return;
            }
            switch (bossToKill) {
                case BLOOD:
                    if (moonsState != MoonsState.GOING_TO_BLOOD_MOON) {
                        moonsState = MoonsState.GOING_TO_BLOOD_MOON;
                    }
                    break;
                case MOON:
                    if (moonsState != MoonsState.GOING_TO_BLUE_MOON) {
                        moonsState = MoonsState.GOING_TO_BLUE_MOON;
                    }
                    break;
                case ECLIPSE:
                    if (moonsState != MoonsState.GOING_TO_ECLIPSE) {
                        moonsState = MoonsState.GOING_TO_ECLIPSE;
                    }
                    break;
                default:
                    Microbot.log("Unknown boss type for state handling.");
            }
        }
    }

    private static boolean IsFighting() {
        return moonsState == MoonsState.FIGHTING_BLOOD_MOON ||
                moonsState == MoonsState.FIGHTING_BLUE_MOON ||
                moonsState == MoonsState.FIGHTING_ECLIPSE;
    }

    private static void handlePrayer() {
        if ((moonsState == MoonsState.FIGHTING_ECLIPSE || moonsState == MoonsState.FIGHTING_BLOOD_MOON || moonsState == MoonsState.FIGHTING_BLUE_MOON) && Rs2Player.hasPrayerPoints()) {
            Rs2Prayer.toggleQuickPrayer(true);
            return;
        }
        Rs2Prayer.toggleQuickPrayer(false);
    }

    private static void handleBloodMoonBossFocus() {
        if (!Rs2Player.isMoving() && !Rs2Player.isInteracting()) {
            var bloodJaguar = Rs2Npc.getNpc(MoonsConstants.BLOOD_JAGUAR_NPC_ID);
            int npcToAttack = (bloodJaguar == null) ? MoonsConstants.BLOOD_MOON_NPC_ID : MoonsConstants.BLOOD_JAGUAR_NPC_ID;
            if (npcToAttack == MoonsConstants.BLOOD_MOON_NPC_ID && Rs2Combat.getSpecEnergy() >= 500) {
                // if we want to, add special attack here
                attackBoss("Blood moon");
            } else if (npcToAttack == MoonsConstants.BLOOD_MOON_NPC_ID && Rs2Combat.getSpecEnergy() < 500) {
                // equip normal weapon
                attackBoss("Blood moon");
            } else {
                // attackBoss("Blood jaguar");
                // Microbot.log("Attacking Blood Jaguar");
                //sleep(600);
            }
        }
    }

    private static void handleFloorSafeSpot(WorldPoint playerLocation, BossToKill bossToKill) {
        NPC floorTileNPC = Rs2Npc.getNpc(MoonsConstants.PERILOUS_MOONS_SAFE_CIRCLE);
        WorldPoint floorTileLocation = (floorTileNPC != null) ? floorTileNPC.getWorldLocation() : null;
        switch (bossToKill) {
            case BLOOD:
                var bloodJaguar = Rs2Npc.getNpc(MoonsConstants.BLOOD_JAGUAR_NPC_ID);
                long currentTime = System.currentTimeMillis();
                if (bloodJaguar != null && (currentTime - lastEatTime) > EAT_COOLDOWN_MS) {
                    return;
                }
                handleFloorTileNormally(bloodMoonSafeCircles, playerLocation, floorTileLocation);
                break;
            case MOON:
                handleFloorTileNormally(blueMoonSafeCircles, playerLocation, floorTileLocation);
                break;
            case ECLIPSE:
                handleFloorTileNormally(eclipseSafeCircles, playerLocation, floorTileLocation);
                break;
            default:
                Microbot.log("Unknown boss type for safe spot handling.");
        }
    }

    private static void handleFloorTileNormally(List<WorldPoint> safeCircles, WorldPoint playerLocation, WorldPoint floorTileLocation) {
        if (floorTileLocation != null) {
            WorldPoint closestTile = safeCircles.stream()
                    .min(Comparator.comparingDouble(tile -> tile.distanceTo2D(floorTileLocation)))
                    .orElse(null);

            // Only move to the safe spot if it's safe to do so
            if (closestTile != null && !playerLocation.equals(closestTile)) {
                Rs2Walker.walkFastCanvas(closestTile);
                Rs2Player.eatAt(65);
            }
        }
    }
    public static void attackBoss(String npcName) {
        Rs2Prayer.toggleQuickPrayer(true);
        attackBosser(Collections.singletonList(npcName));
    }

    public static void attackBosser(List<String> npcNames) {
        for (String npcName : npcNames) {
            var npc = Rs2Npc.getNpc(npcName);
            if (npc == null) continue;
            Rs2Npc.attack(npc);
            return;
        }
    }

    private void reset() {
        moonsState = MoonsState.DEFAULT;
        previousMoonsState = MoonsState.DEFAULT;
        chestsLooted = 0;
    }

    public boolean run(MoonsConfig config) {
        moonsState = config.getState();
        Microbot.enableAutoRunOn = true;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();
                long currentTime = System.currentTimeMillis();

                if (moonsState != previousMoonsState) {
                    Microbot.log("State changed to: " + moonsState);
                    previousMoonsState = moonsState;
                }

                preStateHandling(config);
                WorldPoint playerLocation = Rs2Player.getWorldLocation();
                switch (moonsState) {
                    case GOING_TO_COOKER:
                        if (Rs2Player.isMoving() || Rs2Player.isInteracting()) break;
                        Rs2Walker.walkTo(MoonsConstants.COOKER_LOCATION);
                        if (Rs2Player.distanceTo(MoonsConstants.COOKER_LOCATION) < 5) {
                            moonsState = MoonsState.GETTING_SUPPLIES;
                        }
                        break;
                    case GETTING_SUPPLIES:
                        if (Rs2Player.isMoving() || Rs2Player.isInteracting()) break;
                        handleSupplies(config);
                        healAndPrayWhenGettingSupplies(config, currentTime);
                        break;
                    case GOING_TO_BLOOD_MOON:
                        handleEquipment(config, BossToKill.BLOOD);
                        Rs2AttackStyles.setAttackStyle(AttackType.SLASH);
                        Rs2Walker.walkTo(MoonsConstants.BLOOD_SHRINE_LOCATION);
                        if (Rs2Player.distanceTo(MoonsConstants.BLOOD_SHRINE_LOCATION) < 5) {
                            Rs2GameObject.interact(MoonsConstants.BLOOD_STATUE, "Use");
                            sleep(1200);
                            moonsState = MoonsState.FIGHTING_BLOOD_MOON;
                        }
                        break;
                    case FIGHTING_BLOOD_MOON:
                        handleAttackStyle(BossToKill.BLOOD);
                        handleBloodJaguar();
                        handleFloorSafeSpot(playerLocation, BossToKill.BLOOD);
                        handleBloodspotSpecials(playerLocation);
                        ConsumePotionsAndFood(config, currentTime);
                        handlePrayer();
                        handleBloodMoonBossFocus();
                        var bossKilledAtBlood = MoonsHelpers.getBossesKilled();
                        if (bossKilledAtBlood.contains(BossToKill.BLOOD)) {
                            moonsState = MoonsState.DEFAULT;
                            Rs2Prayer.disableAllPrayers();
                        }
                        break;
                    case GOING_TO_BLUE_MOON:
                        handleEquipment(config, BossToKill.MOON);
                        Rs2AttackStyles.setAttackStyle(AttackType.CRUSH);
                        Rs2Walker.walkTo(MoonsConstants.BLUE_MOON_SHRINE_LOCATION);
                        if (Rs2Player.distanceTo(MoonsConstants.BLUE_MOON_SHRINE_LOCATION) < 5) {
                            Rs2GameObject.interact(MoonsConstants.BLUE_MOON_STATUE, "Use");
                            sleep(1200);
                            moonsState = MoonsState.FIGHTING_BLUE_MOON;
                        }
                        break;
                    case FIGHTING_BLUE_MOON:
                        handleAttackStyle(BossToKill.MOON);
                        handleWeaponFreeze();
                        handleFrostStorm();
                        handleFloorSafeSpot(playerLocation, BossToKill.MOON);
                        ConsumePotionsAndFood(config, currentTime);
                        handlePrayer();
                        handleBlueMoonBossFocus();
                        var bossKilledAtMoon = MoonsHelpers.getBossesKilled();
                        if (bossKilledAtMoon.contains(BossToKill.MOON)) {
                            moonsState = MoonsState.DEFAULT;
                            Rs2Prayer.disableAllPrayers();
                        }
                        break;
                    case GOING_TO_ECLIPSE:
                        handleEquipment(config, BossToKill.ECLIPSE);
                        Rs2AttackStyles.setAttackStyle(AttackType.STAB);
                        Rs2Walker.walkTo(MoonsConstants.ECLIPSE_SHRINE_LOCATION);
                        if (Rs2Player.distanceTo(MoonsConstants.ECLIPSE_SHRINE_LOCATION) < 5) {
                            Rs2GameObject.interact(MoonsConstants.ECLIPSE_MOON_STATUE, "Use");
                            sleep(1200);
                            moonsState = MoonsState.FIGHTING_ECLIPSE;
                        }
                        break;
                    case FIGHTING_ECLIPSE:
                        handleAttackStyle(BossToKill.ECLIPSE);
                        handleShield();
                        handleClones();
                        handleFloorSafeSpot(playerLocation, BossToKill.ECLIPSE);
                        ConsumePotionsAndFood(config, currentTime);
                        handlePrayer();
                        handleEclipseBossFocus();
                        var bossKilledAtEclipse = MoonsHelpers.getBossesKilled();
                        if (!bossKilledAtEclipse.isEmpty()) {
                            if (bossKilledAtEclipse.contains(BossToKill.ECLIPSE)) {
                                moonsState = MoonsState.DEFAULT;
                                Rs2Prayer.disableAllPrayers();
                            }
                        }
                        break;
                    case GOING_TO_LOOT:
                        Rs2Prayer.disableAllPrayers();
                        Rs2Player.eatAt(80);
                        Rs2GameObject.interact(51362, "Make-cuppa");
                        sleep(3000);
                        Rs2Walker.walkTo(MoonsConstants.LOOT_LOCATION);
                        if (Rs2Player.distanceTo(MoonsConstants.LOOT_LOCATION) < 5) {
                            Rs2GameObject.interact(51346, "Claim");
                            sleep(2000);
                        }
                        if (Rs2Widget.getWidget(56885268) != null) {
                            Widget widget = Rs2Widget.getWidget(56885268);
                            if (widget == null) return;
                            Microbot.getMouse().click(widget.getBounds());
                            chestsLooted++;
                            sleep(1200);
                            var moonlightOne = Rs2Inventory.itemQuantity(MoonsConstants.MOONLIGHT_POTIONS[0]);
                            var moonlightTwo = Rs2Inventory.itemQuantity(MoonsConstants.MOONLIGHT_POTIONS[1]);
                            var moonlightThree = Rs2Inventory.itemQuantity(MoonsConstants.MOONLIGHT_POTIONS[2]);
                            var moonlightFour = Rs2Inventory.itemQuantity(MoonsConstants.MOONLIGHT_POTIONS[3]);

                            var potionsTotal = moonlightOne + moonlightTwo + moonlightThree + moonlightFour;

                            if (Rs2Inventory.itemQuantity("Cooked bream") >= config.foodAmount() && potionsTotal >= 1) {
                                moonsState = MoonsState.GOING_TO_BLOOD_MOON;
                            } else {
                                Rs2GameObject.interact(new WorldPoint(1513, 9598, 0), "Pass-through");
                                moonsState = MoonsState.GETTING_SUPPLIES;
                            }
                        }
                        break;

                }

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
            } catch (Exception e) {
                Microbot.log("Error: " + e);
                e.printStackTrace();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleEquipment(MoonsConfig config, BossToKill bossToKill) {
        if (Rs2Inventory.isFull()) {
            Microbot.log("Inventory is full, eating to equip items.");
            Rs2Player.eatAt(100);
        }
        if (bossToKill == BossToKill.ECLIPSE) {
            var equipment = config.stabWeapon();
            if (equipment != null && !equipment.isBlank()) {
                Microbot.log("Equipping stab weapon for Eclipse Moon: " + equipment);
                var equipmentToWearCrush = equipment.trim().split(",");
                for (String item : equipmentToWearCrush) {
                    if (!Rs2Equipment.isWearing(item)) {
                        Rs2Equipment.interact(item, "Wield");
                    }
                }
            }
        } else if (bossToKill == BossToKill.MOON) {
            var equipment = config.crushWeapon();
            if (equipment != null && !equipment.isBlank()) {
                Microbot.log("Equipping crush weapon for Blue Moon: " + equipment);
                var equipmentToWearCrush = equipment.trim().split(",");
                for (String item : equipmentToWearCrush) {
                    if (!Rs2Equipment.isWearing(item)) {
                        Rs2Equipment.interact(item, "Wield");
                    }
                }
            }
        } else if (bossToKill == BossToKill.BLOOD) {
            var equipment = config.slashWeapon();
            if (equipment != null && !equipment.isBlank()) {
                Microbot.log("Equipping slash weapon for Blood moon: " + equipment);
                var equipmentToWearSlash = equipment.trim().split(",");
                for (String item : equipmentToWearSlash) {
                    if (!Rs2Equipment.isWearing(item)) {
                        Rs2Equipment.interact(item, "Wield");
                    }
                }
            }
        }
    }

    private void healAndPrayWhenGettingSupplies(MoonsConfig config, long currentTime) {
        Rs2Player.eatAt(90);
        int currentPrayerPoints = Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER);
        if (currentTime - lastPrayerTime > PRAYER_COOLDOWN_MS && currentPrayerPoints <= Microbot.getClient().getRealSkillLevel(Skill.PRAYER) - 20) {
            Rs2Inventory.interact("Moonlight potion", "drink");
            lastPrayerTime = currentTime;
            Microbot.log("Drinking prayer potion at " + config.prayerAt() + "% prayer points.");
        }
    }

    private void handleClones() {
        if (!Rs2Player.getWorldLocation().equals(new WorldPoint(1488, 9632, 0))) {
            return;
        }

        Microbot.status = "Handling Eclipse Clones";

        // Get all Eclipse NPCs from the game with valid locations
        List<Rs2NpcModel> gameEclipseNpcs = Rs2Npc.getNpcs(MoonsConstants.ECLIPSE_NPC_ID)
                .filter(npc -> npc.getWorldLocation() != null)
                .collect(Collectors.toList());

        if (gameEclipseNpcs.isEmpty()) {
            return; // No valid Eclipse NPCs found
        }

        // First priority: Use tracked Eclipse NPCs with tick information
        if (!MoonsPlugin.eclipseNpcs.isEmpty()) {
            // Find the newest Eclipse NPC based on spawn tick
            var newestEclipse = MoonsPlugin.eclipseNpcs.stream()
                    .filter(x -> MoonsPlugin.globalTickCount - x.getValue() < 6)
                    .min(Comparator.comparingLong(pair -> MoonsPlugin.globalTickCount - pair.getValue()))
                    .orElse(null);
            if (newestEclipse != null) {
                WorldPoint targetLocation = newestEclipse.getKey();
                Microbot.log("Attacking newest Eclipse clone (spawned " + (MoonsPlugin.globalTickCount - newestEclipse.getValue()) + " ticks ago)");
                Rs2Walker.walkFastCanvas(targetLocation);
            }
        }
    }

    private void handleShield() {
        // No shield NPC, ignore shield
        var shieldNpc = Rs2Npc.getNpc(MoonsConstants.ECLIPSE_SHIELD_ID);
        if (shieldNpc == null) {
            return;
        }

        // If shield location is null, log and return
        WorldPoint shieldLocation = shieldNpc.getWorldLocation();
        if (shieldLocation == null) {
            Microbot.log("Null shield location");
            return;
        }

        // Safe tile has spawned, prioritize it over shield following
        NPC floorTileNPC = Rs2Npc.getNpc(MoonsConstants.PERILOUS_MOONS_SAFE_CIRCLE);
        if (floorTileNPC != null) {
            Microbot.log("Safe tile found, prioritizing safe tile over shield");
            Rs2Walker.walkTo(floorTileNPC.getWorldLocation());
            return;
        }

        // Get Eclipse boss location (center of rotation)
        var eclipseNpc = Rs2Npc.getNpc(MoonsConstants.ECLIPSE_NPC_ID);
        if (eclipseNpc == null) {
            return;
        }

        WorldPoint centerPoint = eclipseNpc.getWorldLocation();
        WorldPoint playerLocation = Rs2Player.getWorldLocation();

        // Calculate shield's position relative to center
        int relX = shieldLocation.getX() - centerPoint.getX();
        int relY = shieldLocation.getY() - centerPoint.getY();

        // Determine shield's movement direction based on its position
        int dirX = 0;
        int dirY = 0;

        // Top edge - moving right
        if (relY > 0 && Math.abs(relY) > Math.abs(relX)) {
            dirX = 1;
            dirY = 0;
        }
        // Right edge - moving down
        else if (relX > 0 && Math.abs(relX) >= Math.abs(relY)) {
            dirX = 0;
            dirY = -1;
        }
        // Bottom edge - moving left
        else if (relY < 0 && Math.abs(relY) >= Math.abs(relX)) {
            dirX = -1;
            dirY = 0;
        }
        // Left edge - moving up
        else if (relX < 0 && Math.abs(relX) > Math.abs(relY)) {
            dirX = 0;
            dirY = 1;
        }

        // Calculate the vector from center to shield
        double vectorX = relX;
        double vectorY = relY;

        // Normalize the vector
        double length = Math.sqrt(vectorX * vectorX + vectorY * vectorY);
        if (length > 0) {
            vectorX /= length;
            vectorY /= length;
        }

        // Position 1 tile OUTSIDE the shield (away from center)
        // and 2 tiles AHEAD of the shield's movement (increased from 1)
        int targetX = shieldLocation.getX() + (int)Math.round(vectorX) + (dirX * 2);
        int targetY = shieldLocation.getY() + (int)Math.round(vectorY) + (dirY * 2);

        WorldPoint targetPosition = new WorldPoint(targetX, targetY, shieldLocation.getPlane());

        // Only move if not already at target position
        if (playerLocation.distanceTo(targetPosition) > 0) {
            Rs2Walker.walkFastCanvas(targetPosition);
            Microbot.log("Following shield: Moving outside and 2 tiles ahead of shield");
        }
    }

    private void handleEclipseBossFocus() {
        if (!Rs2Player.isMoving() && !Rs2Player.isInteracting()) {
            var shieldNpc = Rs2Npc.getNpc(MoonsConstants.ECLIPSE_SHIELD_ID);
            if (shieldNpc != null) {
                return;
            }
            var eclipse = Rs2Npc.getNpc(MoonsConstants.ECLIPSE_NPC_ID);
            if (eclipse != null) {
                var composition= eclipse.getComposition();
                if (composition != null) {
                    var actions = composition.getActions();
                    if (actions != null) {
                        if (Arrays.stream(actions).anyMatch ("Attack"::equals)) {
                            Rs2Npc.attack(eclipse);
                        }
                    }
                }
            } else {
                Microbot.log("Eclipse NPC not found.");
            }
        }
    }

    private void handleBlueMoonBossFocus() {
        if (!Rs2Player.isMoving() && !Rs2Player.isInteracting()) {
            var blueMoon = Rs2Npc.getNpc(MoonsConstants.BLUE_MOON_NPC_ID);
            if (blueMoon != null) {
                if (!blueMoon.isDead()) {
                    Rs2Npc.attack(blueMoon);
                }
            }
        }
    }

    private void handleFrostStorm() {
        var frostStorm = Rs2GameObject.getGameObjects(x -> x.getId() == MoonsConstants.BLUE_MOON_STORMS);
        var braziers = Rs2GameObject.getGameObjects(x -> MoonsConstants.BRAZIER_IDS.contains(x.getId()) && Rs2GameObject.hasAction(Rs2GameObject.convertToObjectComposition(x), "Light"));
        if (braziers.isEmpty() && frostStorm.isEmpty()) {
            return;
        }
        if (!braziers.isEmpty()) {
            var closestBrazier = braziers.stream()
                    .min(Comparator.comparingDouble(b -> b.getWorldLocation().distanceTo(Rs2Player.getWorldLocation())))
                    .orElse(null);
            if (Rs2Player.isInteracting() || Rs2Player.isMoving()) {
                if (Rs2Player.isInteracting()) {
                    Microbot.log("Exiting due to interacting with an object.");
                }
                if (Rs2Player.isMoving()) {
                    Microbot.log("Exiting due to player movement.");
                }
                return;
            }
            if (closestBrazier != null) {
                Rs2GameObject.interact(closestBrazier, "Light");
            }
            return;
        }
        try {
            WorldPoint playerLocation = Rs2Player.getWorldLocation();
            handleFloorTileNormally(blueMoonSafeCircles, playerLocation, new WorldPoint(1440, 9675, 0));
        } catch (Exception e) {
            Microbot.log("Error handling frost storm safe spots: " + e.getMessage());
        }
    }

    private void handleWeaponFreeze() {
        var weaponFreeze = Rs2Npc.getNpcs("Frozen weapons").collect(Collectors.toList());
        if (weaponFreeze.isEmpty()) {
            return;
        }

        var correctAnimatingNpc = weaponFreeze.stream()
                .filter(npc -> npc.getAnimation() == MoonsConstants.BLUE_MOON_FROZEN_WEAPON_ANIMATION_ID)
                .findFirst()
                .orElse(null);
        if (correctAnimatingNpc == null) {
            Microbot.log("No weapon freeze NPC found with the correct animation.");
            return;
        }

        List<WorldPoint> dangerousWorldPoints = Rs2Tile.getDangerousGraphicsObjectTiles()
                .stream()
                .filter(x -> x.getValue() < 1200)
                .map(Pair::getKey)
                .collect(Collectors.toList());

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (!dangerousWorldPoints.isEmpty()) {
            if (dangerousWorldPoints.contains(playerLocation)) {
                final WorldPoint safeTile = findSafeTile(playerLocation, dangerousWorldPoints);
                if (safeTile != null) {
                    Rs2Walker.walkFastCanvas(safeTile);
                    Microbot.log("Walking to safe tile: " + safeTile);
                    return;
                }
            }
        }

        if (Rs2Player.isMoving() || Rs2Player.isInteracting()) {
            Microbot.log("Exiting due to player movement or interaction.");
            return;
        }
        Rs2Npc.attack(correctAnimatingNpc);
    }

    private void handleAttackStyle(BossToKill bossToKill) {
        return;
//        var current = Rs2Combat.getWeaponAttackStyle();
//        if (current == null || current.isBlank() || current.equals("None") || current.equals("Punch")) {
//            return;
//        }
//        switch (bossToKill) {
//            case BLOOD:
//                if (!Objects.equals(current, "Slash")) {
//                    attemptStyleSwitch("Slash");
//                }
//                break;
//            case MOON:
//                if (!Objects.equals(current, "Crush")) {
//                    attemptStyleSwitch("Crush");
//                }
//                break;
//            case ECLIPSE:
//                if (!Objects.equals(current, "Stab")) {
//                    attemptStyleSwitch("Stab");
//                }
//                break;
//        }
    }

    private void attemptStyleSwitch(String style) {
        if (attempts < 3 && Rs2Combat.setAttackStyle(WidgetInfo.COMBAT_STYLE_ONE)) {
            if (Rs2Combat.getWeaponAttackStyle().equals(style)) {
                attempts = 0; // Reset attempts if successful
            } else {
                attempts++;
            }
        } else if (attempts < 3 && Rs2Combat.setAttackStyle(WidgetInfo.COMBAT_STYLE_TWO)) {
            if (Rs2Combat.getWeaponAttackStyle().equals(style)) {
                attempts = 0; // Reset attempts if successful
            } else {
                attempts++;
            }
        } else if (attempts < 3 && Rs2Combat.setAttackStyle(WidgetInfo.COMBAT_STYLE_THREE)) {
            if (Rs2Combat.getWeaponAttackStyle().equals(style)) {
                attempts = 0; // Reset attempts if successful
            } else {
                attempts++;
            }
        } else if (attempts < 3 && Rs2Combat.setAttackStyle(WidgetInfo.COMBAT_STYLE_THREE)) {
            if (Rs2Combat.getWeaponAttackStyle().equals(style)) {
                attempts = 0; // Reset attempts if successful
            } else {
                attempts++;
            }
        }
    }

    private BossToKill getBossToKill(MoonsConfig config) {
        var bossesKilled = MoonsHelpers.getBossesKilled();
        var totalBossesToKill = (config.killBloodMoon() ? 1 : 0) + (config.killBlueMoon() ? 1 : 0) + (config.killEclipse() ? 1 : 0);
        if (bossesKilled.size() == totalBossesToKill) {
            moonsState = MoonsState.GOING_TO_LOOT;
            return null;
        }
        if (!bossesKilled.contains(BossToKill.BLOOD) && config.killBloodMoon()) {
            return BossToKill.BLOOD;
        }
        if (!bossesKilled.contains(BossToKill.MOON) && config.killBlueMoon()) {
            return BossToKill.MOON;
        }
        if (!bossesKilled.contains(BossToKill.ECLIPSE) && config.killEclipse()) {
            return BossToKill.ECLIPSE;
        }
        return null;
    }

    public void handleBloodJaguar() {
        // Get safe circle if it exists
        var floorTileNPC = Rs2Npc.getNpc(MoonsConstants.PERILOUS_MOONS_SAFE_CIRCLE);
        var npcs = Rs2Npc.getNpcs(MoonsConstants.PERILOUS_MOONS_SAFE_CIRCLE).collect(Collectors.toList());
        WorldPoint floorTileLocation = (floorTileNPC != null) ? floorTileNPC.getWorldLocation() : null;

        // Find the jaguar
        Rs2NpcModel bloodJaguar = MoonsHelpers.getClosestJaguar(floorTileLocation);

        if (bloodJaguar == null || floorTileLocation == null) {
            bloodJaguar = Rs2Npc.getNpc("Blood Jaguar");
            if (bloodJaguar != null) {
                Microbot.log("Blood Jaguar found, but no safe tile available.");
            }
            if (floorTileLocation == null) {
                if (!npcs.isEmpty()) {
                    Microbot.log("Safe circle NPCs found, but no safe tile location available.");
                }
            }
            return;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        WorldPoint jaguarLocation = bloodJaguar.getWorldLocation();

        // Prioritize safe tile if it exists and we're far from it
        if (floorTileLocation != null && playerLocation.distanceTo(floorTileLocation) > 5) {
            Microbot.log("Moving to safe tile before attacking Blood Jaguar");
            Rs2Walker.walkFastCanvas(floorTileLocation);
            return;
        }
        // If we're too far from the jaguar and not already moving or interacting
        if (playerLocation.distanceTo(jaguarLocation) > 5) {
            Microbot.log("Moving to Blood Jaguar");
            Rs2Walker.walkFastCanvas(jaguarLocation);
        }
    }

    private void ConsumePotionsAndFood(MoonsConfig config, long currentTime) {
        int currentHitpoints = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
        if (currentTime - lastEatTime > EAT_COOLDOWN_MS && currentHitpoints <= config.eatAt()) {
            Rs2Player.useFood();
            lastEatTime = currentTime;
            Microbot.log("Eating food at " + config.eatAt() + "% health.");
        }

        int currentPrayerPoints = Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER);
        if (currentTime - lastPrayerTime > PRAYER_COOLDOWN_MS && currentPrayerPoints <= config.prayerAt()) {
            Rs2Inventory.interact("Moonlight potion", "drink");
            lastPrayerTime = currentTime;
            Microbot.log("Drinking prayer potion at " + config.prayerAt() + "% prayer points.");
        }
    }

    private void handleBloodspotSpecials(WorldPoint playerLocation) {
        List<WorldPoint> bloodSpotsWorldPoints = Rs2GameObject.getGameObjects(x -> x.getId() == 51046)
                .stream()
                .map(GameObject::getWorldLocation)
                .collect(Collectors.toList());

        if (!bloodSpotsWorldPoints.isEmpty() && Rs2Npc.getNpc(MoonsConstants.BLOOD_JAGUAR_NPC_ID) == null) {
            if (Rs2Player.eatAt(80)) {
                Microbot.log("Eating at 80% health to avoid healing in attack phase");
            }
            if (bloodSpotsWorldPoints.contains(playerLocation)) {
                final WorldPoint safeTile = findSafeTile(playerLocation, bloodSpotsWorldPoints);
                if (safeTile != null) {
                    Rs2Walker.walkFastCanvas(safeTile);
                }
            }
        }
    }

    private void handleSupplies(MoonsConfig config) {
        int potionsTotal = calculatePotionTotal();
        if (Rs2Player.getWorldLocation().distanceTo(MoonsConstants.COOKER_LOCATION) > 15) {
            Rs2Walker.walkTo(MoonsConstants.COOKER_LOCATION);
            return;
        }
        // Handle vials and fishing net first
        if (handleInventoryCleanup()) return;

        // Calculate potion quantities
        if (needMorePotions(potionsTotal, config)) {
            getHerbloreSupplies();
        } else if (shouldProcessGrubs()) {
            processGrubs();
        } else if (shouldCreatePotions()) {
            createPotions();
        } else if (shouldCollectGrubs()) {
            collectGrubs();
        } else if (shouldDropPestleAfterPotions(config)) {
            Rs2Inventory.drop(pestleAndMortar);
        } else if (hasExcessVials(config)) {
            dropExcessVials();
        } else if (needMoreFood(config)) {
            getFishingSupplies();
        } else if (shouldStartFishing(config)) {
            startFishing(config);
        } else if (shouldCookFood()) {
            cookFood();
        } else if (shouldMakeTea()) {
            makeTea();
        } else if (shouldMoveToExit(config)) {
            moveToExit();
        } else if (isAtExit()) {
            passThrough();
        }
    }

    private boolean handleInventoryCleanup() {
        if (Rs2Inventory.hasItem("Vial", true)) {
            Rs2Inventory.dropAll("Vial");
            return true;
        }

        if (Rs2Player.getAnimation() == 11042 && Rs2Inventory.hasItem(bigFishingNet)) {
            Rs2Inventory.drop(bigFishingNet);
            return true;
        }

        return Rs2Player.isAnimating();
    }

    private int calculatePotionTotal() {
        int moonlightOne = Rs2Inventory.itemQuantity(MoonsConstants.MOONLIGHT_POTIONS[0]);
        int moonlightTwo = Rs2Inventory.itemQuantity(MoonsConstants.MOONLIGHT_POTIONS[1]);
        int moonlightThree = Rs2Inventory.itemQuantity(MoonsConstants.MOONLIGHT_POTIONS[2]);
        int moonlightFour = Rs2Inventory.itemQuantity(MoonsConstants.MOONLIGHT_POTIONS[3]);

        return moonlightOne + moonlightTwo + moonlightThree + moonlightFour +
                Rs2Inventory.itemQuantity(vialOfWater);
    }

    private boolean needMoreFood(MoonsConfig config) {
        return !Rs2Inventory.hasItem(bigFishingNet) &&
                (Rs2Inventory.itemQuantity(MoonsConstants.COOKED_FOOD_ID) + Rs2Inventory.itemQuantity(MoonsConstants.RAW_FOOD_ID)) < config.foodAmount();
    }

    private void getFishingSupplies() {
        if (Rs2Player.getWorldLocation().distanceTo(MoonsConstants.COOKER_LOCATION) > 5) {
            Rs2Walker.walkTo(MoonsConstants.COOKER_LOCATION);
            return;
        }
        Rs2GameObject.interact(MoonsConstants.SUPPLY_BOX);
        sleepUntil(() -> Rs2Widget.hasWidget("Take fishing supplies"), 4000);
        Rs2Widget.clickWidget("Take fishing supplies");
        sleep(600);
    }

    private boolean needMorePotions(int potionsTotal, MoonsConfig config) {
        return potionsTotal < config.moonPotions();
    }

    private void getHerbloreSupplies() {
        if (Rs2Player.getWorldLocation().distanceTo(MoonsConstants.COOKER_LOCATION) > 5) {
            Rs2Walker.walkTo(MoonsConstants.COOKER_LOCATION);
            return;
        }

        Rs2GameObject.interact(MoonsConstants.SUPPLY_BOX);
        sleepUntil(() -> Rs2Widget.hasWidget("Take herblore supplies"), 4000);
        Rs2Widget.clickWidget("Take herblore supplies");
        sleep(600);
    }

    private boolean hasExcessVials(MoonsConfig config) {
        return Rs2Inventory.itemQuantity(vialOfWater) > 0;
    }

    private void dropExcessVials() {
        Rs2Inventory.drop(vialOfWater);
        Rs2Inventory.drop(vialOfWater);
    }

    private boolean shouldCollectGrubs() {
        return Rs2Inventory.itemQuantity(vialOfWater) > (Rs2Inventory.itemQuantity(moonlightGrub) + Rs2Inventory.itemQuantity(moonlightGrubPaste)) && Rs2Inventory.hasItem(vialOfWater);
    }

    private void collectGrubs() {
        Rs2GameObject.interact(MoonsConstants.GRUB_HERBLORE_ID, "Collect-from");
    }

    private boolean shouldProcessGrubs() {
        return Rs2Inventory.hasItem(moonlightGrub) &&
                Rs2Inventory.hasItem(pestleAndMortar);
    }

    private void processGrubs() {
        Rs2Inventory.combine(pestleAndMortar, moonlightGrub);
        sleep(1600, 2400);
    }

    private boolean shouldCreatePotions() {
        return Rs2Inventory.hasItem(moonlightGrubPaste);
    }

    private void createPotions() {
        if (!Rs2Inventory.hasItem(vialOfWater)) {
            Rs2Inventory.dropAll(moonlightGrubPaste);
        } else {
            Rs2Inventory.combine(moonlightGrubPaste, vialOfWater);
            Rs2Inventory.waitForInventoryChanges(1200);
        }
    }

    private boolean shouldDropPestleAfterPotions(MoonsConfig config) {
        return Rs2Inventory.hasItemAmount("Moonlight potion", config.moonPotions()) &&
                Rs2Inventory.hasItem(pestleAndMortar);
    }

    private boolean shouldStartFishing(MoonsConfig config) {
        return Rs2Inventory.hasItem(bigFishingNet) &&
                (Rs2Inventory.itemQuantity(MoonsConstants.RAW_FOOD_ID) +
                        Rs2Inventory.itemQuantity(MoonsConstants.COOKED_FOOD_ID)) < config.foodAmount() &&
                !Rs2Inventory.isFull();
    }

    private void startFishing(MoonsConfig config) {
        Rs2GameObject.interact(MoonsConstants.FISHING_SPOT_ID, "Fish");
        while (shouldStartFishing(config)) {
            if (!this.isRunning()) {
                Microbot.log("Script stopped while fishing.");
                return;
            }
            var fishingStream = Rs2GameObject.getGameObjects(MoonsConstants.FISHING_STREAM_ID);
            sleep(1200, 1600);
        }
        Rs2GameObject.interact(MoonsConstants.FISHING_SPOT_ID, "Exit");
    }

    private boolean shouldCookFood() {
        return Rs2Inventory.hasItem(MoonsConstants.RAW_FOOD_ID);
    }

    private void cookFood() {
        Rs2GameObject.interact(MoonsConstants.COOKER_OBJECT_ID, "Cook");
        sleep(600);
    }

    private boolean shouldMakeTea() {
        return Rs2Inventory.hasItem(29217) &&
                !Rs2Inventory.hasItem(29216) &&
                Microbot.getClient().getEnergy() < 7_000 &&
                Rs2Player.getWorldLocation().distanceTo(MoonsConstants.SUPPLIES_EXIT_LOCATION) > 10;
    }

    private void makeTea() {
        Rs2GameObject.interact(51362, "Make-cuppa");
    }

    private boolean shouldMoveToExit(MoonsConfig config) {
        return Rs2Inventory.itemQuantity(MoonsConstants.COOKED_FOOD_ID) >= config.foodAmount() &&
                !Rs2Inventory.hasItem(MoonsConstants.RAW_FOOD_ID) &&
                Microbot.getClient().getEnergy() > 6_000 && Rs2Player.distanceTo(MoonsConstants.SUPPLIES_EXIT_LOCATION) > 10;
    }

    private void moveToExit() {
        Rs2Walker.walkTo(MoonsConstants.SUPPLIES_EXIT_LOCATION);
        Microbot.log("Done getting supplies, should be going through!");
    }

    private boolean isAtExit() {
        return Rs2Player.getWorldLocation().distanceTo(MoonsConstants.SUPPLIES_EXIT_LOCATION) < 3;
    }

    private void passThrough() {
        // MenuEntryImpl(getOption=Pass-through, getTarget=<col=ffff>Entrance, getIdentifier=51378, getType=GAME_OBJECT_FIRST_OPTION, getParam0=49, getParam1=56, getItemId=-1, isForceLeftClick=false, getWorldViewId=-1, isDeprioritized=false)
        Rs2GameObject.interact(MoonsConstants.EXIT_SUPPLY_DOOR_ID, "Pass-through");
        moonsState = MoonsState.GOING_TO_BLOOD_MOON;
    }

    private WorldPoint findSafeTile(WorldPoint playerLocation, List<WorldPoint> dangerousWorldPoints) {
        List<WorldPoint> nearbyTiles = List.of(
                new WorldPoint(playerLocation.getX() + 1, playerLocation.getY(), playerLocation.getPlane()),// normal
                new WorldPoint(playerLocation.getX() - 1, playerLocation.getY(), playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX(), playerLocation.getY() + 1, playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX(), playerLocation.getY() - 1, playerLocation.getPlane()),

                new WorldPoint(playerLocation.getX() - 1, playerLocation.getY() + 1, playerLocation.getPlane()),// diagonal
                new WorldPoint(playerLocation.getX() - 1, playerLocation.getY() - 1, playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX() + 1, playerLocation.getY() - 1, playerLocation.getPlane()),
                new WorldPoint(playerLocation.getX() + 1, playerLocation.getY() + 1, playerLocation.getPlane())
        );

        for (WorldPoint tile : nearbyTiles) {
            if (!dangerousWorldPoints.contains(tile)) {
                return tile;
            }
        }
        Microbot.log("No safe tile found!");
        return null;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        reset();
        Rs2Prayer.disableAllPrayers();
    }
}
