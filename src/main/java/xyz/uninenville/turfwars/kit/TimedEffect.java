package xyz.uninenville.turfwars.kit;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.dynamic.Codecs;

import java.util.ArrayList;
import java.util.List;

public record TimedEffect(
    StatusEffectInstance statusEffect,
    int initialTime,
    int renewalTime,
    Either<String, List<String>> regionMarker,
    boolean removeOnRegionLeave
) {
    public static final Codec<TimedEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        StatusEffectInstance.CODEC.fieldOf("status_effect").forGetter(TimedEffect::statusEffect),
        Codecs.NON_NEGATIVE_INT.fieldOf("initial_time").forGetter(TimedEffect::initialTime),
        Codec.INT.optionalFieldOf("renewal_time", -1).forGetter(TimedEffect::renewalTime),
        Codec.either(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("region_marker", Either.left("play_area")).forGetter(TimedEffect::regionMarker),
        Codec.BOOL.optionalFieldOf("remove_on_region_leave", true).forGetter(TimedEffect::removeOnRegionLeave)
    ).apply(instance, TimedEffect::new));

    public List<String> getRegions() {
        List<String> regions = new ArrayList<>();
        regionMarker().mapBoth(regions::add, regions::addAll);

        return regions;
    }

    public TimedEffect withRegion(String marker) {
        return new TimedEffect(statusEffect(), initialTime(), renewalTime(), Either.left(marker), removeOnRegionLeave());
    }

    public TimedEffect withRegions(List<String> marker) {
        return new TimedEffect(statusEffect(), initialTime(), renewalTime(), Either.right(marker), removeOnRegionLeave());
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof TimedEffect(var effect1, var initialTime1, var renewalTime1, var marker1, var removeOnRegionLeave1)) {
            return statusEffect.equals(effect1)
                && initialTime == initialTime1
                && renewalTime == renewalTime1
                && regionMarker.equals(marker1)
                && removeOnRegionLeave == removeOnRegionLeave1;
        }

        return false;
    }
}
