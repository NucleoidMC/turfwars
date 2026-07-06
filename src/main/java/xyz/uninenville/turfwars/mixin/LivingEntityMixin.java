package xyz.uninenville.turfwars.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.uninenville.turfwars.attachment.ModAttachments;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At(value = "HEAD"), cancellable = true)
    public void applyProjectileDamageOverride(DamageSource damageSource, float damage, CallbackInfoReturnable<Float> cir) {
        if (damageSource.getDirectEntity() != null && damageSource.getDirectEntity() instanceof Projectile projectile) {
            if (projectile.hasAttached(ModAttachments.PROJECTILE_DAMAGE_OVERRIDE)) {
                cir.setReturnValue(projectile.getAttachedOrElse(ModAttachments.PROJECTILE_DAMAGE_OVERRIDE, damage));
            }
        }
    }
}
