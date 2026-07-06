package xyz.uninenville.turfwars.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.uninenville.turfwars.attachment.ModAttachments;
import xyz.uninenville.turfwars.component.ModComponents;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Allow using {@link ModComponents#PROJECTILE_DAMAGE_OVERRIDE} component on projectiles that normally deal no damage, for example snowballs.
     */
    @ModifyReturnValue(method = "hurtServer", at = @At(value = "RETURN", ordinal = 3))
    public boolean damage(boolean original, ServerLevel level, DamageSource source, float damage) {
        if (damage == 0.0F && source.getDirectEntity() != null && source.getDirectEntity() instanceof Projectile projectile) {
            if (projectile.hasAttached(ModAttachments.PROJECTILE_DAMAGE_OVERRIDE)) {
                return super.hurtServer(level, source, damage);
            }
        }

        return original;
    }
}
