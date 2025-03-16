package net.runelite.client.plugins.microbot.liftedmango.herbrun.models;

import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldPoint;

public class HerbPatch {
    public String HerbPatchName;
    public int herbPatchID;
    public WorldPoint herbPatchLocation;
    public int LeprechaunID;
    public TransportType transportType;
    public int itemID;

    public HerbPatch(String HerbPatchName, int harmonyHerbPatchID, WorldPoint harmonyHerb, int leprechaunID, TransportType transportType, int itemID) {
        this.HerbPatchName = HerbPatchName;
        this.herbPatchID = harmonyHerbPatchID;
        this.herbPatchLocation = harmonyHerb;
        this.LeprechaunID = leprechaunID;
        this.transportType = transportType;
        this.itemID = itemID;
    }
}
