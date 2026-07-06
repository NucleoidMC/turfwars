package xyz.uninenville.turfwars.util;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class InventoryUtil {

    /**
     * Counts amount of {@link ItemStack} {@link ServerPlayerEntity} has.
     * Use this method if you want to include the stacks in cursor and crafting grids.
     *
     * @param player {@link ServerPlayerEntity} to count items from
     * @param stack  {@link ItemStack} to count
     * @return total amount of the item player has
     */
    public static int countItems(ServerPlayerEntity player, ItemStack stack) {
        int items = 0;

        for (ItemStack itemStack : player.currentScreenHandler.getStacks()) {
            if (ItemStack.areItemsAndComponentsEqual(itemStack, stack)) {
                items += itemStack.getCount();
            }
        }

        if (ItemStack.areItemsAndComponentsEqual(player.currentScreenHandler.getCursorStack(), stack)) {
            items += player.currentScreenHandler.getCursorStack().getCount();
        }

        return items;
    }

}
