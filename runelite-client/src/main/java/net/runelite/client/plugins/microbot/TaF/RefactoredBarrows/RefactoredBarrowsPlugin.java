package net.runelite.client.plugins.microbot.TaF.RefactoredBarrows;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
        name = PluginDescriptor.TaFCat + "Refactored Barrows",
        description = "Runs barrows for you",
        tags = {"Barrows", "mm", "Money making"},
        enabledByDefault = false
)
@Slf4j
public class RefactoredBarrowsPlugin extends Plugin {
    @Inject
    RefactoredBarrowsScript refactoredBarrowsScript;
    int ticks = 10;
    @Inject
    private RefactoredBarrowsConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private RefactoredBarrowsOverlay refactoredBarrowsOverlay;

    private Instant scriptStartTime;

    @Provides
    RefactoredBarrowsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RefactoredBarrowsConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        scriptStartTime = Instant.now();
        if (overlayManager != null) {
            overlayManager.add(refactoredBarrowsOverlay);
        }
        refactoredBarrowsScript.run(config);
        RefactoredBarrowsScript.outOfPoweredStaffCharges = false;

    }

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    protected void shutDown() {
        refactoredBarrowsScript.shutdown();
        overlayManager.remove(refactoredBarrowsOverlay);
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        String msg = chatMessage.getMessage();
        //need to add the chat message we get when we try to attack an NPC with an empty staff.

        if (msg.contains("out of charges")) {
            RefactoredBarrowsScript.outOfPoweredStaffCharges = true;
        }

        if (msg.contains("no charges")) {
            RefactoredBarrowsScript.outOfPoweredStaffCharges = true;
        }

    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (ticks > 0) {
            ticks--;
        } else {
            ticks = 10;
        }

    }

}
