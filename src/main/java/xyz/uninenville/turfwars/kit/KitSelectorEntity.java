package xyz.uninenville.turfwars.kit;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import xyz.uninenville.turfwars.game.TurfWarsGame;
import xyz.uninenville.turfwars.game.TurfWarsParticipant;

public class KitSelectorEntity extends ArmorStand {
    private final TurfWarsGame game;
    private final TurfWarsKit kit;

    public KitSelectorEntity(TurfWarsGame game, ServerLevel level, Vec3 position, KitSelector kitSelector, int color) {
        super(EntityType.ARMOR_STAND, level);

        this.game = game;
        this.kit = kitSelector.getKit();

        if (kit != null) {
            kit.giveKit(this, color);
        }
        snapTo(position, kitSelector.rotation(), 0);
        setNoBasePlate(true);
        setShowArms(true);

        // Nametag
        var holder = new ElementHolder();
        var nametag = new TextDisplayElement();

        nametag.setOffset(new Vec3(0, 2, 0));
        nametag.setText(kit.getName());
        nametag.setYaw(getYRot());

        holder.addElement(nametag);
        EntityAttachment.of(holder, this);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
        if (player instanceof ServerPlayer serverPlayer) {
            changeKit(serverPlayer, kit);
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.isDirect() && source.getEntity() instanceof ServerPlayer player) {
            changeKit(player, kit);
        }

        return false;
    }

    public void changeKit(ServerPlayer player, TurfWarsKit kit) {
        TurfWarsParticipant participant = game.getParticipant(player);

        if (participant != null && participant.getKit() != kit) {
            participant.changeKit(kit);
        }
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        // Prevent kit selector moving
    }
}
