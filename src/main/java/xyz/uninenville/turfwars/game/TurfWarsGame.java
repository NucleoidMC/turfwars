package xyz.uninenville.turfwars.game;

import com.google.common.collect.Multimap;
import eu.pb4.sidebars.api.Sidebar;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Language;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamList;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.stats.GameStatisticBundle;
import xyz.nucleoid.plasmid.api.game.stats.StatisticMap;
import xyz.nucleoid.plasmid.api.util.ColoredItems;
import xyz.nucleoid.plasmid.api.util.PlayerPos;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.item.ItemPickupEvent;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;
import xyz.nucleoid.stimuli.event.projectile.ArrowFireEvent;
import xyz.nucleoid.stimuli.event.projectile.ProjectileHitEvent;
import xyz.uninenville.turfwars.TurfWars;
import xyz.uninenville.turfwars.attachment.ModAttachments;
import xyz.uninenville.turfwars.config.TurfWarsConfig;
import xyz.uninenville.turfwars.map.TurfWarsMap;
import xyz.uninenville.turfwars.mixin.ItemEntityAccessor;
import xyz.uninenville.turfwars.mixin.PersistentProjectileEntityAccessor;
import xyz.uninenville.turfwars.util.ColoredBlockUtil;

import java.util.List;
import java.util.Set;

public class TurfWarsGame {
    public final GameSpace gameSpace;
    public final TurfWarsConfig config;
    public final TurfWarsMap map;
    public final ServerWorld world;
    public final TeamManager teamManager;
    public final GameStatisticBundle statistics;
    public final Sidebar sidebar;
    public final TimedItemManager timedItemManager;
    public final TimedEffectManager timedEffectManager;

    private final Object2ObjectMap<PlayerRef, TurfWarsParticipant> participants = new Object2ObjectOpenHashMap<>();

    private final TurfWarsTeam blueTeam;
    private final TurfWarsTeam redTeam;
    private TurfWarsPhase phase;
    private long phaseStartTime;
    private long phaseDuration;
    private int linesPerKill = 0;

    public TurfWarsGame(GameActivity activity, TurfWarsConfig config, TurfWarsMap map, ServerWorld world, Multimap<GameTeamKey, ServerPlayerEntity> teamPrefrences, GameTeamList teams) {
        this.gameSpace = activity.getGameSpace();
        this.config = config;
        this.map = map;
        this.world = world;
        this.teamManager = TeamManager.addTo(activity);
        this.blueTeam = new TurfWarsTeam(this, teams.byKey(TeamKeys.BLUE));
        this.redTeam = new TurfWarsTeam(this, teams.byKey(TeamKeys.RED));
        this.statistics = gameSpace.getStatistics().bundle(config.statisticBundleNamespace());
        this.sidebar = GlobalWidgets.addTo(activity).addSidebar(Text.translatable("turfwars.sidebar.title"));
        this.timedItemManager = TimedItemManager.addTo(activity, teamManager);
        this.timedEffectManager = TimedEffectManager.addTo(activity, map.getRegions());

        teamManager.addTeams(teams);
        for (GameTeamKey team : teamPrefrences.keySet()) {
            for (ServerPlayerEntity player : teamPrefrences.get(team)) {
                this.addParticipant(player, team);
            }
        }

        setPhase(TurfWarsPhase.GAME_START_PHASE);
    }

