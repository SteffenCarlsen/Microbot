package net.runelite.client.plugins.microbot.TaF.RefactoredBarrows;

import net.runelite.api.ItemID;
import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.inventorysetups.InventorySetup;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;

@ConfigGroup("RefactoredBarrows")
@ConfigInformation("1. Start with your ring of dueling equipped.<br /><br /> " +
        "2. Your auto-cast spell selected or powered staff equipped. <br /><br /> " +
        "Required items: prayer potions or moonlight moth mixes(2), barrows teleports tablets, " +
        "or teleport to house tablets/runes, food, runes for your chosen spell, and a spade.<br /><br /> ")
public interface RefactoredBarrowsConfig extends Config {

    // ===== CONFIG SECTIONS =====

    @ConfigSection(
            name = "General Settings",
            description = "General settings",
            position = 0
    )
    String generalSection = "General Settings";

    @ConfigSection(
            name = "Combat",
            description = "Combat settings",
            position = 1
    )
    String combatSection = "Combat";

    @ConfigSection(
            name = "Equipment Settings",
            description = "Settings for equipment",
            position = 2
    )
    String equipmentSettings = "EquipmentSettings";

    @ConfigSection(
            name = "Banking & Supply Settings",
            description = "Settings for resupplying and banking",
            position = 3
    )
    String supplySettings = "SupplySettings";

    @ConfigSection(
            name = "Developer Settings",
            description = "Settings for development",
            position = 4
    )
    String developerSettings = "DeveloperSettings";

    // ===== GENERAL SETTINGS =====

    @ConfigItem(
            keyName = "shouldGainRP",
            name = "Aim for 86+% rewards potential",
            description = "Should we gain additional RP other than the barrows brothers?",
            section = generalSection,
            position = 0
    )
    default boolean shouldGainRP() {
        return false;
    }

    @ConfigItem(
            keyName = "selectedToBarrowsTPMethod",
            name = "Barrows TP Method",
            description = "Between using a barrows teleport tablet, or your POH portal.",
            section = generalSection,
            position = 1
    )
    default BarrowsTeleportChoice selectedToBarrowsTPMethod() {
        return BarrowsTeleportChoice.Tablet;
    }

    @ConfigItem(
            keyName = "groupIronmanTeammateName",
            name = "Teammate name",
            description = "Teleport to group ironman teammate's house. The name is case sensitive.",
            section = generalSection,
            position = 2
    )
    default String groupIronmanTeammateName() {
        return "";
    }

    @ConfigItem(
            keyName = "teleportToFeroxOnEachKill",
            name = "Force ferox teleport on each kill",
            description = "If enabled, the bot will teleport to Ferox Enclave after each kill.",
            section = generalSection,
            position = 3
    )
    default boolean teleportToFeroxOnEachKill() {
        return false;
    }

    // ===== COMBAT SETTINGS =====

    @ConfigItem(
            keyName = "minPrayerPercent",
            name = "Minimum Prayer Percent",
            description = "Percentage of prayer points below which the bot will drink a prayer potion",
            section = combatSection,
            position = 0
    )
    default int minPrayerPercent() {
        return 35;
    }

    @ConfigItem(
            keyName = "healthThreshold",
            name = "Health Threshold to Exit",
            description = "Minimum health percentage to stay and fight",
            section = combatSection,
            position = 1
    )
    default int healthThreshold() {
        return 50;
    }

    // ===== EQUIPMENT SETTINGS =====

    @ConfigItem(
            keyName = "useInventorySetups",
            name = "Use Inventory Setup",
            description = "If enabled, the bot will ignore other banking options and use the inventory setup",
            section = equipmentSettings,
            position = 0
    )
    default boolean useInventorySetups() {
        return false;
    }

    @ConfigItem(
            keyName = "inventorySetup",
            name = "Inventory Setup",
            description = "Select your saved inventory setup to use",
            section = equipmentSettings,
            position = 1
    )
    default InventorySetup inventorySetup() {
        return null;
    }

    // ===== SUPPLY SETTINGS =====

    @ConfigItem(
            keyName = "prayerRestoreType",
            name = "Prayer Restore Type",
            description = "Type of prayer restore potion to use",
            section = supplySettings,
            position = 0
    )
    default prayerRestoreType prayerRestoreType() {
        return prayerRestoreType.Prayer_Potion;
    }

    @ConfigItem(
            keyName = "prayerRestoreCount",
            name = "Prayer Restore Count",
            description = "Max amount of prayer potions or mixes to withdraw from the bank",
            section = supplySettings,
            position = 1
    )
    @Range(min = 1, max = 20)
    default int prayerRestoreCount() {
        return 8;
    }

    @ConfigItem(
            keyName = "forgottenBrewCount",
            name = "Forgotten Brews Count",
            description = "Amount of forgotten brews to withdraw (0 to disable)",
            section = supplySettings,
            position = 2
    )
    @Range(min = 0, max = 6)
    default int forgottenBrewCount() {
        return 4;
    }

    @ConfigItem(
            keyName = "food",
            name = "Food",
            description = "What food to use?",
            section = combatSection,
            position = 3
    )
    default Rs2Food food() {
        return Rs2Food.KARAMBWAN;
    }

    @ConfigItem(
            keyName = "foodCount",
            name = "Food Count",
            description = "Food count",
            section = supplySettings,
            position = 4
    )
    @Range(min = 1, max = 20)
    default int foodCount() {
        return 4;
    }

    // ===== DEVELOPER SETTINGS =====

    @ConfigItem(
            keyName = "overrideState",
            name = "Override State",
            description = "Enable to override script starting state",
            section = developerSettings,
            position = 0
    )
    default boolean overrideState() {
        return false;
    }

    @ConfigItem(
            keyName = "startState",
            name = "Starting State",
            description = "The starting state of the bot (only used if override state is enabled)",
            section = developerSettings,
            position = 1
    )
    default BarrowsState startState() {
        return BarrowsState.BANKING;
    }

    // ===== ENUMS =====

    enum prayerRestoreType {
        Prayer_Potion(ItemID.PRAYER_POTION4, "Prayer potion(4)"),
        MoonlightMothMix(ItemID.MOONLIGHT_MOTH_MIX_2, "Moonlight moth mix (2)");

        private final int id;
        private final String name;

        prayerRestoreType(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getPrayerRestoreTypeID() {
            return id;
        }

        public String getPrayerRestoreTypeName() {
            return name;
        }
    }
}