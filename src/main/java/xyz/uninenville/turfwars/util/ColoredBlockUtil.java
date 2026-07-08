package xyz.uninenville.turfwars.util;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class ColoredBlockUtil {

    public static Block block(Block block, DyeColor color) {
        return block(block.defaultBlockState(), color);
    }

    public static Block block(BlockState blockState, DyeColor color) {
        if (blockState.is(BlockTags.WOOL)) {
            return Blocks.WOOL.pick(color);
        } else if (blockState.is(BlockTags.WOOL_CARPETS)) {
            return Blocks.CARPET.pick(color);
        } else if (blockState.is(BlockTags.TERRACOTTA)) {
            return Blocks.DYED_TERRACOTTA.pick(color);
        } else if (blockState.is(ConventionalBlockTags.GLAZED_TERRACOTTAS)) {
            return Blocks.GLAZED_TERRACOTTA.pick(color);
        } else if (blockState.is(ConventionalBlockTags.CONCRETES)) {
            return Blocks.CONCRETE.pick(color);
        } else if (blockState.is(BlockTags.CONCRETE_POWDERS)) {
            return Blocks.CONCRETE_POWDER.pick(color);
        } else if (blockState.is(ConventionalBlockTags.GLASS_BLOCKS_CHEAP)) {
            return Blocks.STAINED_GLASS.pick(color);
        } else if (blockState.is(ConventionalBlockTags.GLASS_PANES)) {
            return Blocks.STAINED_GLASS_PANE.pick(color);
        } else if (blockState.is(BlockTags.BEDS)) {
            return Blocks.BED.pick(color);
        } else if (blockState.is(BlockTags.BANNERS)) {
            return Blocks.BANNER.pick(color);
        } else if (blockState.is(BlockTags.SHULKER_BOXES)) {
            return Blocks.DYED_SHULKER_BOX.pick(color);
        } else if (blockState.is(BlockTags.CANDLES)) {
            return Blocks.DYED_CANDLE.pick(color);
        } else if (blockState.is(BlockTags.CANDLE_CAKES)) {
            return Blocks.DYED_CANDLE_CAKE.pick(color);
        }

        return blockState.getBlock();
    }

    public static Item getItemWithColor(Item item, DyeColor color) {
        return getStackWithColor(item.getDefaultInstance(), color).getItem();
    }

    public static ItemStack getStackWithColor(ItemStack stack, DyeColor color) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            stack = stack.transmuteCopy(ColoredBlockUtil.block(blockItem.getBlock(), color).asItem());
        } else if (stack.is(ConventionalItemTags.DYED)) {
            stack = ItemStackBuilder.of(stack).setDyeColor(color.getTextureDiffuseColor()).build();
        }

        return stack;
    }

    public static Item getItemWithTeamColor(Item item, TeamManager manager, PlayerRef playerRef) {
        return getStackWithTeamColor(item.getDefaultInstance(), manager, playerRef).getItem();
    }

    public static ItemStack getStackWithTeamColor(ItemStack stack, TeamManager manager, PlayerRef playerRef) {
        if (manager != null) {
            GameTeamKey team = manager.teamFor(playerRef);
            if (team != null) {
                stack = ColoredBlockUtil.getStackWithColor(stack, manager.getTeamConfig(team).blockDyeColor());
            }
        }

        return stack;
    }
}
