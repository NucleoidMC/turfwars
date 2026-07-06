package xyz.uninenville.turfwars.game;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;
import xyz.nucleoid.plasmid.api.util.PlayerPos;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.uninenville.turfwars.kit.KitRegistry;
import xyz.uninenville.turfwars.kit.TurfWarsKit;
import xyz.uninenville.turfwars.map.TurfWarsMap;
import xyz.uninenville.turfwars.util.InventoryUtil;

import java.util.Set;

public class TurfWarsParticipant {
    private final GameSpace gameSpace;
    private final PlayerRef playerRef;
    private final TurfWarsGame game;
    private TurfWarsTeam team;
    private TurfWarsKit kit;
    private PlayerPos spawn;

    public int kills = 0;
    public int killstreak = 0;
    public int deaths = 0;
    public long deathTime = 0;
    ServerPlayer lastAttacker;
    long lastAttack = 0;

    public TurfWarsParticipant(GameSpace gameSpace, PlayerRef playerRef, TurfWarsTeam team, TurfWarsGame game) {
        this.gameSpace = gameSpace;
        this.playerRef = playerRef;
        this.game = game;

        setTeam(team);

        playerRef.ifOnline(gameSpace, player -> {
            var kit = TurfWarsPlayerDataStorage.get(player).getSelectedKit();
            if (kit != null) {
                setKit(kit);
            }
        });
        if (kit == null) {
            setKit(KitRegistry.getRandomKit());
        }

        game.timedEffectManager.add(playerRef, game.map.getTimedEffects());
    }

    public void tick() {
        playerRef.ifOnline(gameSpace, player -> {
            var playerPos = player.blockPosition();
            var gamePhase = game.getPhase();

            if (!player.isSpectator()) {
                // Prevent player from moving if game is starting and map doesn't allow moving during game start phase
                if (gamePhase.isGameStartPhase() && !game.map.canMoveDuringStartingPhase()) {
                    player.teleportTo(spawn.world(), spawn.x(), spawn.y(), spawn.z(), Relative.ROTATION, 0, 0, false);
                    return;
                }

                BlockBounds playArea = game.map.getRegion(TurfWarsMap.PLAY_AREA);
                if (!playArea.contains(playerPos)) {
                    player.hurtServer(player.level(), playArea.centerBottom().y() >= player.position().y() ?
                        player.damageSources().fellOutOfWorld() : player.damageSources().outOfBorder(), Float.MAX_VALUE);
                } else if (!gamePhase.isGameEndPhase()
                    && ((game.map.getRegion(team.isBlue() ? TurfWarsMap.RED_AREA : TurfWarsMap.BLUE_AREA).contains(playerPos)
                    && (gamePhase.isBuildPhase() || !kit.canEnterEnemyTurf()))
                    || game.map.getRegion(team.isBlue() ? TurfWarsMap.RED_SPAWN_AREA : TurfWarsMap.BLUE_SPAWN_AREA).contains(playerPos))
                ) {
                    double pitchRad = Math.toRadians(20);
                    double yawRad = Math.toRadians(getTeam().isBlue() ? -90 : 90);

                    double horizontal = -Math.cos(pitchRad);
                    player.setDeltaMovement(new Vec3(
                        Math.sin(yawRad) * horizontal, Math.sin(pitchRad), -Math.cos(yawRad) * horizontal
                    ).scale(1.5));
                    player.connection.send(new ClientboundSetEntityMotionPacket(player));
                    player.needsSync = true;
                }
            } else if (gamePhase.isGameStartPhase() || deathTime + game.config.game().respawnDelay() + 1 <= game.gameSpace.getTime()) {
                spawn();
            }
        });
    }

    public void spawn() {
        playerRef.ifOnline(gameSpace, player -> {
            player.clearFire();
            player.fallDistance = 0F;
            player.removeAllEffects();
            player.getInventory().clearContent();
            player.setDeltaMovement(Vec3.ZERO);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5);
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(spawn.world(), spawn.x(), spawn.y(), spawn.z(), Set.of(), spawn.yaw(), spawn.pitch(), false);
        });

        giveKit();
        game.timedItemManager.resetItemGiveTimes(playerRef);
        game.timedEffectManager.resetEffectGiveTimes(playerRef);

        if (deaths > 0) {
            giveBuildBlocks(game.config.game().respawnWoolAmount());
        }
    }

    public PlayerPos getSpawn() {
        return spawn;
    }

    public void giveBuildBlocks(int maxAmount) {
        playerRef.ifOnline(gameSpace, player -> {
            Block block = getTeam().getBuildBlocks().getFirst();
            int count = InventoryUtil.countItems(player, block.asItem().getDefaultInstance());
            int giveCount = count == 0 ? maxAmount : maxAmount - count;

            if (giveCount > 0) {
                player.addItem(ItemStackBuilder.of(block).setCount(giveCount).build());
            }
        });
    }

    public void onKill() {
        kills += 1;
        killstreak += 1;
    }

    public void onDeath() {
        deaths += 1;
        killstreak = 0;
        lastAttacker = null;
        deathTime = game.gameSpace.getTime();
        playerRef.ifOnline(gameSpace, LivingEntity::releaseUsingItem);
    }

    public TurfWarsTeam getTeam() {
        return this.team;
    }

    public void setTeam(TurfWarsTeam team) {
        this.team = team;
        this.spawn = team.getRandomSpawn();
    }

    @Nullable
    public ServerPlayer getLastAttacker() {
        return game.gameSpace.getTime() - lastAttack < 100 ? lastAttacker : null;
    }

    public void setLastAttacker(ServerPlayer attacker) {
        this.lastAttacker = attacker;
        this.lastAttack = game.gameSpace.getTime();
    }

    public TurfWarsKit getKit() {
        return kit;
    }

    public void setKit(TurfWarsKit kit) {
        if (this.kit != null) {
            this.kit.timedItems().forEach(item -> game.timedItemManager.removeEqual(playerRef, item));
            this.kit.timedEffects(team).forEach(effect -> game.timedEffectManager.removeEqual(playerRef, effect));
        }

        this.kit = kit;

        game.timedItemManager.add(playerRef, kit.timedItems());
        game.timedEffectManager.add(playerRef, kit.timedEffects(team));

        playerRef.ifOnline(gameSpace, player -> {
            var data = TurfWarsPlayerDataStorage.get(player);
            data.setSelectedKit(kit);
            TurfWarsPlayerDataStorage.set(player, data);
        });
    }

    public void giveKit() {
        playerRef.ifOnline(gameSpace, player -> {
            kit.giveKit(player, getTeam().getColor());
        });
    }

    public void changeKit(TurfWarsKit kit) {
        setKit(kit);

        playerRef.ifOnline(gameSpace, player -> {
            int currentWoolAmount = InventoryUtil.countItems(player, getTeam().getBuildBlocks().getFirst().asItem().getDefaultInstance());
            int respawnWoolAmount = game.config.game().respawnWoolAmount();
            player.getInventory().clearContent();

            giveKit();
            giveBuildBlocks(Math.max(currentWoolAmount, respawnWoolAmount));
        });
    }
}
