package xyz.uninenville.turfwars.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.uninenville.turfwars.attachment.ModAttachments;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "modifyAppliedDamage", at = @At(value = "HEAD"), cancellable = true)
    public void applyProjectileDamageOverride(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (source.getSource() != null && source.getSource() instanceof ProjectileEntity projectile) {
            if (projectile.hasAttached(ModAttachments.PROJECTILE_DAMAGE_OVERRIDE)) {
                cir.setReturnValue(projectile.getAttachedOrElse(ModAttachments.PROJECTILE_DAMAGE_OVERRIDE, amount));
            }
        }
    }
}
