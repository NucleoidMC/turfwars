package xyz.uninenville.turfwars.kit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import xyz.nucleoid.codecs.MoreCodecs;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;
import xyz.uninenville.turfwars.game.TurfWarsPlayerDataStorage;
import xyz.uninenville.turfwars.game.TurfWarsTeam;

import java.util.List;
import java.util.Map;

public record TurfWarsKit(
    String id,
    ItemStack icon,
    Map<EquipmentSlot, ItemStack> equipment,
    List<ItemStack> items,
    List<TimedItem> timedItems,
    List<TimedEffect> timedEffects,
    boolean canEnterEnemyTurf,
    boolean canBreakEnemyForts,
    boolean canBuildInEnemyTurf
) {
    public static final Codec<TurfWarsKit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("id").forGetter(TurfWarsKit::id),
        MoreCodecs.ITEM_STACK.fieldOf("icon").forGetter(TurfWarsKit::icon),
        Codec.unboundedMap(EquipmentSlot.CODEC, MoreCodecs.ITEM_STACK).optionalFieldOf("equipment", Map.of()).forGetter(TurfWarsKit::equipment),
        MoreCodecs.ITEM_STACK.listOf().optionalFieldOf("items", List.of()).forGetter(TurfWarsKit::items),
        TimedItem.CODEC.listOf().optionalFieldOf("timed_items", List.of()).forGetter(TurfWarsKit::timedItems),
        TimedEffect.CODEC.listOf().optionalFieldOf("timed_effects", List.of()).forGetter(TurfWarsKit::timedEffects),
        Codec.BOOL.optionalFieldOf("can_enter_enemy_turf", false).forGetter(TurfWarsKit::canEnterEnemyTurf),
        Codec.BOOL.optionalFieldOf("can_break_enemy_forts", false).forGetter(TurfWarsKit::canBreakEnemyForts),
        Codec.BOOL.optionalFieldOf("can_build_in_enemy_turf", false).forGetter(TurfWarsKit::canBuildInEnemyTurf)
    ).apply(instance, TurfWarsKit::new));

    public MutableText getName() {
        return Text.translatable("turfwars.kit." + id);
    }

    public List<TimedEffect> timedEffects(TurfWarsTeam team) {
        return timedEffects.stream().map(timedEffect -> timedEffect.withRegions(timedEffect.getRegions().stream().map(marker -> {
            marker = marker.replace("{team}", team.getGameTeam().key().id());
            marker = marker.replace("{team_opposite}", team.getOppositeTeam().getGameTeam().key().id());

            return marker;
        }).toList())).toList();
    }

    public void giveKit(LivingEntity entity, int color) {
        giveEquipment(entity, color);

        if (entity instanceof ServerPlayerEntity player) {
            giveItems(player);
        } else {
            for (ItemStack item : items) {
                if (entity.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
                    entity.equipStack(EquipmentSlot.MAINHAND, ItemStackBuilder.of(item).setDyeColor(color).build());
                    continue;
                }

                if (entity.getStackInHand(Hand.OFF_HAND).isEmpty()) {
                    entity.equipStack(EquipmentSlot.OFFHAND, ItemStackBuilder.of(item).setDyeColor(color).build());
                    break;
                }
            }
        }
    }

    public void giveEquipment(LivingEntity entity, int color) {
        for (Map.Entry<EquipmentSlot, ItemStack> equipment : equipment.entrySet()) {
            entity.equipStack(equipment.getKey(), ItemStackBuilder.of(equipment.getValue()).setDyeColor(color).build());
        }
    }

    public void giveItems(ServerPlayerEntity player) {
        for (ItemStack stack : items) {
            int slot = TurfWarsPlayerDataStorage.get(player).getPreferredKitItemSlots(KitRegistry.getKitId(this)).getSlot(stack.getItem());
            player.getInventory().insertStack(slot, stack.copy());
        }
    }
}
