package xyz.uninenville.turfwars.kit;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import xyz.uninenville.turfwars.game.TurfWarsGame;
import xyz.uninenville.turfwars.game.TurfWarsParticipant;

public class KitSelectorEntity extends ArmorStandEntity {
    private final TurfWarsGame game;
    private final TurfWarsKit kit;

    public KitSelectorEntity(TurfWarsGame game, ServerWorld world, Vec3d position, KitSelector kitSelector, int color) {
        super(EntityType.ARMOR_STAND, world);

        this.game = game;
        this.kit = kitSelector.getKit();

        if (kit != null) {
            kit.giveKit(this, color);
        }
        refreshPositionAndAngles(position, kitSelector.rotation(), 0);
        setHideBasePlate(true);
        setShowArms(true);

        // Nametag
        var holder = new ElementHolder();
        var nametag = new TextDisplayElement();

        nametag.setOffset(new Vec3d(0, 2, 0));
        nametag.setText(kit.getName());
        nametag.setYaw(getYaw());

        holder.addElement(nametag);
        EntityAttachment.of(holder, this);
    }

    @Override
    public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            changeKit(serverPlayer, kit);
        }

        return ActionResult.FAIL;
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (source.isDirect() && source.getAttacker() instanceof ServerPlayerEntity player) {
            changeKit(player, kit);
        }

        return false;
    }

    public void changeKit(ServerPlayerEntity player, TurfWarsKit kit) {
        TurfWarsParticipant participant = game.getParticipant(player);

        if (participant != null && participant.getKit() != kit) {
            participant.changeKit(kit);
        }
    }

    @Override
    public void move(MovementType type, Vec3d movement) {
        // Prevent kit selector moving
    }
}
