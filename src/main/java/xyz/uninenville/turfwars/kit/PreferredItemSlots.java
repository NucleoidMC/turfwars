package xyz.uninenville.turfwars.kit;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class PreferredItemSlots {
    final Int2ObjectOpenHashMap<Item> slots = new Int2ObjectOpenHashMap<>();

    public int getSlot(Item item) {
        if (slots.containsValue(item)) {
            return slots.int2ObjectEntrySet().stream().filter(entry -> entry.getValue() == item)
                .map(Int2ObjectOpenHashMap.Entry::getIntKey).toList().getFirst();
        }

        return -1;
    }

    public void setSlot(int slot, Item item) {
        this.slots.put(slot, item);
    }

    public Item getItem(int slot) {
        if (slots.containsKey(slot)) {
            return slots.get(slot);
        }

        return Items.AIR;
    }
}
