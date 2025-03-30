package net.runelite.client.plugins.microbot.TaF.AutoGauntlet;

import com.google.inject.Provides;
import net.runelite.api.NpcID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@PluginDescriptor(
        name = PluginDescriptor.TaFCat + "AutoGauntlet",
        description = "Automatically kills Tzhaars with the Venator Bow",
        tags = {"Tzhaar", "Venator", "bow", "range", "xp", "microbot"},
        enabledByDefault = false
)
public class AutoGauntletPlugin extends Plugin {

    private Instant scriptStartTime;
    private ScheduledExecutorService scheduledExecutorService;
    @Inject
    private AutoGauntletConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private AutoGauntletOverlay tzhaarVenatorBowOverlay;
    @Inject
    private AutoGauntletScript gauntletScript;

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    @Override
    protected void startUp() throws AWTException {
        scriptStartTime = Instant.now();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        if (overlayManager != null) {
            overlayManager.add(tzhaarVenatorBowOverlay);
        }
        gauntletScript.run(config);
    }

    @Override
    protected void shutDown() {
        gauntletScript.shutdown();
        overlayManager.remove(tzhaarVenatorBowOverlay);
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }
    }

    @Provides
    AutoGauntletConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoGauntletConfig.class);
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned npcSpawned)
    {
        if (!Microbot.getClient().isInInstancedRegion() || !GauntletUtilities.isInGauntlet())
        {
            return;
        }

        WorldPoint player = Microbot.getClient().getLocalPlayer().getWorldLocation();

        switch (npcSpawned.getNpc().getId())
        {
            case NpcID.CORRUPTED_BEAR:
            case NpcID.CRYSTALLINE_BEAR:
            case NpcID.CORRUPTED_DARK_BEAST:
            case NpcID.CRYSTALLINE_DARK_BEAST:
            case NpcID.CORRUPTED_DRAGON:
            case NpcID.CRYSTALLINE_DRAGON:
                gauntletScript.updateDemiBossLocations(player, npcSpawned.getNpc());
                break;
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned npcDespawned)
    {
        if (!Microbot.getClient().isInInstancedRegion() || !GauntletUtilities.isInGauntlet())
        {
            return;
        }

        WorldPoint player = Microbot.getClient().getLocalPlayer().getWorldLocation();

        switch (npcDespawned.getNpc().getId())
        {
            case NpcID.CORRUPTED_BEAR:
            case NpcID.CRYSTALLINE_BEAR:
            case NpcID.CORRUPTED_DARK_BEAST:
            case NpcID.CRYSTALLINE_DARK_BEAST:
            case NpcID.CORRUPTED_DRAGON:
            case NpcID.CRYSTALLINE_DRAGON:
                gauntletScript.updateDemiBossLocations(player, npcDespawned.getNpc());
                break;
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {

        if (!Microbot.getClient().isInInstancedRegion() || !GauntletUtilities.isInGauntlet())
        {
            return;
        }

        if (gauntletScript.isNewSession())
        {
            return;
        }

        gauntletScript.updateCurrentRoom(Microbot.getClient().getLocalPlayer().getWorldLocation());
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned gameObjectSpawned)
    {
        if (!Microbot.getClient().isInInstancedRegion() || !GauntletUtilities.isInGauntlet())
        {
            return;
        }

        gauntletScript.gameObjectSpawned(gameObjectSpawned.getGameObject());
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned gameObjectDespawned)
    {
        if (!Microbot.getClient().isInInstancedRegion() || !GauntletUtilities.isInGauntlet())
        {
            return;
        }

        gauntletScript.gameObjectDespawned(gameObjectDespawned.getGameObject());
    }
}
