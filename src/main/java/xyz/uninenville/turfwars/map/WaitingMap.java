package xyz.uninenville.turfwars.map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.codecs.MoreCodecs;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.api.util.PlayerPos;

import java.util.ArrayList;
import java.util.List;

import static xyz.uninenville.turfwars.TurfWars.LOGGER;

public class WaitingMap {
    public static final String SPAWN = "spawn";
    public static final String SPAWN_ROTATION_KEY = "spawn_rotation";

    public final MapTemplate template;
    public final Identifier mapId;

    public final List<PlayerPos> spawnPositions = new ArrayList<>();

    public WaitingMap(MapTemplate template, Identifier mapId) {
        this.template = template;
        this.mapId = mapId;

        checkRegion(template, SPAWN);
        for (TemplateRegion region : template.getMetadata().getRegions()) {
            if (region.getMarker().equals(SPAWN)) {
                Vec3 pos = region.getBounds().centerBottom();
                Vec2 rotation = getSpawnRotation(region, Vec2.ZERO);
                this.spawnPositions.add(new PlayerPos(null, pos.x(), pos.y(), pos.z(), rotation.x, rotation.y));
            }
        }
    }

    public PlayerPos getRandomSpawn() {
        return spawnPositions.get(RandomSource.create().nextInt(spawnPositions.size()));
    }

    public void checkRegion(MapTemplate template, String marker) {
        if (template.getMetadata().getFirstRegionBounds(marker) == null) {
            throw new GameOpenException(Component.literal(String.format("[%s] %s region not found", mapId, marker)));
        }
    }

    public Vec2 getSpawnRotation(String marker, Vec2 defaultRotation) {
        return getSpawnRotation(template.getMetadata().getFirstRegion(marker), defaultRotation);
    }

    public Vec2 getSpawnRotation(TemplateRegion region, Vec2 defaultRotation) {
        CompoundTag data = region.getData();
        if (data.contains(SPAWN_ROTATION_KEY)) {
            ListTag nbt = data.getList(SPAWN_ROTATION_KEY).orElseThrow();
            return new Vec2(nbt.getFloat(0).orElseThrow(), nbt.getFloat(1).orElseThrow());
        }

        return defaultRotation;
    }

    public List<BlockState> getBlocksAt(String marker, String key, List<Block> defaultBlocks) {
        CompoundTag data = template.getMetadata().getFirstRegion(marker).getData();
        if (data.contains(key)) {
            List<BlockState> blocks = new ArrayList<>();
            data.getList(key).ifPresent(list -> list.forEach(block -> MoreCodecs.BLOCK_STATE.parse(NbtOps.INSTANCE, block)
                .resultOrPartial(LOGGER::error).ifPresent(blocks::add)));

            return blocks;
        }

        return defaultBlocks.stream().map(Block::defaultBlockState).toList();
    }

    public List<ItemStack> getItemsAt(String marker, String key, List<Item> defaultItems) {
        CompoundTag data = template.getMetadata().getFirstRegion(marker).getData();
        if (data.contains(key)) {
            List<ItemStack> items = new ArrayList<>();
            data.getList(key).ifPresent(list -> list.forEach(item -> MoreCodecs.ITEM_STACK.parse(NbtOps.INSTANCE, item)
                .resultOrPartial(LOGGER::error).ifPresent(items::add)));

            return items;
        }

        return defaultItems.stream().map(Item::getDefaultInstance).toList();
    }

    public ChunkGenerator asGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }
}
