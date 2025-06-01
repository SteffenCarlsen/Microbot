package net.runelite.client.plugins.microbot.TaF.MoonsofPeril;

import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.TaF.MoonsofPeril.enums.MoonsState;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public class MoonsOverlay extends OverlayPanel {
    private final MoonsPlugin plugin;
    private final MoonsConfig config;
    private final Instant startTime;

    @Inject
    MoonsOverlay(MoonsPlugin plugin, MoonsConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
        startTime = Instant.now();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 300));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("TaF Moons of Peril V1.0")
                    .color(Color.GREEN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            // Script State
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("State:")
                    .right(MoonsScript.moonsState.toString())
                    .rightColor(getStateColor(MoonsScript.moonsState))
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Microbot state:")
                    .right(Microbot.status)
                    .rightColor(getStateColor(MoonsScript.moonsState))
                    .build());
            // Player Status
            if (Microbot.isLoggedIn()) {
                int currentHp = Microbot.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
                int maxHp = Microbot.getClient().getRealSkillLevel(Skill.HITPOINTS);
                int currentPrayer = Microbot.getClient().getBoostedSkillLevel(Skill.PRAYER);
                int maxPrayer = Microbot.getClient().getRealSkillLevel(Skill.PRAYER);

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("HP:")
                        .right(currentHp + "/" + maxHp)
                        .rightColor(getHealthColor(currentHp, maxHp))
                        .build());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Prayer:")
                        .right(currentPrayer + "/" + maxPrayer)
                        .rightColor(getPrayerColor(currentPrayer, maxPrayer))
                        .build());
            }

            // Blood Jaguar Status
            NPC bloodJaguar = Rs2Npc.getNpc(MoonsConstants.BLOOD_JAGUAR_NPC_ID);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Blood Jaguar:")
                    .right(bloodJaguar != null ? "Present" : "Not present")
                    .rightColor(bloodJaguar != null ? Color.RED : Color.GREEN)
                    .build());

            if (bloodJaguar != null) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Bloodspawn tick:")
                        .right(String.valueOf(MoonsPlugin.lastBloodSpecialSpawnTick))
                        .rightColor(MoonsPlugin.lastBloodSpecialSpawnTick == MoonsPlugin.globalTickCount ? Color.GREEN : Color.RED)
                        .build());
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Current tick:")
                        .right(String.valueOf(MoonsPlugin.globalTickCount))
                        .rightColor(Color.GREEN)
                        .build());
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Safe to Move:")
                        .right(String.valueOf(MoonsScript.moveToBloodTile))
                        .rightColor(MoonsScript.moveToBloodTile ? Color.GREEN : Color.RED)
                        .build());
            }

            // Inventory Info
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Food:")
                    .right(String.valueOf(Rs2Inventory.itemQuantity(MoonsConstants.COOKED_FOOD_ID)))
                    .build());

            // Calculate potion total
            int moonlightOne = Rs2Inventory.itemQuantity(29080);
            int moonlightTwo = Rs2Inventory.itemQuantity(29081);
            int moonlightThree = Rs2Inventory.itemQuantity(29082);
            int moonlightFour = Rs2Inventory.itemQuantity(29083);
            int potionsTotal = moonlightOne + moonlightTwo + moonlightThree + moonlightFour;

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Potions:")
                    .right(String.valueOf(potionsTotal))
                    .build());

            // Settings
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Eat at:")
                    .right(config.eatAt() + "%")
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Pray at:")
                    .right(config.prayerAt() + "%")
                    .build());

            // Runtime
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Runtime:")
                    .right(plugin.getTimeRunning())
                    .build());

            // Dangerous Tile Count
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Danger Tiles:")
                    .right(String.valueOf(plugin.dangerousGraphicsObjectTiles.size()))
                    .rightColor(plugin.dangerousGraphicsObjectTiles.isEmpty() ? Color.GREEN : Color.RED)
                    .build());

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }

    private Color getStateColor(MoonsState moonsState) {
        switch (moonsState) {
            case FIGHTING_BLOOD_MOON:
                return Color.RED;
            case GOING_TO_BLOOD_MOON:
            case GOING_TO_COOKER:
            case GOING_TO_LOOT:
                return Color.YELLOW;
            case GETTING_SUPPLIES:
                return Color.CYAN;
            default:
                return Color.WHITE;
        }
    }

    private Color getHealthColor(int current, int max) {
        double ratio = (double) current / max;
        if (ratio <= 0.25) return Color.RED;
        if (ratio <= 0.5) return Color.ORANGE;
        if (ratio <= 0.75) return Color.YELLOW;
        return Color.GREEN;
    }

    private Color getPrayerColor(int current, int max) {
        double ratio = (double) current / max;
        if (ratio <= 0.25) return Color.RED;
        if (ratio <= 0.5) return Color.ORANGE;
        return Color.CYAN;
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}