package xyz.uninenville.turfwars.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.uninenville.turfwars.attachment.ModAttachments;
import xyz.uninenville.turfwars.component.ModComponents;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Allow using {@link ModComponents#PROJECTILE_DAMAGE_OVERRIDE} component on projectiles that normally deal no damage, for example snowballs.
     */
    @ModifyReturnValue(method = "damage", at = @At(value = "RETURN", ordinal = 3))
    public boolean damage(boolean original, ServerWorld world, DamageSource source, float amount) {
        if (amount == 0.0F && source.getSource() != null && source.getSource() instanceof ProjectileEntity projectile) {
            if (projectile.hasAttached(ModAttachments.PROJECTILE_DAMAGE_OVERRIDE)) {
                return super.damage(world, source, amount);
            }
        }

        return original;
    }
}
