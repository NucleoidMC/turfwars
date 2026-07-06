package xyz.uninenville.turfwars.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
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
@Mixin(value = RangedWeaponItem.class, priority = 900)
public abstract class RangedWeaponItemMixin {

    @Shadow
    protected abstract ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack, boolean critical);

    @Shadow
    protected abstract void shoot(LivingEntity shooter, ProjectileEntity projectile, int index, float speed, float divergence, float yaw, @Nullable LivingEntity target);

    @Shadow
    protected abstract int getWeaponStackDamage(ItemStack projectile);

    @WrapOperation(
        method = "shootAll",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/projectile/ProjectileEntity;spawn(Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/entity/projectile/ProjectileEntity;"
        )
    )
    private ProjectileEntity shootBarrageProjectiles(
        ProjectileEntity projectile,
        ServerWorld world,
        ItemStack projectileStack,
        Consumer<ProjectileEntity> beforeSpawn,
        Operation<ProjectileEntity> original,
        @Local(argsOnly = true, ordinal = 0) LivingEntity shooter,
        @Local(argsOnly = true) Hand hand,
        @Local(argsOnly = true) ItemStack weaponStack,
        @Local(argsOnly = true, ordinal = 0) float speed,
        @Local(argsOnly = true, ordinal = 1) float divergence,
        @Local(argsOnly = true) boolean critical,
        @Local(argsOnly = true, ordinal = 1) LivingEntity target
    ) {
        if (weaponStack != null) {
            BarrageComponent barrage = weaponStack.get(ModComponents.BARRAGE_ABILITY);
            List<ItemStack> projectiles = createBarrageProjectileStacks(weaponStack, projectileStack);

            if (barrage != null && !projectiles.isEmpty()) {
                Iterator<ItemStack> iterator = projectiles.iterator();
                float health = shooter.getHealth();

                if (barrage.shotInterval() > 0) {
                    Scheduler.INSTANCE.repeatWhile(server -> {
                        shootBarrageProjectile(world, shooter, hand, weaponStack, iterator.next(), speed, divergence, critical, target, barrage, projectiles);
                    }, value -> iterator.hasNext() && !weaponStack.shouldBreak() && shooter.isAlive() && (!barrage.damageCancelsBarrage() || shooter.getHealth() == health), 0, barrage.shotInterval());
                } else {
                    while (iterator.hasNext()) {
                        shootBarrageProjectile(world, shooter, hand, weaponStack, iterator.next(), speed, divergence, critical, target, barrage, projectiles);
                    }
                }

                return null;
            }
        }

        return original.call(projectile, world, projectileStack, beforeSpawn);
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
        ServerWorld world,
        LivingEntity shooter,
        Hand hand,
        ItemStack weapon,
        ItemStack projectile,
        float speed,
        float divergence,
        boolean critical,
        @Nullable LivingEntity target,
        BarrageComponent barrage,
        List<ItemStack> projectiles
    ) {
        ProjectileEntity.spawn(
            createArrowEntity(world, shooter, weapon, projectile, critical),
            world,
            projectile,
            projectileEntity -> shoot(shooter, projectileEntity, 0, speed, divergence, 0, target)
        );

        weapon.damage(getWeaponStackDamage(projectile), shooter, hand);

        world.playSound(
            null,
            shooter.getX(),
            shooter.getY(),
            shooter.getZ(),
            SoundEvents.ENTITY_ARROW_SHOOT,
            SoundCategory.PLAYERS,
            1.0F,
            1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F
        );

        if (shooter instanceof ServerPlayerEntity player && barrage.chargingBar().isEnabled() && projectiles.size() > 1) {
            int ticksUsed = barrage.chargeTime() * projectiles.size() - projectiles.indexOf(projectile) * barrage.chargeTime() - barrage.chargeTime();
            player.sendMessage(barrage.getChargingBar(ticksUsed), true);
        }
    }
}
