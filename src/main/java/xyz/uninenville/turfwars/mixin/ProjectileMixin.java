package xyz.uninenville.turfwars.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.uninenville.turfwars.attachment.ModAttachments;
import xyz.uninenville.turfwars.component.ModComponents;

@Mixin(Projectile.class)
public abstract class ProjectileMixin extends Entity {

    public ProjectileMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "applyOnProjectileSpawned", at = @At(value = "HEAD"))
    public void triggerProjectileSpawned(ServerLevel serverLevel, ItemStack pickupItemStack, CallbackInfo ci) {
        if (pickupItemStack.has(ModComponents.PROJECTILE_DAMAGE_OVERRIDE)) {
            this.setAttached(ModAttachments.PROJECTILE_DAMAGE_OVERRIDE, pickupItemStack.get(ModComponents.PROJECTILE_DAMAGE_OVERRIDE));
        }

        if (pickupItemStack.has(ModComponents.FLETCHING_PROJECTILE)) {
            this.setAttached(ModAttachments.FLETCHING_PROJECTILE, Unit.INSTANCE);
        }
    }
}
