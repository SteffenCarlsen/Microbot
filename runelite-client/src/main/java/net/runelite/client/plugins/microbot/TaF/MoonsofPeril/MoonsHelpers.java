package net.runelite.client.plugins.microbot.TaF.MoonsofPeril;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.TaF.MoonsofPeril.enums.BossToKill;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MoonsHelpers {
    public static List<BossToKill> getBossesKilled() {
        List<BossToKill> bossesKilled = new ArrayList<>();
        if (Microbot.getVarbitValue(VarbitID.PMOON_BOSS_BLOOD_DEAD) == 1) {
            bossesKilled.add(BossToKill.BLOOD);
        }
        if (Microbot.getVarbitValue(VarbitID.PMOON_BOSS_BLUE_DEAD) == 1) {
            bossesKilled.add(BossToKill.MOON);
        }
        if (Microbot.getVarbitValue(VarbitID.PMOON_BOSS_ECLIPSE_DEAD) == 1) {
            bossesKilled.add(BossToKill.ECLIPSE);
        }
        return bossesKilled;
    }

    public static @Nullable Rs2NpcModel getClosestJaguar(WorldPoint safeTile) {
        // If there's no safe tile, return null or handle it differently
        if (safeTile == null) {
            // Option 1: Use player's position instead
            WorldPoint playerLocation = Rs2Player.getWorldLocation();
            return Rs2Npc.getNpcs("Blood jaguar")
                    .min(Comparator.comparingInt(jaguar ->
                            jaguar.getWorldLocation().distanceTo(playerLocation)))
                    .orElse(null);
        }

        // Original code - only executes when safeTile is not null
        return Rs2Npc.getNpcs("Blood jaguar")
                .min(Comparator.comparingInt(jaguar ->
                        jaguar.getWorldLocation().distanceTo(safeTile)))
                .orElse(null);
    }

    public static List<WorldPoint> generateSafeCircles(int centerX, int centerY, int plane) {
        List<WorldPoint> safeCircles = new ArrayList<>();

        // Top row (y = centerY + 3)
        safeCircles.add(new WorldPoint(centerX - 2, centerY + 3, plane));
        safeCircles.add(new WorldPoint(centerX - 1, centerY + 3, plane));
        safeCircles.add(new WorldPoint(centerX + 1, centerY + 3, plane));
        safeCircles.add(new WorldPoint(centerX + 2, centerY + 3, plane));

        // Right column (x = centerX + 3)
        safeCircles.add(new WorldPoint(centerX + 3, centerY + 2, plane));
        safeCircles.add(new WorldPoint(centerX + 3, centerY + 1, plane));
        safeCircles.add(new WorldPoint(centerX + 3, centerY - 1, plane));
        safeCircles.add(new WorldPoint(centerX + 3, centerY - 2, plane));

        // Bottom row (y = centerY - 3)
        safeCircles.add(new WorldPoint(centerX + 2, centerY - 3, plane));
        safeCircles.add(new WorldPoint(centerX + 1, centerY - 3, plane));
        safeCircles.add(new WorldPoint(centerX - 1, centerY - 3, plane));
        safeCircles.add(new WorldPoint(centerX - 2, centerY - 3, plane));

        // Left column (x = centerX - 3)
        safeCircles.add(new WorldPoint(centerX - 3, centerY - 2, plane));
        safeCircles.add(new WorldPoint(centerX - 3, centerY - 1, plane));
        safeCircles.add(new WorldPoint(centerX - 3, centerY + 1, plane));
        safeCircles.add(new WorldPoint(centerX - 3, centerY + 2, plane));

        return safeCircles;
    }

    public static boolean needsRestock(MoonsConfig config) {
        // Check if we have enough food
        boolean hasEnoughFood = Rs2Inventory.itemQuantity(MoonsConstants.COOKED_FOOD_ID) >= config.foodAmount();

        // Check if we have enough potions
        int potionCount = 0;
        for (int potionId : MoonsConstants.MOONLIGHT_POTIONS) {
            potionCount += Rs2Inventory.itemQuantity(potionId);
        }
        boolean hasEnoughPotions = potionCount >= config.moonPotions();

        return !hasEnoughFood || !hasEnoughPotions;
    }
}
