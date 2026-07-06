package xyz.uninenville.turfwars.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.uninenville.turfwars.component.ModComponents;
import xyz.uninenville.turfwars.component.type.BarrageComponent;
import xyz.uninenville.turfwars.util.SoundInstance;

import java.util.List;

@Mixin(BowItem.class)
public abstract class BowItemMixin extends ProjectileWeaponItem {

    public BowItemMixin(Properties settings) {
        super(settings);
    }

    @Inject(method = "use", at = @At(value = "HEAD"))
    private void use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide()) {
            ItemStack bow = player.getItemInHand(hand);

            if (bow.has(ModComponents.BARRAGE_ABILITY)) {
                bow.set(ModComponents.BARRAGE_PROJECTILES_LOADED, 0);
            }
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        BarrageComponent barrage = stack.get(ModComponents.BARRAGE_ABILITY);

        if (barrage != null) {
            int projectilesLoaded = stack.getOrDefault(ModComponents.BARRAGE_PROJECTILES_LOADED, 0);

            if (!level.isClientSide() && user instanceof ServerPlayer player) {
                int ticksUsed = stack.getUseDuration(player) - remainingUseTicks;

                // Barrage ability is only used when the first arrow is fully loaded
                if (BowItem.getPowerForTime(ticksUsed) != 1.0F) {
                    return;
                }

                // Minus the real arrow charging time and add time it would take to load first arrow
                ticksUsed = ticksUsed - 20 + barrage.chargeTime();

                SoundInstance sound = null;
                if (projectilesLoaded < barrage.projectileLimit()) {
                    if ((projectilesLoaded == 0 || ticksUsed % barrage.chargeTime() == 0)) {
                        List<SoundInstance> chargeSounds = barrage.chargingSounds().getChargeSounds();
                        projectilesLoaded += 1;

                        if (projectilesLoaded == 1) {
                            // Barrage starting to load
                            sound = barrage.chargingSounds().start();
                        } else if (projectilesLoaded == barrage.projectileLimit()) {
                            // Barrage fully loaded
                            sound = barrage.chargingSounds().full();
                        } else if (projectilesLoaded > 1 && projectilesLoaded < barrage.projectileLimit() && !chargeSounds.isEmpty()) {
                            // Barrage loading
                            int soundIndex = projectilesLoaded - 2;
                            int chargeSoundIndex = chargeSounds.size() > soundIndex ? soundIndex : soundIndex % chargeSounds.size();
                            sound = chargeSounds.get(chargeSoundIndex);
                        }
                    }
                }

                // Play charging sound
                if (sound != null) {
                    sound.playSound(player);
                }

                // Send bow charging bar
                if (barrage.chargingBar().isEnabled()) {
                    player.sendSystemMessage(barrage.getChargingBar(ticksUsed), true);
                }

                stack.set(ModComponents.BARRAGE_PROJECTILES_LOADED, projectilesLoaded);
            }
        }
    }

    @Inject(
        method = "releaseUsing",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"
        ),
        cancellable = true
    )
    private void cancelSoundIfUsingBarrage(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime, CallbackInfoReturnable<Boolean> cir) {
        if (itemStack.has(ModComponents.BARRAGE_ABILITY)) {
            cir.cancel();
        }
    }

    @ModifyArg(
        method = "shootProjectile",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/Projectile;shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V"
        ),
        index = 5
    )
    protected float modifyBarrageProjectileSpread(float original, @Local(argsOnly = true) Projectile projectileEntity) {
        ItemStack weapon = projectileEntity.getWeaponItem();

        if (weapon != null) {
            BarrageComponent barrage = weapon.get(ModComponents.BARRAGE_ABILITY);

            if (barrage != null) {
                int projectilesLoaded = weapon.getOrDefault(ModComponents.BARRAGE_PROJECTILES_LOADED, 0);

                if (projectilesLoaded > 1) {
                    return barrage.shotSpread();
                }
            }
        }

        return original;
    }
}