    public static GameResult startGame(GameSpace gameSpace, TurfWarsConfig config, TurfWarsMap map, ServerWorld world, Multimap<GameTeamKey, ServerPlayerEntity> players, GameTeamList teams) {
        gameSpace.setActivity(activity -> {
            TurfWarsGame game = new TurfWarsGame(activity, config, map, world, players, teams);

            activity.allow(GameRuleType.allOf(
                GameRuleType.PVP
            ));
            activity.deny(GameRuleType.allOf(
                GameRuleType.FALL_DAMAGE, GameRuleType.HUNGER, GameRuleType.CRAFTING,
                GameRuleType.THROW_ITEMS, GameRuleType.MODIFY_ARMOR, GameRuleType.PORTALS
            ));

            activity.listen(GameActivityEvents.ENABLE, game::enableActivity);
            activity.listen(GameActivityEvents.DISABLE, game::disableActivity);
            activity.listen(GameActivityEvents.TICK, game::tick);

            activity.listen(GamePlayerEvents.ACCEPT, game::acceptPlayer);
            activity.listen(GamePlayerEvents.ADD, game::addPlayer);
            activity.listen(GamePlayerEvents.REMOVE, game::removePlayer);

            activity.listen(BlockUseEvent.EVENT, game::onUseBlock);
            activity.listen(BlockPlaceEvent.BEFORE, game::onBlockPlace);
            activity.listen(BlockBreakEvent.EVENT, game::onBlockBreak);
            activity.listen(PlayerDamageEvent.EVENT, game::onPlayerDamage);
            activity.listen(PlayerAttackEntityEvent.EVENT, game::onPlayerAttack);
            activity.listen(ArrowFireEvent.EVENT, game::onArrowFire);
            activity.listen(ProjectileHitEvent.ENTITY, game::onProjectileHitEntity);
            activity.listen(ProjectileHitEvent.BLOCK, game::onProjectileHitBlock);
            activity.listen(PlayerDeathEvent.EVENT, game::onPlayerDeath);
            activity.listen(ItemPickupEvent.EVENT, game::onItemPickup);
        });

        return GameResult.ok();
    }

    private void enableActivity() {
        gameSpace.setAttachment(TurfWars.GAME, this);
        map.spawnKitSelectorEntities(this, world);
    }

    private void disableActivity() {
        gameSpace.setAttachment(TurfWars.GAME, null);
    }

    private JoinAcceptorResult acceptPlayer(JoinAcceptor offer) {
        return offer.teleport(world, map.getRegion(TurfWarsMap.SPECTATOR_SPAWN).center()).thenRunForEach(this::addPlayer);
    }

    private void addPlayer(ServerPlayerEntity player) {
        PlayerRef playerRef = PlayerRef.of(player);
        JoinIntent intent = gameSpace.getPlayers().participants().contains(playerRef) ? JoinIntent.PLAY : JoinIntent.SPECTATE;

        if (intent.canPlay() && (phase.isGameStartPhase() || config.game().allowJoinAfterStart())) {
            if (!participants.containsKey(playerRef)) {
                addParticipant(player, teamManager.getSmallestTeam());
            }

            getParticipant(player).spawn();
        } else {
            player.changeGameMode(GameMode.SPECTATOR);
            teamManager.removePlayer(playerRef);
            participants.remove(playerRef);

            if (intent.canPlay()) {
                gameSpace.getPlayers().modifyIntent(player, JoinIntent.SPECTATE);
            }
        }

        player.getEntityWorld().getServer().getCommandManager().sendCommandTree(player);
        sidebar.addPlayer(player);
    }

    private void addParticipant(ServerPlayerEntity player, GameTeamKey teamKey) {
        participants.put(PlayerRef.of(player), new TurfWarsParticipant(gameSpace, PlayerRef.of(player), teamKey.equals(TeamKeys.BLUE) ? getBlueTeam() : getRedTeam(), this));
        teamManager.addPlayerTo(player, teamKey);
    }

    private void removePlayer(ServerPlayerEntity player) {
        player.getEntityWorld().getServer().getCommandManager().sendCommandTree(player);
        sidebar.removePlayer(player);
    }

    private void tick() {
        // Tick game phases
        if (shouldStartNextPhase()) {
            // Close game if game end phase is ending
            if (phase.isGameEndPhase()) {
                gameSpace.close(GameCloseReason.FINISHED);
                return;
            } else {
                setPhase(phase.getNextPhase());
            }
        }

        // Tick sidebar
        if (gameSpace.getTime() % 20 == 0) {
            long secondsUntilPhaseEnds = (phaseStartTime + phaseDuration - gameSpace.getTime()) / 20;
            long minutes = secondsUntilPhaseEnds / 60;
            long seconds = secondsUntilPhaseEnds % 60;

            sidebar.set(b -> {
                b.add(Text.translatable("turfwars.sidebar.phase", getPhase().getName(), Text.of(String.format("%02d:%02d", minutes, seconds))));
                b.add(Text.empty());
                b.add(Text.translatable("turfwars.sidebar.score", getBlueTeam().getName(), Text.of(String.valueOf(getBlueTeam().getScore()))));
                b.add(Text.translatable("turfwars.sidebar.score", getRedTeam().getName(), Text.of(String.valueOf(getRedTeam().getScore()))));

                if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                    b.add(Text.empty());
                    b.add(Text.literal("Time played: " + (gameSpace.getTime() / 20) + "s"));
                }
            });
        }

