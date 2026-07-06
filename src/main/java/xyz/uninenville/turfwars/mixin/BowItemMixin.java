package xyz.uninenville.turfwars.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
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
public abstract class BowItemMixin extends RangedWeaponItem {

    public BowItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "use", at = @At(value = "HEAD"))
    private void use(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!world.isClient()) {
            ItemStack bow = user.getStackInHand(hand);

            if (bow.contains(ModComponents.BARRAGE_ABILITY)) {
                bow.set(ModComponents.BARRAGE_PROJECTILES_LOADED, 0);
            }
        }
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        BarrageComponent barrage = stack.get(ModComponents.BARRAGE_ABILITY);

        if (barrage != null) {
            int projectilesLoaded = stack.getOrDefault(ModComponents.BARRAGE_PROJECTILES_LOADED, 0);

            if (!world.isClient() && user instanceof ServerPlayerEntity player) {
                int ticksUsed = stack.getMaxUseTime(player) - remainingUseTicks;

                // Barrage ability is only used when the first arrow is fully loaded
                if (BowItem.getPullProgress(ticksUsed) != 1.0F) {
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
                    player.sendMessage(barrage.getChargingBar(ticksUsed), true);
                }

                stack.set(ModComponents.BARRAGE_PROJECTILES_LOADED, projectilesLoaded);
            }
        }
    }

    @Inject(
        method = "onStoppedUsing",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/Entity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"
        ),
        cancellable = true
    )
    private void cancelSoundIfUsingBarrage(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (stack.contains(ModComponents.BARRAGE_ABILITY)) {
            cir.cancel();
        }
    }

    @ModifyArg(
        method = "shoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/projectile/ProjectileEntity;setVelocity(Lnet/minecraft/entity/Entity;FFFFF)V"
        ),
        index = 5
    )
    protected float modifyBarrageProjectileSpread(float original, @Local(argsOnly = true) ProjectileEntity projectile) {
        ItemStack weapon = projectile.getWeaponStack();

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
