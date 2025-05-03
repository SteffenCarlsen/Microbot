package net.runelite.client.plugins.microbot.TaF.IronmanDailies;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.inventorysetups.InventorySetup;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;

@ConfigInformation(
        "Daily ironscape")
@ConfigGroup("TzHaarVenator")
public interface IronmanDailiesConfig extends Config {
    @ConfigSection(
            name = "Dailyscape",
            description = "Settings for dailyscape - Mostly for irons",
            position = 0
    )
    String dailyscape = "Dailyscape";
    @ConfigItem(
            keyName = "inventorySetup",
            name = "Inventory setup",
            description = "The inventory setup to use for the dailyscape",
            section = dailyscape,
            position = 0
    )
    default InventorySetup inventorySetup() {
        return null;
    }
    @ConfigItem(
            keyName = "battlestaffs",
            name = "Zaff's battlestaffs",
            description = "Buy battlestaffs from Zaff",
            section = dailyscape,
            position = 1
    )
    default boolean battlestaffs() {
        return false;
    }

    @ConfigItem(
            keyName = "sand",
            name = "Claim sand",
            description = "Claim sand from Bert",
            section = dailyscape,
            position = 2
    )
    default boolean sand() {
        return false;
    }
    @ConfigItem(
            keyName = "flax",
            name = "Claim flax",
            description = "Claim flax from the flax keeper",
            section = dailyscape,
            position = 2
    )
    default boolean flax() {
        return false;
    }
    @ConfigItem(
            keyName = "essence",
            name = "Collect essence",
            description = "Collect free pure essence from Wizard Cromperty",
            section = dailyscape,
            position = 3
    )
    default boolean essence() {
        return false;
    }

    @ConfigItem(
            keyName = "slimeBonemeal",
            name = "Slime & bonemeal",
            description = "Convert 13, 26 or 39 bones directly into bonemeal and buckets of slime by speaking to Robin in The Green Ghost inn in Port Phasmatys",
            section = dailyscape,
            position = 4
    )
    default boolean slimeBonemeal() {
        return false;
    }


    @ConfigItem(
            keyName = "miscFavor",
            name = "Miscellania favor",
            description = "Grind out miscellania favor by woodcutting or mining",
            section = dailyscape,
            position = 5
    )
    default boolean miscFavor() {
        return false;
    }

    @ConfigItem(
            keyName = "miscFillup",
            name = "Fill up Miscellania with gold",
            description = "Fill up Miscellania with gold",
            section = dailyscape,
            position = 6
    )
    default boolean miscFillup() {
        return false;
    }

    @ConfigItem(
            keyName = "ogreArrows",
            name = "Claim ogre arrows",
            description = "Receive 25, 50, 100 or 150 free ogre arrows from Rantz.",
            section = dailyscape,
            position = 7
    )
    default boolean ogreArrows() {
        return false;
    }

    @ConfigItem(
            keyName = "herbBoxes",
            name = "Collect herb boxes",
            description = "Collect daily herb boxes from NMZ",
            section = dailyscape,
            position = 8
    )
    default boolean herbBoxes() {
        return false;
    }
}
