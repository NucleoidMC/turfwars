package xyz.uninenville.turfwars.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;

public record SoundInstance(
    SoundEvent sound,
    String category,
    float volume,
    float pitch
) {
    public static final Codec<SoundInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("sound_id").forGetter(SoundInstance::sound),
        ExtraCodecs.NON_EMPTY_STRING.optionalFieldOf("category", SoundSource.MASTER.toString()).forGetter(SoundInstance::category),
        ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("volume", 1.0F).forGetter(SoundInstance::volume),
        ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("pitch", 1.0F).forGetter(SoundInstance::pitch)
    ).apply(instance, SoundInstance::new));

    public static SoundInstance of(SoundEvent sound) {
        return new SoundInstance(sound, SoundSource.MASTER.toString(), 1.0F, 1.0F);
    }

    public SoundInstance sound(SoundEvent sound) {
        return new SoundInstance(sound, category(), volume(), pitch());
    }

    public SoundInstance category(SoundSource category) {
        return new SoundInstance(sound(), category.toString(), volume(), pitch());
    }

    public SoundInstance volume(float volume) {
        return new SoundInstance(sound(), category(), volume, pitch());
    }

    public SoundInstance pitch(float pitch) {
        return new SoundInstance(sound(), category(), volume(), pitch);
    }

    public Holder<SoundEvent> getSound() {
        return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
    }

    public SoundSource getCategory() {
        try {
            return SoundSource.valueOf(category);
        } catch (IllegalArgumentException ignored) {
            return SoundSource.MASTER;
        }
    }

    public void playSound(ServerPlayer player) {
        player.connection.send(new ClientboundSoundPacket(getSound(), getCategory(), player.getX(), player.getY(), player.getZ(), volume, pitch, player.level().getRandom().nextLong()));
    }
}
