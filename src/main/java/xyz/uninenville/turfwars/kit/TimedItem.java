package xyz.uninenville.turfwars.kit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.dynamic.Codecs;

public record TimedItem(
    ItemStack stack,
    int time,
    int thresholdAmount
) {
    public static final Codec<TimedItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.CODEC.fieldOf("item").forGetter(TimedItem::stack),
        Codecs.NON_NEGATIVE_INT.fieldOf("time").forGetter(TimedItem::time),
        Codecs.POSITIVE_INT.fieldOf("threshold_amount").forGetter(TimedItem::thresholdAmount)
    ).apply(instance, TimedItem::new));

    public TimedItem withItemStack(ItemStack itemStack) {
        return new TimedItem(itemStack, time, thresholdAmount);
    }

    public TimedItem withItem(Item item) {
        return new TimedItem(stack.withItem(item), time, thresholdAmount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof TimedItem(var stack1, var time1, var thresholdAmount1)) {
            return ItemStack.areItemsAndComponentsEqual(stack, stack1)
                && time == time1 && thresholdAmount == thresholdAmount1;
        }

        return false;
    }
}
