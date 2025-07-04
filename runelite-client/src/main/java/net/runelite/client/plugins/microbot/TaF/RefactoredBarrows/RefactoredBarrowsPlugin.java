package net.runelite.client.plugins.microbot.TaF.RefactoredBarrows;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
        name = PluginDescriptor.TaFCat + "Refactored Barrows - Real",
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
    @Inject
    private ItemManager itemManager;
    private Instant scriptStartTime;

    public long totalLoot = 0;

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
        totalLoot = 0;
    }

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    protected void shutDown() {
        refactoredBarrowsScript.shutdown();
        overlayManager.remove(refactoredBarrowsOverlay);
        totalLoot = 0;
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
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == InterfaceID.BARROWS_REWARD)
        {
            ItemContainer barrowsRewardContainer = Microbot.getClient().getItemContainer(InventoryID.TRAIL_REWARDINV);
            if (barrowsRewardContainer == null)
            {
                return;
            }

            Item[] items = barrowsRewardContainer.getItems();
            long chestPrice = 0;

            for (Item item : items)
            {
                long itemStack = (long) itemManager.getItemPrice(item.getId()) * item.getQuantity();
                chestPrice += itemStack;
            }
            totalLoot += chestPrice;
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
