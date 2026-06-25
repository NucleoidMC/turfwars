package xyz.uninenville.turfwars.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;

public record TurfWarsGameConfig(
    int gameStartTime,
    int gameEndTime,
    int initialBuildTime,
    int combatTime,
    int buildTime,
    int initialBuildTimeWoolAmount,
    int buildTimeWoolAmount,
    int respawnWoolAmount,
    int respawnDelay,
    boolean allowJoinAfterStart
) {
    public static final TurfWarsGameConfig DEFAULT = new TurfWarsGameConfig(
        200, 200,800, 1800, 400,
        64, 32, 6, 0,
        true
    );

    public static final Codec<TurfWarsGameConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codecs.NON_NEGATIVE_INT.optionalFieldOf("game_start_time", DEFAULT.gameStartTime).forGetter(TurfWarsGameConfig::gameStartTime),
        Codecs.NON_NEGATIVE_INT.optionalFieldOf("game_end_time", DEFAULT.gameEndTime).forGetter(TurfWarsGameConfig::gameEndTime),
        Codecs.NON_NEGATIVE_INT.optionalFieldOf("initial_build_time", DEFAULT.initialBuildTime).forGetter(TurfWarsGameConfig::initialBuildTime),
        Codecs.NON_NEGATIVE_INT.optionalFieldOf("combat_time", DEFAULT.combatTime).forGetter(TurfWarsGameConfig::combatTime),
        Codecs.NON_NEGATIVE_INT.optionalFieldOf("build_time", DEFAULT.buildTime).forGetter(TurfWarsGameConfig::buildTime),
        Codecs.NON_NEGATIVE_INT.optionalFieldOf("initial_build_time_wool_amount", DEFAULT.initialBuildTimeWoolAmount).forGetter(TurfWarsGameConfig::initialBuildTimeWoolAmount),
        Codecs.NON_NEGATIVE_INT.optionalFieldOf("build_time_wool_amount", DEFAULT.buildTimeWoolAmount).forGetter(TurfWarsGameConfig::buildTimeWoolAmount),
        Codecs.NON_NEGATIVE_INT.optionalFieldOf("respawn_wool_amount", DEFAULT.respawnWoolAmount).forGetter(TurfWarsGameConfig::respawnWoolAmount),
        Codecs.NON_NEGATIVE_INT.optionalFieldOf("respawn_delay", DEFAULT.respawnDelay).forGetter(TurfWarsGameConfig::respawnDelay),
        Codec.BOOL.optionalFieldOf("allow_join_after_start", DEFAULT.allowJoinAfterStart).forGetter(TurfWarsGameConfig::allowJoinAfterStart)
    ).apply(instance, TurfWarsGameConfig::new));
}
