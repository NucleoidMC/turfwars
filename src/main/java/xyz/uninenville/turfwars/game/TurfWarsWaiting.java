package xyz.uninenville.turfwars.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import net.minecraft.world.rule.GameRules;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.*;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamList;
import xyz.nucleoid.plasmid.api.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.util.PlayerPos;
import xyz.uninenville.turfwars.TurfWars;
import xyz.uninenville.turfwars.config.TurfWarsConfig;
import xyz.uninenville.turfwars.map.TurfWarsMap;
import xyz.uninenville.turfwars.map.TurfWarsMapGenerator;
import xyz.uninenville.turfwars.mixin.plasmid.GameWaitingLobbyAccessor;

import java.util.*;

public class TurfWarsWaiting {
    private final GameSpace gameSpace;
    private final TurfWarsConfig config;
    private final GameWaitingLobby waitingLobby;
    private final TeamSelectionLobby teamSelectionLobby;
    private final GameTeamList teams;
    private ServerWorld world;
    private TurfWarsMap map;

    private final Map<UUID, Identifier> mapPreference = new Object2ObjectOpenHashMap<>();
    private Identifier mapId;

    public TurfWarsWaiting(GameActivity activity, TurfWarsConfig config, TurfWarsMap lobbyMap, ServerWorld world) {
        this.gameSpace = activity.getGameSpace();
        this.config = config;
        this.waitingLobby = GameWaitingLobby.addTo(activity, config.players());
        this.teams = config.getTeams();
        this.teamSelectionLobby = TeamSelectionLobby.addTo(activity, teams);
        this.map = lobbyMap;
        this.world = world;
    }

    public static GameOpenProcedure open(GameOpenContext<TurfWarsConfig> context) {
        TurfWarsConfig config = context.config();
        Identifier lobbyMapId = config.lobbyMap().isPresent() ? config.lobbyMap().get() : config.getRandomMap();
        TurfWarsMap lobbyMap = generateMap(lobbyMapId, context.server(), config.lobbyMap().isPresent());

        var worldConfig = new RuntimeWorldConfig()
            .setDimensionType(Fantasy.DEFAULT_DIM_TYPE)
            .setGenerator(lobbyMap.asGenerator(context.server()))
            .setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false)
            .setGameRule(GameRules.ADVANCE_TIME, false);

