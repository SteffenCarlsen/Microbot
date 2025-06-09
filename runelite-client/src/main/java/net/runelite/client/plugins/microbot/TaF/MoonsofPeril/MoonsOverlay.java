package net.runelite.client.plugins.microbot.TaF.MoonsofPeril;

import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.TaF.MoonsofPeril.enums.BossToKill;
import net.runelite.client.plugins.microbot.TaF.MoonsofPeril.enums.MoonsState;
import net.runelite.client.plugins.microbot.TaF.salamanders.SalamanderScript;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
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
            if (plugin.moonsScript.moonsState.equals(MoonsState.FIGHTING_BLOOD_MOON)) {
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
            }
            // Boss kill status
            var killedNpcs = MoonsHelpers.getBossesKilled();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Blood Moon:")
                    .right(killedNpcs.contains(BossToKill.BLOOD) ? "✓" : "✗")
                    .rightColor(killedNpcs.contains(BossToKill.BLOOD) ? Color.GREEN : Color.RED)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Eclipse:")
                    .right(killedNpcs.contains(BossToKill.ECLIPSE) ? "✓" : "✗")
                    .rightColor(killedNpcs.contains(BossToKill.ECLIPSE) ? Color.GREEN : Color.RED)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Blue Moon:")
                    .right(killedNpcs.contains(BossToKill.MOON) ? "✓" : "✗")
                    .rightColor(killedNpcs.contains(BossToKill.MOON) ? Color.GREEN : Color.RED)
                    .build());

            // Loot
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Lunar chests:")
                    .right(String.valueOf(plugin.moonsScript.chestsLooted))
                    .leftColor(Color.WHITE)
                    .rightColor(Color.GREEN)
                    .build());
            // Runtime
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Runtime:")
                    .right(plugin.getTimeRunning())
                    .build());

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }

    private Color getStateColor(MoonsState moonsState) {
        switch (moonsState) {
            case FIGHTING_BLOOD_MOON:
            case GOING_TO_BLOOD_MOON:
                return Color.RED;
            case GOING_TO_ECLIPSE:
            case FIGHTING_ECLIPSE:
                return Color.YELLOW;
            case GOING_TO_BLUE_MOON:
            case FIGHTING_BLUE_MOON:
                return Color.cyan;
            case GOING_TO_COOKER:
            case GOING_TO_LOOT:
                return Color.GREEN;
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
}