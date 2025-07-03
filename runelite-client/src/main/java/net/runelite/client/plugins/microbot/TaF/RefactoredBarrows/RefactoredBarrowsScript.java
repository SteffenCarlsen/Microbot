package net.runelite.client.plugins.microbot.TaF.RefactoredBarrows;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.JewelleryLocationEnum;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.magic.*;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.TaF.RefactoredBarrows.BarrowsConstants.BARROWS_TELEPORT_HOUSE_ID;


public class RefactoredBarrowsScript extends Script {
    public static int ChestsOpened = 0;
    public static List<String> barrowsPieces = new ArrayList<>();
    public static boolean outOfPoweredStaffCharges = false;
    public static boolean usingPoweredStaffs = false;
    public BarrowsState state = BarrowsState.BANKING;
    public BarrowsBrothers tunnelBrother = null;
    private int tunnelLoopCount = 0;
    private WorldPoint FirstLoopTile;
    private ScheduledFuture<?> WalkToTheChestFuture;
    private FightingMoundBrotherState fightingMoundBrotherState = FightingMoundBrotherState.ENTER_MOUND;
    public Rs2NpcModel currentBrother = null;
    private List<BarrowsBrothers> triedTunnelBrothers = new ArrayList<>();
    private boolean SearchingForTunnel = false;

    private void teleportToFerox() {
        stopFutureWalker();
        if (Rs2Equipment.useRingAction(JewelleryLocationEnum.FEROX_ENCLAVE)) {
            Microbot.log("We're out of supplies. Teleporting.");
            sleepUntil(() -> Rs2Player.isAnimating(), Rs2Random.between(2000, 4000));
            sleepUntil(() -> !Rs2Player.isAnimating(), Rs2Random.between(6000, 10000));
            Rs2Prayer.disableAllPrayers();
        }
    }

    public boolean run(RefactoredBarrowsConfig config) {
        Microbot.enableAutoRunOn = false;
        if (!preflightChecks()) {
            shutdown();
            return false;
        }
        if (config.overrideState()) {
            state = config.startState();
        }
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!super.run()) return;
            if (!Microbot.isLoggedIn()) return;
            preStateHandling(config);
            switch (state) {
                case BANKING:
                    handleBanking(config);
                    break;
                case TRAVELING_TO_BARROWS:
                    Rs2Prayer.disableAllPrayers();
                    handleTravellingToBarrows(config);
                    break;
                case NAVIGATING_MOUND:
                    handleNavigatingMound(config);
                    break;
                case FIGHTING_MOUND_BROTHER:
                    var alreadyKilledBrothers = getAlreadyKilledBrothers();
                    var brotherToKill = getBrotherToKill(alreadyKilledBrothers);
                    handleFightingMoundBrother(config, brotherToKill);
                    break;
                case NAVIGATING_TUNNELS:
                    handleNavigatingTunnels(config);
                    break;
                case ENTERING_TUNNELS:
                    handleEnteringTunnels(config);
                    break;
            }

        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleNavigatingTunnels(RefactoredBarrowsConfig config) {
        tunnelsFailsafe();
        solvePuzzle();
        checkForBrother(config);
        eatFood();
        outOfSupplies(config);
        gainRP(config);

        //threaded walk because the brother could appear, the puzzle door could be there.
        if (!Rs2Player.isMoving()) {
            startWalkingToTheChest();
        }

        solvePuzzle();
        checkForBrother(config);

        if (Rs2Player.getWorldLocation().distanceTo(BarrowsConstants.BARROWS_CHEST_AREA) == 5) {
            //too close for the walker to engage but too far to want to click the chest.
            stopFutureWalker();
            //stop the walker and future
            Microbot.log("Walking on screen to the chest");
            Rs2Walker.walkCanvas(BarrowsConstants.BARROWS_CHEST_AREA);
            sleepUntil(() -> !Rs2Player.isMoving() || BarrowsConstants.BARROWS_CHEST_AREA.distanceTo(Rs2Player.getWorldLocation()) <= 4, Rs2Random.between(2000, 5000));
        }

        // When player is close enough to the chest
        if (Rs2Player.getWorldLocation().distanceTo(BarrowsConstants.BARROWS_CHEST_AREA) <= 4) {
            stopFutureWalker();
            TileObject chest = Rs2GameObject.findObjectById(BarrowsConstants.BARROWS_CHEST_ID);

            // First click on chest (either spawns brother or allows looting)
            if (Rs2GameObject.interact(chest, "Open")) {
                // Wait to see if a brother spawns (hint arrow appears)
                sleepUntil(() -> Microbot.getClient().getHintArrowNpc() != null || Rs2Widget.hasWidget("Barrows chest"), Rs2Random.between(5000, 6000));
                sleep(1600,2400);
            }

            // If a brother spawned, handle combat
            if (Microbot.getClient().getHintArrowNpc() != null) {
                // Fight the brother using existing method
                checkForBrother(config);

                // After brother is defeated, wait briefly
                sleep(500, 1000);
            }

            // Second click to loot chest (only when no brother is present)
            if (Microbot.getClient().getHintArrowNpc() == null) {
                Rs2Walker.walkFastCanvas(BarrowsConstants.BARROWS_CHEST_AREA);
                // Try to loot chest
                if (Rs2GameObject.interact(chest, "Search")) {
                    sleep(600,1200);
                    sleepUntil(() -> Rs2Widget.hasWidget("Barrows chest"), Rs2Random.between(4000, 5000));
                    Rs2GameObject.interact(chest, "Search");
                    sleepUntil(() -> Rs2Widget.hasWidget("Barrows chest"), Rs2Random.between(2000, 4000));
                }

                // If chest interface appears, handle looting
                if (Rs2Widget.hasWidget("Barrows chest")) {
                    // Reset for next run
                    suppliesCheck(config);
                    ChestsOpened++;
                    tunnelBrother = null;
                    triedTunnelBrothers.clear();
                    SearchingForTunnel = false;
                    stopFutureWalker();
                    // Handle post-chest state changes
                    if (state != BarrowsState.BANKING) {
                        if (config.teleportToFeroxOnEachKill()) {
                            state = BarrowsState.BANKING;
                            teleportToFerox();
                        } else {
                            state = BarrowsState.TRAVELING_TO_BARROWS;
                        }
                    }
                }
            }
        }
        tunnelLoopCount++;
    }

