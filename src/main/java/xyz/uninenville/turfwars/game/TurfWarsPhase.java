package xyz.uninenville.turfwars.game;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum TurfWarsPhase {
    GAME_START_PHASE,
    GAME_END_PHASE,
    INITIAL_BUILD,
    COMBAT,
    BUILD;

    public MutableComponent getName() {
        return Component.translatable("turfwars.phase." + this.toString().toLowerCase());
    }

    public TurfWarsPhase getNextPhase() {
        return switch (this) {
            case GAME_START_PHASE -> INITIAL_BUILD;
            case INITIAL_BUILD, BUILD -> COMBAT;
            case COMBAT -> BUILD;
            default -> GAME_END_PHASE;
        };
    }

    public boolean isGameStartPhase() {
        return this == GAME_START_PHASE;
    }

    public boolean isGameEndPhase() {
        return this == GAME_END_PHASE;
    }

    public boolean isInitialBuildPhase() {
        return this == INITIAL_BUILD;
    }

    public boolean isBuildPhase() {
        return isInitialBuildPhase() || this == BUILD;
    }

    public boolean isCombatPhase() {
        return this == COMBAT;
    }
}
