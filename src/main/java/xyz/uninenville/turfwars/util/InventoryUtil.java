package xyz.uninenville.turfwars.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class InventoryUtil {

    /**
     * Counts amount of {@link ItemStack} {@link ServerPlayer} has.
     * Use this method if you want to include the stacks in cursor and crafting grids.
     *
     * @param player {@link ServerPlayer} to count items from
     * @param stack  {@link ItemStack} to count
     * @return total amount of the item player has
     */
    public static int countItems(ServerPlayer player, ItemStack stack) {
        int items = 0;

        for (ItemStack itemStack : player.containerMenu.getItems()) {
            if (ItemStack.isSameItemSameComponents(itemStack, stack)) {
                items += itemStack.getCount();
            }
        }

        if (ItemStack.isSameItemSameComponents(player.containerMenu.getCarried(), stack)) {
            items += player.containerMenu.getCarried().getCount();
        }

        return items;
    }

}