    private void handleEnteringTunnels(RefactoredBarrowsConfig config) {
        if (tunnelBrother == null) {
            Microbot.log("No tunnel brother set, skipping to next state.");
            state = BarrowsState.NAVIGATING_MOUND;
            return;
        }
        stopFutureWalker();
        goToTheMound(tunnelBrother);
        digIntoTheMound(tunnelBrother);

        // Add a maximum attempt counter to prevent infinite looping
        int maxAttempts = 2;
        int attempts = 0;

        // Replace while loop with a do-while with safety checks
        do {
            GameObject sarc = Rs2GameObject.get("Sarcophagus");

            if (!super.isRunning()) {
                break;
            }

            if (Rs2GameObject.interact(sarc, "Search")) {
                sleepUntil(() -> Rs2Player.isMoving(), Rs2Random.between(1000, 3000));
                sleepUntil(() -> !Rs2Player.isMoving() || Rs2Player.isInCombat(), Rs2Random.between(3000, 6000));

                // Check for either dialogue or if we're already in the tunnel (Y > 9600)
                sleepUntil(() -> Rs2Dialogue.isInDialogue() || Rs2Player.getWorldLocation().getY() > 9600,
                        Rs2Random.between(3000, 6000));
            }

            // If we're in a dialogue, handle entering the tunnel
            if (Rs2Dialogue.isInDialogue()) {
                dialogueEnterTunnels();
                triedTunnelBrothers.clear();
                break;
            }

            // If we've already entered the tunnel without a dialogue
            if (Rs2Player.getWorldLocation().getY() > 9600) {
                Microbot.log("Entered tunnels without dialogue");
                state = BarrowsState.NAVIGATING_TUNNELS;
                return;
            }

            // If we're not in the mound anymore
            if (Rs2Player.getWorldLocation().getPlane() != 3) {
                break;
            }

            attempts++;
            Microbot.log("Tunnel search attempt " + attempts + " of " + maxAttempts);

        } while (attempts <= maxAttempts);

        // If we've exceeded our attempts, move on to the next brother
        if (attempts >= maxAttempts) {
            Microbot.log("Could not find tunnel entrance after " + maxAttempts + " attempts");
            if (tunnelBrother != null) {
                triedTunnelBrothers.add(tunnelBrother);
            }
            leaveTheMound();
            state = BarrowsState.NAVIGATING_MOUND;
            return;
        }

        state = BarrowsState.NAVIGATING_TUNNELS;
    }

    private void handleFightingMoundBrother(RefactoredBarrowsConfig config, BarrowsBrothers brother) {
        if (Rs2Player.getWorldLocation().getPlane() != 3) {
            Microbot.log("We are not in the mound, skipping to next state.");
            state = BarrowsState.NAVIGATING_MOUND;
            return;
        }

        activatePrayer(brother);

        switch (fightingMoundBrotherState) {
            case ENTER_MOUND:
                Microbot.log("Found the Sarcophagus");
                fightingMoundBrotherState = FightingMoundBrotherState.SEARCH_SARCOPHAGUS;
                break;

            case SEARCH_SARCOPHAGUS:
                GameObject sarc = Rs2GameObject.get("Sarcophagus");
                if (sarc == null) break;

                Microbot.log("Searching the Sarcophagus");
                if (Rs2GameObject.interact(sarc, "Search")) {
                    sleepUntil(() -> Rs2Player.isMoving(), Rs2Random.between(1000, 3000));
                    sleepUntil(() -> !Rs2Player.isMoving() || Rs2Player.isInCombat(), Rs2Random.between(3000, 6000));
                    sleepUntil(() -> Microbot.getClient().getHintArrowNpc() != null || Rs2Dialogue.isInDialogue(), Rs2Random.between(750, 1500));

                    if (Rs2Dialogue.isInDialogue()) {
                        tunnelBrother = brother;
                        fightingMoundBrotherState = FightingMoundBrotherState.CHECK_TUNNEL;
                    } else if (Microbot.getClient().getHintArrowNpc() != null) {
                        currentBrother = new Rs2NpcModel(Microbot.getClient().getHintArrowNpc());
                        fightingMoundBrotherState = FightingMoundBrotherState.ATTACK_BROTHER;
                    } else {
                        if (SearchingForTunnel) {
                            Microbot.log("Searching for tunnel brother failed, resetting state.");
                            fightingMoundBrotherState = FightingMoundBrotherState.LEAVE_MOUND;
                            triedTunnelBrothers.add(brother);
                        }
                        break;
                    }
                }
                break;

            case ATTACK_BROTHER:
                if (currentBrother == null || Rs2Player.isInCombat()) {
                    fightingMoundBrotherState = FightingMoundBrotherState.FIGHTING_BROTHER;
                    break;
                }

                Microbot.log("Attacking the brother");
                if (Rs2Npc.interact(currentBrother, "Attack")) {
                    sleepUntil(Rs2Player::isInCombat, Rs2Random.between(3000, 6000));
                    if (Rs2Player.isInCombat()) {
                        fightingMoundBrotherState = FightingMoundBrotherState.FIGHTING_BROTHER;
                    }
                }
                break;

            case FIGHTING_BROTHER:
                if (currentBrother == null || Microbot.getClient().getHintArrowNpc() == null) {
                    fightingMoundBrotherState = FightingMoundBrotherState.LEAVE_MOUND;
                    break;
                }

                if (currentBrother.isDead()) {
                    disablePrayer();
                    fightingMoundBrotherState = FightingMoundBrotherState.LEAVE_MOUND;
                    break;
                }

                activatePrayer(brother);
                eatFood();
                outOfSupplies(config);
                antiPatternDropVials();
                drinkforgottonbrew();
                drinkPrayerPot();
                break;

            case CHECK_TUNNEL:
                if (Rs2Dialogue.isInDialogue()) {
                    if (brother.name().equals(tunnelBrother.name()) && brother.name().contains("Verac")) {
                        dialogueEnterTunnels();
                        return;
                    } else {
                        fightingMoundBrotherState = FightingMoundBrotherState.LEAVE_MOUND;
                    }
                } else {
                    fightingMoundBrotherState = FightingMoundBrotherState.LEAVE_MOUND;
                }
                break;

            case LEAVE_MOUND:
                leaveTheMound();
                // Reset state for next brother
                fightingMoundBrotherState = FightingMoundBrotherState.ENTER_MOUND;
                currentBrother = null;
                break;
        }
    }

    private void handleNavigatingMound(RefactoredBarrowsConfig config) {
        var alreadyKilledBrothers = getAlreadyKilledBrothers();
        var brotherToKill = getBrotherToKill(alreadyKilledBrothers);
        if (tunnelBrother != null && tunnelBrother.equals(brotherToKill)) {
            state = BarrowsState.ENTERING_TUNNELS;
            return;
        }
        outOfSupplies(config);
        if (state == BarrowsState.BANKING) {
            stopFutureWalker();
            return;
        }
        stopFutureWalker();
        if (Rs2Player.getWorldLocation().getPlane() != 3) {
            goToTheMound(brotherToKill);
            digIntoTheMound(brotherToKill);
        }
    }

