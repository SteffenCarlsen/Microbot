package net.runelite.client.plugins.microbot.TaF.RefactoredBarrows;

import net.runelite.client.plugins.microbot.util.coords.Rs2WorldArea;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;

public enum BarrowsBrothers {
    DHAROK("Dharok the Wretched", new Rs2WorldArea(3573, 3296, 3, 3, 0), Rs2PrayerEnum.PROTECT_MELEE),
    GUTHAN("Guthan the Infested", new Rs2WorldArea(3575, 3280, 3, 3, 0), Rs2PrayerEnum.PROTECT_MELEE),
    KARIL("Karil the Tainted", new Rs2WorldArea(3564, 3274, 3, 3, 0), Rs2PrayerEnum.PROTECT_RANGE),
    AHRIM("Ahrim the Blighted", new Rs2WorldArea(3563, 3288, 3, 3, 0), Rs2PrayerEnum.PROTECT_MAGIC),
    TORAG("Torag the Corrupted", new Rs2WorldArea(3552, 3282, 2, 2, 0), Rs2PrayerEnum.PROTECT_MELEE),
    VERAC("Verac the Defiled", new Rs2WorldArea(3556, 3297, 3, 3, 0), Rs2PrayerEnum.PROTECT_MELEE);

    private final String name;

    private final Rs2WorldArea humpWP;

    private final Rs2PrayerEnum whatToPray;


    BarrowsBrothers(String name, Rs2WorldArea humpWP, Rs2PrayerEnum whatToPray) {
        this.name = name;
        this.humpWP = humpWP;
        this.whatToPray = whatToPray;
    }

    public String getName() {
        return name;
    }

    public Rs2WorldArea getHumpWP() {
        return humpWP;
    }

    public Rs2PrayerEnum getWhatToPray() {
        return whatToPray;
    }

}