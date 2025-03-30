package net.runelite.client.plugins.microbot.TaF.AutoGauntlet;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class AutoGauntletOverlay extends OverlayPanel {
    private final AutoGauntletPlugin plugin;

    @Inject
    AutoGauntletOverlay(AutoGauntletPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }


    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 300));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("AutoGauntlet - v" + AutoGauntletScript.VERSION)
                    .color(Color.GREEN)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder().build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Running: " + plugin.getTimeRunning())
                    .leftColor(Color.WHITE)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status: " + AutoGauntletScript.BOT_STATUS)
                    .leftColor(Color.WHITE)
                    .build());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Total loot: " + AutoGauntletScript.TotalLootValue)
                    .leftColor(Color.WHITE)
                    .build());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
