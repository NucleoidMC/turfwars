package xyz.uninenville.turfwars.attachment;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Unit;
import net.minecraft.util.dynamic.Codecs;
import xyz.uninenville.turfwars.TurfWars;

public class ModAttachments {
    public static void initialize() {
    }

    public static final AttachmentType<Float> PROJECTILE_DAMAGE_OVERRIDE = AttachmentRegistry.createPersistent(
        TurfWars.id("projectile_damage_override"), Codecs.NON_NEGATIVE_FLOAT
    );

    public static final AttachmentType<Unit> FLETCHING_PROJECTILE = AttachmentRegistry.createPersistent(
        TurfWars.id("fletching_projectile"), Unit.CODEC
    );
}
