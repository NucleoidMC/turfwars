package xyz.uninenville.turfwars.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamList;
import xyz.nucleoid.plasmid.api.game.stats.GameStatisticBundle;
import xyz.uninenville.turfwars.TurfWars;
import xyz.uninenville.turfwars.game.TeamKeys;

import java.util.List;
import java.util.Optional;

public record TurfWarsConfig(
    WaitingLobbyConfig players,
    TurfWarsGameConfig game,
    TeamsConfig teams,
    Optional<Identifier> lobbyMap,
    List<Identifier> maps,
    boolean randomMap,
    String statisticBundleNamespace
) {

    public static final MapCodec<TurfWarsConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(TurfWarsConfig::players),
        TurfWarsGameConfig.CODEC.optionalFieldOf("game", TurfWarsGameConfig.DEFAULT).forGetter(TurfWarsConfig::game),
        TeamsConfig.CODEC.optionalFieldOf("teams", TeamsConfig.DEFAULT).forGetter(TurfWarsConfig::teams),
        Identifier.CODEC.optionalFieldOf("lobby_map").forGetter(TurfWarsConfig::lobbyMap),
        Identifier.CODEC.listOf().fieldOf("maps").forGetter(TurfWarsConfig::maps),
        Codec.BOOL.optionalFieldOf("random_map", true).forGetter(TurfWarsConfig::randomMap),
        GameStatisticBundle.NAMESPACE_CODEC.optionalFieldOf("statistic_bundle_namespace", TurfWars.MOD_ID).forGetter(TurfWarsConfig::statisticBundleNamespace)
    ).apply(instance, TurfWarsConfig::new));

    public GameTeamList getTeams() {
        return new GameTeamList(List.of(
            new GameTeam(TeamKeys.BLUE, teams.blueTeam()),
            new GameTeam(TeamKeys.RED, teams.redTeam())
        ));
    }

    public Identifier getRandomMap() {
        return maps.get(Random.create().nextInt(maps().size()));
    }
}
