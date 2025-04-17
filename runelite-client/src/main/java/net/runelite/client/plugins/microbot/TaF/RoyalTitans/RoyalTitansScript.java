package net.runelite.client.plugins.microbot.TaF.RoyalTitans;

import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.misc.Rs2Potion;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.event.KeyEvent;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer.disableAllPrayers;

public class RoyalTitansScript extends Script {
    private static final int BOSS_REGION = 11669;
    public static String version = "1.0";
    private final Integer ICE_TITAN_ID = 14147;
    private final Integer FIRE_TITAN_DEAD_ID = 14148;
    private final Integer ICE_TITAN_DEAD_ID = 14149;
    private final Integer FIRE_TITAN_ID = 12596;
    private final Integer MELEE_TITAN_ICE_REGION_X = 34;
    private final Integer MELEE_TITAN_FIRE_REGION_X = 26;
    private final Integer FIRE_MINION_ID = 14150;
    private final Integer ICE_MINION_ID = 14151;
    private final Integer FIRE_WALL = 14152;
    private final Integer ICE_WALL = 14153;
    private final Integer TUNNEL_ID = 55986;
    private final Integer WIDGET_START_A_FIGHT = 14352385;
    private final WorldPoint BOSS_LOCATION = new WorldPoint(2951, 9574, 0);

    public BotStatus state = BotStatus.TRAVELLING;
    public volatile Tile enrageTile = null;
    private TravelStatus travelStatus = TravelStatus.TO_BANK;
    private Instant waitingTimeStart = null;
    private boolean waitedLastIteration = false;
    private boolean isRunning;
    private RoyalTitansConfig.RoyalTitan LootedTitan = null;
    public String subState = "";
    private boolean LootedTitanLastIteration = false;

    public boolean run(RoyalTitansConfig config) {
        isRunning = true;
        enrageTile = null;
        waitingTimeStart = null;
        LootedTitan = null;
        travelStatus = TravelStatus.TO_BANK;
        state = BotStatus.TRAVELLING;
        Microbot.enableAutoRunOn = false;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                switch (state) {
                    case BANKING:
                        handleBanking(config);
                        break;
                    case TRAVELLING:
                        handleTravelling(config);
                        break;
                    case WAITING:
                        handleWaiting(config);
                        break;
                    case FIGHTING:
                        handleFighting(config);
                        break;
                }


            } catch (Exception e) {
                e.printStackTrace();
                Microbot.log("Exception: " + e.getMessage());
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    private void equipArmor(String gearCfg) {
        if (gearCfg == null || gearCfg.trim().isEmpty()) {
            return;
        }
        var split = gearCfg.split("\\s*,\\s*");
        for (String s : split)
        {
            s = s.trim();
            if (s.isEmpty()) {
                continue;
            }
            try
            {
                int itemId = Integer.parseInt(s);
                if (Rs2Inventory.contains(itemId))
                {
                    Rs2Inventory.equip(itemId);
                }
            }
            catch (NumberFormatException ignored) { }
        }
    }

    private void handleWaiting(RoyalTitansConfig config) {
        var teammate = Rs2Player.getPlayers(x -> Objects.equals(x.getName(), config.teammateName())).findFirst().orElse(null);
        if (waitingTimeStart == null && teammate == null && !waitedLastIteration) {
            waitingTimeStart = Instant.now();
            waitedLastIteration = true;
            return;
        }
        if (teammate != null) {
            if (teammate.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) < 5) {
                waitedLastIteration = false;
                waitingTimeStart = null;
                state = BotStatus.TRAVELLING;
                travelStatus = TravelStatus.TO_INSTANCE;
                evaluateAndConsumePotions(config);
                sleep(1200, 2400);
                return;
            }

        }

        if (waitingTimeStart != null && teammate == null && Instant.now().isAfter(waitingTimeStart.plusSeconds(config.waitingTimeForTeammate()))) {
            Microbot.log("Teammate did not show after " + config.waitingTimeForTeammate() + " seconds, shutting down");
            shutdown();
        }
    }

