package xyz.uninenville.turfwars.map;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.api.util.PlayerPos;
import xyz.uninenville.turfwars.game.TurfWarsGame;
import xyz.uninenville.turfwars.kit.KitSelector;
import xyz.uninenville.turfwars.kit.KitSelectorEntity;
import xyz.uninenville.turfwars.kit.TimedEffect;

import java.util.*;

import static xyz.uninenville.turfwars.TurfWars.LOGGER;

public class TurfWarsMap extends WaitingMap {
    public static final String PLAY_AREA = "play_area";
    public static final String BLUE_AREA = "blue_area";
    public static final String RED_AREA = "red_area";
    public static final String BLUE_SPAWN_AREA = "blue_spawn_area";
    public static final String RED_SPAWN_AREA = "red_spawn_area";
    public static final String BLUE_SPAWN = "blue_spawn";
    public static final String RED_SPAWN = "red_spawn";
    public static final String SPECTATOR_SPAWN = "spectator_spawn";
    public static final String SELECT_KIT = "select_kit";

    // Map data keys
    public static final String ALLOW_MOVING_DURING_STARTING_PHASE_KEY = "allow_moving_during_starting_phase";
    public static final String AUTHORS_KEY = "authors";

    // TemplateRegion data keys
    public static final String TIMED_EFFECTS_KEY = "timed_effects";
    public static final String FLOOR_BLOCKS_KEY = "floor_blocks";
    public static final String BUILD_BLOCKS_KEY = "build_blocks";
    public static final String TEAMS_KEY = "teams";

    private final Map<String, BlockBounds> regions = new Object2ObjectOpenHashMap<>();
    private final List<TimedEffect> timedEffects = new ArrayList<>();
    private final Set<BlockPos> spawnBarriers = new HashSet<>();
    public final List<PlayerPos> spectatorSpawns = new ArrayList<>();
    private boolean allowMovingDuringStartingPhase = false;

    public TurfWarsMap(MapTemplate template, Identifier mapId, boolean isLobby) {
        super(template, mapId);

        if (!isLobby) {
            // Check that map has required regions for the game to work
            checkRegion(template, BLUE_AREA);
            checkRegion(template, RED_AREA);
            checkRegion(template, BLUE_SPAWN_AREA);
            checkRegion(template, RED_SPAWN_AREA);
            checkRegion(template, BLUE_SPAWN);
            checkRegion(template, RED_SPAWN);

            if (!regions.containsKey(PLAY_AREA)) {
                setRegion(PLAY_AREA, template.getBounds());
            }
            if (!regions.containsKey(SPECTATOR_SPAWN)) {
                setRegion(SPECTATOR_SPAWN, getRegion(SPAWN));
            }

            NbtCompound data = template.getMetadata().getData();
            this.allowMovingDuringStartingPhase = data.getBoolean(ALLOW_MOVING_DURING_STARTING_PHASE_KEY, false);

            template.getMetadata().getRegions().forEach(region -> {
                this.regions.put(region.getMarker(), region.getBounds());
                NbtCompound regionData = region.getData();

                // Load region based timed effects
                if (regionData.contains(TIMED_EFFECTS_KEY)) {
                    TimedEffect.CODEC.listOf().parse(NbtOps.INSTANCE, regionData.get(TIMED_EFFECTS_KEY)).resultOrPartial(LOGGER::error)
                        .ifPresent(effects -> effects.forEach(effect -> timedEffects.add(effect.withRegion(region.getMarker()))));
                }

                if (region.getMarker().equals(SPECTATOR_SPAWN)) {
                    Vec3d pos = region.getBounds().centerBottom();
                    Vec2f rotation = getSpawnRotation(region, Vec2f.ZERO);
                    this.spectatorSpawns.add(new PlayerPos(null, pos.getX(), pos.getY(), pos.getZ(), rotation.x, rotation.y));
                }
            });
        }
    }

    public MutableText getName() {
        return Text.translatable(mapId.getNamespace() + ".map." + mapId.getPath());
    }

    public Map<String, BlockBounds> getRegions() {
        return regions;
    }

    public BlockBounds getRegion(String name) {
        return regions.get(name);
    }

    public void setRegion(String name, BlockBounds bounds) {
        this.regions.put(name, bounds);
    }

    public List<TimedEffect> getTimedEffects() {
        return this.timedEffects;
    }

    public PlayerPos getRandomSpectatorSpawn() {
        return spectatorSpawns.get(Random.create().nextInt(spectatorSpawns.size()));
    }

    public boolean canMoveDuringStartingPhase() {
        return allowMovingDuringStartingPhase;
    }

    public void placeSpawnBarriers(ServerWorld world) {
        if (this.spawnBarriers.isEmpty()) {
            regions.forEach((marker, bounds) -> {
                if (marker.equals(BLUE_SPAWN_AREA) || marker.equals(RED_SPAWN_AREA)) {
                    bounds.iterator().forEachRemaining(pos -> {
                        if (world.getBlockState(pos).isOf(Blocks.STRUCTURE_VOID)) {
                            world.setBlockState(pos, Blocks.BARRIER.getDefaultState());
                            this.spawnBarriers.add(pos.toImmutable());
                        }
                    });
                }
            });
            return;
        }

        for (BlockPos pos : this.spawnBarriers) {
            world.setBlockState(pos, Blocks.BARRIER.getDefaultState());
        }
    }

    public void removeSpawnBarriers(ServerWorld world) {
        for (BlockPos pos : this.spawnBarriers) {
            world.removeBlock(pos, false);
        }
    }

    public void spawnKitSelectorEntities(TurfWarsGame game, ServerWorld world) {
        template.getMetadata().getRegions().forEach(region -> {
            if (region.getMarker().equals(SELECT_KIT)) {
                KitSelector.CODEC.parse(NbtOps.INSTANCE, region.getData()).resultOrPartial(LOGGER::error).ifPresent(kitSelector -> {
                    if (kitSelector.getKit() != null) {
                        int color = template.getMetadata().getFirstRegionBounds(BLUE_SPAWN_AREA).intersects(region.getBounds())
                            ? game.getBlueTeam().getColor() : game.getRedTeam().getColor();
                        KitSelectorEntity entity = new KitSelectorEntity(game, world, region.getBounds().centerBottom(), kitSelector, color);
                        world.spawnEntity(entity);
                    }
                });
            }
        });
    }
}
