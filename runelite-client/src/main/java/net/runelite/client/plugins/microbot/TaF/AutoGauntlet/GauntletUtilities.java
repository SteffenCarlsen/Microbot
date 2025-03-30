package net.runelite.client.plugins.microbot.TaF.AutoGauntlet;

import net.runelite.client.plugins.microbot.Microbot;

public class GauntletUtilities {

    private static final int CRYSTAL_GAUNTLET_REGION_ID = 7512;
    private static final int CORRUPTED_GAUNTLET_REGION_ID = 7768;
    public static boolean isInNormal() {
        if (Microbot.getClient().getLocalPlayer() == null)
            return false;

        return Microbot.getClient().getMapRegions()[0] == CRYSTAL_GAUNTLET_REGION_ID;
    }

    public static boolean isInCorrupted() {
        if (Microbot.getClient().getLocalPlayer() == null)
            return false;

        return Microbot.getClient().getMapRegions()[0] == CORRUPTED_GAUNTLET_REGION_ID;
    }

    public static boolean isInGauntlet() {
        return isInNormal() || isInCorrupted();
    }
}
