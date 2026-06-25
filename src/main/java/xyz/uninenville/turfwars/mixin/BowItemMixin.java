package xyz.uninenville.turfwars.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.uninenville.turfwars.TurfWars;
import xyz.uninenville.turfwars.component.ModComponents;
import xyz.uninenville.turfwars.component.type.BarrageComponent;
import xyz.uninenville.turfwars.util.SoundInstance;

import java.util.ArrayList;
import java.util.List;

@Mixin(BowItem.class)
public abstract class BowItemMixin extends RangedWeaponItem {
    @Unique
    private static final AttachmentType<Integer> BARRAGE_PROJECTILES_LOADED = AttachmentRegistry.create(
        TurfWars.id("barrage_projectiles_loaded"), builder -> builder.initializer(() -> 0)
    );
    @Unique
    private static final AttachmentType<Boolean> BARRAGE_FULLY_LOADED = AttachmentRegistry.create(
        TurfWars.id("barrage_fully_loaded"), builder -> builder.initializer(() -> false)
    );

    public BowItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "use", at = @At(value = "HEAD"))
    private void use(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!world.isClient()) {
            ItemStack bow = user.getStackInHand(hand);

            if (bow.contains(ModComponents.BARRAGE_ABILITY)) {
                user.setAttached(BARRAGE_PROJECTILES_LOADED, 0);
                user.setAttached(BARRAGE_FULLY_LOADED, false);
            }
        }
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        BarrageComponent barrage = stack.get(ModComponents.BARRAGE_ABILITY);

        if (barrage != null) {
            int projectilesLoaded = user.getAttached(BARRAGE_PROJECTILES_LOADED);

            if (!world.isClient() && user instanceof ServerPlayerEntity player) {
                int ticksUsed = stack.getMaxUseTime(player) - remainingUseTicks;

                // Barrage ability is only used when the first arrow is fully loaded
                if (BowItem.getPullProgress(ticksUsed) != 1.0F) {
                    return;
                }

                // Minus the real arrow charging time and add time it would take to load first arrow
                ticksUsed = ticksUsed - 20 + barrage.chargeTime();

                SoundInstance sound = null;
                if (!user.getAttached(BARRAGE_FULLY_LOADED)) {
                    if ((projectilesLoaded == 0 || ticksUsed % barrage.chargeTime() == 0) && projectilesLoaded < barrage.projectileLimit()) {
                        List<SoundInstance> chargeSounds = barrage.chargingSounds().getChargeSounds();
                        projectilesLoaded += 1;

                        if (projectilesLoaded == 1) {
                            sound = barrage.chargingSounds().start();
                        } else if (projectilesLoaded > 1 && projectilesLoaded < barrage.projectileLimit() && !chargeSounds.isEmpty()) {
                            int soundIndex = projectilesLoaded - 2;
                            int chargeSoundIndex = chargeSounds.size() > soundIndex ? soundIndex : soundIndex % chargeSounds.size();
                            sound = chargeSounds.get(chargeSoundIndex);
                        }
                    } else if (projectilesLoaded == barrage.projectileLimit()) {
                        user.setAttached(BARRAGE_FULLY_LOADED, true);
                        sound = barrage.chargingSounds().full();
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

                user.setAttached(BARRAGE_PROJECTILES_LOADED, projectilesLoaded);
            }
        }
    }

    @WrapOperation(
        method = "onStoppedUsing",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/BowItem;load(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;)Ljava/util/List;"
        )
    )
    private List<ItemStack> loadBarrageProjectiles(ItemStack stack, ItemStack projectileStack, LivingEntity shooter, Operation<List<ItemStack>> original) {
        BarrageComponent barrage = stack.get(ModComponents.BARRAGE_ABILITY);

        if (barrage != null) {
            int projectilesLoaded = shooter.getAttached(BARRAGE_PROJECTILES_LOADED);

            if (!shooter.getEntityWorld().isClient()) {
                if (projectilesLoaded > 1) {
                    List<ItemStack> list = new ArrayList<>(projectilesLoaded);
                    ItemStack itemStack = projectileStack.copy();

                    for (int i = 0; i < projectilesLoaded; i++) {
                        ItemStack itemStack2 = getProjectile(stack, i == 0 ? projectileStack : itemStack, shooter, i > 0);
                        if (!itemStack2.isEmpty()) {
                            list.add(itemStack2);
                        }
                    }

                    return list;
                }
            }
        }

        return original.call(stack, projectileStack, shooter);
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
    protected float modifyBarrageProjectileSpread(float original, @Local(argsOnly = true, ordinal = 0) LivingEntity shooter, @Local(argsOnly = true) ProjectileEntity projectile) {
        BarrageComponent barrage = projectile.getWeaponStack().get(ModComponents.BARRAGE_ABILITY);

        if (barrage != null) {
            int projectilesLoaded = shooter.getAttached(BARRAGE_PROJECTILES_LOADED);

            if (projectilesLoaded > 1) {
                return barrage.shotSpread();
            }
        }

        return original;
    }
}
