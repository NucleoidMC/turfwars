package xyz.uninenville.turfwars.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xyz.nucleoid.plasmid.api.util.Scheduler;
import xyz.uninenville.turfwars.component.ModComponents;
import xyz.uninenville.turfwars.component.type.BarrageComponent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

// Lower priority to allow Stimuli cancel Barrage with ArrowFireEvent
@Mixin(value = ProjectileWeaponItem.class, priority = 900)
public abstract class ProjectileWeaponItemMixin {

    @Shadow
    protected abstract Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack projectile, boolean isCrit);

    @Shadow
    protected abstract void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float speed, float divergence, float yaw, @Nullable LivingEntity target);

    @Shadow
    protected abstract int getDurabilityUse(ItemStack projectile);

    @WrapOperation(
        method = "shoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/entity/projectile/Projectile;"
        )
    )
    private Projectile shootBarrageProjectiles(
        Projectile projectile,
        ServerLevel serverLevel,
        ItemStack itemStack,
        Consumer<Projectile> shootFunction,
        Operation<Projectile> original,
        @Local(argsOnly = true, ordinal = 0) LivingEntity shooter,
        @Local(argsOnly = true) InteractionHand hand,
        @Local(argsOnly = true) ItemStack weapon,
        @Local(argsOnly = true, ordinal = 0) float power,
        @Local(argsOnly = true, ordinal = 1) float uncertainty,
        @Local(argsOnly = true) boolean isCrit,
        @Local(argsOnly = true, ordinal = 1) LivingEntity targetOverride
    ) {
        if (weapon != null) {
            BarrageComponent barrage = weapon.get(ModComponents.BARRAGE_ABILITY);
            List<ItemStack> projectiles = createBarrageProjectileStacks(weapon, itemStack);

            if (barrage != null && !projectiles.isEmpty()) {
                Iterator<ItemStack> iterator = projectiles.iterator();
                float health = shooter.getHealth();

                if (barrage.shotInterval() > 0) {
                    Scheduler.INSTANCE.repeatWhile(server -> {
                        shootBarrageProjectile(serverLevel, shooter, hand, weapon, iterator.next(), power, uncertainty, isCrit, targetOverride, barrage, projectiles);
                    }, value -> iterator.hasNext() && !weapon.isBroken() && shooter.isAlive() && (!barrage.damageCancelsBarrage() || shooter.getHealth() == health), 0, barrage.shotInterval());
                } else {
                    while (iterator.hasNext()) {
                        shootBarrageProjectile(serverLevel, shooter, hand, weapon, iterator.next(), power, uncertainty, isCrit, targetOverride, barrage, projectiles);
                    }
                }

                return null;
            }
        }

        return original.call(projectile, serverLevel, itemStack, shootFunction);
    }

    @Unique
    private List<ItemStack> createBarrageProjectileStacks(ItemStack weaponStack, ItemStack projectileStack) {
        int projectilesLoaded = weaponStack.getOrDefault(ModComponents.BARRAGE_PROJECTILES_LOADED, 0);
        List<ItemStack> list = new ArrayList<>(projectilesLoaded);
        for (int i = 0; i < projectilesLoaded; i++) {
            list.add(projectileStack.copy());
        }

        return list;
    }

    @Unique
    private void shootBarrageProjectile(
        ServerLevel level,
        LivingEntity shooter,
        InteractionHand hand,
        ItemStack weapon,
        ItemStack projectile,
        float speed,
        float divergence,
        boolean critical,
        @Nullable LivingEntity target,
        BarrageComponent barrage,
        List<ItemStack> projectiles
    ) {
        Projectile.spawnProjectile(
            createProjectile(level, shooter, weapon, projectile, critical),
            level,
            projectile,
            projectileEntity -> shootProjectile(shooter, projectileEntity, 0, speed, divergence, 0, target)
        );

        weapon.hurtAndBreak(getDurabilityUse(projectile), shooter, hand);

        level.playSound(
            null,
            shooter.getX(),
            shooter.getY(),
            shooter.getZ(),
            SoundEvents.ARROW_SHOOT,
            SoundSource.PLAYERS,
            1.0F,
            1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F
        );

        if (shooter instanceof ServerPlayer player && barrage.chargingBar().isEnabled() && projectiles.size() > 1) {
            int ticksUsed = barrage.chargeTime() * projectiles.size() - projectiles.indexOf(projectile) * barrage.chargeTime() - barrage.chargeTime();
            player.sendSystemMessage(barrage.getChargingBar(ticksUsed), true);
        }
    }
}
