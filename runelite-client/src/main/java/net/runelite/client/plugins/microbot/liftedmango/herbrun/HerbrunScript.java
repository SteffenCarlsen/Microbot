package net.runelite.client.plugins.microbot.liftedmango.herbrun;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.liftedmango.herbrun.models.HerbPatch;
import net.runelite.client.plugins.microbot.liftedmango.herbrun.models.TransportType;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.ArrayList;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.liftedmango.herbrun.HerbrunInfo.states;


public class HerbrunScript extends Script {

    // Define the herb patch locations
    private static final WorldPoint trollheimHerb = new WorldPoint(2826, 3693, 0);
    private static final WorldPoint catherbyHerb = new WorldPoint(2812, 3465, 0);
    private static final WorldPoint morytaniaHerb = new WorldPoint(3604, 3529, 0);
    private static final WorldPoint varlamoreHerb = new WorldPoint(1582, 3093, 0);
    private static final WorldPoint hosidiusHerb = new WorldPoint(1739, 3552, 0);
    private static final WorldPoint ardougneHerb = new WorldPoint(2669, 3374, 0);
    private static final WorldPoint cabbageHerb = new WorldPoint(3058, 3310, 0);
    private static final WorldPoint farmingGuildHerb = new WorldPoint(1239, 3728, 0);
    private static final WorldPoint weissHerb = new WorldPoint(2847, 3935, 0);
    private static final WorldPoint harmonyHerb = new WorldPoint(3789, 2840, 0);

    //herb patch Object ID
    private static final int trollheimHerbPatchID = 18816;
    private static final int catherbyHerbPatchID = 8151;
    private static final int morytaniaHerbPatchID = 8153;
    private static final int varlamoreHerbPatchID = 50697;
    private static final int hosidiusHerbPatchID = 27115;
    private static final int ardougneHerbPatchID = 8152; //leprechaun 0
    private static final int cabbageHerbPatchID = 8150; //50698?
    private static final int farmingGuildHerbPatchID = 33979;
    private static final int weissHerbPatchID = 33176;
    private static final int harmonyHerbPatchID = 9372;

    //Leprechaun IDs:
    //IDS that are 0: Ardougne, Farming guild, morytania, hosidius, catherby, falador, weiss, harmony
    private static final int varlamoreLeprechaunID = NpcID.TOOL_LEPRECHAUN_12765;
    private static final int trollHeimLeprechaunID = NpcID.TOOL_LEPRECHAUN_757;
    private static final Stack<HerbPatch> herbPatches = new Stack<>();

    public HerbrunScript() {
    }

