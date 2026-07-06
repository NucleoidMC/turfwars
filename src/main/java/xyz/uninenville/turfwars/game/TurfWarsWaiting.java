package xyz.uninenville.turfwars.game;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.*;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamList;
import xyz.nucleoid.plasmid.api.game.common.team.TeamSelectionLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
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
    private ServerLevel level;
    private TurfWarsMap map;

    private final Map<UUID, Identifier> mapPreference = new Object2ObjectOpenHashMap<>();
    private Identifier mapId;

    public TurfWarsWaiting(GameActivity activity, TurfWarsConfig config, TurfWarsMap lobbyMap, ServerLevel level) {
        this.gameSpace = activity.getGameSpace();
        this.config = config;
        this.waitingLobby = GameWaitingLobby.addTo(activity, config.players());
        this.teams = config.getTeams();
        this.teamSelectionLobby = TeamSelectionLobby.addTo(activity, teams);
        this.map = lobbyMap;
        this.level = level;
    }

    public static GameOpenProcedure open(GameOpenContext<TurfWarsConfig> context) {
        TurfWarsConfig config = context.config();
        Identifier lobbyMapId = config.lobbyMap().isPresent() ? config.lobbyMap().get() : config.getRandomMap();
        TurfWarsMap lobbyMap = generateMap(lobbyMapId, context.server(), config.lobbyMap().isPresent());

        var levelConfig = new RuntimeLevelConfig()
            .setDimensionType(Fantasy.DEFAULT_DIM_TYPE)
            .setGenerator(lobbyMap.asGenerator(context.server()))
            .setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false)
            .setGameRule(GameRules.ADVANCE_TIME, false);

        return context.openWithLevel(levelConfig, (activity, level) -> {
            TurfWarsWaiting waiting = new TurfWarsWaiting(activity, config, lobbyMap, level);

            activity.listen(GameActivityEvents.ENABLE, waiting::enableActivity);
            activity.listen(GameActivityEvents.DISABLE, waiting::disableActivity);
            activity.listen(GameActivityEvents.TICK, waiting::tick);
            activity.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);

            activity.listen(GamePlayerEvents.JOIN, waiting::addPlayer);
            activity.listen(GamePlayerEvents.LEAVE, waiting::removePlayer);
            activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
            activity.listen(GamePlayerEvents.ACCEPT, waiting::acceptPlayer);
        });
    }

    private GameResult requestStart() {
        if (config.lobbyMap().isPresent()) {
            this.map = generateMap(isMapSelected() ? mapId : config.getRandomMap(), gameSpace.getServer(), false);
            var levelConfig = new RuntimeLevelConfig()
                .setDimensionType(Fantasy.DEFAULT_DIM_TYPE)
                .setGenerator(map.asGenerator(gameSpace.getServer()))
                .setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false)
                .setGameRule(GameRules.ADVANCE_TIME, false);

            this.level = gameSpace.getLevels().add(levelConfig);
        }

        Multimap<GameTeamKey, ServerPlayer> players = HashMultimap.create();
        teamSelectionLobby.allocate(gameSpace.getPlayers(), players::put);

        return TurfWarsGame.startGame(gameSpace, config, map, level, players, teams);
    }

    private void enableActivity() {
        gameSpace.setAttachment(TurfWars.WAITING, this);
    }

    private void disableActivity() {
        gameSpace.setAttachment(TurfWars.WAITING, null);
    }

    private void tick() {
        gameSpace.getPlayers().forEach(player -> {
            if (!map.template.getBounds().contains(player.blockPosition())) {
                PlayerPos spawn = map.getRandomSpawn();
                player.teleportTo(level, spawn.x(), spawn.y(), spawn.z(), Set.of(), spawn.yaw(), spawn.pitch(), true);
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
        return offer.teleport(level, new Vec3(spawn.x(), spawn.y(), spawn.z()), spawn.yaw(), spawn.pitch()) //fixme level null?
            .thenRunForEach(player -> player.setGameMode(GameType.ADVENTURE));
    }

    private void addPlayer(ServerPlayer player) {
        level.getServer().getCommands().sendCommands(player);

        if (!config.randomMap() && mapId == null) {
            player.sendSystemMessage(Component.translatable("turfwars.map.vote.format", getMaps()));
        }
    }

    private Component getMaps() {
        MutableComponent maps = Component.empty();
        for (Identifier map : config.maps()) {
            maps.append(Component.translatable("turfwars.map.vote.candinate",
                Component.translationArg(map),
                Component.nullToEmpty(String.valueOf(config.maps().indexOf(map) + 1)),
                Component.translatable(map.getNamespace() + ".map." + map.getPath()),
                Component.nullToEmpty(String.valueOf(mapPreference.values().stream().filter(map::equals).count()))
            ).append(map != config.maps().getLast() ? Component.translatable("turfwars.map.vote.separator") : Component.empty()));
        }

        return maps;
    }

    private void removePlayer(ServerPlayer player) {
        level.getServer().getCommands().sendCommands(player);
        mapPreference.remove(player.getUUID());
    }

    public Component getMapName(Identifier mapId) {
        return Component.translatable(mapId.getNamespace() + ".map." + mapId.getPath());
    }

    public boolean isMapSelected() {
        return mapId != null;
    }

    public void selectMap(Identifier map) {
        this.mapId = map;
        gameSpace.getPlayers().sendMessage(Component.translatable("turfwars.map.selected", getMapName(this.mapId)));
    }

    public void voteForMap(ServerPlayer player, Identifier mapId) {
        mapPreference.put(player.getUUID(), mapId);
        player.sendSystemMessage(Component.translatable("turfwars.map.vote.success", getMapName(mapId)));
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

        return winners.get(RandomSource.create().nextInt(winners.size()));
    }

    private static TurfWarsMap generateMap(Identifier mapId, MinecraftServer server, boolean lobby) {
        return new TurfWarsMapGenerator(mapId).create(server, lobby);
    }
}