        // Tick players
        participants.values().forEach(TurfWarsParticipant::tick);

        // Tick spectators
        for (ServerPlayerEntity player : gameSpace.getPlayers().spectators()) {
            if (!map.getRegion(TurfWarsMap.PLAY_AREA).contains(player.getBlockPos())) {
                PlayerPos pos = map.getRandomSpectatorSpawn();
                player.teleport(world, pos.x(), pos.y(), pos.z(), Set.of(), pos.yaw(), pos.pitch(), false);
            }
        }
    }

    public boolean shouldStartNextPhase() {
        return (phaseStartTime + phaseDuration) <= gameSpace.getTime();
    }

    public TurfWarsPhase getPhase() {
        return this.phase;
    }

    public void setPhase(TurfWarsPhase phase) {
        this.phase = phase;
        this.phaseStartTime = gameSpace.getTime();
        this.phaseDuration = getPhaseDuration();

        if (phase.isGameStartPhase()) {
            onGameStart();
        } else if (phase.isBuildPhase()) {
            onStartBuildPhase();
        } else if (phase.isCombatPhase()) {
            this.linesPerKill += 1;
        }

        Text message = getPhaseStartMessage();
        if (message != null) {
            gameSpace.getPlayers().sendMessage(getPhaseStartMessage());
        }
    }

    private long getPhaseDuration() {
        return getPhaseDuration(phase);
    }

    private long getPhaseDuration(TurfWarsPhase phase) {
        return switch (phase) {
            case GAME_START_PHASE -> config.game().gameStartTime();
            case GAME_END_PHASE -> config.game().gameEndTime();
            case INITIAL_BUILD -> config.game().initialBuildTime();
            case COMBAT -> config.game().combatTime();
            case BUILD -> config.game().buildTime();
        };
    }

    public Text getPhaseStartMessage() {
        String key = "turfwars.phase." + phase.toString().toLowerCase() + ".start";
        if (Language.getInstance().hasTranslation(key)) {
            return Text.translatable(
                "turfwars.phase." + phase.toString().toLowerCase() + ".start",
                phase.getName(),
                Text.of(String.valueOf(getPhaseDuration() / 20)),
                Text.of(String.valueOf(linesPerKill))
            );
        }

        return null;
    }

    private void onGameStart() {
        map.placeSpawnBarriers(world);
        timedItemManager.setShouldTick(false);
        timedEffectManager.setShouldTick(false);
    }

    private void onStartBuildPhase() {
        if (phase.isInitialBuildPhase()) {
            map.removeSpawnBarriers(world);
            timedItemManager.setShouldTick(true);
            timedEffectManager.setShouldTick(true);
        }

        for (PlayerRef ref : participants.keySet()) {
            ref.ifOnline(gameSpace, player -> {
                TurfWarsParticipant participant = getParticipant(player);
                int amount = phase.isInitialBuildPhase() ? config.game().initialBuildTimeWoolAmount()
                    : config.game().buildTimeWoolAmount();

                participant.giveBuildBlocks(amount);
            });
        }
    }

    private ActionResult onUseBlock(ServerPlayerEntity player, Hand hand, BlockHitResult blockHitResult) {
        BlockState block = world.getBlockState(blockHitResult.getBlockPos());

        // Protect blocks that can be interacted with/changed
        if (block.getBlock() instanceof BlockWithEntity
            || block.isIn(BlockTags.DOORS)
            || block.isIn(BlockTags.TRAPDOORS)
            || block.isIn(BlockTags.FLOWER_POTS)
        ) {
            player.currentScreenHandler.updateToClient();
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    private EventResult onBlockPlace(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, ItemUsageContext context) {
        TurfWarsTeam team = getTeamInControlOf(pos);
        boolean isOwnTurf = getTeam(player).getTurf().contains(pos);
        boolean canBuildInEnemyTurf = getParticipant(player).getKit().canBuildInEnemyTurf();

        Text message = null;
        EventResult result = EventResult.PASS;
        if (phase.isGameEndPhase()) {
            result = EventResult.DENY;
        } else if (team == null) {
            message = Text.translatable("turfwars.game.error." + (canBuildInEnemyTurf ? "can_only_build_in_turf" : "can_only_build_in_own_turf"));
        } else if ((isOwnTurf || phase.isCombatPhase() && canBuildInEnemyTurf) && !team.getBuildBlocks().contains(state.getBlock())) {
            message = Text.translatable("turfwars.game.error.can_only_use_building_blocks");
        } else if (!isOwnTurf) {
            if (!phase.isCombatPhase() && canBuildInEnemyTurf) {
                message = Text.translatable("turfwars.game.error.can_only_build_in_enemy_turf_during_combat");
            } else if (!canBuildInEnemyTurf) {
                message = Text.translatable("turfwars.game.error.can_only_build_in_own_turf");
            }
        }

        if (message != null) {
            player.sendMessage(message);
            result = EventResult.DENY;
        }

        player.currentScreenHandler.updateToClient();

        return result;
    }

    private EventResult onBlockBreak(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        TurfWarsTeam team = getTeamInControlOf(pos);
        boolean isOwnTurf = getTeam(player).getTurf().contains(pos);
        boolean canBreakEnemyForts = getParticipant(player).getKit().canBreakEnemyForts();
        Block block = world.getBlockState(pos).getBlock();

        Text message = null;
        EventResult result = EventResult.PASS;
        if (phase.isGameEndPhase()) {
            result = EventResult.DENY;
        } else if (team == null) {
            message = Text.translatable("turfwars.game.error." + (canBreakEnemyForts ? "can_only_break_in_turf" : "can_only_break_in_own_turf"));
        } else if ((isOwnTurf || phase.isCombatPhase() && canBreakEnemyForts)
            && !team.getBuildBlocks().contains(block) && !team.getOppositeTeam().getBuildBlocks().contains(block)) {
            message = Text.translatable("turfwars.game.error.can_only_break_building_blocks");
        } else if (!isOwnTurf) {
            if (!phase.isCombatPhase() && canBreakEnemyForts) {
                message = Text.translatable("turfwars.game.error.can_only_break_in_enemy_turf_during_combat");
            } else if (!canBreakEnemyForts) {
                message = Text.translatable("turfwars.game.error.can_only_break_in_own_turf");
            }
        }

        if (message != null) {
            player.sendMessage(message);
            result = EventResult.DENY;
        }

        player.currentScreenHandler.updateToClient();

        return result;
    }

    private EventResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        if (!phase.isGameEndPhase()) {
            if (source.getAttacker() != null && source.getAttacker() instanceof ServerPlayerEntity attacker) {
                TurfWarsParticipant participant = getParticipant(player);

                if (participant != null) {
                    participant.setLastAttacker(attacker);
                }
            }

            return EventResult.PASS;
        }

        return EventResult.DENY;
    }

    private EventResult onPlayerAttack(ServerPlayerEntity player, Hand hand, Entity entity, EntityHitResult entityHitResult) {
        if (phase.isCombatPhase()) {
            return EventResult.PASS;
        } else if (entity instanceof ServerPlayerEntity p && getTeam(player) != getTeam(p)) {
            player.sendMessage(Text.translatable("turfwars.game.error.can_only_attack_during_combat"));
        }

        return EventResult.DENY;
    }

    private EventResult onArrowFire(ServerPlayerEntity player, ItemStack weapon, ArrowItem arrow, int i, PersistentProjectileEntity projectileEntity) {
        if (phase.isCombatPhase()) {
            statistics.forPlayer(player).increment(StatisticKeys.ARROWS_SHOT, 1);
            return EventResult.PASS;
        } else if (!phase.isGameEndPhase()) {
            // Give player arrow back when firing is denied
            if (EnchantmentHelper.getAmmoUse(world, weapon, projectileEntity.getItemStack(), 1) != 0) {
                if (!projectileEntity.getItemStack().contains(DataComponentTypes.INTANGIBLE_PROJECTILE)) {
                    player.giveItemStack(projectileEntity.getItemStack().copy());
                }
            }

            player.sendMessage(Text.translatable("turfwars.game.error.can_only_shoot_during_combat"));
        }

        return EventResult.DENY;
    }

    private EventResult onProjectileHitEntity(ProjectileEntity entity, EntityHitResult entityHitResult) {
        if (!phase.isGameEndPhase() && entity.getOwner() instanceof ServerPlayerEntity attacker) {
            if (entityHitResult.getEntity() instanceof ServerPlayerEntity target) {
                if (teamManager.teamFor(attacker) != teamManager.teamFor(target)) {
                    if (entity.getType().isIn(EntityTypeTags.ARROWS)) {
                        statistics.forPlayer(attacker).increment(StatisticKeys.ARROWS_HIT, 1);
                    }
                }
            }
        }

        return EventResult.PASS;
    }

    private EventResult onProjectileHitBlock(ProjectileEntity entity, BlockHitResult hitResult) {
        BlockPos blockPos = hitResult.getBlockPos();
        Block block = world.getBlockState(blockPos).getBlock();

        if (!phase.isGameEndPhase() && entity.getOwner() instanceof ServerPlayerEntity player) {
            TurfWarsTeam team = getTeam(player);
            TurfWarsTeam oppositeTeam = team.getOppositeTeam();

            if (entity.hasAttached(ModAttachments.FLETCHING_PROJECTILE)
                && (team.getTurf().contains(blockPos) || oppositeTeam.getTurf().contains(blockPos))
                && (team.getBuildBlocks().contains(block) || oppositeTeam.getBuildBlocks().contains(block))) {
                world.breakBlock(blockPos, true);
                entity.kill(world);
            }
        }

        // Protect blocks that can be broken with projectiles
        if (block instanceof ChorusFlowerBlock || block instanceof DecoratedPotBlock
            || block instanceof PointedDripstoneBlock || (entity.isOnFire() && block instanceof TntBlock)) {
            entity.kill(world);
            return EventResult.DENY;
        }

        // Prevent picking up persistent projectiles & make them despawn faster (10s despawn)
        if (entity instanceof PersistentProjectileEntity projectile) {
            projectile.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
            ((PersistentProjectileEntityAccessor) projectile).setLife(1000);
        }

        return EventResult.PASS;
    }

    public EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        TurfWarsParticipant participant = getParticipant(player);
        TurfWarsTeam team = participant.getTeam();

        Text message = Text.translatable("turfwars.death." + source.getName(), player.getName());
        int turfLinesConquered = Math.min(linesPerKill, team.getScore());

        ServerPlayerEntity attacker = source.getAttacker() != null && source.getAttacker().isPlayer()
            ? (ServerPlayerEntity) source.getAttacker() : participant.getLastAttacker();
        if (attacker != null) {
            TurfWarsParticipant attackingParticipant = getParticipant(attacker);
            StatisticMap stats = statistics.forPlayer(attacker);
            attackingParticipant.onKill();

            stats.increment(StatisticKeys.KILLS, 1);
            if (stats.get(StatisticKeys.HIGHEST_KILLSTREAK, 0) < attackingParticipant.killstreak) {
                stats.set(StatisticKeys.HIGHEST_KILLSTREAK, attackingParticipant.killstreak);
            }

            if (source.isIn(DamageTypeTags.IS_PLAYER_ATTACK)) {
                stats.increment(StatisticKeys.MELEE_KILLS, 1);
            } else if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                stats.increment(StatisticKeys.RANGED_KILLS, 1);
            }

            message = Text.translatable("turfwars.death." + source.getName() + ".player", attacker.getName(), player.getName());
        }

        // Spawn death "particles"
        for (int i = 0; i < 5; i++) {
            var entity = new ItemEntity(world, player.getX(), player.getY(), player.getZ(), ColoredItems.dye(team.getDyeColor()).getDefaultStack());
            ((ItemEntityAccessor) entity).setItemAge(5980);
            entity.setPickupDelayInfinite();
            world.spawnEntity(entity);
        }

        statistics.forPlayer(player).increment(StatisticKeys.DEATHS, 1);
        player.changeGameMode(GameMode.SPECTATOR);
        participant.onDeath();

        moveTurfLine(attacker, team, turfLinesConquered);
        gameSpace.getPlayers().sendMessage(message);

        return EventResult.DENY;
    }

    private void moveTurfLine(ServerPlayerEntity attacker, TurfWarsTeam team, int amount) {
        TurfWarsTeam oppositeTeam = team.getOppositeTeam();

        oppositeTeam.expandTurf(amount);
        team.contractTurf(amount);

        if (attacker != null) {
            statistics.forPlayer(attacker).increment(StatisticKeys.TURF_LINES_CONQUERED, amount);
        }

        oppositeTeam.getTurf().iterator().forEachRemaining(pos -> {
            BlockState block = world.getBlockState(pos);
            Vec3d centerPos = pos.toCenterPos();

            if (team.getBuildBlocks().contains(block.getBlock())) {
                world.removeBlock(pos, false);
                world.spawnParticles(new DustParticleEffect(block.getMapColor(world, pos).color, 1.0F),
                    centerPos.getX(), centerPos.getY(), centerPos.getZ(), 5, 0, 0, 0, 0);
            } else if (team.getFloorBlocks().isEmpty() || team.getFloorBlocks().contains(block)) {
                BlockState newBlock = ColoredBlockUtil.block(block, oppositeTeam.getDyeColor()).getStateWithProperties(block);
                if (!team.getFloorBlocks().isEmpty()) {
                    newBlock = oppositeTeam.getFloorBlocks().get(team.getFloorBlocks().indexOf(block));

                    if (newBlock.equals(newBlock.getBlock().getDefaultState())) {
                        newBlock = newBlock.getBlock().getStateWithProperties(block);
                    }
                }

                world.setBlockState(pos, newBlock);

                if (world.getBlockState(pos.up()).isAir()) {
                    world.spawnParticles(new DustParticleEffect(block.getMapColor(world, pos).color, 1.0F),
                        centerPos.getX(), centerPos.getY() + 1, centerPos.getZ(), 5, 0, 0, 0, 0);
                }
            }
        });

        if (team.getScore() == 0) {
            for (PlayerRef ref : participants.keySet()) {
                ref.ifOnline(gameSpace, player -> {
                    if (getTeam(player) != team) {
                        statistics.forPlayer(player).increment(StatisticKeys.GAMES_WON, 1);
                    } else {
                        statistics.forPlayer(player).increment(StatisticKeys.GAMES_LOST, 1);
                    }

                    statistics.forPlayer(player).increment(StatisticKeys.GAMES_PLAYED, 1);
                });
            }

            gameSpace.getPlayers().sendMessage(Text.translatable("turfwars.game.won", oppositeTeam.getName()));
            setPhase(TurfWarsPhase.GAME_END_PHASE);
        }
    }

    private EventResult onItemPickup(ServerPlayerEntity player, ItemEntity itemEntity, ItemStack itemStack) {
        if (itemStack.getItem() instanceof BlockItem item) {
            List<Block> buildBlocks = getTeam(player).getOppositeTeam().getBuildBlocks();
            Block block = Block.getBlockFromItem(item);

            if (buildBlocks.contains(block)) {
                Item newItem = getTeam(player).getBuildBlocks().get(buildBlocks.indexOf(block)).asItem();
                if (player.getInventory().insertStack(itemStack.withItem(newItem))) {
                    player.sendPickup(itemEntity, itemStack.getCount());
                    itemEntity.kill(world);
                    return EventResult.DENY;
                }
            }
        }

        return EventResult.PASS;
    }

    public Object2ObjectMap<PlayerRef, TurfWarsParticipant> getParticipants() {
        return participants;
    }

    public TurfWarsParticipant getParticipant(ServerPlayerEntity player) {
        return participants.get(PlayerRef.of(player));
    }

    public TurfWarsTeam getTeam(ServerPlayerEntity player) {
        TurfWarsParticipant participant = getParticipant(player);

        if (participant != null) {
            return participant.getTeam();
        }

        return null;
    }

    public TurfWarsTeam getBlueTeam() {
        return this.blueTeam;
    }

    public TurfWarsTeam getRedTeam() {
        return this.redTeam;
    }

    @Nullable
    public TurfWarsTeam getTeamInControlOf(BlockPos pos) {
        if (map.getRegion(TurfWarsMap.BLUE_AREA).contains(pos)) {
            return getBlueTeam();
        } else if (map.getRegion(TurfWarsMap.RED_AREA).contains(pos)) {
            return getRedTeam();
        }

        return null;
    }

    public void trySwitchTeamFor(ServerPlayerEntity player, boolean balancedTeams) {
        TurfWarsParticipant participant = getParticipant(player);
        TurfWarsTeam team = participant.getTeam().getOppositeTeam();
        GameTeamKey teamKey = team.getGameTeam().key();

        if ((!balancedTeams || teamManager.playersIn(teamManager.teamFor(player)).size() > teamManager.playersIn(teamKey).size())
            && teamManager.addPlayerTo(PlayerRef.of(player), teamKey)) {
            participant.setTeam(team);
            participant.spawn();
            participant.setKit(participant.getKit());
            player.sendMessage(Text.translatable("turfwars.team.join", team.getName()));
        } else {
            player.sendMessage(Text.translatable("turfwars.team.request.denied"));
        }
    }
}
