package xyz.uninenville.turfwars.map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.codecs.MoreCodecs;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.api.util.PlayerPos;

import java.util.ArrayList;
import java.util.List;

import static xyz.uninenville.turfwars.TurfWars.LOGGER;

public class WaitingMap {
    public static final String SPAWN = "spawn";
    public static final String SPAWN_ROTATION_KEY = "spawn_rotation";

    public final MapTemplate template;
    public final Identifier mapId;

    public List<PlayerPos> spawnPositions = new ArrayList<>();

    public WaitingMap(MapTemplate template, Identifier mapId) {
        this.template = template;
        this.mapId = mapId;

        checkRegion(template, SPAWN);
        for (TemplateRegion region : template.getMetadata().getRegions()) {
            if (region.getMarker().equals(SPAWN)) {
                Vec3d pos = region.getBounds().centerBottom();
                Vec2f rotation = getSpawnRotation(region, Vec2f.ZERO);
                this.spawnPositions.add(new PlayerPos(null, pos.getX(), pos.getY(), pos.getZ(), rotation.x, rotation.y));
            }
        }
    }

    public PlayerPos getRandomSpawn() {
        return spawnPositions.get(Random.create().nextInt(spawnPositions.size()));
    }

    public void checkRegion(MapTemplate template, String marker) {
        if (template.getMetadata().getFirstRegionBounds(marker) == null) {
            throw new GameOpenException(Text.literal(String.format("[%s] %s region not found", mapId, marker)));
        }
    }

    public Vec2f getSpawnRotation(String marker, Vec2f defaultRotation) {
        return getSpawnRotation(template.getMetadata().getFirstRegion(marker), defaultRotation);
    }

    public Vec2f getSpawnRotation(TemplateRegion region, Vec2f defaultRotation) {
        NbtCompound data = region.getData();
        if (data.contains(SPAWN_ROTATION_KEY)) {
            NbtList nbt = data.getList(SPAWN_ROTATION_KEY).orElseThrow();
            return new Vec2f(nbt.getFloat(0).orElseThrow(), nbt.getFloat(1).orElseThrow());
        }

        return defaultRotation;
    }

    public List<BlockState> getBlocksAt(String marker, String key, List<Block> defaultBlocks) {
        NbtCompound data = template.getMetadata().getFirstRegion(marker).getData();
        if (data.contains(key)) {
            List<BlockState> blocks = new ArrayList<>();
            data.getList(key).ifPresent(list -> list.forEach(block -> MoreCodecs.BLOCK_STATE.parse(NbtOps.INSTANCE, block)
                .resultOrPartial(LOGGER::error).ifPresent(blocks::add)));

            return blocks;
        }

        return defaultBlocks.stream().map(Block::getDefaultState).toList();
    }

    public List<ItemStack> getItemsAt(String marker, String key, List<Item> defaultItems) {
        NbtCompound data = template.getMetadata().getFirstRegion(marker).getData();
        if (data.contains(key)) {
            List<ItemStack> items = new ArrayList<>();
            data.getList(key).ifPresent(list -> list.forEach(item -> MoreCodecs.ITEM_STACK.parse(NbtOps.INSTANCE, item)
                .resultOrPartial(LOGGER::error).ifPresent(items::add)));

            return items;
        }

        return defaultItems.stream().map(Item::getDefaultStack).toList();
    }

    public ChunkGenerator asGenerator(MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }
}