    public boolean run(HerbrunConfig config) {

        int seedToPlant = config.SEED().getItemId();

        Microbot.enableAutoRunOn = false;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                initializeHerbRun(config);
                gearPlayer(config);
                while (!herbPatches.empty()) {
                    var herbPatch = herbPatches.pop();
                    Microbot.log("Handling herb patch: " + herbPatch);
                    var transportStatus = transportPlayer(herbPatch);
                    var harvestStatus = harvestHerbs(herbPatch);
                    var plantedHerbs = planHerbs(herbPatch, seedToPlant, config);

                    if (transportStatus && harvestStatus && plantedHerbs) {
                        Microbot.log("Herb patch " + herbPatch.HerbPatchName + " handled successfully");
                    } else {
                        Microbot.log("Herb patch " + herbPatch.HerbPatchName + " failed");
                    }
                }
            } catch (Exception ex) {
                 Microbot.log(ex.getMessage());
            }
        }, 0, 90, TimeUnit.MINUTES);
        return true;
    }

    private boolean planHerbs(HerbPatch herbPatch, int seedToPlant, HerbrunConfig config) {
        // Check that the player is idle before interacting with the patch
        if (!Rs2Player.isMoving() && !Rs2Player.isAnimating() &&
                !Rs2Player.isInteracting()) {

            // Apply compost based on configuration
            Microbot.log("Applying compost...");
            int compostItemId = config.COMPOST() ? ItemID.BOTTOMLESS_COMPOST_BUCKET_22997 : ItemID.ULTRACOMPOST;
            Rs2Inventory.use(compostItemId);
            Rs2GameObject.interact(herbPatch.herbPatchID, "use");
            // Wait for farming XP drop to confirm compost application
            Rs2Player.waitForXpDrop(Skill.FARMING);
            sleep(50, 1200);

            // Plant seeds in the patch
            Microbot.log("Planting seeds...");
            Rs2Inventory.use(seedToPlant);
            Rs2GameObject.interact(herbPatch.herbPatchID, "use");

            // Wait until interaction is complete
            sleepUntil(Rs2Player::isInteracting);
            if (Rs2Inventory.contains(ItemID.EMPTY_BUCKET)) Rs2Inventory.drop(ItemID.EMPTY_BUCKET);
        }
        return true;
    }

    private boolean harvestHerbs(HerbPatch herbPatchSettings) {
        // Define possible actions the herb patch could have
        if (!Rs2Player.isMoving() &&
                !Rs2Player.isAnimating() &&
                !Rs2Player.isInteracting()) {

            String[] possibleActions = {"pick", "rake", "Clear", "Inspect"};

            GameObject herbPatch = null;
            String foundAction = null;

            // Loop through the possible actions and try to find the herb patch with any valid action
            for (String action : possibleActions) {
                herbPatch = Rs2GameObject.findObjectByImposter(herbPatchSettings.herbPatchID, action);  // Find object by patchId and action
                if (herbPatch != null) {
                    foundAction = action;
                    break;  // Exit the loop once we find the patch with a valid action
                }
            }

            // If no herb patch is, print an error and return
            if (herbPatch == null) {
                 Microbot.log("Herb patch not found with any of the possible actions!");
                return false;
            }

            // Handle the patch based on the action found
            switch (foundAction) {
                case "pick":
                    handlePickAction(herbPatch, herbPatchSettings.herbPatchID);
                    break;
                case "rake":
                    handleRakeAction(herbPatch);
                    break;
                case "Clear":
                    handleClearAction(herbPatch);
                    break;
                default:
                     Microbot.log("Unexpected action found on herb patch: " + foundAction);
                    break;
            }

        }
        return true;
    }

    private boolean transportPlayer(HerbPatch herbPatch) {
        boolean didWeTeleport = false;
        if (herbPatch.transportType == TransportType.Teletablet)
        {
            didWeTeleport = Rs2Inventory.interact(herbPatch.itemID, "Break");
        }
        else if (herbPatch.transportType == TransportType.Item)
        {
            didWeTeleport = Rs2Inventory.interact(herbPatch.itemID, getActionString(herbPatch.itemID, herbPatch.herbPatchID));
        }
        var didWeLand = sleepUntil(() -> !Rs2Player.isAnimating() && !Rs2Player.isMoving(), 30000);
        var walkingToPatch = handleWalkingToPatch(herbPatch);
        return didWeTeleport && didWeLand && walkingToPatch;
    }

    private String getActionString(int itemID, int herbPatchID) {
        switch (herbPatchID) {
            case trollheimHerbPatchID:
                if (itemID == ItemID.TROLLHEIM_TELEPORT) {
                    return "Break";
                } else {
                    return "Troll Stronghold";
                }
            case catherbyHerbPatchID:
                return "Break";
            case morytaniaHerbPatchID:
                if (itemID == ItemID.ECTOPHIAL) {
                    return "empty";
                } else {
                    return "Break";
                }
            case varlamoreHerbPatchID:
                if (itemID == ItemID.CIVITAS_ILLA_FORTIS_TELEPORT) {
                    return "Break";
                } else {
                    return "Signal";
                }
            case hosidiusHerbPatchID:
                return "rub";
            case ardougneHerbPatchID:
                return "Break";
            case cabbageHerbPatchID:
                return "Break";
            case farmingGuildHerbPatchID:
                return "Break";
            case weissHerbPatchID:
                return "Break";
            case harmonyHerbPatchID:
                return "Break";
        }
        return "Break";
    }

    private void initializeHerbRun(HerbrunConfig config) {
        if (config.enableTrollheim()) {
            herbPatches.push(new HerbPatch("Trollheim",trollheimHerbPatchID, trollheimHerb, trollHeimLeprechaunID, TransportType.Item, ItemID.TROLLHEIM_TELEPORT));
        }
        if (config.enableCatherby()) {
            herbPatches.push(new HerbPatch("Catherby",catherbyHerbPatchID, catherbyHerb, 0, TransportType.Item, ItemID.CAMELOT_TELEPORT));
        }
        if (config.enableMorytania()) {
            herbPatches.push(new HerbPatch("Morytania",morytaniaHerbPatchID, morytaniaHerb, 0, TransportType.Item, ItemID.ECTOPHIAL));
        }
        if (config.enableVarlamore()) {
            herbPatches.push(new HerbPatch("Varlamore",varlamoreHerbPatchID, varlamoreHerb, varlamoreLeprechaunID, TransportType.Item, getVarlamoreTeleportItem()));
        }
        if (config.enableHosidius()) {
            herbPatches.push(new HerbPatch("Hosidius",hosidiusHerbPatchID, hosidiusHerb, 0, TransportType.Item, ItemID.XERICS_TALISMAN));
        }
        if (config.enableArdougne()) {
            herbPatches.push(new HerbPatch("Ardougne",ardougneHerbPatchID, ardougneHerb, 0, TransportType.Item, ItemID.ARDOUGNE_TELEPORT));
        }
        if (config.enableGuild()) {
            herbPatches.push(new HerbPatch("Farming guild",farmingGuildHerbPatchID, farmingGuildHerb, 0, TransportType.Item, ItemID.SKILLS_NECKLACE6));
        }
        if (config.enableFalador()) {
            herbPatches.push(new HerbPatch("Falador",cabbageHerbPatchID, cabbageHerb, 0, TransportType.Teletablet, ItemID.FALADOR_TELEPORT));
        }
        if (config.enableWeiss()) {
            herbPatches.push(new HerbPatch("Weikss",weissHerbPatchID, weissHerb, 0, TransportType.Item, ItemID.ICY_BASALT));
        }
        if (config.enableHarmony()) {
            herbPatches.push(new HerbPatch("Harmony island",harmonyHerbPatchID, harmonyHerb, 0 , TransportType.Teletablet, ItemID.HARMONY_ISLAND_TELEPORT));
        }
        Microbot.log("Herb patches initialized");
    }

    private int getVarlamoreTeleportItem() {
        if (Rs2Bank.hasItem(ItemID.PERFECTED_QUETZAL_WHISTLE)) {
            return ItemID.PERFECTED_QUETZAL_WHISTLE;
        } else if (Rs2Bank.hasItem(ItemID.ENHANCED_QUETZAL_WHISTLE)) {
             return ItemID.ENHANCED_QUETZAL_WHISTLE;
        } else if (Rs2Bank.hasItem(ItemID.BASIC_QUETZAL_WHISTLE)) {
            return ItemID.BASIC_QUETZAL_WHISTLE;
        } else {
           return ItemID.CIVITAS_ILLA_FORTIS_TELEPORT;
        }
    }

    private void gearPlayer(HerbrunConfig config) {
        if (config.enableGearing()) {
            if (!Rs2Bank.isOpen()) {
                Rs2Bank.useBank();
                Rs2Bank.depositAll();
                if (config.GRACEFUL()) {
                    Rs2Bank.depositEquipment();
                    sleep(Rs2Random.between(200,300));
                    equipGraceful(config);
                }
            }
            withdrawHerbSetup(config);
            Rs2Bank.closeBank();
            sleep(Rs2Random.between(200,300), Rs2Random.between(800,900));
        }
        Microbot.log("Finished gearing");
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    private void checkBeforeWithdrawAndEquip(String itemName) {
        if (!Rs2Equipment.isWearing(itemName)) {
            Rs2Bank.withdrawAndEquip(itemName);
        }
    }

    private void equipGraceful(HerbrunConfig config) {
        checkBeforeWithdrawAndEquip("GRACEFUL HOOD");
        if (!config.FARMING_CAPE()) {
            checkBeforeWithdrawAndEquip("GRACEFUL CAPE");
        }
        checkBeforeWithdrawAndEquip("GRACEFUL BOOTS");
        checkBeforeWithdrawAndEquip("GRACEFUL GLOVES");
        checkBeforeWithdrawAndEquip("GRACEFUL TOP");
        checkBeforeWithdrawAndEquip("GRACEFUL LEGS");
    }

    private void withdrawHerbSetup(HerbrunConfig config) {
        Rs2Bank.withdrawX(config.SEED().getItemId(), 10);
        if (config.COMPOST()) {
            Rs2Bank.withdrawOne(ItemID.BOTTOMLESS_COMPOST_BUCKET_22997);
        } else {
            Rs2Bank.withdrawX(ItemID.ULTRACOMPOST, 8);
        }
        Rs2Bank.withdrawOne(ItemID.RAKE);
        Rs2Bank.withdrawOne(ItemID.SEED_DIBBER);
        Rs2Bank.withdrawOne(ItemID.SPADE);
        if (config.enableMorytania()) {
            if (config.USE_ECTOPHIAL()) {
                Rs2Bank.withdrawOne(ItemID.ECTOPHIAL);
            } else {
                Rs2Bank.withdrawOne(ItemID.FENKENSTRAINS_CASTLE_TELEPORT);
            }
        }
        if (config.enableVarlamore()) {
            if (config.USE_QUETZAL_WHISTLE()) {
                if (Rs2Bank.hasItem(ItemID.PERFECTED_QUETZAL_WHISTLE)) {
                    Rs2Bank.withdrawOne(ItemID.PERFECTED_QUETZAL_WHISTLE);
                } else if (Rs2Bank.hasItem(ItemID.ENHANCED_QUETZAL_WHISTLE)) {
                    Rs2Bank.withdrawOne(ItemID.ENHANCED_QUETZAL_WHISTLE);
                } else if (Rs2Bank.hasItem(ItemID.BASIC_QUETZAL_WHISTLE)) {
                    Rs2Bank.withdrawOne(ItemID.BASIC_QUETZAL_WHISTLE);
                }
            } else {
                Rs2Bank.withdrawOne(ItemID.CIVITAS_ILLA_FORTIS_TELEPORT);
            }
        }
        if (config.enableHosidius()) {
            Rs2Bank.withdrawOne(ItemID.XERICS_TALISMAN);
        }
        if (config.enableArdougne()) {
            if (config.ARDOUGNE_TELEPORT_OPTION()) {
                Rs2Bank.withdrawOne(config.CLOAK().getItemId());
            } else {
                Rs2Bank.withdrawOne(ItemID.ARDOUGNE_TELEPORT);
            }
        }
        if (config.enableGuild()) {
            if (!config.FARMING_CAPE()) {
                if (Rs2Bank.hasItem(ItemID.SKILLS_NECKLACE1)) {
                    Rs2Bank.withdrawOne(ItemID.SKILLS_NECKLACE1);
                } else if (Rs2Bank.hasItem(ItemID.SKILLS_NECKLACE2)) {
                    Rs2Bank.withdrawOne(ItemID.SKILLS_NECKLACE2);
                } else if (Rs2Bank.hasItem(ItemID.SKILLS_NECKLACE3)) {
                    Rs2Bank.withdrawOne(ItemID.SKILLS_NECKLACE3);
                } else if (Rs2Bank.hasItem(ItemID.SKILLS_NECKLACE4)) {
                    Rs2Bank.withdrawOne(ItemID.SKILLS_NECKLACE4);
                } else if (Rs2Bank.hasItem(ItemID.SKILLS_NECKLACE5)) {
                    Rs2Bank.withdrawOne(ItemID.SKILLS_NECKLACE5);
                } else {
                    Rs2Bank.withdrawOne(ItemID.SKILLS_NECKLACE6);
                }
            } else {
                if (Rs2Bank.hasItem(ItemID.FARMING_CAPE)) {
                    Rs2Bank.withdrawOne(ItemID.FARMING_CAPE);
                } else if (Rs2Bank.hasItem(ItemID.FARMING_CAPET)) {
                    Rs2Bank.withdrawOne(ItemID.FARMING_CAPET);
                }
            }
        }
        if (config.enableFalador()) {
            if (config.FALADOR_TELEPORT_OPTION()) {
                Rs2Bank.withdrawOne(config.RING().getItemId());
            } else {
                Rs2Bank.withdrawOne(ItemID.FALADOR_TELEPORT);
            }
        }
        if (config.enableWeiss()) {
            Rs2Bank.withdrawOne(ItemID.ICY_BASALT);
        }
        if (config.enableCatherby()) {
            Rs2Bank.withdrawOne(ItemID.CAMELOT_TELEPORT);
        }
        if (config.enableTrollheim()) {
            if (config.TROLLHEIMTELEPORT() == HerbrunInfo.trollheimTeleport.TROLLHEIM_TAB) {
                Rs2Bank.withdrawOne(ItemID.TROLLHEIM_TELEPORT);
            } else {
                Rs2Bank.withdrawOne(ItemID.STONY_BASALT);
            }
        }
        if (config.enableHarmony()) {
            Rs2Bank.withdrawOne(ItemID.HARMONY_ISLAND_TELEPORT);
        }
        checkBeforeWithdrawAndEquip("Magic secateurs");
    }


    private boolean handleWalkingToPatch(HerbPatch patch) {
        Microbot.log("Walking to " + patch.HerbPatchName + " herb patch");

        // Start walking to the location
        Rs2Walker.walkTo(patch.herbPatchLocation);
        // Wait until the player reaches within 2 tiles of the location and has stopped moving
        sleepUntil(() -> Rs2Player.distanceTo(patch.herbPatchLocation) < 5);
        if (Rs2Player.distanceTo(patch.herbPatchLocation) < 5) {
            Microbot.log("Arrived at " + patch.HerbPatchName + " herb patch");
            return true;
        }
        return false;
    }

    private void handlePickAction(GameObject herbPatch, int patchId) {

        Rs2NpcModel leprechaun = Rs2Npc.getNpc("Tool leprechaun");
        if (Rs2Inventory.isFull()) {
            var herbs = new ArrayList<>(Rs2Inventory.all().stream()
                    .filter(item -> item.getName().toLowerCase().startsWith("grimy"))
                    .collect(Collectors.toCollection(() -> new TreeSet<>((item1, item2) -> item1.getName().compareToIgnoreCase(item2.getName())))));
            for (var herb : herbs) {
                Rs2Inventory.useItemOnNpc(herb.id, leprechaun);
                sleep(Rs2Random.between(1000, 2000));
            }
             Microbot.log("Noting herbs with tool leprechaun...");
            Rs2Player.waitForAnimation();
        }
        int timesToLoop = 2 + (int) (Math.random() * 6);

        Rs2GameObject.interact(herbPatch, "pick");
        Rs2Player.waitForXpDrop(Skill.FARMING);
        for (int i = 0; i < timesToLoop; i++) {
            Rs2GameObject.interact(herbPatch, "pick");
            sleep(25, 100);
        }
        Rs2Player.waitForAnimation();

        // Wait for the picking to complete (player stops animating and patch no longer has the "Pick" action)
        sleepUntil(() -> !Rs2GameObject.hasAction(Rs2GameObject.findObjectComposition(patchId), "Pick") ||
                (!Rs2Player.isAnimating() && !Rs2Player.isInteracting() && !Rs2Player.isMoving()));

        // After picking herbs, check if "rake" is an available action
        if (Rs2GameObject.hasAction(Rs2GameObject.findObjectComposition(patchId), "rake")) {
             Microbot.log("Weeds grew, switching to rake action...");
            handleRakeAction(herbPatch);  // Handle raking if weeds grew
            return;  // Exit the method after raking
        }

        // If the inventory becomes full again while picking, note the herbs and pick the remaining ones
        if (Rs2GameObject.hasAction(Rs2GameObject.findObjectComposition(patchId), "Pick") && Rs2Inventory.isFull()) {
             Microbot.log("Noting herbs with tool leprechaun...");
            var herbs = new ArrayList<>(Rs2Inventory.all().stream()
                    .filter(item -> item.getName().toLowerCase().startsWith("grimy"))
                    .collect(Collectors.toCollection(() -> new TreeSet<>((item1, item2) -> item1.getName().compareToIgnoreCase(item2.getName())))));
            for (var herb : herbs) {
                Rs2Inventory.useItemOnNpc(herb.id, leprechaun);
                sleep(Rs2Random.between(1000, 2000));
            }
            Rs2Player.waitForAnimation();

            // Pick any remaining herbs
            Rs2GameObject.interact(herbPatch, "pick");

            if (true) {
                Rs2Player.waitForXpDrop(Skill.FARMING);
                timesToLoop = 2 + (int) (Math.random() * 6);
                for (int i = 0; i < timesToLoop; i++) {
                    Rs2GameObject.interact(herbPatch, "pick");
                    sleep(25, 100);
                }
            }

            Rs2Player.waitForAnimation();
            sleepUntil(() -> !Rs2Player.isAnimating() && !Rs2Player.isInteracting());
        }

        // Final check to ensure no lingering animations or interactions
        sleepUntil(() -> !Rs2Player.isAnimating() && !Rs2Player.isMoving() && !Rs2Player.isInteracting(), 200);
    }


    private void handleRakeAction(GameObject herbPatch) {
         Microbot.log("Raking the patch...");

        // Rake the patch
        Rs2GameObject.interact(herbPatch, "rake");

        Rs2Player.waitForAnimation();
        sleepUntil(() -> !Rs2Player.isAnimating() && !Rs2Player.isInteracting());

        // Drop the weeds (assuming weeds are added to the inventory)
        if (!Rs2Player.isMoving() &&
                !Rs2Player.isAnimating() &&
                !Rs2Player.isInteracting()) {
             Microbot.log("Dropping weeds...");
            Rs2Inventory.dropAll(ItemID.WEEDS);
            Rs2Player.waitForAnimation();
            sleepUntil(() -> !Rs2Player.isAnimating() && !Rs2Player.isInteracting());
        }
    }

    private void handleClearAction(GameObject herbPatch) {
        Microbot.log("Clearing the herb patch...");

        // Try to interact with the patch using the "clear" action
        boolean interactionSuccess = Rs2GameObject.interact(herbPatch, "clear");
        Rs2Player.waitForAnimation();
        sleepUntil(() -> !Rs2Player.isAnimating());

        if (!interactionSuccess) {
             Microbot.log("Failed to interact with the herb patch to clear it.");
            return;
        }

        // Wait for the clearing animation to finish
        Rs2Player.waitForAnimation();
        sleepUntil(() -> !Rs2Player.isAnimating() && Rs2Player.isInteracting() && Rs2Player.isMoving());
    }
}