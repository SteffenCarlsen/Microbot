package net.runelite.client.plugins.microbot.TaF.RefactoredBarrows;

import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;

public class BarrowsUtility {

    public static int calculateWithdrawQuantity(int itemId, int quantity) {
        int withdrawQuantity;
        Rs2ItemModel rs2Item = Rs2Inventory.get(itemId);
        if (rs2Item != null && rs2Item.isStackable()) {
            withdrawQuantity = quantity - rs2Item.getQuantity();
            if (Rs2Inventory.hasItemAmount(itemId, quantity)) {
                return 0;
            }
            return withdrawQuantity;
        } else {
            if (Rs2Inventory.hasItemAmount(itemId, quantity)) {
                return 0;
            }
        }
        var inventCount = Rs2Inventory.count(itemId);
        withdrawQuantity = quantity - inventCount;
        if (Rs2Inventory.hasItemAmount(itemId, quantity)) {
            return 0;
        }
        return withdrawQuantity;
    }
}