    private void evaluateAndConsumePotions(RoyalTitansConfig config) {
        int threshold = config.boostedStatsThreshold();

        if (!isCombatPotionActive(threshold)) {
            consumePotion(Rs2Potion.getCombatPotionsVariants());
        }

        if (!isRangingPotionActive(threshold)) {
            consumePotion(Rs2Potion.getRangePotionsVariants());
        }
    }
    private boolean isCombatPotionActive(int threshold) {
        return Rs2Player.hasDivineCombatActive() || (Rs2Player.hasAttackActive(threshold) && Rs2Player.hasStrengthActive(threshold));
    }

    private boolean isRangingPotionActive(int threshold) {
        return Rs2Player.hasRangingPotionActive(threshold) || Rs2Player.hasDivineBastionActive() || Rs2Player.hasDivineRangedActive();
    }
    private void consumePotion(List<String> keyword) {
        var potion = Rs2Inventory.get(keyword);
        if (potion != null) {
            Rs2Inventory.interact(potion, "Drink");
            Rs2Player.waitForAnimation(1200);
        }
    }

    private void handleFighting(RoyalTitansConfig config) {
        var wantedToEScape = handleEscaping(config);
        if (wantedToEScape) {
            //Microbot.log("Trying to escape");
        }
        handleEating(config);
        handlePrayers(config);
        handleDangerousTiles();
        var handledMinions = handleMinions(config);
        if (handledMinions) {
            return;
        }
        var handledWalls = handleWalls(config);
        if (handledWalls) {
            return;
        }
        if (!handledWalls && !handledMinions) {
            attackBoss(config);
        }

        handleLooting(config);
    }

    private void handleEating(RoyalTitansConfig config) {
        subState = "Handling eating";
        var ate = Rs2Player.eatAt(config.minEatPercent());
        var ppot = Rs2Player.drinkPrayerPotionAt(config.minPrayerPercent());
    }

    private void handleLooting(RoyalTitansConfig config) {
        var iceTitanDead = Rs2Npc.getNpcs(ICE_TITAN_DEAD_ID).findFirst().orElse(null);
        var fireTitanDead = Rs2Npc.getNpcs(FIRE_TITAN_DEAD_ID).findFirst().orElse(null);
        var iceTitan = Rs2Npc.getNpcs(ICE_TITAN_ID).findFirst().orElse(null);
        var fireTitan = Rs2Npc.getNpcs(FIRE_TITAN_ID).findFirst().orElse(null);
        if (iceTitan != null && !iceTitan.isDead() || fireTitan != null && !fireTitan.isDead()) {
            return;
        }
        if (fireTitanDead == null && iceTitanDead == null) {
            return;
        }
        if (LootedTitanLastIteration) {
            return;
        }
        subState = "Handling looting";
        if (!Rs2Inventory.isFull()) {
//            var nearbyItems = Rs2GroundItem.getAll(50);
//            for (var item : nearbyItems) {
//                Rs2GroundItem.interact(item);
//                sleepUntil(() -> Rs2Inventory.waitForInventoryChanges(600));
//            }
        }
        Microbot.log("Trying to loot");
        boolean looted = false;
        switch (config.loot()) {
            case ICE_TITAN:
                looted = Rs2Npc.interact(iceTitanDead, "Loot");
                break;
            case FIRE_TITAN:
                looted = Rs2Npc.interact(fireTitanDead, "Loot");
                break;
            case ALTERNATE:
                if (LootedTitan == null) {
                    LootedTitan = RoyalTitansConfig.RoyalTitan.ICE_TITAN;
                } else if (LootedTitan == RoyalTitansConfig.RoyalTitan.ICE_TITAN) {
                    LootedTitan = RoyalTitansConfig.RoyalTitan.FIRE_TITAN;
                } else {
                    LootedTitan = RoyalTitansConfig.RoyalTitan.ICE_TITAN;
                }
                if (LootedTitan == RoyalTitansConfig.RoyalTitan.ICE_TITAN) {
                    looted = Rs2Npc.interact(iceTitanDead, "Loot");
                } else {
                    looted = Rs2Npc.interact(fireTitanDead, "Loot");
                }
                break;
            case RANDOM:
                if (Math.random() < 0.5) {
                    looted = Rs2Npc.interact(iceTitanDead, "Loot");
                } else {
                    looted = Rs2Npc.interact(fireTitanDead, "Loot");
                }
                break;
        }
        if (looted) {
            Rs2Player.waitForAnimation(1200);
            sleepUntil(() -> Rs2Inventory.waitForInventoryChanges(1200));
            LootedTitanLastIteration = true;
        }
        evaluateAndConsumePotions(config);
    }

