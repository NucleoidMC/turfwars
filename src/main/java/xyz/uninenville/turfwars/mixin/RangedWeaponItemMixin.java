package xyz.uninenville.turfwars.mixin;

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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.uninenville.turfwars.component.ModComponents;
import xyz.uninenville.turfwars.component.type.BarrageComponent;
import xyz.nucleoid.plasmid.api.util.Scheduler;

import java.util.Iterator;
import java.util.List;

// Higher priority because Stimuli must fire ArrowFireEvent first
@Mixin(value = RangedWeaponItem.class, priority = 1500)
public abstract class RangedWeaponItemMixin {

    @Shadow
    protected abstract ProjectileEntity createArrowEntity(World world, LivingEntity shooter, ItemStack weaponStack, ItemStack projectileStack, boolean critical);

    @Shadow
    protected abstract void shoot(LivingEntity shooter, ProjectileEntity projectile, int index, float speed, float divergence, float yaw, @Nullable LivingEntity target);

    @Shadow
    protected abstract int getWeaponStackDamage(ItemStack projectile);

    @Inject(
        method = "shootAll",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/projectile/ProjectileEntity;spawn(Lnet/minecraft/entity/projectile/ProjectileEntity;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/entity/projectile/ProjectileEntity;"
        ),
        cancellable = true
    )
    private void shootBarrageProjectiles(ServerWorld world, LivingEntity shooter, Hand hand, ItemStack weapon, List<ItemStack> projectiles, float speed, float divergence, boolean critical, @Nullable LivingEntity target, CallbackInfo ci) {
        if (weapon.contains(ModComponents.BARRAGE_ABILITY)) {
            BarrageComponent barrage = weapon.get(ModComponents.BARRAGE_ABILITY);
            Iterator<ItemStack> iterator = projectiles.iterator();

            if (barrage.shotInterval() > 0) {
                Scheduler.INSTANCE.repeatWhile(server -> {
                    shootBarrageProjectile(world, shooter, hand, weapon, iterator.next(), speed, divergence, critical, target, barrage, projectiles);
                }, value -> iterator.hasNext() && !weapon.shouldBreak(), 0, barrage.shotInterval());
            } else {
                while (iterator.hasNext()) {
                    shootBarrageProjectile(world, shooter, hand, weapon, iterator.next(), speed, divergence, critical, target, barrage, projectiles);
                }
            }

            ci.cancel();
        }
    }

    @Unique
    private void shootBarrageProjectile(ServerWorld world, LivingEntity shooter, Hand hand, ItemStack weapon, ItemStack projectile, float speed, float divergence, boolean critical, @Nullable LivingEntity target, BarrageComponent barrage, List<ItemStack> projectiles) {
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