    private BarrowsBrothers getBrotherToKill(List<BarrowsBrothers> alreadyKilledBrothers) {
        // First try to find a non-killed, non-tunnel brother to kill
        for (BarrowsBrothers brother : BarrowsBrothers.values()) {
            if (!alreadyKilledBrothers.contains(brother)) {
                if (tunnelBrother != null && tunnelBrother.equals(brother)) {
                    continue; // Skip the tunnel brother
                }
                return brother;
            }
        }

        // If tunnelBrother is known, return it
        if (tunnelBrother != null) {
            return tunnelBrother;
        }

        // If all brothers are killed and tunnel brother is unknown (after teleporting),
        // systematically try each brother's mound until we find the tunnel
        if (alreadyKilledBrothers.size() == BarrowsBrothers.values().length) {
            Microbot.log("All brothers killed but tunnel is unknown. Finding tunnel brother...");

            // Try brothers in order, skipping those we've already tried
            for (BarrowsBrothers brother : BarrowsBrothers.values()) {
                if (!triedTunnelBrothers.contains(brother)) {
                    Microbot.log("Trying " + brother.name() + "'s mound to find tunnel");
                    // Don't add to triedTunnelBrothers here, do it only after confirming
                    return brother;
                }
            }

            // If we've tried all brothers, reset and start over
            // This shouldn't happen unless there's another issue
            if (triedTunnelBrothers.size() >= BarrowsBrothers.values().length) {
                Microbot.log("Tried all brothers and couldn't find tunnel. Resetting search.");
                triedTunnelBrothers.clear();
                return BarrowsBrothers.values()[0];
            }
        }

        // Fallback - should not reach here, but return first brother as default
        Microbot.log("Returning a default brother as fallback. This should never happen, please report how you got to this state.");
        return BarrowsBrothers.values()[0];
    }

    private List<BarrowsBrothers> getAlreadyKilledBrothers() {
        if (everyBrotherWasKilled()) {
            SearchingForTunnel = true;
            Microbot.log("All brothers marked as killed - Checking all brothers for tunnel.");
        }
        List<BarrowsBrothers> alreadyKilledBrothers = new ArrayList<>();

        if (Microbot.getVarbitValue(Varbits.BARROWS_KILLED_DHAROK) == 1) {
            alreadyKilledBrothers.add(BarrowsBrothers.DHAROK);
        }

        if (Microbot.getVarbitValue(Varbits.BARROWS_KILLED_GUTHAN) == 1) {
            alreadyKilledBrothers.add(BarrowsBrothers.GUTHAN);
        }

        if (Microbot.getVarbitValue(Varbits.BARROWS_KILLED_KARIL) == 1) {
            alreadyKilledBrothers.add(BarrowsBrothers.KARIL);
        }

        if (Microbot.getVarbitValue(Varbits.BARROWS_KILLED_TORAG) == 1) {
            alreadyKilledBrothers.add(BarrowsBrothers.TORAG);
        }

        if (Microbot.getVarbitValue(Varbits.BARROWS_KILLED_VERAC) == 1) {
            alreadyKilledBrothers.add(BarrowsBrothers.VERAC);
        }

        if (Microbot.getVarbitValue(Varbits.BARROWS_KILLED_AHRIM) == 1) {
            alreadyKilledBrothers.add(BarrowsBrothers.AHRIM);
        }

        return alreadyKilledBrothers;
    }

