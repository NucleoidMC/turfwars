package xyz.uninenville.turfwars.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import xyz.nucleoid.plasmid.api.game.GameAttachment;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.uninenville.turfwars.TurfWars;
import xyz.uninenville.turfwars.config.TurfWarsConfig;
import xyz.uninenville.turfwars.game.TurfWarsGame;
import xyz.uninenville.turfwars.game.TurfWarsPhase;
import xyz.uninenville.turfwars.game.TurfWarsWaiting;
import xyz.uninenville.turfwars.kit.KitRegistry;
import xyz.uninenville.turfwars.kit.TurfWarsKit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TurfWarsCommand {
    private static final SuggestionProvider<CommandSourceStack> MAP_SUGGESTION_PROVIDER = (ctx, builder) -> {
        GameSpace gameSpace = GameSpaceManager.get().byLevel(ctx.getSource().getLevel());
        List<Identifier> maps = new ArrayList<>();
        if (gameSpace != null) {
            TurfWarsConfig config = (TurfWarsConfig) gameSpace.getMetadata().sourceConfig().value().config();
            maps.addAll(config.maps());
        }

        return SharedSuggestionProvider.suggestResource(maps, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("turfwars")
            .requires(TurfWarsCommand::isSourceInTurfWarsGame)
            .then(literal("team")
                .requires(CommandSourceStack::isPlayer)
                .requires(TurfWarsCommand::isGameActive)
                .then(literal("switch")
                    .executes(ctx -> switchTeam(ctx, ctx.getSource().getPlayerOrException()))
                )
            )
            .then(literal("kit")
                .requires(CommandSourceStack::isPlayer)
                .then(literal("select")
                    .requires(PermissionPredicates.require(TurfWars.id("turfwars.command.kit.select"), PermissionLevel.GAMEMASTERS))
                    .then(argument("id", IdentifierArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(KitRegistry.getKitIdentifiers(), builder))
                        .executes(ctx -> selectKit(ctx, IdentifierArgument.getId(ctx, "id")))
                    )
                )
            )
            .then(literal("map")
                .requires(TurfWarsCommand::isGameWaiting)
                .then(literal("vote")
                    .requires(CommandSourceStack::isPlayer)
                    .requires(TurfWarsCommand::canMapsBeVoted)
                    .then(argument("id", IdentifierArgument.id())
                        .suggests(MAP_SUGGESTION_PROVIDER)
                        .executes(ctx -> voteForMap(ctx.getSource().getPlayerOrException(), IdentifierArgument.getId(ctx, "id")))
                    )
                )
                .then(literal("select")
                    .requires(PermissionPredicates.require(TurfWars.id("turfwars.command.map.select"), PermissionLevel.GAMEMASTERS))
                    .then(argument("id", IdentifierArgument.id())
                        .suggests(MAP_SUGGESTION_PROVIDER)
                        .executes(ctx -> selectMap(ctx, IdentifierArgument.getId(ctx, "id")))
                    )
                )
            )
            .then(literal("phase")
                .requires(TurfWarsCommand::isGameActive)
                .requires(PermissionPredicates.require(TurfWars.id("turfwars.command.phase"), PermissionLevel.GAMEMASTERS))
                .then(literal("set")
                    .then(argument("phase", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(TurfWarsPhase.values()).map(Enum::toString), builder))
                        .executes(TurfWarsCommand::setNextPhase)
                    )
                )
                .then(literal("next")
                    .executes(TurfWarsCommand::startNextPhase)
                )
            )
        );
    }

    private static boolean isSourceInTurfWarsGame(CommandSourceStack source) {
        GameSpace gameSpace = GameSpaceManager.get().byLevel(source.getLevel());
        if (gameSpace != null) {
            return gameSpace.getMetadata().sourceConfig().value().type() == GameType.get(TurfWars.id(TurfWars.MOD_ID));
        }

        return false;
    }

    private static <T> T getAttachment(ServerLevel level, GameAttachment<T> attachment) {
        GameSpace gameSpace = GameSpaceManager.get().byLevel(level);
        if (gameSpace != null) {
            return gameSpace.getAttachment(attachment);
        }

        return null;
    }

    private static boolean isGameWaiting(CommandSourceStack source) {
        return getAttachment(source.getLevel(), TurfWars.WAITING) != null;
    }

    private static boolean isGameActive(CommandSourceStack source) {
        return getAttachment(source.getLevel(), TurfWars.GAME) != null;
    }

    private static int switchTeam(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        TurfWarsGame game = getAttachment(ctx.getSource().getLevel(), TurfWars.GAME);
        if (game != null) {
            game.trySwitchTeamFor(player, true);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int selectKit(CommandContext<CommandSourceStack> ctx, Identifier id) {
        TurfWarsGame game = getAttachment(ctx.getSource().getLevel(), TurfWars.GAME);
        TurfWarsKit kit = KitRegistry.getKit(id);
        if (game != null) {
            game.getParticipant(ctx.getSource().getPlayer()).changeKit(kit);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static boolean canMapsBeVoted(CommandSourceStack source) {
        GameSpace gameSpace = GameSpaceManager.get().byLevel(source.getLevel());
        TurfWarsWaiting waiting = getAttachment(source.getLevel(), TurfWars.WAITING);
        if (gameSpace != null && waiting != null) {
            TurfWarsConfig config = (TurfWarsConfig) gameSpace.getMetadata().sourceConfig().value().config();
            return !config.randomMap() && config.maps().size() > 1 && !waiting.isMapSelected();
        }

        return false;
    }

    private static int voteForMap(ServerPlayer player, Identifier map) {
        TurfWarsWaiting waiting = getAttachment(player.level(), TurfWars.WAITING);
        if (waiting != null) {
            waiting.voteForMap(player, map);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int selectMap(CommandContext<CommandSourceStack> ctx, Identifier mapId) {
        TurfWarsWaiting waiting = getAttachment(ctx.getSource().getLevel(), TurfWars.WAITING);
        if (waiting != null) {
            waiting.selectMap(mapId);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int setNextPhase(CommandContext<CommandSourceStack> ctx) {
        TurfWarsGame game = getAttachment(ctx.getSource().getLevel(), TurfWars.GAME);
        TurfWarsPhase phase = TurfWarsPhase.valueOf(StringArgumentType.getString(ctx, "phase"));
        if (game != null) {
            game.setPhase(phase);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int startNextPhase(CommandContext<CommandSourceStack> ctx) {
        TurfWarsGame game = getAttachment(ctx.getSource().getLevel(), TurfWars.GAME);
        if (game != null && !game.getPhase().isGameEndPhase()) {
            game.setPhase(game.getPhase().getNextPhase());
        }

        return Command.SINGLE_SUCCESS;
    }
}