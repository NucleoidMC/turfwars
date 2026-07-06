package xyz.uninenville.turfwars.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public record LoadingBarComponent(
    String format,
    TextColor loaded,
    TextColor loading,
    TextColor notLoaded
) {
    public static final LoadingBarComponent DEFAULT = new LoadingBarComponent(
        "█████",
        TextColor.fromLegacyFormat(ChatFormatting.GREEN),
        TextColor.fromLegacyFormat(ChatFormatting.YELLOW),
        TextColor.fromLegacyFormat(ChatFormatting.GRAY)
    );

    public static final Codec<LoadingBarComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("format", DEFAULT.format()).forGetter(LoadingBarComponent::format),
        TextColor.CODEC.optionalFieldOf("loaded", DEFAULT.loaded()).forGetter(LoadingBarComponent::loaded),
        TextColor.CODEC.optionalFieldOf("loading", DEFAULT.loading()).forGetter(LoadingBarComponent::loading),
        TextColor.CODEC.optionalFieldOf("not_loaded", DEFAULT.notLoaded()).forGetter(LoadingBarComponent::notLoaded)
    ).apply(instance, LoadingBarComponent::new));

    public boolean isEnabled() {
        return !format.isEmpty();
    }

    public MutableComponent get(float percent) {
        if (percent == 1.0F) {
            return Component.literal(format).withColor(loaded.getValue());
        } else {
            MutableComponent chargingBar = Component.empty();
            for (int i = 0; i < format.length(); i++) {
                boolean isStepComplete = percent > (1.0F / format.length()) * i;
                chargingBar.append(Component.literal(format.substring(i, i + 1))
                    .withColor(isStepComplete ? loading.getValue() : notLoaded.getValue()));
            }

            return chargingBar;
        }
    }

}