    private void handleTravellingToBarrows(RefactoredBarrowsConfig config) {
        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
        }
        if (Rs2Player.getWorldLocation().distanceTo(BarrowsConstants.BARROWS_TP_AREA) > 60) {
            switch (config.selectedToBarrowsTPMethod()) {
                case Tablet:
                    Rs2Inventory.interact(ItemID.BARROWS_TELEPORT, "Break");
                    sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(BarrowsConstants.BARROWS_TP_AREA) < 20, Rs2Random.between(4000, 9000));
                    if (Rs2Player.getWorldLocation().distanceTo(BarrowsConstants.BARROWS_TP_AREA) < 60) {
                        state = BarrowsState.NAVIGATING_MOUND;
                    }
                    break;
                case POH_TABLET:
                    if (Rs2GameObject.getGameObject(BARROWS_TELEPORT_HOUSE_ID) == null) {
                        Rs2Inventory.interact("Teleport to house", "Inside");
                        sleepUntil(() -> Rs2Player.getAnimation() == 4069, Rs2Random.between(2000, 4000));
                        sleepUntil(() -> !Rs2Player.isAnimating(), Rs2Random.between(6000, 10000));
                        sleepUntil(() -> Rs2GameObject.getGameObject(4525) != null, Rs2Random.between(6000, 10000));
                        handlePOH(config);
                    }
                    break;
                case POH_TELEPORT:
                    if (Rs2GameObject.getGameObject(BARROWS_TELEPORT_HOUSE_ID) == null) {
                        Rs2Magic.cast(MagicAction.TELEPORT_TO_HOUSE);
                        sleepUntil(() -> Rs2Player.getAnimation() == 4069, Rs2Random.between(2000, 4000));
                        sleepUntil(() -> !Rs2Player.isAnimating(), Rs2Random.between(6000, 10000));
                        sleepUntil(() -> Rs2GameObject.getGameObject(BARROWS_TELEPORT_HOUSE_ID) != null, Rs2Random.between(6000, 10000));
                        handlePOH(config);
                        break;
                    }
                case POH_GROUP_IRONMAN:
                    if (config.groupIronmanTeammateName().isEmpty()) {
                        Microbot.log("You must set your group ironman teammate's name in the config to use this teleport method.");
                        shutdown();
                        return;
                    }
                    if (Rs2GameObject.getGameObject(BARROWS_TELEPORT_HOUSE_ID) == null) {
                        Rs2Magic.cast(MagicAction.TELEPORT_TO_HOUSE, "Group: Choose", 4);
                        interactWithGroupIronmanHouseTeleportWidget(config.groupIronmanTeammateName());
                        sleepUntil(() -> Rs2Player.getAnimation() == 4069, Rs2Random.between(2000, 4000));
                        sleepUntil(() -> !Rs2Player.isAnimating(), Rs2Random.between(6000, 10000));
                        sleepUntil(() -> Rs2GameObject.getGameObject(BARROWS_TELEPORT_HOUSE_ID) != null, Rs2Random.between(6000, 10000));
                        handlePOH(config);
                        break;
                    }
                    break;
            }
        }
    }
    private static boolean interactWithGroupIronmanHouseTeleportWidget(String teammateName) {
        // Wait for the widget to become visible
        boolean isAdventureLogVisible = sleepUntilTrue(() -> !Rs2Widget.isHidden(ComponentID.ADVENTURE_LOG_CONTAINER), Rs2Player::isMoving, 100, 10000);

        if (!isAdventureLogVisible) {
            Microbot.log("Widget did not become visible within the timeout.");
            return false;
        }

        for (var house : Houselocations.values()) {
            String destinationString = teammateName + " (" + house.getName() +")" + " - Inside";
            Widget destinationWidget = Rs2Widget.findWidget(destinationString, List.of(Rs2Widget.getWidget(187, 3)));
            if (destinationWidget != null) {
                Rs2Widget.clickWidget(destinationWidget);
                return true;
            }
        }

        return false;
    }

    private void handleBanking(RefactoredBarrowsConfig config) {
        if (Rs2Bank.walkToBank(BankLocation.FEROX_ENCLAVE)) {
            handleFeroxPool();
            if (Rs2Bank.openBank()) {
                if (config.useInventorySetups()) {
                    if (config.inventorySetup() == null) {
                        Microbot.log("Inventory setup not found. Please select a valid inventory setup in the config. If you just made the inventory setup, please reselect the inventory setup in the config.");
                        shutdown();
                        return;
                    }
                    var inventorySetup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);
                    if (inventorySetup.doesInventoryMatch() && inventorySetup.doesEquipmentMatch()) {
                        state = BarrowsState.TRAVELING_TO_BARROWS;
                        return;
                    }
                    if (!inventorySetup.doesEquipmentMatch()) {
                        inventorySetup.loadEquipment();
                    }
                    if (!inventorySetup.doesInventoryMatch()) {
                        inventorySetup.loadInventory();
                    }
                    Rs2Bank.closeBank();
                    state = BarrowsState.TRAVELING_TO_BARROWS;
                    return;
                } else {

                    Rs2Bank.depositAllExcept("blood rune", "law rune", "death rune", "wrath rune", "air rune", "earth rune", "Rune pouch", "Moonlight moth", "Moonlight moth mix (2)", "Teleport to house", "Spade", "Prayer potion(4)", "Prayer potion(3)", "Forgotten brew(4)", "Forgotten brew(3)", "Barrows teleport", config.food().getName());
                    if (!Rs2Inventory.hasItem("Spade")) {
                        Rs2Bank.withdrawOne("Spade", true);
                    }
                    if (config.selectedToBarrowsTPMethod().equals(BarrowsTeleportChoice.POH_TELEPORT) || config.selectedToBarrowsTPMethod().equals(BarrowsTeleportChoice.POH_GROUP_IRONMAN)) {
                        var runes = getRequiredRunes(Rs2Spells.TELEPORT_TO_HOUSE);
                        for (var rune : runes.keySet()) {
                            Rs2Bank.withdrawAll(rune.getId());
                            sleepUntil(() -> Rs2Inventory.waitForInventoryChanges(2400));
                        }
                        if (!Rs2Magic.hasRequiredRunes(Rs2Spells.TELEPORT_TO_HOUSE)) {
                            Microbot.log("Not enough runes to cast Teleport to House. Shutting down...");
                            shutdown();
                            return;
                        }
                    }
                    if (config.selectedToBarrowsTPMethod().equals(BarrowsTeleportChoice.Tablet) && Rs2Inventory.count("Barrows teleport") < 5) {
                        Rs2Bank.withdrawAllButOne("Barrows teleport");
                    }
                    if (config.forgottenBrewCount() > 0) {
                        var quantity = BarrowsUtility.calculateWithdrawQuantity(27629, config.forgottenBrewCount());
                        if (quantity > 0) {
                            Rs2Bank.withdrawX("Forgotten brew(4)", quantity);
                            sleepUntil(() -> Rs2Inventory.waitForInventoryChanges(2400));
                        }
                    }
                    var prayerPotsQuanity = BarrowsUtility.calculateWithdrawQuantity(config.prayerRestoreType().getPrayerRestoreTypeID(), config.prayerRestoreCount());
                    if (prayerPotsQuanity > 0) {
                        Rs2Bank.withdrawX(config.prayerRestoreType().getPrayerRestoreTypeID(), prayerPotsQuanity);
                        sleepUntil(() -> Rs2Inventory.waitForInventoryChanges(2400));
                    }
                    var foodQuanity = BarrowsUtility.calculateWithdrawQuantity(config.food().getId(), config.foodCount());
                    if (foodQuanity > 0) {
                        Rs2Bank.withdrawX(config.food().getId(), foodQuanity);
                        sleepUntil(() -> Rs2Inventory.waitForInventoryChanges(2400));
                    }
                    if (!Rs2Equipment.isWearing("Ring of dueling")) {
                        Rs2Bank.withdrawAndEquip("Ring of dueling(8)");
                    }
                    var runes = getRequiredRunes();
                    for (var rune : runes.keySet()) {
                        Rs2Bank.withdrawAll(rune.getItemId());
                        sleepUntil(() -> Rs2Inventory.waitForInventoryChanges(2400));
                    }
                    if (!hasCombatRunes()) {
                        Microbot.log("Not enough runes to continue. shutting down...");
                        shutdown();
                        return;
                    }
                    Rs2Bank.closeBank();
                }

            }
        }

        state = BarrowsState.TRAVELING_TO_BARROWS;
    }

    private void handleFeroxPool() {
        if (Rs2Player.getHealthPercentage() < 70 || Rs2Player.getPrayerPercentage() < 90) {
            var feroxPool = Rs2GameObject.get("Pool of Refreshment", true);
            if (feroxPool != null) {
                Rs2GameObject.interact(feroxPool, "Drink");
                sleepUntil(Rs2Player::isMoving, Rs2Random.between(1000,3000));
                sleepUntil(()-> !Rs2Player.isMoving(), Rs2Random.between(5000,10000));
                sleepUntil(Rs2Player::isAnimating, Rs2Random.between(1000,4000));
                sleepUntil(()-> !Rs2Player.isAnimating(), Rs2Random.between(1000,4000));
                Rs2Bank.walkToBank(BankLocation.FEROX_ENCLAVE);
            }
        }
    }

    private void preStateHandling(RefactoredBarrowsConfig config) {
        // For overlay
        if (barrowsPieces.isEmpty()) {
            barrowsPieces.add("Nothing yet.");
        }

        // For modifying the state
        if (Rs2Player.getWorldLocation().getY() > 9600 && Rs2Player.getWorldLocation().getY() < 9730) {
        } else {
            if (tunnelLoopCount != 0) {
                //reset the tunnels loop counter
                tunnelLoopCount = 0;
            }
        }

        //powered staffs
        usingPoweredStaffs = Rs2Equipment.get(EquipmentInventorySlot.WEAPON).getName().contains("Trident of the") ||
                Rs2Equipment.get(EquipmentInventorySlot.WEAPON).getName().contains("Tumeken's") ||
                Rs2Equipment.get(EquipmentInventorySlot.WEAPON).getName().contains("sceptre") ||
                Rs2Equipment.get(EquipmentInventorySlot.WEAPON).getName().contains("Sanguinesti") ||
                Rs2Equipment.get(EquipmentInventorySlot.WEAPON).getName().contains("Crystal staff");

        if (usingPoweredStaffs && outOfPoweredStaffCharges) {
            shutdown();
        }

        if (!usingPoweredStaffs) {
            if (Rs2Magic.getCurrentAutoCastSpell() == null) {
                Microbot.log("Please select your wind spell in auto-cast then restart the script. stopping...");
                super.shutdown();
            }

            if (!hasCombatRunes()) {
                Microbot.log("Not enough runes to continue. banking...");
                state = BarrowsState.BANKING;
            }

            if (Rs2Inventory.getInventoryFood().isEmpty()) {
                Microbot.log("No food left in inventory. Banking...");
                state = BarrowsState.BANKING;
            }
        }

        outOfSupplies(config);
    }

    private boolean preflightChecks() {
        if (Rs2Player.getQuestState(Quest.HIS_FAITHFUL_SERVANTS) != QuestState.FINISHED) {
            Microbot.log("You must complete the 'His Faithful Servants' quest to use the Barrows script. Complete one manual barrows run to unlock the webwalker.");
            return false;
        }

        return true;
    }

    public void closeBank() {
        if (Rs2Bank.isOpen()) {
            while (Rs2Bank.isOpen()) {

                if (!super.isRunning()) {
                    break;
                }

                if (Rs2Bank.closeBank()) {
                    sleepUntil(() -> !Rs2Bank.isOpen(), Rs2Random.between(2000, 4000));
                }
            }
        }
    }

    public void handlePOH(RefactoredBarrowsConfig config) {
        if (Rs2GameObject.getGameObject(BARROWS_TELEPORT_HOUSE_ID) != null) {
            Microbot.log("We're in our POH");
            var housePool = Rs2GameObject.getGameObject(BarrowsConstants.HOUSE_REJUVINATION_POOLS);
            if (housePool != null) {
                if (Rs2GameObject.interact(housePool, "Drink")) {
                    sleepUntil(() -> Rs2Player.isMoving(), Rs2Random.between(2000, 4000));
                    sleepUntil(() -> !Rs2Player.isMoving(), Rs2Random.between(10000, 15000));
                }
            }
            GameObject regularPortal = Rs2GameObject.getGameObject("Barrows Portal");
            if (regularPortal != null) {
                while (Rs2GameObject.getGameObject(BARROWS_TELEPORT_HOUSE_ID) != null) {
                    if (!super.isRunning()) {
                        break;
                    }
                    if (!Rs2Player.isMoving()) {
                        if (Rs2GameObject.interact(regularPortal, "Enter")) {
                            sleepUntil(() -> Rs2Player.isMoving(), Rs2Random.between(2000, 4000));
                            sleepUntil(() -> !Rs2Player.isMoving(), Rs2Random.between(10000, 15000));
                            sleepUntil(() -> Rs2GameObject.getGameObject("Barrows Portal") == null, Rs2Random.between(10000, 15000));
                        }
                    }
                }
                state = BarrowsState.NAVIGATING_MOUND;
            }
        }
    }

    public boolean everyBrotherWasKilled() {
        return Microbot.getVarbitValue(Varbits.BARROWS_KILLED_DHAROK) == 1 && Microbot.getVarbitValue(Varbits.BARROWS_KILLED_GUTHAN) == 1 && Microbot.getVarbitValue(Varbits.BARROWS_KILLED_KARIL) == 1 &&
                Microbot.getVarbitValue(Varbits.BARROWS_KILLED_TORAG) == 1 && Microbot.getVarbitValue(Varbits.BARROWS_KILLED_VERAC) == 1 && Microbot.getVarbitValue(Varbits.BARROWS_KILLED_AHRIM) == 1;
    }

    public void dialogueEnterTunnels() {
        if (Rs2Dialogue.isInDialogue()) {
            while (Rs2Dialogue.isInDialogue()) {
                if (!super.isRunning()) {
                    break;
                }
                if (Rs2Dialogue.hasContinue()) {
                    Rs2Dialogue.clickContinue();
                    sleepUntil(() -> Rs2Dialogue.hasDialogueOption("Yeah I'm fearless!"), Rs2Random.between(2000, 5000));
                    sleep(300, 600);
                }
                if (Rs2Dialogue.hasDialogueOption("Yeah I'm fearless!")) {
                    if (Rs2Dialogue.clickOption("Yeah I'm fearless!")) {
                        sleepUntil(() -> Rs2Player.getWorldLocation().getY() > 9600 && Rs2Player.getWorldLocation().getY() < 9730, Rs2Random.between(2500, 6000));
                        //allow some time for the tunnel to load.
                        sleep(1000, 2000);
                        state = BarrowsState.NAVIGATING_TUNNELS;
                    }
                }
                if (!Rs2Dialogue.isInDialogue()) {
                    break;
                }
                if (Rs2Player.getWorldLocation().getPlane() != 3) {
                    //we're not in the mound
                    break;
                }
            }
        }
    }

    public void digIntoTheMound(BarrowsBrothers brothers) {
        Microbot.status = "Digging into the mound of " + brothers.name();
        while (brothers.getHumpWP().contains(Rs2Player.getWorldLocation()) && Rs2Player.getWorldLocation().getPlane() != 3) {

            if (!super.isRunning()) {
                break;
            }

            antiPatternEnableWrongPrayer(brothers);
            antiPatternActivatePrayer(brothers);

            if (Rs2Inventory.contains("Spade")) {
                if (Rs2Inventory.interact("Spade", "Dig")) {
                    sleepUntil(() -> Rs2Player.getWorldLocation().getPlane() == 3, Rs2Random.between(3000, 5000));
                }
            }

            if (Rs2Player.getWorldLocation().getPlane() == 3) {
                //we made it in
                break;
            }
        }
        sleepUntil(() -> Rs2Player.getWorldLocation().getPlane() == 3, Rs2Random.between(3000, 5000));
        state = BarrowsState.FIGHTING_MOUND_BROTHER;
    }

    public void goToTheMound(BarrowsBrothers brother) {
        Microbot.status = "Walking to the mound of " + brother.name();
        while (!brother.getHumpWP().contains(Rs2Player.getWorldLocation())) {
            int totalTiles = brother.getHumpWP().toWorldPointList().size();
            WorldPoint randomMoundTile;
            if (!super.isRunning()) {
                break;
            }

            //antipattern turn on prayer early
            antiPatternEnableWrongPrayer(brother);

            antiPatternActivatePrayer(brother);

            antiPatternDropVials();
            //antipattern

            // We're not in the mound yet.
            randomMoundTile = brother.getHumpWP().toWorldPointList().get(Rs2Random.between(0, (totalTiles - 1)));
            if (Rs2Walker.walkTo(randomMoundTile)) {
                sleepUntil(() -> !Rs2Player.isMoving(), Rs2Random.between(2000, 4000));
            }
            if (brother.getHumpWP().contains(Rs2Player.getWorldLocation())) {
                if (!Rs2Player.isMoving()) {
                    break;
                }
            } else {
                if (!Rs2Player.isMoving() && brother.getHumpWP().contains(Rs2Player.getWorldLocation())) {
                    return;
                }
                Microbot.log("At the mound, but we can't dig yet.");
                randomMoundTile = brother.getHumpWP().toWorldPointList().get(Rs2Random.between(0, (totalTiles - 1)));

                //strange old man body blocking us
                if (Rs2Npc.getNpc("Strange Old Man") != null) {
                    if (Rs2Npc.getNpc("Strange Old Man").getWorldLocation() != null) {
                        if (Rs2Npc.getNpc("Strange Old Man").getWorldLocation() == randomMoundTile) {
                            while (Rs2Npc.getNpc("Strange Old Man").getWorldLocation() == randomMoundTile) {
                                if (!super.isRunning()) {
                                    break;
                                }
                                randomMoundTile = brother.getHumpWP().toWorldPointList().get(Rs2Random.between(0, (totalTiles - 1)));
                                sleep(250, 500);
                            }
                        }
                    }
                }

                Rs2Walker.walkCanvas(randomMoundTile);
                sleepUntil(() -> !Rs2Player.isMoving(), Rs2Random.between(2000, 4000));
            }
        }
    }

    public void leaveTheMound() {
        Rs2Prayer.disableAllPrayers();
        if (Rs2GameObject.get("Staircase", true) != null) {
            if (Rs2GameObject.hasLineOfSight(Rs2GameObject.get("Staircase", true))) {
                if (Rs2Player.getWorldLocation().getPlane() == 3) {
                    while (Rs2Player.getWorldLocation().getPlane() == 3) {
                        if (!super.isRunning()) {
                            break;
                        }
                        if (Rs2GameObject.interact("Staircase", "Climb-up")) {
                            sleepUntil(() -> Rs2Player.getWorldLocation().getPlane() != 3, Rs2Random.between(3000, 6000));
                        }
                        if (Rs2Player.getWorldLocation().getPlane() != 3) {
                            disablePrayer();
                            break;
                        }
                    }
                }
            }
            state = BarrowsState.NAVIGATING_MOUND;
        }
    }

    public void gainRP(RefactoredBarrowsConfig config) {
        if (config.shouldGainRP()) {
            int RP = Microbot.getVarbitValue(Varbits.BARROWS_REWARD_POTENTIAL);
            if (RP > 870) {
                return;
            }
            if (getAlreadyKilledBrothers().size() < 6 && RP > 70) {
                return;
            }
            Rs2NpcModel skele = Rs2Npc.getNpc("Skeleton");
            if (skele == null || skele.isDead()) {
                return;
            }
            if (Rs2Npc.hasLineOfSight(skele)) {
                stopFutureWalker();
                if (!Rs2Player.isInCombat()) {
                    if (Rs2Npc.attack(skele)) {
                        sleepUntil(() -> Rs2Player.isInCombat() && !Rs2Player.isMoving(), Rs2Random.between(4000, 8000));
                    }
                }
                if (Rs2Player.isInCombat()) {
                    while (Rs2Player.isInCombat()) {
                        if (!super.isRunning()) {
                            break;
                        }
                        sleep(750, 1500);
                        eatFood();
                        outOfSupplies(config);
                        antiPatternDropVials();

                        if (state == BarrowsState.BANKING) {
                            return;
                        }

                        if (!Rs2Player.isInCombat()) {
                            Microbot.log("Breaking out we're no longer in combat.");
                            break;
                        }

                        if (skele.isDead()) {
                            Microbot.log("Breaking out the skeleton is dead.");
                            break;
                        }

                        if (Microbot.getVarbitValue(Varbits.BARROWS_REWARD_POTENTIAL) > 870) {
                            Microbot.log("Breaking out we have enough RP.");
                            break;
                        }

                        if (Microbot.getClient().getHintArrowNpc() != null) {
                            Rs2NpcModel barrowsbrother = new Rs2NpcModel(Microbot.getClient().getHintArrowNpc());
                            if (Rs2Npc.hasLineOfSight(barrowsbrother)) {
                                Microbot.log("The brother is here.");
                                break;
                            }
                        }

                    }
                }
            }
        }
    }

    public void stopFutureWalker() {
        if (WalkToTheChestFuture != null) {
            Rs2Walker.setTarget(null);
            WalkToTheChestFuture.cancel(true);
            //stop the walker and future
        }
    }

    public void suppliesCheck(RefactoredBarrowsConfig config) {
        if (!usingPoweredStaffs) {
            if (Rs2Equipment.get(EquipmentInventorySlot.RING) == null ||
                    !Rs2Inventory.contains("Spade") ||
                    Rs2Inventory.count(Rs2Inventory.getInventoryFood().get(0).getName()) < 2 ||
                    (Rs2Inventory.get(config.selectedToBarrowsTPMethod().getToBarrowsTPMethodItemID()) == null &&
                            config.selectedToBarrowsTPMethod().equals(BarrowsTeleportChoice.Tablet)) ||
                    (Rs2Inventory.count(it -> it != null && it.getName().contains("Forgotten brew(")) < 1 &&
                            config.forgottenBrewCount() > 0) ||
                    Rs2Inventory.count(config.prayerRestoreType().getPrayerRestoreTypeID()) < 1 ||
                    !hasCombatRunes() ||
                    Rs2Player.getRunEnergy() <= 5) {

                Microbot.log("Changing state to BANKING");
                state = BarrowsState.BANKING;
                return;
            }
        }

        if (usingPoweredStaffs) {
            if (Rs2Equipment.get(EquipmentInventorySlot.RING) == null ||
                    !Rs2Inventory.contains("Spade") ||
                    Rs2Inventory.count(Rs2Inventory.getInventoryFood().get(0).getName()) < 2 ||
                    (Rs2Inventory.get(config.selectedToBarrowsTPMethod().getToBarrowsTPMethodItemID()) == null &&
                            config.selectedToBarrowsTPMethod().equals(BarrowsTeleportChoice.Tablet)) ||
                    (Rs2Inventory.count(it -> it != null && it.getName().contains("Forgotten brew(")) < 1 &&
                            config.forgottenBrewCount() > 0) ||
                    Rs2Inventory.count(config.prayerRestoreType().getPrayerRestoreTypeID()) < 1 ||
                    outOfPoweredStaffCharges ||
                    Rs2Player.getRunEnergy() <= 5) {

                Microbot.log("Changing state to BANKING");
                state = BarrowsState.BANKING;
            }
        }
    }

    public void tunnelsFailsafe() {
        //needed for rare occasions where the walker messes up
        if (tunnelLoopCount < 1) {
            FirstLoopTile = Rs2Player.getWorldLocation();
        }
        if (tunnelLoopCount >= 15) {
            WorldPoint currentTile = Rs2Player.getWorldLocation();
            if (currentTile != null && FirstLoopTile != null) {
                if (currentTile.equals(FirstLoopTile)) {
                    Microbot.log("We seem to be stuck. Resetting the walker");
                    stopFutureWalker();
                    tunnelLoopCount = 0;
                }
            }
        }
        if (tunnelLoopCount >= 30) {
            tunnelLoopCount = 0;
        }
    }

    public boolean hasCombatRunes() {
        Rs2CombatSpells ourSpell = Rs2Magic.getCurrentAutoCastSpell();
        if (ourSpell == null) {
            Microbot.log("No autocast spell set, stopping...");
            shutdown();
            return false;
        }
        var ourStaff = Rs2Equipment.get(EquipmentInventorySlot.WEAPON);
        var runesToCast = Rs2Magic.getMissingRunes(ourSpell, 200);
        if (runesToCast.isEmpty()) {
            return true;
        }
        return false;
    }

    public Map<Runes, Integer> getRequiredRunes() {
        Rs2CombatSpells ourSpell = Rs2Magic.getCurrentAutoCastSpell();
        return Rs2Magic.getMissingRunes(ourSpell, 200);
    }

    public Map<Runes, Integer> getRequiredRunes(Rs2Spells spell) {
        var ourStaff = Rs2Equipment.get(EquipmentInventorySlot.WEAPON);
        return Rs2Magic.getMissingRunes(spell, 10);
    }

    public void activatePrayer(BarrowsBrothers brother) {
        if (!Rs2Prayer.isPrayerActive(brother.getWhatToPray())) {
            while (!Rs2Prayer.isPrayerActive(brother.getWhatToPray())) {
                if (!super.isRunning()) {
                    break;
                }
                drinkPrayerPot();
                Rs2Prayer.toggle(brother.getWhatToPray());
                sleep(0, 750);
                if (Rs2Prayer.isPrayerActive(brother.getWhatToPray())) {
                    break;
                }
            }
        }
    }

    public void antiPatternEnableWrongPrayer(BarrowsBrothers brother) {
        if (!Rs2Prayer.isPrayerActive(brother.getWhatToPray())) {
            if (Rs2Random.between(0, 100) <= Rs2Random.between(1, 4)) {
                Rs2PrayerEnum wrongPrayer = null;
                int random = Rs2Random.between(0, 100);
                if (random <= 50) {
                    wrongPrayer = Rs2PrayerEnum.PROTECT_MELEE;
                }
                if (random > 50 && random < 75) {
                    wrongPrayer = Rs2PrayerEnum.PROTECT_RANGE;
                }
                if (random >= 75) {
                    wrongPrayer = Rs2PrayerEnum.PROTECT_MAGIC;
                }
                drinkPrayerPot();
                Rs2Prayer.toggle(wrongPrayer);
                sleep(0, 750);
            }
        }
    }

    public void antiPatternActivatePrayer(BarrowsBrothers brother) {
        if (!Rs2Prayer.isPrayerActive(brother.getWhatToPray())) {
            if (Rs2Random.between(0, 100) <= Rs2Random.between(1, 8)) {
                drinkPrayerPot();
                Rs2Prayer.toggle(brother.getWhatToPray());
                sleep(0, 750);
            }
        }
    }

    public void antiPatternDropVials() {
        if (Rs2Random.between(0, 100) <= Rs2Random.between(1, 25)) {
            Rs2ItemModel whatToDrop = Rs2Inventory.get(it -> it != null && it.getName().contains("Vial") || it.getName().contains("Butterfly jar"));
            if (whatToDrop != null) {
                if (Rs2Inventory.contains(whatToDrop.getName())) {
                    if (Rs2Inventory.drop(whatToDrop.getName())) {
                        sleep(0, 750);
                    }
                }
            }
        }
    }

    public void outOfSupplies(RefactoredBarrowsConfig config) {
        suppliesCheck(config);
        // Needed because the walker won't teleport to the enclave while in the tunnels or in a barrow
        if (state == BarrowsState.BANKING && (Rs2Player.getWorldLocation().getPlane() == 3)) {
            teleportToFerox();
        }
    }

    public void disablePrayer() {
        if (Rs2Random.between(0, 100) >= Rs2Random.between(0, 5)) {
            Rs2Prayer.disableAllPrayers();
            sleep(0, 750);
        }
    }

    public void reJfount() {
        int rejat = Rs2Random.between(10, 30);
        int runener = Rs2Random.between(50, 65);
        while (Rs2Player.getBoostedSkillLevel(Skill.PRAYER) < rejat || Rs2Player.getRunEnergy() <= runener) {
            if (!super.isRunning()) {
                break;
            }
            if (Rs2Bank.isOpen()) {
                if (Rs2Bank.closeBank()) {
                    sleepUntil(() -> !Rs2Bank.isOpen(), Rs2Random.between(2000, 4000));
                }
            } else {
                GameObject rej = Rs2GameObject.get("Pool of Refreshment", true);
                Microbot.log("Drinking");
                if (Rs2GameObject.interact(rej, "Drink")) {
                    sleepUntil(() -> Rs2Player.isMoving(), Rs2Random.between(1000, 3000));
                    sleepUntil(() -> !Rs2Player.isMoving(), Rs2Random.between(5000, 10000));
                    sleepUntil(() -> Rs2Player.isAnimating(), Rs2Random.between(1000, 4000));
                    sleepUntil(() -> !Rs2Player.isAnimating(), Rs2Random.between(1000, 4000));
                }
            }
            if (Rs2Player.getBoostedSkillLevel(Skill.PRAYER) >= rejat && Rs2Player.getRunEnergy() >= runener) {
                break;
            }

        }
    }

    public void drinkPrayerPot() {
        if (Rs2Player.getBoostedSkillLevel(Skill.PRAYER) <= Rs2Random.between(9, 15)) {
            if (Rs2Inventory.contains(it -> it != null && it.getName().contains("Prayer potion") || it.getName().contains("moth mix") || it.getName().contains("Moonlight moth"))) {
                Rs2ItemModel prayerpotion = Rs2Inventory.get(it -> it != null && it.getName().contains("Prayer potion") || it.getName().contains("moth mix") || it.getName().contains("Moonlight moth"));
                String action = "Drink";
                if (prayerpotion.getName().equals("Moonlight moth")) {
                    action = "Release";
                }
                if (Rs2Inventory.interact(prayerpotion, action)) {
                    sleep(0, 750);
                }
            }
        }
    }

    public void checkForBrother(RefactoredBarrowsConfig config) {
        NPC hintArrow = Microbot.getClient().getHintArrowNpc();
        Rs2NpcModel currentBrother = null;
        if (hintArrow != null) {
            currentBrother = new Rs2NpcModel(hintArrow);
            stopFutureWalker();
            Rs2PrayerEnum neededprayer = Rs2PrayerEnum.PROTECT_MELEE;
            if (Rs2Npc.hasLineOfSight(currentBrother)) {
                if (currentBrother.getName().contains("Ahrim")) {
                    neededprayer = Rs2PrayerEnum.PROTECT_MAGIC;
                }
                if (currentBrother.getName().contains("Karil")) {
                    neededprayer = Rs2PrayerEnum.PROTECT_RANGE;
                }
                //activate prayer
                if (!Rs2Prayer.isPrayerActive(neededprayer)) {
                    Microbot.log("Turning on Prayer.");
                    while (!Rs2Prayer.isPrayerActive(neededprayer)) {
                        if (!super.isRunning()) {
                            break;
                        }
                        drinkPrayerPot();
                        Rs2Prayer.toggle(neededprayer);
                        sleep(0, 750);
                        if (Rs2Prayer.isPrayerActive(neededprayer)) {
                            //we made it in
                            Microbot.log("Praying");
                            break;
                        }
                    }
                }
                //fight brother
                if (currentBrother != null && !Rs2Player.isInCombat()) {
                    while (!Rs2Player.isInCombat()) {
                        if (!super.isRunning()) {
                            break;
                        }
                        Rs2Npc.interact(currentBrother, "Attack");
                        sleepUntil(() -> Rs2Player.isInCombat(), Rs2Random.between(3000, 6000));
                    }
                }
                //fighting
                while (Microbot.getClient().getHintArrowNpc() != null) {
                    if (!super.isRunning()) {
                        break;
                    }
                    if (!Rs2Npc.hasLineOfSight(currentBrother)) {
                        break;
                    }
                    sleep(750, 1500);
                    drinkPrayerPot();
                    eatFood();
                    outOfSupplies(config);
                    antiPatternDropVials();
                    drinkforgottonbrew();

                    if (!Rs2Prayer.isPrayerActive(neededprayer)) {
                        Microbot.log("Turning on Prayer.");
                        while (!Rs2Prayer.isPrayerActive(neededprayer)) {
                            if (!super.isRunning()) {
                                break;
                            }
                            drinkPrayerPot();
                            Rs2Prayer.toggle(neededprayer);
                            sleep(0, 750);
                            if (Rs2Prayer.isPrayerActive(neededprayer)) {
                                //we made it in
                                Microbot.log("Praying");
                                break;
                            }
                        }
                    }

                    if (!Rs2Player.isInCombat()) {
                        if (Microbot.getClient().getHintArrowNpc() == null) {
                            // if we're not in combat and the brother isn't there.
                            Microbot.log("Breaking out hint arrow is null.");
                            break;
                        } else {
                            // if we're not in combat and the brother is there.
                            Microbot.log("Attacking the brother");
                            Rs2Npc.interact(currentBrother, "Attack");
                            sleepUntil(() -> Rs2Player.isInCombat(), Rs2Random.between(3000, 6000));
                        }
                    }

                    if (currentBrother.isDead()) {
                        Microbot.log("Breaking out the brother is dead.");
                        sleepUntil(() -> Microbot.getClient().getHintArrowNpc() == null, Rs2Random.between(3000, 6000));
                        break;
                    }

                }
            }
        }
    }

    private void walkToChest() {
        Rs2Walker.walkTo(BarrowsConstants.BARROWS_CHEST_AREA);
    }

    private void startWalkingToTheChest() {
        if (WalkToTheChestFuture == null || WalkToTheChestFuture.isCancelled() || WalkToTheChestFuture.isDone()) {
            if (state == BarrowsState.NAVIGATING_TUNNELS) {
                WalkToTheChestFuture = scheduledExecutorService.scheduleWithFixedDelay(
                        this::walkToChest,
                        0,
                        500,
                        TimeUnit.MILLISECONDS
                );
            }
        }
    }

    public void drinkforgottonbrew() {
        if (Rs2Inventory.contains(it -> it != null && it.getName().contains("Forgotten brew"))) {
            if (Rs2Player.getBoostedSkillLevel(Skill.MAGIC) <= (Rs2Player.getRealSkillLevel(Skill.MAGIC) + Rs2Random.between(1, 4))) {
                Microbot.log("Drinking a Forgotten brew.");
                if (Rs2Inventory.contains("Forgotten brew(1)")) {
                    Rs2Inventory.interact("Forgotten brew(1)", "Drink");
                    sleep(300, 1000);
                    return;
                }
                if (Rs2Inventory.contains("Forgotten brew(2)")) {
                    Rs2Inventory.interact("Forgotten brew(2)", "Drink");
                    sleep(300, 1000);
                    return;
                }
                if (Rs2Inventory.contains("Forgotten brew(3)")) {
                    Rs2Inventory.interact("Forgotten brew(3)", "Drink");
                    sleep(300, 1000);
                    return;
                }
                if (Rs2Inventory.contains("Forgotten brew(4)")) {
                    Rs2Inventory.interact("Forgotten brew(4)", "Drink");
                    sleep(300, 1000);
                }
            }
        }
    }

    public void eatFood() {
        if (Rs2Player.getHealthPercentage() <= 60) {
            if (Rs2Inventory.contains(it -> it != null && it.isFood())) {
                Rs2ItemModel food = Rs2Inventory.get(it -> it != null && it.isFood());
                if (Rs2Inventory.interact(food, "Eat")) {
                    sleep(0, 750);
                }
            }
        }
    }

    public void solvePuzzle() {

        //correct model ids are  6725, 6731, 6713, 6719
        //widget ids are 1638413, 1638415,1638417
        if (Rs2Widget.getWidget(1638413) != null) {
            stopFutureWalker();
            if (Rs2Widget.getWidget(1638413).getModelId() == 6725 || Rs2Widget.getWidget(1638413).getModelId() == 6731
                    || Rs2Widget.getWidget(1638413).getModelId() == 6713 || Rs2Widget.getWidget(1638413).getModelId() == 6719) {
                Microbot.log("Solution found");
                if (Rs2Widget.getWidget(1638413) != null) {
                    Rs2Widget.clickWidget(1638413);
                    sleep(500, 1500);
                }
            }
        }

        if (Rs2Widget.getWidget(1638415) != null) {
            stopFutureWalker();
            if (Rs2Widget.getWidget(1638415).getModelId() == 6725 || Rs2Widget.getWidget(1638415).getModelId() == 6731
                    || Rs2Widget.getWidget(1638415).getModelId() == 6713 || Rs2Widget.getWidget(1638415).getModelId() == 6719) {
                Microbot.log("Solution found");
                if (Rs2Widget.getWidget(1638415) != null) {
                    Rs2Widget.clickWidget(1638415);
                    sleep(500, 1500);
                }
            }
        }

        if (Rs2Widget.getWidget(1638417) != null) {
            stopFutureWalker();
            if (Rs2Widget.getWidget(1638417).getModelId() == 6725 || Rs2Widget.getWidget(1638417).getModelId() == 6731
                    || Rs2Widget.getWidget(1638417).getModelId() == 6713 || Rs2Widget.getWidget(1638417).getModelId() == 6719) {
                Microbot.log("Solution found");
                if (Rs2Widget.getWidget(1638417) != null) {
                    Rs2Widget.clickWidget(1638417);
                    sleep(500, 1500);
                }
            }
        }

    }


    @Override
    public void shutdown() {
        Microbot.log("Shutting down...");
        ChestsOpened = 0;
        barrowsPieces = new ArrayList<>();
        stopFutureWalker();
        super.shutdown();
    }
}
