package xyz.uninenville.turfwars.map;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
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

            CompoundTag data = template.getMetadata().getData();
            this.allowMovingDuringStartingPhase = data.getBooleanOr(ALLOW_MOVING_DURING_STARTING_PHASE_KEY, false);

            template.getMetadata().getRegions().forEach(region -> {
                this.regions.put(region.getMarker(), region.getBounds());
                CompoundTag regionData = region.getData();

                // Load region based timed effects
                if (regionData.contains(TIMED_EFFECTS_KEY)) {
                    TimedEffect.CODEC.listOf().parse(NbtOps.INSTANCE, regionData.get(TIMED_EFFECTS_KEY)).resultOrPartial(LOGGER::error)
                        .ifPresent(effects -> effects.forEach(effect -> timedEffects.add(effect.withRegion(region.getMarker()))));
                }

                if (region.getMarker().equals(SPECTATOR_SPAWN)) {
                    Vec3 pos = region.getBounds().centerBottom();
                    Vec2 rotation = getSpawnRotation(region, Vec2.ZERO);
                    this.spectatorSpawns.add(new PlayerPos(null, pos.x(), pos.y(), pos.z(), rotation.x, rotation.y));
                }
            });
        }
    }

    public MutableComponent getName() {
        return Component.translatable(mapId.getNamespace() + ".map." + mapId.getPath());
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
        return spectatorSpawns.get(RandomSource.create().nextInt(spectatorSpawns.size()));
    }

    public boolean canMoveDuringStartingPhase() {
        return allowMovingDuringStartingPhase;
    }

    public void placeSpawnBarriers(ServerLevel level) {
        if (this.spawnBarriers.isEmpty()) {
            regions.forEach((marker, bounds) -> {
                if (marker.equals(BLUE_SPAWN_AREA) || marker.equals(RED_SPAWN_AREA)) {
                    bounds.iterator().forEachRemaining(pos -> {
                        if (level.getBlockState(pos).is(Blocks.STRUCTURE_VOID)) {
                            level.setBlockAndUpdate(pos, Blocks.BARRIER.defaultBlockState());
                            this.spawnBarriers.add(pos.immutable());
                        }
                    });
                }
            });
            return;
        }

        for (BlockPos pos : this.spawnBarriers) {
            level.setBlockAndUpdate(pos, Blocks.BARRIER.defaultBlockState());
        }
    }

    public void removeSpawnBarriers(ServerLevel level) {
        for (BlockPos pos : this.spawnBarriers) {
            level.removeBlock(pos, false);
        }
    }

    public void spawnKitSelectorEntities(TurfWarsGame game, ServerLevel level) {
        template.getMetadata().getRegions().forEach(region -> {
            if (region.getMarker().equals(SELECT_KIT)) {
                KitSelector.CODEC.parse(NbtOps.INSTANCE, region.getData()).resultOrPartial(LOGGER::error).ifPresent(kitSelector -> {
                    if (kitSelector.getKit() != null) {
                        int color = template.getMetadata().getFirstRegionBounds(BLUE_SPAWN_AREA).intersects(region.getBounds())
                            ? game.getBlueTeam().getColor() : game.getRedTeam().getColor();
                        KitSelectorEntity entity = new KitSelectorEntity(game, level, region.getBounds().centerBottom(), kitSelector, color);
                        level.addFreshEntity(entity);
                    }
                });
            }
        });
    }
}
