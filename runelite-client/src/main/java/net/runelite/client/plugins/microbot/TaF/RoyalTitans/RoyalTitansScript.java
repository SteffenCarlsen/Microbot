package net.runelite.client.plugins.microbot.TaF.RoyalTitans;

import net.runelite.api.KeyCode;
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
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.misc.Rs2Potion;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer.disableAllPrayers;

public class RoyalTitansScript extends Script {
    public static String version = "1.0";
    private final Integer ICE_TITAN_ID = 14147;
    private final Integer FIRE_TITAN_ID = 12596;
    private final List<Integer> MeleeIceTitanXValues = List.of(2914, 2915, 2916);
    private final List<Integer> MeleeFireTitanXValues = List.of(2908, 2907, 2906);

    private final Integer FIRE_MINION_ID = 12596;
    private final Integer ICE_MINION_ID = 12596;
    private final Integer FIRE_WALL = 14152;
    private final Integer ICE_WALL = 14153;
    private final Integer TUNNEL_ID = 55986;
    private final Integer WIDGET_START_A_FIGHT = 14352385;
    public static final int ROYAL_TITANS_ICE_WALL = 14153;
    public static final int ROYAL_TITANS_FIRE_WALL = 14152;
    private final WorldPoint BOSS_LOCATION = new WorldPoint(2951, 9574, 0);
    private final WorldPoint BOSS_ROOM = new WorldPoint(2911, 9573, 0);
    public BotStatus state = BotStatus.TRAVELLING;
    public Tile enrageTile = null;
    private TravelStatus travelStatus = TravelStatus.TO_BANK;
    private Instant waitingTimeStart = Instant.now();
    private boolean waitedLastIteration = false;
    private boolean isRunning;

    public boolean run(RoyalTitansConfig config) {
        isRunning = true;
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
        var allGearEquipped = isGearEquipped(List.of(split));
        if (!allGearEquipped) {
            Microbot.log("Failed to equip armor - Trying again");
            equipArmor(gearCfg);
        }
    }

    private boolean isGearEquipped(List<String> gear) {
        return gear.stream().allMatch(Rs2Equipment::isWearing);
    }

    private void handleWaiting(RoyalTitansConfig config) {
        var teammate = Rs2Player.getPlayers(x -> Objects.equals(x.getName(), config.teammateName())).findFirst().orElse(null);

        if (teammate == null && !waitedLastIteration) {
            waitingTimeStart = Instant.now();
            waitedLastIteration = true;
            Microbot.log("Waiting for teammate");
            return;
        }
        if (teammate != null) {
            waitedLastIteration = false;
            state = BotStatus.TRAVELLING;
            travelStatus = TravelStatus.TO_INSTANCE;
            Microbot.log("Teammate found, entering instance");
        }

        if (teammate == null && waitingTimeStart.plusSeconds(config.waitingTimeForTeammate()).isAfter(Instant.now())) {
            Microbot.log("Teammate did not show after " + config.waitingTimeForTeammate() + " seconds, shutting down");
            shutdown();
        }
    }

    private void handleFighting(RoyalTitansConfig config) {
        handleEscaping(config);
        handlePrayers(config);
        handleEnragePhase();
        handleDangerousTiles();
        handleMinions(config);
        handleWalls(config);
        attackBoss(config);
    }

    private void handleEscaping(RoyalTitansConfig config) {
        int currentHealth = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
        int currentPrayer = Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER);
        boolean noFood = Rs2Inventory.getInventoryFood().isEmpty();
        boolean noPrayerPotions = Rs2Inventory.items().stream()
                .noneMatch(item -> item != null && item.getName() != null && !Rs2Potion.getPrayerPotionsVariants().contains(item.getName()));

