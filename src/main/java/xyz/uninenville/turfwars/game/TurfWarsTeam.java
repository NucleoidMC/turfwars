package xyz.uninenville.turfwars.game;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.util.PlayerPos;
import xyz.uninenville.turfwars.config.TeamsConfig;
import xyz.uninenville.turfwars.map.TurfWarsMap;

import java.util.ArrayList;
import java.util.List;

import static xyz.uninenville.turfwars.TurfWars.LOGGER;
import static xyz.uninenville.turfwars.map.TurfWarsMap.*;

public class TurfWarsTeam {
    private final TurfWarsGame game;
    private GameTeam team;

    private final List<PlayerPos> spawnPositions = new ArrayList<>();
    private List<BlockState> floorBlocks;
    private List<Block> buildBlocks;

    private int score;

    public TurfWarsTeam(TurfWarsGame game, GameTeam team) {
        this.game = game;
        this.team = team;

        TurfWarsMap map = game.map;
        NbtCompound data = map.template.getMetadata().getData();
        // Load map specific team data (team name & colors)
        if (data.contains(TEAMS_KEY)) {
            TeamsConfig.CODEC.parse(NbtOps.INSTANCE, data.get(TEAMS_KEY)).resultOrPartial(LOGGER::error).ifPresent(teams -> {
                GameTeamConfig defaultConfig = isBlue() ? TeamsConfig.BLUE : TeamsConfig.RED;
                GameTeamConfig config = isBlue() ? teams.blueTeam() : teams.redTeam();
                GameTeamConfig.Builder builder = GameTeamConfig.builder(team.config());

                if (!defaultConfig.name().equals(config.name())) {
                    builder.setName(config.name());
                }
                if (!defaultConfig.colors().equals(config.colors())) {
                    builder.setColors(config.colors());
                }

                this.team = team.withConfig(builder.build());
            });
        }

        map.template.getMetadata().getRegions().forEach(region -> {
            // Initialize spawn positions
            if (region.getMarker().equals(isBlue() ? BLUE_SPAWN : RED_SPAWN)) {
                ServerWorld world = game.world;
                BlockBounds bounds = region.getBounds();
                Vec2f rotation = map.getSpawnRotation(region, new Vec2f(isBlue() ? 90.0F : -90.0F, 0F));

                bounds.iterator().forEachRemaining(pos -> {
                    if (world.getBlockState(pos.down()).isFullCube(world, pos.down()) && world.isAir(pos) && world.isAir(pos.up())) {
                        Vec3d spawn = pos.toBottomCenterPos();
                        spawnPositions.add(new PlayerPos(world, spawn.getX(), spawn.getY(), spawn.getZ(), rotation.x, rotation.y));
                    }
                });
            }

            // Initialize build and floor blocks
            this.buildBlocks = map.getBlocksAt(isBlue() ? BLUE_AREA : RED_AREA, BUILD_BLOCKS_KEY, isBlue() ? List.of(Blocks.BLUE_WOOL) : List.of(Blocks.RED_WOOL)).stream().map(AbstractBlock.AbstractBlockState::getBlock).toList();
            this.floorBlocks = map.getBlocksAt(isBlue() ? BLUE_AREA : RED_AREA, FLOOR_BLOCKS_KEY, List.of());
        });

        this.score = (int) game.map.getRegion(isBlue() ? BLUE_AREA : RED_AREA).asBox().getLengthX();
    }

    public GameTeam getGameTeam() {
        return team;
    }

    public Text getName() {
        return team.config().name();
    }

    public int getColor() {
        return team.config().dyeColor().getRgb();
    }

    public DyeColor getDyeColor() {
        return team.config().blockDyeColor();
    }

    public PlayerPos getRandomSpawn() {
        var spawnPositions = this.spawnPositions.stream().filter(pos -> game.getParticipants().values().stream().noneMatch(p -> p.getSpawn().equals(pos))).toList();

        if (spawnPositions.isEmpty()) {
            Vec3d pos = game.map.getRegion(isBlue() ? BLUE_SPAWN : RED_SPAWN).centerBottom();
            Vec2f rotation = new Vec2f(isBlue() ? 90.0F : -90.0F, 0F);
            return new PlayerPos(game.world, pos.getX(), pos.getY(), pos.getZ(), rotation.x, rotation.y);
        }

        return spawnPositions.get(Random.create().nextInt(spawnPositions.size()));
    }

    public BlockBounds getTurf() {
        return game.map.getRegion(isBlue() ? BLUE_AREA : RED_AREA);
    }

    public void expandTurf(int amount) {
        this.score += amount;

        BlockPos max = getTurf().max();
        BlockPos min = getTurf().min();

        game.map.setRegion(isBlue() ? BLUE_AREA : RED_AREA, BlockBounds.of(
            !isBlue() ? (max.getX() + amount) : max.getX(), max.getY(), max.getZ(),
            isBlue() ? (min.getX() - amount) : min.getX(), min.getY(), min.getZ()
        ));
    }

    public void contractTurf(int amount) {
        this.score -= amount;

        BlockPos max = getTurf().max();
        BlockPos min = getTurf().min();

        game.map.setRegion(isBlue() ? BLUE_AREA : RED_AREA, BlockBounds.of(
            !isBlue() ? (max.getX() - amount) : max.getX(), max.getY(), max.getZ(),
            isBlue() ? (min.getX() + amount) : min.getX(), min.getY(), min.getZ()
        ));
    }

    public List<BlockState> getFloorBlocks() {
        return this.floorBlocks;
    }

    public List<Block> getBuildBlocks() {
        return this.buildBlocks;
    }

    public int getScore() {
        return this.score;
    }

    public boolean isBlue() {
        return team.key() == TeamKeys.BLUE;
    }

    public TurfWarsTeam getOppositeTeam() {
        if (isBlue()) {
            return game.getRedTeam();
        } else {
            return game.getBlueTeam();
        }
    }
}
