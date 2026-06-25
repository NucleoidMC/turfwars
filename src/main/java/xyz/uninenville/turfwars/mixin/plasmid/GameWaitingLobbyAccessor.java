package xyz.uninenville.turfwars.mixin.plasmid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;

@Mixin(value = GameWaitingLobby.class, remap = false)
public interface GameWaitingLobbyAccessor {
    @Accessor
    long getCountdownStart();

    @Invoker
    long invokeGetRemainingTicks(long time);
}
