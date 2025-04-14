package net.runelite.client.plugins.microbot.TaF.RoyalTitans;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GraphicsObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.containers.FixedSizeQueue;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tile.DangerousTileIds;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "Royal Titans",
        description = "Kills the Royal Titans boss with another bot",
        tags = {"Combat", "bossing", "TaF", "Royal Titans", "Ice giant", "Fire giant", "Duo"},
        enabledByDefault = false
)
@Slf4j
public class RoyalTitansPlugin extends Plugin {
    private final Integer GRAPHICS_OBJECT_FIRE =  3218;
    private final Integer GRAPHICS_OBJECT_ICE =  3221;
    private final Integer ENRAGE_ELEMENTAL_BLAST_SAFESPOT =  56003;
    @Inject
    private RoyalTitansConfig config;

    @Provides
    RoyalTitansConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RoyalTitansConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private RoyalTitansOverlay royalTitansOverlay;

    @Inject
    public RoyalTitansScript royalTitansScript;

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(royalTitansOverlay);
        }
        royalTitansScript.run(config);
    }

    @Override
    protected void shutDown() {
        royalTitansScript.shutdown();
        overlayManager.remove(royalTitansOverlay);
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event)
    {
        final GraphicsObject graphicsObject = event.getGraphicsObject();
        if (graphicsObject.getId() == GRAPHICS_OBJECT_ICE || graphicsObject.getId() == GRAPHICS_OBJECT_FIRE) {
            Rs2Tile.addDangerousGraphicsObjectTile(graphicsObject, 600 * 5);
        }
    }

    @Subscribe
    public void onGroundObjectDespawned(GroundObjectDespawned event) {
        final GroundObject groundObject = event.getGroundObject();
        final Tile tile = event.getTile();
        if (groundObject.getId() == ENRAGE_ELEMENTAL_BLAST_SAFESPOT) {
            royalTitansScript.enrageTile = null;
        }
    }

    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event) {
        final GroundObject groundObject = event.getGroundObject();
        final Tile tile = event.getTile();
        if (groundObject.getId() == ENRAGE_ELEMENTAL_BLAST_SAFESPOT) {
            royalTitansScript.enrageTile = tile;
        }
    }



    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned gameObjectSpawned)
    {
        if (!Microbot.getClient().isInInstancedRegion())
        {
            return;
        }

        //royalTitansScript.gameObjectSpawned(gameObjectSpawned.getGameObject());
    }
}
