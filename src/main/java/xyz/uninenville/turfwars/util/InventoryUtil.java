package xyz.uninenville.turfwars.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class InventoryUtil {

    /**
     * Counts amount of specific item player has. Includes everything in players current screen handler
     * @param item to count
     * @param player to count slots from
     * @return total amount of the item player has
     */
    public static int countItemsPlayerHas(ServerPlayerEntity player, Item item) {
        int items = 0;

        for (ItemStack itemStack : player.currentScreenHandler.getStacks()) {
            if (itemStack.isOf(item)) {
                items += itemStack.getCount();
            }
        }

        if (player.currentScreenHandler.getCursorStack().isOf(item)) {
            items += player.currentScreenHandler.getCursorStack().getCount();
        }

        return items;
    }

}