        return context.openWithWorld(worldConfig, (activity, world) -> {
            TurfWarsWaiting waiting = new TurfWarsWaiting(activity, config, lobbyMap, world);

            activity.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);
            activity.listen(GameActivityEvents.ENABLE, waiting::enableActivity);
            activity.listen(GameActivityEvents.DISABLE, waiting::disableActivity);
            activity.listen(GameActivityEvents.TICK, waiting::tick);
            activity.listen(GamePlayerEvents.ACCEPT, waiting::acceptPlayer);
            activity.listen(GamePlayerEvents.JOIN, waiting::addPlayer);
            activity.listen(GamePlayerEvents.LEAVE, waiting::removePlayer);
        });
    }

    private GameResult requestStart() {
        if (config.lobbyMap().isPresent()) {
            this.map = generateMap(isMapSelected() ? mapId : config.getRandomMap(), gameSpace.getServer(), false);
            var worldConfig = new RuntimeWorldConfig()
                .setDimensionType(Fantasy.DEFAULT_DIM_TYPE)
                .setGenerator(map.asGenerator(gameSpace.getServer()))
                .setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false)
                .setGameRule(GameRules.ADVANCE_TIME, false);

            this.world = gameSpace.getWorlds().add(worldConfig);
        }

        Multimap<GameTeamKey, ServerPlayerEntity> players = HashMultimap.create();
        teamSelectionLobby.allocate(gameSpace.getPlayers(), players::put);

        return TurfWarsGame.startGame(gameSpace, config, map, world, players, teams);
    }

    private void enableActivity() {
        gameSpace.setAttachment(TurfWars.WAITING, this);
    }

    private void disableActivity() {
        gameSpace.setAttachment(TurfWars.WAITING, null);
    }

    private void tick() {
        gameSpace.getPlayers().forEach(player -> {
            if (!map.template.getBounds().contains(player.getBlockPos())) {
                PlayerPos spawn = map.getRandomSpawn();
                player.teleport(world, spawn.x(), spawn.y(), spawn.z(), Set.of(), spawn.yaw(), spawn.pitch(), true);
            }
        });

        if (mapId == null) {
            if (((GameWaitingLobbyAccessor) (Object) waitingLobby).getCountdownStart() != -1) {
                long time = gameSpace.getTime();
                long remainingSeconds = ((GameWaitingLobbyAccessor) (Object) waitingLobby).invokeGetRemainingTicks(time) / 20;

                if (remainingSeconds <= 5) {
                    if (!config.randomMap() && !mapPreference.isEmpty()) {
                        selectMap(getWinningMap());
                    }
                }
            }
        }
    }

    private JoinAcceptorResult acceptPlayer(JoinAcceptor offer) {
        PlayerPos spawn = map.getRandomSpawn();
        return offer.teleport(world, new Vec3d(spawn.x(), spawn.y(), spawn.z()), spawn.yaw(), spawn.pitch())
            .thenRunForEach(player -> player.changeGameMode(GameMode.ADVENTURE));
    }

    private void addPlayer(ServerPlayerEntity player) {
        player.getEntityWorld().getServer().getCommandManager().sendCommandTree(player);

        if (!config.randomMap() && mapId == null) {
            player.sendMessage(Text.translatable("turfwars.map.vote.format", getMaps()));
        }
    }

    private Text getMaps() {
        MutableText maps = Text.empty();
        for (Identifier map : config.maps()) {
            maps.append(Text.translatable("turfwars.map.vote.candinate",
                Text.of(map),
                Text.of(String.valueOf(config.maps().indexOf(map) + 1)),
                Text.translatable(map.getNamespace() + ".map." + map.getPath()),
                Text.of(String.valueOf(mapPreference.values().stream().filter(map::equals).count()))
            ).append(map != config.maps().getLast() ? Text.translatable("turfwars.map.vote.separator") : Text.empty()));
        }

        return maps;
    }

    private void removePlayer(ServerPlayerEntity player) {
        player.getEntityWorld().getServer().getCommandManager().sendCommandTree(player);
        mapPreference.remove(player.getUuid());
    }

    public Text getMapName(Identifier mapId) {
        return Text.translatable(mapId.getNamespace() + ".map." + mapId.getPath());
    }

    public boolean isMapSelected() {
        return mapId != null;
    }

    public void selectMap(Identifier map) {
        this.mapId = map;
        gameSpace.getPlayers().sendMessage(Text.translatable("turfwars.map.selected", getMapName(this.mapId)));
    }

    public void voteForMap(ServerPlayerEntity player, Identifier mapId) {
        mapPreference.put(player.getUuid(), mapId);
        player.sendMessage(Text.translatable("turfwars.map.vote.success", getMapName(mapId)));
    }

    public Identifier getWinningMap() {
        List<Identifier> winners = new ArrayList<>();
        int winningVotes = 0;

        for (Identifier identifier : mapPreference.values()) {
            int votes = mapPreference.values().stream().filter(identifier::equals).toList().size();

            if (votes > winningVotes) {
                winners.clear();
                winningVotes = votes;
                winners.add(identifier);
            } else if (votes == winningVotes) {
                winners.add(identifier);
            }
        }

        return winners.get(Random.create().nextInt(winners.size()));
    }

    private static TurfWarsMap generateMap(Identifier mapId, MinecraftServer server, boolean lobby) {
        return new TurfWarsMapGenerator(mapId).create(server, lobby);
    }
}
