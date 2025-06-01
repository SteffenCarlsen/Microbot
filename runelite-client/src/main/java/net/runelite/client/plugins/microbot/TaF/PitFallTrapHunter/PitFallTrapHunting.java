package net.runelite.client.plugins.microbot.TaF.PitFallTrapHunter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum PitFallTrapHunting {

    SPINED_LARUPIA("Spined larupia", 51675,51673, new WorldPoint(2532, 2908, 0), List.of(526), List.of(), false),
    HORNED_GRAAHK("Horned graahk", 51675,51673, new WorldPoint(2760, 3003, 0), List.of(526, 10103), List.of(), false),
    SABRE_TOOTHED_KYATT("Sabre-toothed kyatt", 51675,51673, new WorldPoint(2349, 3650, 0), List.of(526, 10117), List.of(), false),
    SUNLIGHT_ANTELOPE("Sunlight antelope", 51675,51673, new WorldPoint(1747, 3010, 0), List.of(ItemID.BIG_BONES, 29178), List.of(ItemID.SUNFIRESPLINTER), true),
    MOONLIGHT_ANTELOPE("Moonlight antelope", 51675,51673, new WorldPoint(2569, 2862, 0), List.of(526, 29177), List.of(), false);

    private final String name;
    private final int trapId;
    private final int spikedTrapId;
    private final WorldPoint huntingPoint;
    private final List<Integer> itemsToDrop;
    private final List<Integer> lootId;
    private final boolean canProcessItems;

    @Override
    public String toString() {
        return name;
    }
}