    private boolean handleEscaping(RoyalTitansConfig config) {
        var shouldLeave = false;
        int currentHealth = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
        int currentPrayer = Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER);
        boolean noFood = Rs2Inventory.getInventoryFood().isEmpty();
        boolean noPrayerPotions = Rs2Inventory.items().stream()
                .noneMatch(item -> item != null && item.getName() != null && !Rs2Potion.getPrayerPotionsVariants().contains(item.getName()));
        var teammate = Rs2Player.getPlayers(x -> Objects.equals(x.getName(), config.teammateName())).findFirst().orElse(null);
        if (teammate != null) {
            waitingTimeStart = null;
        }
        if (teammate == null && waitingTimeStart == null && config.resupplyWithTeammate()) {
            waitingTimeStart = Instant.now();
        } else if (config.resupplyWithTeammate() && teammate == null && Instant.now().isAfter(waitingTimeStart.plusSeconds(config.waitingTimeForTeammate()))) {
            shouldLeave = true;
        }
        if ((noFood && currentHealth <= config.healthThreshold()) || (noPrayerPotions && currentPrayer < 10)) {
            shouldLeave = true;
        }
        if (shouldLeave) {
            if (config.emergencyTeleport() != 0) {
                Rs2Inventory.interact(config.emergencyTeleport(), "Break");
                Rs2Player.waitForAnimation(1200);
            }
            else {
                Rs2GameObject.interact(TUNNEL_ID, "Quick-leave");
            }
            state = BotStatus.TRAVELLING;
            travelStatus = TravelStatus.TO_BANK;
            return true;
        }
        return false;
    }

    private void handlePrayers(RoyalTitansConfig config) {
        subState = "Handling prayers";
        var isPrayerActive = Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE);
        if (Rs2Combat.inCombat()) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
            return;
        }
        if (Rs2Npc.getNpcs().anyMatch(x -> x.getId() == ICE_TITAN_ID || x.getId() == FIRE_TITAN_ID)) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, true);
            return;
        }
        Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false);
    }

    private void attackBoss(RoyalTitansConfig config) {
        var iceTitan = Rs2Npc.getNpcs(ICE_TITAN_ID).findFirst().orElse(null);
        var fireTitan = Rs2Npc.getNpcs(FIRE_TITAN_ID).findFirst().orElse(null);
        if (iceTitan == null && fireTitan == null) {
            Microbot.log("No titans found");
            return;
        }
        LootedTitanLastIteration = false;
        handleBossFocus(config, iceTitan, fireTitan);
        handleSpecialAttacks(config, iceTitan, fireTitan);
    }

    private void handleSpecialAttacks(RoyalTitansConfig config, Rs2NpcModel iceTitan, Rs2NpcModel fireTitan) {

    }

    private void handleBossFocus(RoyalTitansConfig config, Rs2NpcModel iceTitan, Rs2NpcModel fireTitan) {
        if (enrageTile != null) {
            subState = "Handling enrage tile";
            if (Rs2Player.getWorldLocation().equals(enrageTile.getWorldLocation())) {
                equipArmor(config.rangedEquipment());
            } else {
                Rs2Walker.walkFastCanvas(enrageTile.getWorldLocation());
                equipArmor(config.rangedEquipment());
            }

            if (fireTitan != null && !fireTitan.isDead()) {
                Rs2Npc.attack(fireTitan);
            } else if (iceTitan != null) {
                Rs2Npc.attack(iceTitan);
            }
        }
        // Both bosses alive - Handle focus
        if (config.royalTitanToFocus() == RoyalTitansConfig.RoyalTitan.FIRE_TITAN && fireTitan != null && !fireTitan.isDead()) {
            subState = "Attacking fire titan";
            int fireX = fireTitan.getWorldLocation().getRegionX();
            if (enrageTile == null && (fireX == MELEE_TITAN_FIRE_REGION_X ||
                    (fireX > MELEE_TITAN_FIRE_REGION_X && fireX < MELEE_TITAN_ICE_REGION_X))) {
                equipArmor(config.meleeEquipment());
            } else {
                equipArmor(config.rangedEquipment());
            }
            Rs2Npc.attack(fireTitan);
            return;
        } else if (config.royalTitanToFocus() == RoyalTitansConfig.RoyalTitan.ICE_TITAN && iceTitan != null && !iceTitan.isDead()) {
            subState = "Attacking ice titan";
            if (enrageTile == null && (iceTitan.getWorldLocation().getRegionX() == MELEE_TITAN_ICE_REGION_X)) {
                equipArmor(config.meleeEquipment());
            } else {
                equipArmor(config.rangedEquipment());
            }
            Rs2Npc.attack(iceTitan);
            return;
        }
        // Only one boss alive
        if (iceTitan != null && !iceTitan.isDead()) {
            subState = "Only 1 boss alive, attacking ice titan";
            int iceX = iceTitan.getWorldLocation().getRegionX();
            if (enrageTile == null && (iceX == MELEE_TITAN_ICE_REGION_X ||
                    (iceX > MELEE_TITAN_FIRE_REGION_X && iceX < MELEE_TITAN_ICE_REGION_X))) {
                equipArmor(config.meleeEquipment());
            } else {
                equipArmor(config.rangedEquipment());
            }
            Rs2Npc.attack(iceTitan);
            return;
        }
        if (fireTitan != null && !fireTitan.isDead()) {
            subState = "Only 1 boss alive, attacking fire titan";
            int fireX = fireTitan.getWorldLocation().getRegionX();
            if (enrageTile == null && (fireX == MELEE_TITAN_FIRE_REGION_X ||
                    (fireX > MELEE_TITAN_FIRE_REGION_X && fireX < MELEE_TITAN_ICE_REGION_X))) {
                equipArmor(config.meleeEquipment());
            } else {
                equipArmor(config.rangedEquipment());
            }
            Rs2Npc.attack(fireTitan);
        }
    }

    private boolean handleWalls(RoyalTitansConfig config) {
        subState = "Handling walls";
        var walls = Rs2Npc.getNpcs(config.minionResponsibility() == RoyalTitansConfig.Minions.FIRE_MINIONS ? FIRE_WALL : ICE_WALL).collect(Collectors.toList());
        if (walls.isEmpty()) {
            return false;
        }
        if (walls.size() < 8) {
            return false;
        }
        equipArmor(config.magicEquipment());
        for (var wall : walls) {
            if (wall != null && wall.getId() != -1 && !wall.isDead()) {
                Rs2Npc.interact(wall, config.minionResponsibility() == RoyalTitansConfig.Minions.FIRE_MINIONS ? "Douse"  : "Melt");
            }
        }

        return true;
    }

    private boolean handleMinions(RoyalTitansConfig config) {
        subState = "Handling minions";
        var minions = Rs2Npc.getNpcs(config.minionResponsibility() == RoyalTitansConfig.Minions.FIRE_MINIONS ? FIRE_MINION_ID : ICE_MINION_ID).collect(Collectors.toList());
        if (minions.isEmpty()) {
            return false;
        }
        equipArmor(config.magicEquipment());
        for (var minion : minions) {
            if (minion != null && !minion.isDead()) {
                Rs2Npc.attack(minion);
            }
        }
        return true;
    }

    private void handleDangerousTiles() {
        subState = "Handling dangerous tiles";
        // I have no idea why this is needed - It should be handled by the Initialization of Rs2Tile.Init()
        var dangerousGraphicsObjectTiles = Rs2Tile.getDangerousGraphicsObjectTiles().stream().filter(x -> x.getValue() > 0).collect(Collectors.toList());
        List<WorldPoint> dangerousWorldPoints = dangerousGraphicsObjectTiles
                .stream()
                .map(Pair::getKey)
                .collect(Collectors.toList());

        if (dangerousWorldPoints.isEmpty()) {
            return;
        }
        if (dangerousWorldPoints.stream().noneMatch(x->x.equals(Rs2Player.getWorldLocation()))) {
            return;
        }

        final WorldPoint safeTile = findSafeTile(Rs2Player.getWorldLocation(), dangerousWorldPoints);
        if (safeTile != null) {
            Rs2Walker.walkFastCanvas(safeTile);
        }
    }

    private WorldPoint findSafeTile(WorldPoint playerLocation, List<WorldPoint> dangerousWorldPoints) {
        List<WorldPoint> nearbyTiles = List.of(
                new WorldPoint(playerLocation.getX(), playerLocation.getY(), playerLocation.getPlane()),
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
            // Tiles outside the arena returns true for isWalkable - Discard them
            if (tile.getRegionX() > MELEE_TITAN_ICE_REGION_X - 1 ||
                    tile.getRegionX() < MELEE_TITAN_FIRE_REGION_X + 1) {
                continue;
            }
            final LocalPoint location = LocalPoint.fromWorld(Microbot.getClient(), tile);
            if (!dangerousWorldPoints.contains(tile) && Rs2Tile.isWalkable(location)) {
                return tile;
            }
        }

        return null;
    }

    private void handleTravelling(RoyalTitansConfig config) {
        switch (travelStatus) {
            case TO_BANK:
                subState = "Walking to bank";
                var isAtBank = Rs2Bank.walkToBank();
                if (isAtBank) {
                    state = BotStatus.BANKING;
                }
                break;
            case TO_TITANS:
                subState = "Walking to titans";
                var gotToTitans = Rs2Walker.walkTo(BOSS_LOCATION, 3);
                if (gotToTitans) {
                    state = BotStatus.WAITING;
                    travelStatus = TravelStatus.TO_BANK;
                } else {
                    Rs2Walker.walkTo(BOSS_LOCATION, 3);
                }
                break;
            case TO_INSTANCE:
                subState = "Walking to instance";
                var isVisible = Rs2Widget.isWidgetVisible(WIDGET_START_A_FIGHT);
                if (isVisible) {
                    if (config.currentBotInstanceOwner()) {
                        sleep(600, 1200);
                        Rs2Widget.clickWidget("Start a fight (Your friends will be able to join you).");
                        sleep(600, 1200);
                        state = BotStatus.FIGHTING;
                    } else {
                        var teammate = Rs2Player.getPlayers(x -> Objects.equals(x.getName(), config.teammateName())).findFirst().orElse(null);
                        if (teammate != null) {
                            Microbot.log("Waiting for teammate to enter the instance");
                            return;
                        }
                        Rs2Widget.clickWidget("Join a fight.");
                        sleep(600, 1200);
                        Rs2Keyboard.typeString(config.teammateName());
                        sleep(600, 1200);
                        Rs2Keyboard.keyPress(KeyEvent.VK_ENTER);
                        sleep(600, 1200);
                        state = BotStatus.FIGHTING;
                        sleep(1200, 1600);
                    }
                } else {
                    Rs2GameObject.interact(TUNNEL_ID, "Enter");
                }
                break;
        }
    }

    private void handleBanking(RoyalTitansConfig config) {
        subState = "Equipping gear";
        equipArmor(config.meleeEquipment());
        Rs2InventorySetup inventorySetup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);
        boolean equipmentLoaded;
        boolean inventoryLoaded;
        if (!inventorySetup.doesEquipmentMatch()) {
            equipmentLoaded = inventorySetup.loadEquipment();
        } else {
            equipmentLoaded = true;
        }
        subState = "Loading inventory";
        if (!inventorySetup.doesInventoryMatch()) {
            inventoryLoaded = inventorySetup.loadInventory();
        } else {
            inventoryLoaded = true;
        }

        if (equipmentLoaded && inventoryLoaded) {
            Rs2Bank.closeBank();
            travelStatus = TravelStatus.TO_TITANS;
            state = BotStatus.TRAVELLING;
        } else {
            Microbot.log("Failed to load inventory/equipment");
            shutdown();
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        isRunning = false;
        state = BotStatus.BANKING;
        travelStatus = TravelStatus.TO_BANK;
        enrageTile = null;
        disableAllPrayers();
        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }
        Microbot.log("Shutting down Royal Titans script");
    }
}
