package xyz.uninenville.turfwars.kit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public record KitSelector(
    Identifier kit,
    float rotation
) {
    public static final Codec<KitSelector> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("kit").forGetter(KitSelector::kit),
        Codec.FLOAT.optionalFieldOf("rotation", 0F).forGetter(KitSelector::rotation)
    ).apply(instance, KitSelector::new));

    @Nullable
    public TurfWarsKit getKit() {
        return KitRegistry.getKit(kit);
    }
}
