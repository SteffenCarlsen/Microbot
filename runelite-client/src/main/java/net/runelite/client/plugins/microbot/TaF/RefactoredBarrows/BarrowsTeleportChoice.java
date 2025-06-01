package net.runelite.client.plugins.microbot.TaF.RefactoredBarrows;

import net.runelite.api.ItemID;

public enum BarrowsTeleportChoice {
    Tablet(ItemID.BARROWS_TELEPORT, "Barrows teleport"),
    POH_TABLET(ItemID.TELEPORT_TO_HOUSE, "Teleport to house"),
    POH_TELEPORT(0, "Teleport to house"),
    POH_GROUP_IRONMAN(0, "Teleport to house");

    private final int id;
    private final String name;

    BarrowsTeleportChoice(int id, String name) {
        this.id = id;
        this.name = name;
    }


    public int getToBarrowsTPMethodItemID() {
        return id;
    }

    public String getToBarrowsTPMethodItemName() {
        return name;
    }

}