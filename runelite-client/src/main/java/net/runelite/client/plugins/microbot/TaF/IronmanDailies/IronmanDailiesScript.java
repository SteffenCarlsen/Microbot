package net.runelite.client.plugins.microbot.TaF.IronmanDailies;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.TaF.TzhaarVenatorBow.TzhaarVenatorBowScript.TravelStatus.TO_BANK;
import static net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity.VERY_LOW;

public class IronmanDailiesScript extends Script {
    public static final String VERSION = "1.0";
    public static IronmanDailiesState BOT_STATUS = IronmanDailiesState.BANKING;

    // Completion status tracking
    private boolean bankingComplete = false;
    private boolean battlestaffsComplete = false;
    private boolean sandComplete = false;
    private boolean flaxComplete = false;
    private boolean essenceComplete = false;
    private boolean slimeAndBonemealComplete = false;
    private boolean miscellaniaFavorComplete = false;
    private boolean miscellaniaStockUpComplete = false;
    private boolean ogreArrowsComplete = false;
    private boolean herbBoxesComplete = false;

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
        Rs2Antiban.setActivityIntensity(VERY_LOW);
    }

    public boolean run(IronmanDailiesConfig config) {
        BOT_STATUS = IronmanDailiesState.BANKING;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) return;
                switch (BOT_STATUS) {
                    case BANKING:
                        handleBanking(config);
                        break;
                    case BATTLESTAFFS:
                        handleBattlestaffs(config);
                        break;
                    case SAND:
                        handleSand(config);
                        break;
                    case FLAX:
                        handleFlax(config);
                        break;
                    case ESSENCE:
                        handleEssence(config);
                        break;
                }
            } catch (Exception ex) {
                System.out.println("Exception message: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleEssence(IronmanDailiesConfig config) {
        WorldPoint ardyEssTp = new WorldPoint(2682,3325,0);
        Rs2Walker.walkTo(ardyEssTp);
        sleepUntil(() -> Rs2Walker.isNear(ardyEssTp));
        Rs2Npc.interact("Wizard Cromperty", "Talk-to");
        Rs2Dialogue.clickContinue();
        Rs2Dialogue.clickContinue();

    }

    private void handleFlax(IronmanDailiesConfig config) {
        if (Rs2Inventory.hasNotedItem("Flax")) {
            WorldPoint flaxField = new WorldPoint(2741,3443,0);
            Rs2Walker.walkTo(flaxField);
            sleepUntil(() -> Rs2Walker.isNear(flaxField));
            Rs2Npc.interact("Flax keeper", "Exchange");
            Rs2Dialogue.clickContinue();
            Rs2Dialogue.clickOption("Agree");
        }
        flaxComplete = true;
        setNextTask(config);
    }

    private void handleSand(IronmanDailiesConfig config) {
        WorldPoint bertLocation = new WorldPoint(2551,3099,0);
        Rs2Walker.walkTo(bertLocation);
        sleepUntil(() -> Rs2Walker.isNear(bertLocation));
        Rs2Npc.interact("Bert", "Sand");
        Rs2Dialogue.clickContinue();
        sandComplete = true;
        setNextTask(config);
    }

    private void handleBattlestaffs(IronmanDailiesConfig config) {
        WorldPoint zaffLocation = new WorldPoint(3202,3432,0);
        Rs2Walker.walkTo(zaffLocation);
        sleepUntil(() -> Rs2Walker.isNear(zaffLocation));
        Rs2GameObject.interact("Barrel", "Claim-staves");
        Rs2Dialogue.clickContinue();
        sleepUntil(() -> Rs2Dialogue.hasDialogueOption("Yes."), 1000);
        Rs2Dialogue.clickOption("Yes.");
        Rs2Inventory.waitForInventoryChanges(1200);
        battlestaffsComplete = true;
        setNextTask(config);
    }

    private void handleBanking(IronmanDailiesConfig config) {
        Rs2InventorySetup inventorySetup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);
        if (inventorySetup.doesInventoryMatch() && inventorySetup.doesEquipmentMatch()) {
            setNextTask(config);
            return;
        }
        Rs2Bank.walkToBank();
        Rs2Bank.openBank();
        sleepUntil(Rs2Bank::isOpen, 2000);
        inventorySetup.loadEquipment();
        inventorySetup.loadInventory();
        bankingComplete = true;
        setNextTask(config);
    }

    private void setNextTask(IronmanDailiesConfig config) {

    }

    public enum IronmanDailiesState {BANKING, BATTLESTAFFS, SAND,FLAX,ESSENCE,SLIME_AND_BONEMEAL, MISCELLANIA_FAVOR, MISCELLANIA_STOCK_UP,OGRE_ARROWS,HERB_BOXES}

}
