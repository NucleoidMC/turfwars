package xyz.uninenville.turfwars.kit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public record TimedItem(
    ItemStackTemplate stack,
    int time,
    int thresholdAmount
) {
    public static final Codec<TimedItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStackTemplate.CODEC.fieldOf("item").forGetter(TimedItem::stack),
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("time").forGetter(TimedItem::time),
        ExtraCodecs.POSITIVE_INT.fieldOf("threshold_amount").forGetter(TimedItem::thresholdAmount)
    ).apply(instance, TimedItem::new));

    public TimedItem withItemStack(ItemStack itemStack) {
        return new TimedItem(ItemStackTemplate.fromNonEmptyStack(itemStack), time, thresholdAmount);
    }

    public TimedItem withItem(Item item) {
        return new TimedItem(new ItemStackTemplate(item), time, thresholdAmount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof TimedItem(var stack1, var time1, var thresholdAmount1)) {
            return ItemStack.isSameItemSameComponents(stack.create(), stack1.create())
                && time == time1 && thresholdAmount == thresholdAmount1;
        }

        return false;
    }
}
