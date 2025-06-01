package net.runelite.client.plugins.microbot.TaF.MoonsofPeril;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.TaF.MoonsofPeril.enums.MoonsState;

@ConfigInformation(
        "<html>" +
                "<h2 style='color: #6d9eeb;'>Moons of Peril by TaF</h2>" +
                "<h3 style='color: #93c47d;'>Requirements:</h3>" +
                "<p style='margin-left: 10px; margin-top: 2px;'><span style='display: inline-block; width: 15px;'>•</span><b>Food:</b> Configured amount of food to farm (default: 20)</p>" +
                "<p style='margin-left: 10px; margin-top: 2px;'><span style='display: inline-block; width: 15px;'>•</span><b>Moon Potions:</b> Configured amount of potions to make (default: 4)</p>" +
                "<p style='margin-left: 10px; margin-top: 2px;'><span style='display: inline-block; width: 15px;'>•</span><b>Combat Gear:</b> Different weapon types for the different bosses</p>" +
                "<h3 style='color: #93c47d;'>Weapon Settings:</h3>" +
                "<p style='margin-left: 10px; margin-top: 2px;'><span style='display: inline-block; width: 15px;'>•</span><b>Slash/Crush/Stab Weapons:</b> Enter specific weapon names for each attack style</p>" +
                "<p style='margin-left: 10px; margin-top: 2px;'><span style='display: inline-block; width: 15px;'>•</span><b>Empty Fields:</b> If left empty, will use your currently equipped weapon</p>" +
                "<h3 style='color: #93c47d;'>Instructions:</h3>" +
                "<p style='margin-left: 10px; margin-top: 2px;'><span style='display: inline-block; width: 15px;'>•</span><b>Start Location:</b> Bot must be started inside the chamber of moons of peril</p>" +
                "<p style='margin-left: 10px; margin-top: 2px;'><span style='display: inline-block; width: 15px;'>•</span><b>Consumables:</b> Configure eating and prayer thresholds as needed</p>" +
                "<p style='color: #cc0000;'><i>Note: Ensure you have all required items before starting the bot.</i></p>" +
                "</html>")
@ConfigGroup("Moons")
public interface MoonsConfig extends Config {

    @ConfigSection(
            name = "General Settings",
            description = "General settings for the script",
            position = 0
    )
    String generalSettings = "generalSettings";

    @ConfigSection(
            name = "Consumable Settings",
            description = "Consumable settings for the script",
            position = 1
    )
    String consumableSettings = "consumableSettings";

    @ConfigSection(
            name = "Farming Settings",
            description = "Which bosses to farm",
            position = 2
    )
    String farmingSettings = "farmingSettings";

    // General settings for the script
    @ConfigItem(
            keyName = "foodAmount",
            name = "Food Amount",
            description = "Number of fish needed for a boss fight",
            position = 0,
            section = generalSettings
    )
    @Range(
            min = 1,
            max = 22
    )
    default int foodAmount() {
        return 20;
    }

    @ConfigItem(
            keyName = "moonPotions",
            name = "Moon potions Amount",
            description = "Number of moon potions needed for a boss fight",
            position = 1,
            section = generalSettings
    )
    @Range(
            min = 1,
            max = 8
    )
    default int moonPotions() {
        return 4;
    }

    @ConfigItem(
            keyName = "slashWeapon",
            name = "Slash weapon",
            description = "Slash weapon",
            position = 2,
            section = generalSettings
    )
    default String slashWeapon() {
        return "";
    }

    @ConfigItem(
            keyName = "crushWeapon",
            name = "Crush weapon",
            description = "Crush weapon",
            position = 3,
            section = generalSettings
    )
    default String crushWeapon() {
        return "";
    }

    @ConfigItem(
            keyName = "stabWeapon",
            name = "Stab weapon",
            description = "Stab weapon",
            position = 4,
            section = generalSettings
    )
    default String stabWeapon() {
        return "";
    }

    @ConfigItem(
            keyName = "State",
            name = "State",
            description = "Choose state.",
            position = 5,
            section = generalSettings
    )
    default MoonsState getState() {
        return MoonsState.DEFAULT;
    }

    // Farming settings for the script
    @ConfigItem(
            keyName = "killBloodMoon",
            name = "Kill Blood Moon",
            description = "Enable to kill the Blood Moon boss.",
            position = 0,
            section = farmingSettings
    )
    default boolean killBloodMoon() {
        return true;
    }

    @ConfigItem(
            keyName = "killBlueMoon",
            name = "Kill Blue Moon",
            description = "Enable to kill the Blue Moon boss.",
            position = 1,
            section = farmingSettings
    )
    default boolean killBlueMoon() {
        return true;
    }

    @ConfigItem(
            keyName = "killEclipse",
            name = "Kill Eclipse",
            description = "Enable to kill the Eclipse boss.",
            position = 2,
            section = farmingSettings
    )
    default boolean killEclipse() {
        return true;
    }

    // Consumable settings for the script
    @ConfigItem(
            keyName = "eatAt",
            name = "Eat at %?",
            description = "Eat food when health is below this percentage.",
            position = 1,
            section = consumableSettings
    )
    default int eatAt() {
        return 40;
    }

    @ConfigItem(
            keyName = "prayerAt",
            name = "Prayer at %?",
            description = "Drink prayer potion when prayer is below this percentage.",
            position = 2,
            section = consumableSettings
    )
    default int prayerAt() {
        return 40;
    }
}
