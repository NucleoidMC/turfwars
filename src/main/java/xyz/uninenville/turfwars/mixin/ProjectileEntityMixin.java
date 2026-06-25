package xyz.uninenville.turfwars.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.uninenville.turfwars.attachment.ModAttachments;
import xyz.uninenville.turfwars.component.ModComponents;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin extends Entity {

    public ProjectileEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "triggerProjectileSpawned", at = @At(value = "HEAD"))
    public void triggerProjectileSpawned(ServerWorld world, ItemStack projectileStack, CallbackInfo ci) {
        if (projectileStack.contains(ModComponents.PROJECTILE_DAMAGE_OVERRIDE)) {
            this.setAttached(ModAttachments.PROJECTILE_DAMAGE_OVERRIDE, projectileStack.get(ModComponents.PROJECTILE_DAMAGE_OVERRIDE));
        }

        if (projectileStack.contains(ModComponents.FLETCHING_PROJECTILE)) {
            this.setAttached(ModAttachments.FLETCHING_PROJECTILE, Unit.INSTANCE);
        }
    }
}
