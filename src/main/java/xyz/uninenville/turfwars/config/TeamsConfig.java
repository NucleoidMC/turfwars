package xyz.uninenville.turfwars.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;

public record TeamsConfig(
    GameTeamConfig blueTeam,
    GameTeamConfig redTeam
) {
    public static final GameTeamConfig BLUE = GameTeamConfig.builder()
        .setName(Text.translatable("turfwars.team.blue"))
        .setColors(GameTeamConfig.Colors.from(DyeColor.BLUE))
        .setFriendlyFire(false)
        .setCollision(AbstractTeam.CollisionRule.PUSH_OTHER_TEAMS)
        .setNameTagVisibility(AbstractTeam.VisibilityRule.HIDE_FOR_OTHER_TEAMS)
        .build();
    public static final GameTeamConfig RED = GameTeamConfig.builder()
        .setName(Text.translatable("turfwars.team.red"))
        .setColors(GameTeamConfig.Colors.from(DyeColor.RED))
        .setFriendlyFire(false)
        .setCollision(AbstractTeam.CollisionRule.PUSH_OTHER_TEAMS)
        .setNameTagVisibility(AbstractTeam.VisibilityRule.HIDE_FOR_OTHER_TEAMS)
        .build();
    public static final TeamsConfig DEFAULT = new TeamsConfig(BLUE, RED);

    public static final Codec<TeamsConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        GameTeamConfig.CODEC.optionalFieldOf("blue", DEFAULT.blueTeam()).forGetter(TeamsConfig::blueTeam),
        GameTeamConfig.CODEC.optionalFieldOf("red", DEFAULT.redTeam()).forGetter(TeamsConfig::redTeam)
    ).apply(instance, TeamsConfig::new));
}
