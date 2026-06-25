package xyz.uninenville.turfwars.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import xyz.uninenville.turfwars.TurfWars;
import xyz.uninenville.turfwars.config.TurfWarsConfig;
import xyz.uninenville.turfwars.game.TurfWarsGame;
import xyz.uninenville.turfwars.game.TurfWarsPhase;
import xyz.uninenville.turfwars.game.TurfWarsWaiting;
import xyz.uninenville.turfwars.kit.KitRegistry;
import xyz.uninenville.turfwars.kit.TurfWarsKit;
import xyz.nucleoid.plasmid.api.game.GameAttachment;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.plasmid.api.game.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TurfWarsCommand {
    private static final SuggestionProvider<ServerCommandSource> MAP_SUGGESTION_PROVIDER = (ctx, builder) -> {
        GameSpace gameSpace = GameSpaceManager.get().byWorld(ctx.getSource().getWorld());
        List<Identifier> maps = new ArrayList<>();
        if (gameSpace != null) {
            TurfWarsConfig config = (TurfWarsConfig) gameSpace.getMetadata().sourceConfig().value().config();
            maps.addAll(config.maps());
        }

        return CommandSource.suggestIdentifiers(maps, builder);
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("turfwars")
            .requires(TurfWarsCommand::isSourceInTurfWarsGame)

            .then(literal("team")
                .requires(ServerCommandSource::isExecutedByPlayer)

                .then(literal("switch")
                    .requires(TurfWarsCommand::isGameActive)

                    .executes(ctx -> switchTeam(ctx, ctx.getSource().getPlayerOrThrow()))
                )
            )

            .then(literal("kit")
                .requires(ServerCommandSource::isExecutedByPlayer)

                .then(literal("select")
                    .requires(Permissions.require("turfwars.command.kit.select", 2))

                    .then(argument("id", IdentifierArgumentType.identifier())
                        .suggests((ctx, builder) -> CommandSource.suggestIdentifiers(KitRegistry.getKitIdentifiers(), builder))
                        .executes(ctx -> selectKit(ctx, IdentifierArgumentType.getIdentifier(ctx, "id")))
                    )
                )
            )

            .then(literal("map")
                .requires(TurfWarsCommand::isGameWaiting)

                .then(literal("vote")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .requires(TurfWarsCommand::canMapsBeVoted)

                    .then(argument("id", IdentifierArgumentType.identifier())
                        .suggests(MAP_SUGGESTION_PROVIDER)
                        .executes(ctx -> voteForMap(ctx.getSource().getPlayerOrThrow(), IdentifierArgumentType.getIdentifier(ctx, "id")))
                    )
                )

                .then(literal("select")
                    .requires(Permissions.require("turfwars.command.map.select", 2))

                    .then(argument("id", IdentifierArgumentType.identifier())
                        .suggests(MAP_SUGGESTION_PROVIDER)
                        .executes(ctx -> selectMap(ctx, IdentifierArgumentType.getIdentifier(ctx, "id")))
                    )
                )
            )

            .then(literal("phase")
                .requires(TurfWarsCommand::isGameActive)
                .requires(Permissions.require("turfwars.command.phase", 2))

                .then(literal("set")
                    .then(argument("phase", StringArgumentType.word())
                        .suggests((ctx, builder) -> CommandSource.suggestMatching(Arrays.stream(TurfWarsPhase.values()).map(Enum::toString), builder))
                        .executes(TurfWarsCommand::setNextPhase)
                    )
                )

                .then(literal("next")
                    .executes(TurfWarsCommand::startNextPhase)
                )
            )
        );
    }

    private static boolean isSourceInTurfWarsGame(ServerCommandSource source) {
        GameSpace gameSpace = GameSpaceManager.get().byWorld(source.getWorld());
        if (gameSpace != null) {
            return gameSpace.getMetadata().sourceConfig().value().type() == GameType.get(TurfWars.id(TurfWars.MOD_ID));
        }

        return false;
    }

    private static <T> T getAttachment(ServerWorld world, GameAttachment<T> attachment) {
        GameSpace gameSpace = GameSpaceManager.get().byWorld(world);
        if (gameSpace != null) {
            return gameSpace.getAttachment(attachment);
        }

        return null;
    }

    private static boolean isGameWaiting(ServerCommandSource source) {
        return getAttachment(source.getWorld(), TurfWars.WAITING) != null;
    }

    private static boolean isGameActive(ServerCommandSource source) {
        return getAttachment(source.getWorld(), TurfWars.GAME) != null;
    }

    private static int switchTeam(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player) {
        TurfWarsGame game = getAttachment(ctx.getSource().getWorld(), TurfWars.GAME);
        if (game != null) {
            game.trySwitchTeamFor(player, true);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int selectKit(CommandContext<ServerCommandSource> ctx, Identifier id) {
        TurfWarsGame game = getAttachment(ctx.getSource().getWorld(), TurfWars.GAME);
        TurfWarsKit kit = KitRegistry.getKit(id);
        if (game != null) {
            game.getParticipant(ctx.getSource().getPlayer()).changeKit(kit);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static boolean canMapsBeVoted(ServerCommandSource source) {
        GameSpace gameSpace = GameSpaceManager.get().byWorld(source.getWorld());
        TurfWarsWaiting waiting = getAttachment(source.getWorld(), TurfWars.WAITING);
        if (gameSpace != null && waiting != null) {
            TurfWarsConfig config = (TurfWarsConfig) gameSpace.getMetadata().sourceConfig().value().config();
            return !config.randomMap() && config.maps().size() > 1 && !waiting.isMapSelected();
        }

        return false;
    }

    private static int voteForMap(ServerPlayerEntity player, Identifier map) {
        TurfWarsWaiting waiting = getAttachment(player.getEntityWorld(), TurfWars.WAITING);
        if (waiting != null) {
            waiting.voteForMap(player, map);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int selectMap(CommandContext<ServerCommandSource> ctx, Identifier mapId) {
        TurfWarsWaiting waiting = getAttachment(ctx.getSource().getWorld(), TurfWars.WAITING);
        if (waiting != null) {
            waiting.selectMap(mapId);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int setNextPhase(CommandContext<ServerCommandSource> ctx) {
        TurfWarsGame game = getAttachment(ctx.getSource().getWorld(), TurfWars.GAME);
        TurfWarsPhase phase = TurfWarsPhase.valueOf(StringArgumentType.getString(ctx, "phase"));
        if (game != null) {
            game.setPhase(phase);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int startNextPhase(CommandContext<ServerCommandSource> ctx) {
        TurfWarsGame game = getAttachment(ctx.getSource().getWorld(), TurfWars.GAME);
        if (game != null && !game.getPhase().isGameEndPhase()) {
            game.setPhase(game.getPhase().getNextPhase());
        }

        return Command.SINGLE_SUCCESS;
    }
}