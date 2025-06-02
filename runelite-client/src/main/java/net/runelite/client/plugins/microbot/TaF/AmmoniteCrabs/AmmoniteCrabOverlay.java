package net.runelite.client.plugins.microbot.TaF.AmmoniteCrabs;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.TaF.AmmoniteCrabs.enums.AmmoniteCrabState;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;

import static net.runelite.client.plugins.microbot.TaF.AmmoniteCrabs.enums.AmmoniteCrabState.FIGHT;

public class AmmoniteCrabOverlay extends OverlayPanel {
    private final AmmoniteCrabPlugin plugin;
    private static final Color TITLE_COLOR = Color.decode("#a4ffff");
    private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 150);
    private static final Color NORMAL_COLOR = Color.WHITE;
    private static final Color WARNING_COLOR = Color.YELLOW;
    private static final Color DANGER_COLOR = Color.RED;
    private static final Color SUCCESS_COLOR = Color.GREEN;
    private static final int MAX_AFK_TIMER = 10;

    @Inject
    AmmoniteCrabOverlay(AmmoniteCrabPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(220, 300));
            panelComponent.setBackgroundColor(BACKGROUND_COLOR);

            // Title section
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("TaF's Ammonite Crabs")
                    .color(TITLE_COLOR)
                    .build());
            // Script running time
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Runtime:")
                    .right(plugin.getTimeRunning())
                    .rightColor(NORMAL_COLOR)
                    .build());
            // State information
            Color stateColor = getStateColor(plugin.ammoniteCrabScript.ammoniteCrabState);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("State:")
                    .right(plugin.ammoniteCrabScript.ammoniteCrabState.name())
                    .rightColor(stateColor)
                    .build());
            var xpGained = plugin.getXpGained();
            var xpPerHour = plugin.getXpPerHour();

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("XP Gained:")
                    .right(formatNumber(xpGained))
                    .rightColor(NORMAL_COLOR)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("XP/Hour:")
                    .right(formatNumber(xpPerHour))
                    .rightColor(xpPerHour > 0 ? SUCCESS_COLOR : NORMAL_COLOR)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder().build()); // Spacer

            // AFK Timer with progress bar
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("AFK Timer:")
                    .right(plugin.ammoniteCrabScript.afkTimer + "/" + MAX_AFK_TIMER)
                    .rightColor(getAfkTimerColor(plugin.ammoniteCrabScript.afkTimer))
                    .build());

            ProgressBarComponent afkProgressBar = new ProgressBarComponent();
            afkProgressBar.setBackgroundColor(new Color(61, 56, 49));
            afkProgressBar.setForegroundColor(getAfkTimerColor(plugin.ammoniteCrabScript.afkTimer));
            afkProgressBar.setMaximum(MAX_AFK_TIMER);
            afkProgressBar.setValue(plugin.ammoniteCrabScript.afkTimer);
            panelComponent.getChildren().add(afkProgressBar);

            panelComponent.getChildren().add(LineComponent.builder().build()); // Spacer

            // Other stats
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Hijack Timer:")
                    .right(Integer.toString(plugin.ammoniteCrabScript.hijackTimer))
                    .rightColor(getHijackTimerColor(plugin.ammoniteCrabScript.hijackTimer))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Times Hopped:")
                    .right(Integer.toString(plugin.ammoniteCrabScript.timesHopped))
                    .rightColor(getHopCountColor(plugin.ammoniteCrabScript.timesHopped))
                    .build());

            // Footer with version
            panelComponent.getChildren().add(LineComponent.builder().build()); // Spacer
            panelComponent.getChildren().add(LineComponent.builder()
                    .right("v" + AmmoniteCrabScript.version)
                    .rightColor(new Color(160, 160, 160))
                    .build());

        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }

    private Color getStateColor(AmmoniteCrabState state) {
        if (state == null) return NORMAL_COLOR;

        switch (state) {
            case FIGHT:
                return SUCCESS_COLOR;
            case AFK:
                return WARNING_COLOR;
            case RESET_AGGRO:
            case HOP_WORLD:
                return DANGER_COLOR;
            default:
                return NORMAL_COLOR;
        }
    }

    private String formatNumber(long number) {
        return String.format("%,d", number);
    }
    private Color getAfkTimerColor(int timer) {
        if (timer >= MAX_AFK_TIMER) return DANGER_COLOR;
        if (timer >= MAX_AFK_TIMER * 0.7) return WARNING_COLOR;
        return SUCCESS_COLOR;
    }

    private Color getHijackTimerColor(int timer) {
        if (timer > 5) return WARNING_COLOR;
        return NORMAL_COLOR;
    }

    private Color getHopCountColor(int count) {
        if (count >= 7) return DANGER_COLOR;
        if (count >= 3) return WARNING_COLOR;
        return NORMAL_COLOR;
    }
}