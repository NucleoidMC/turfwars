package xyz.uninenville.turfwars.kit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStackTemplate;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;
import xyz.uninenville.turfwars.game.TurfWarsPlayerDataStorage;
import xyz.uninenville.turfwars.game.TurfWarsTeam;

import java.util.List;
import java.util.Map;

public record TurfWarsKit(
    String id,
    ItemStackTemplate icon,
    Map<EquipmentSlot, ItemStackTemplate> equipment,
    List<ItemStackTemplate> items,
    List<TimedItem> timedItems,
    List<TimedEffect> timedEffects,
    boolean canEnterEnemyTurf,
    boolean canBreakEnemyForts,
    boolean canBuildInEnemyTurf
) {
    public static final Codec<TurfWarsKit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("id").forGetter(TurfWarsKit::id),
        ItemStackTemplate.CODEC.fieldOf("icon").forGetter(TurfWarsKit::icon),
        Codec.unboundedMap(EquipmentSlot.CODEC, ItemStackTemplate.CODEC).optionalFieldOf("equipment", Map.of()).forGetter(TurfWarsKit::equipment),
        ItemStackTemplate.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(TurfWarsKit::items),
        TimedItem.CODEC.listOf().optionalFieldOf("timed_items", List.of()).forGetter(TurfWarsKit::timedItems),
        TimedEffect.CODEC.listOf().optionalFieldOf("timed_effects", List.of()).forGetter(TurfWarsKit::timedEffects),
        Codec.BOOL.optionalFieldOf("can_enter_enemy_turf", false).forGetter(TurfWarsKit::canEnterEnemyTurf),
        Codec.BOOL.optionalFieldOf("can_break_enemy_forts", false).forGetter(TurfWarsKit::canBreakEnemyForts),
        Codec.BOOL.optionalFieldOf("can_build_in_enemy_turf", false).forGetter(TurfWarsKit::canBuildInEnemyTurf)
    ).apply(instance, TurfWarsKit::new));

    public MutableComponent getName() {
        return Component.translatable("turfwars.kit." + id);
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

        if (entity instanceof ServerPlayer player) {
            giveItems(player);
        } else {
            for (ItemStackTemplate item : items) {
                if (entity.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                    entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStackBuilder.of(item.create()).setDyeColor(color).build());
                    continue;
                }

                if (entity.getItemInHand(InteractionHand.OFF_HAND).isEmpty()) {
                    entity.setItemSlot(EquipmentSlot.OFFHAND, ItemStackBuilder.of(item.create()).setDyeColor(color).build());
                    break;
                }
            }
        }
    }

    public void giveEquipment(LivingEntity entity, int color) {
        for (Map.Entry<EquipmentSlot, ItemStackTemplate> equipment : equipment.entrySet()) {
            entity.setItemSlot(equipment.getKey(), ItemStackBuilder.of(equipment.getValue().create()).setDyeColor(color).build());
        }
    }

    public void giveItems(ServerPlayer player) {
        for (ItemStackTemplate stack : items) {
            int slot = TurfWarsPlayerDataStorage.get(player).getPreferredKitItemSlots(KitRegistry.getKitId(this)).getSlot(stack.item().value());
            player.getInventory().add(slot, stack.create());
        }
    }
}
