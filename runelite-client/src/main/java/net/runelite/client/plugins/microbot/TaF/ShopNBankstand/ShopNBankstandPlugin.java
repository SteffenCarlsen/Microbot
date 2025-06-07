package net.runelite.client.plugins.microbot.TaF.ShopNBankstand;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.Arrays;

@PluginDescriptor(
        name = PluginDescriptor.TaFCat + "TaF's ShopNBankstand",
        description = "For Skilling at the Bank",
        tags = {"bankstander", "bank.js", "bank", "eXioStorm", "storm"},
        enabledByDefault = false
)
@Slf4j
public class ShopNBankstandPlugin extends Plugin {
    @Inject
    private ShopNBankstandConfig config;

    @Provides
    ShopNBankstandConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ShopNBankstandConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ShopNBankstandOverlay shopNBankstandOverlay;

    @Inject
    ShopNBankstandScript shopNBankstandScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(shopNBankstandOverlay);
        }
        shopNBankstandScript.run(config);
    }
    ///* Added by Storm
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged inventory){
        if(inventory.getContainerId()==93){
            if (!Rs2Bank.isOpen()) {
                ShopNBankstandScript.itemsProcessed++;
            }
            if (ShopNBankstandScript.secondItemId != null) { // Use secondItemId if it's available
                if (Arrays.stream(inventory.getItemContainer().getItems())
                        .anyMatch(x -> x.getId() == ShopNBankstandScript.secondItemId)) {
                    // average is 1800, max is 2400~
                    ShopNBankstandScript.previousItemChange = System.currentTimeMillis();
                    //System.out.println("still processing items");
                } else {
                    ShopNBankstandScript.previousItemChange = (System.currentTimeMillis() - 2500);
                }
            } else { // Use secondItemIdentifier if secondItemId is null
                Rs2ItemModel item = Rs2Inventory.get(config.secondItemIdentifier());
                if (item != null) {
                    // average is 1800, max is 2400~
                    ShopNBankstandScript.previousItemChange = System.currentTimeMillis();
                    //System.out.println("still processing items");
                } else {
                    ShopNBankstandScript.previousItemChange = (System.currentTimeMillis() - 2500);
                }
            }
        }
    }
    @Subscribe
    public void onWidgetLoaded(WidgetLoaded widget){
        if (widget.getGroupId()==270) {
            if(ShopNBankstandScript.isWaitingForPrompt) {
                ShopNBankstandScript.isWaitingForPrompt = false;
            }
        }
    }
    //*/ Added by Storm
    protected void shutDown() {
        shopNBankstandScript.shutdown();
        overlayManager.remove(shopNBankstandOverlay);
    }
}
