package xyz.uninenville.turfwars.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.dynamic.Codecs;

public record SoundInstance(
    SoundEvent sound,
    String category,
    float volume,
    float pitch
) {
    public static final Codec<SoundInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Registries.SOUND_EVENT.getCodec().fieldOf("sound_id").forGetter(SoundInstance::sound),
        Codecs.NON_EMPTY_STRING.optionalFieldOf("category", SoundCategory.MASTER.toString()).forGetter(SoundInstance::category),
        Codecs.NON_NEGATIVE_FLOAT.optionalFieldOf("volume", 1.0F).forGetter(SoundInstance::volume),
        Codecs.NON_NEGATIVE_FLOAT.optionalFieldOf("pitch", 1.0F).forGetter(SoundInstance::pitch)
    ).apply(instance, SoundInstance::new));

    public static SoundInstance of(SoundEvent sound) {
        return new SoundInstance(sound, SoundCategory.MASTER.toString(), 1.0F, 1.0F);
    }

    public SoundInstance sound(SoundEvent sound) {
        return new SoundInstance(sound, category(), volume(), pitch());
    }

    public SoundInstance category(SoundCategory category) {
        return new SoundInstance(sound(), category.toString(), volume(), pitch());
    }

    public SoundInstance volume(float volume) {
        return new SoundInstance(sound(), category(), volume, pitch());
    }

    public SoundInstance pitch(float pitch) {
        return new SoundInstance(sound(), category(), volume(), pitch);
    }

    public RegistryEntry<SoundEvent> getSound() {
        return Registries.SOUND_EVENT.getEntry(sound);
    }

    public SoundCategory getCategory() {
        try {
            return SoundCategory.valueOf(category);
        } catch (IllegalArgumentException ignored) {
            return SoundCategory.MASTER;
        }
    }

    public void playSound(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new PlaySoundS2CPacket(getSound(), getCategory(), player.getX(), player.getY(), player.getZ(), volume, pitch, player.getEntityWorld().getRandom().nextLong()));
    }
}
