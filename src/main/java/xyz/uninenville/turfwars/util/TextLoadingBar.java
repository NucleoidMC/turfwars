package xyz.uninenville.turfwars.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public record TextLoadingBar(
    String format,
    TextColor loaded,
    TextColor loading,
    TextColor notLoaded
) {
    public static final TextLoadingBar DEFAULT = new TextLoadingBar(
        "█████",
        TextColor.fromFormatting(Formatting.GREEN),
        TextColor.fromFormatting(Formatting.YELLOW),
        TextColor.fromFormatting(Formatting.GRAY)
    );

    public static final Codec<TextLoadingBar> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("format", DEFAULT.format()).forGetter(TextLoadingBar::format),
        TextColor.CODEC.optionalFieldOf("loaded", DEFAULT.loaded()).forGetter(TextLoadingBar::loaded),
        TextColor.CODEC.optionalFieldOf("loading", DEFAULT.loading()).forGetter(TextLoadingBar::loading),
        TextColor.CODEC.optionalFieldOf("not_loaded", DEFAULT.notLoaded()).forGetter(TextLoadingBar::notLoaded)
    ).apply(instance, TextLoadingBar::new));

    public boolean isEnabled() {
        return !format.isEmpty();
    }

    public MutableText get(float percent) {
        if (percent == 1.0F) {
            return Text.literal(format).withColor(loaded.getRgb());
        } else {
            MutableText chargingBar = Text.empty();
            for (int i = 0; i < format.length(); i++) {
                boolean isStepComplete = percent > (1.0F / format.length()) * i;
                chargingBar.append(Text.literal(format.substring(i, i + 1))
                    .withColor(isStepComplete ? loading.getRgb() : notLoaded.getRgb()));
            }

            return chargingBar;
        }
    }

}
