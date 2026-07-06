package xyz.uninenville.turfwars.component.type;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ExtraCodecs;
import xyz.uninenville.turfwars.util.LoadingBarComponent;
import xyz.uninenville.turfwars.util.SoundInstance;

import java.util.ArrayList;
import java.util.List;

public record BarrageComponent(
    int chargeTime,
    int projectileLimit,
    int shotInterval,
    float shotSpread,
    LoadingBarComponent chargingBar,
    ChargingSounds chargingSounds,
    boolean damageCancelsBarrage
) {
    public static final Codec<BarrageComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.POSITIVE_INT.fieldOf("charge_time").forGetter(BarrageComponent::chargeTime),
        ExtraCodecs.POSITIVE_INT.fieldOf("projectile_limit").forGetter(BarrageComponent::projectileLimit),
        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("shot_interval", 1).forGetter(BarrageComponent::shotInterval),
        ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("shot_spread", 5.0F).forGetter(BarrageComponent::shotSpread),
        LoadingBarComponent.CODEC.optionalFieldOf("charging_bar", LoadingBarComponent.DEFAULT).forGetter(BarrageComponent::chargingBar),
        ChargingSounds.CODEC.optionalFieldOf("charging_sounds", ChargingSounds.DEFAULT_SOUNDS).forGetter(BarrageComponent::chargingSounds),
        Codec.BOOL.optionalFieldOf("damage_cancels_barrage", true).forGetter(BarrageComponent::damageCancelsBarrage)
    ).apply(instance, BarrageComponent::new));

    public Component getChargingBar(int ticksUsed) {
        float chargePercent = (float) Math.min(ticksUsed / chargeTime, projectileLimit) / projectileLimit;
        MutableComponent bar = Component.empty();

        for (int i = 1; i <= projectileLimit; i++) {
            // Projectile is loaded
            float percent = (float) i / projectileLimit <= chargePercent ? 1.0F
                // Projectile is loading
                : ticksUsed / chargeTime + 1 == i ? (float) (ticksUsed % chargeTime) / chargeTime
                // Projectile is not loading
                : 0.0F;
            bar.append(chargingBar.get(percent));

            // Separate charging bars for different projectiles
            if (i < projectileLimit) {
                bar.append(" ");
            }
        }

        return bar;
    }

    public record ChargingSounds(
        SoundInstance start,
        Either<SoundInstance, List<SoundInstance>> charge,
        SoundInstance full
    ) {
        public static final SoundInstance CHARGE = SoundInstance.of(SoundEvents.UI_BUTTON_CLICK.value());
        public static final ChargingSounds DEFAULT_SOUNDS = new ChargingSounds(
            SoundInstance.of(SoundEvents.EMPTY),
            Either.right(List.of(CHARGE.pitch(1.8F), CHARGE.pitch(1.6F), CHARGE.pitch(1.4F))),
            CHARGE.pitch(1.2F)
        );

        public static final Codec<ChargingSounds> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SoundInstance.CODEC.optionalFieldOf("start", DEFAULT_SOUNDS.start()).forGetter(ChargingSounds::start),
            Codec.either(SoundInstance.CODEC, SoundInstance.CODEC.listOf()).optionalFieldOf("charge", DEFAULT_SOUNDS.charge()).forGetter(ChargingSounds::charge),
            SoundInstance.CODEC.optionalFieldOf("full", DEFAULT_SOUNDS.full()).forGetter(ChargingSounds::full)
        ).apply(instance, ChargingSounds::new));

        public List<SoundInstance> getChargeSounds() {
            List<SoundInstance> sounds = new ArrayList<>();
            charge.mapBoth(sounds::add, sounds::addAll);

            return sounds;
        }
    }
}