        if ((noFood && currentHealth <= config.healthThreshold()) || (noPrayerPotions && currentPrayer < 10)) {
            Rs2Bank.walkToBank();
        }
    }

    private void handlePrayers(RoyalTitansConfig config) {
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

    }

    private void handleWalls(RoyalTitansConfig config) {
        var walls = Rs2Npc.getNpcs(config.minionResponsibility() == RoyalTitansConfig.Minions.FIRE_MINIONS ? FIRE_WALL : ICE_WALL).collect(Collectors.toList());
        if (walls.isEmpty()) {
            return;
        }
        if (walls.size() < 8) {
            return;
        }
        equipArmor(config.magicEquipment());
        for (var wall : walls) {
            Rs2Npc.attack(wall);
        }
    }

    private void handleMinions(RoyalTitansConfig config) {
        var minions = Rs2Npc.getNpcs(config.minionResponsibility() == RoyalTitansConfig.Minions.FIRE_MINIONS ? FIRE_MINION_ID : ICE_MINION_ID).collect(Collectors.toList());
        if (minions.isEmpty()) {
            return;
        }
        equipArmor(config.magicEquipment());
        for (var minion : minions) {
            Rs2Npc.attack(minion);
        }
    }

    private void handleEnragePhase() {
        if (enrageTile != null) {
            var success = Rs2Walker.walkFastCanvas(enrageTile.getWorldLocation());
            // Safety mechanics to handle attack animations blocking an initial attempt at walking
            if (!success && enrageTile != null) {
                Rs2Walker.walkFastCanvas(enrageTile.getWorldLocation());
            }
        }
    }

    private void handleDangerousTiles() {
        List<WorldPoint> dangerousWorldPoints = Rs2Tile.getDangerousGraphicsObjectTiles()
                .stream()
                .map(Pair::getKey)
                .collect(Collectors.toList());

        final WorldPoint safeTile = findSafeTile(Rs2Player.getWorldLocation(), dangerousWorldPoints);
        if (safeTile != null) {
            Rs2Walker.walkFastCanvas(safeTile);
        }
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
                return tile;
            }
        }

        return null;
    }

    private void handleTravelling(RoyalTitansConfig config) {
        switch (travelStatus) {
            case TO_BANK:
                var isAtBank = Rs2Bank.walkToBank();
                if (isAtBank) {
                    state = BotStatus.BANKING;
                }
                break;
            case TO_TITANS:
                var gotToTitans = Rs2Walker.walkTo(BOSS_LOCATION, 3);
                if (gotToTitans) {
                    state = BotStatus.WAITING;
                    travelStatus = TravelStatus.TO_BANK;
                }
                break;
            case TO_INSTANCE:
                var isVisible = Rs2Widget.isWidgetVisible(WIDGET_START_A_FIGHT);
                if (isVisible) {
                    if (config.currentBotInstanceOwner()) {
                        var interacted = Rs2GameObject.interact(TUNNEL_ID, "Enter");
                        if (interacted) {
                            Rs2Widget.clickWidget("Start a fight (Your friends will be able to join you).");
                            sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(BOSS_ROOM) < 20, 5000);
                            state = BotStatus.FIGHTING;
                        }
                    } else {
                        var interacted = Rs2GameObject.interact(TUNNEL_ID, "Enter");
                        if (interacted) {
                            Rs2Widget.clickWidget("Join a fight.");
                            sleep(600, 1200);
                            Rs2Keyboard.typeString(config.teammateName());
                            Rs2Keyboard.keyPress(KeyCode.KC_ENTER);
                            sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(BOSS_ROOM) < 20, 5000);
                            state = BotStatus.FIGHTING;
                        }
                    }
                }
                break;
        }
    }

    private void handleBanking(RoyalTitansConfig config) {
        Rs2InventorySetup inventorySetup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);
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
                if (!isRunning) {
                    break;
                }
            }
            if (ate) {
                inventorySetup.loadInventory();
            }
            Rs2Bank.closeBank();
            travelStatus = TravelStatus.TO_TITANS;
            state = BotStatus.TRAVELLING;
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        isRunning = false;
        disableAllPrayers();
        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }
        Microbot.log("Shutting down Demonic Gorilla script");
    }
}
