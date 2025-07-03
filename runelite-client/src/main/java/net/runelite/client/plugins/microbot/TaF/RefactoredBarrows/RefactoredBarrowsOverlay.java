package net.runelite.client.plugins.microbot.TaF.RefactoredBarrows;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class RefactoredBarrowsOverlay extends OverlayPanel {

    private final RefactoredBarrowsPlugin plugin;
    private final RefactoredBarrowsScript script;

    @Inject
    RefactoredBarrowsOverlay(RefactoredBarrowsPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.script = plugin.refactoredBarrowsScript;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.getChildren().clear();
            panelComponent.setPreferredSize(new Dimension(200, 300));

            // Title section
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Barrows Refactor by TaF - V2.0")
                    .color(Color.GREEN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Running: ")
                    .right(plugin.getTimeRunning())
                    .leftColor(Color.WHITE)
                    .rightColor(Color.WHITE)
                    .build());
            // Current state information
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("State:")
                    .right(script.state.toString())
                    .leftColor(Color.WHITE)
                    .rightColor(Color.CYAN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Microbot state:")
                    .right(Microbot.status)
                    .leftColor(Color.WHITE)
                    .rightColor(Color.CYAN)
                    .build());

            // Progress tracking
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Chests looted:")
                    .right(Integer.toString(RefactoredBarrowsScript.ChestsOpened))
                    .leftColor(Color.WHITE)
                    .rightColor(Color.ORANGE)
                    .build());

            // Tunnel information
            var tunnelBrother = plugin.refactoredBarrowsScript.tunnelBrother != null ? plugin.refactoredBarrowsScript.tunnelBrother.getName() : "Unknown";
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Tunnel:")
                    .right(tunnelBrother)
                    .leftColor(Color.WHITE)
                    .rightColor(Color.YELLOW)
                    .build());

            var currentBrother = plugin.refactoredBarrowsScript.currentBrother != null ? plugin.refactoredBarrowsScript.currentBrother.getName() : "None";
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Current brother:")
                    .right(currentBrother)
                    .leftColor(Color.WHITE)
                    .rightColor(Color.YELLOW)
                    .build());

            // Rewards section
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Pieces found:")
                    .leftColor(Color.WHITE)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(RefactoredBarrowsScript.barrowsPieces.toString())
                    .leftColor(Color.GREEN)
                    .build());

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}