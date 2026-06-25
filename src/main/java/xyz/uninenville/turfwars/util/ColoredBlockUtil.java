package xyz.uninenville.turfwars.util;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.DyeColor;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.util.ColoredBlocks;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class ColoredBlockUtil {

    public static Block block(Block block, DyeColor color) {
        return block(block.getDefaultState(), color);
    }

    public static Block block(BlockState blockState, DyeColor color) {
        if (blockState.isIn(BlockTags.WOOL)) {
            return ColoredBlocks.wool(color);
        } else if (blockState.isIn(BlockTags.WOOL_CARPETS)) {
            return ColoredBlocks.carpet(color);
        } else if (blockState.isIn(BlockTags.TERRACOTTA)) {
            return ColoredBlocks.terracotta(color);
        } else if (blockState.isIn(ConventionalBlockTags.GLAZED_TERRACOTTAS)) {
            return ColoredBlocks.glazedTerracotta(color);
        } else if (blockState.isIn(ConventionalBlockTags.CONCRETES)) {
            return ColoredBlocks.concrete(color);
        } else if (blockState.isIn(BlockTags.CONCRETE_POWDER)) {
            return ColoredBlocks.concretePowder(color);
        } else if (blockState.isIn(ConventionalBlockTags.GLASS_BLOCKS_CHEAP)) {
            return ColoredBlocks.glass(color);
        } else if (blockState.isIn(ConventionalBlockTags.GLASS_PANES)) {
            return ColoredBlocks.glassPane(color);
        } else if (blockState.isIn(BlockTags.BEDS)) {
            return ColoredBlocks.bed(color);
        } else if (blockState.isIn(BlockTags.BANNERS)) {
            return ColoredBlocks.banner(color);
        } else if (blockState.isIn(BlockTags.SHULKER_BOXES)) {
            return ColoredBlocks.shulkerBox(color);
        } else if (blockState.isIn(BlockTags.CANDLES)) {
            return ColoredBlocks.candle(color);
        } else if (blockState.isIn(BlockTags.CANDLE_CAKES)) {
            return ColoredBlocks.candleCake(color);
        }

        return blockState.getBlock();
    }

    public static Item getItemWithColor(Item item, DyeColor color) {
        return getStackWithColor(item.getDefaultStack(), color).getItem();
    }

    public static ItemStack getStackWithColor(ItemStack stack, DyeColor color) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            stack = stack.withItem(ColoredBlockUtil.block(blockItem.getBlock(), color).asItem());
        } else if (stack.isIn(ItemTags.DYEABLE)) {
            stack = ItemStackBuilder.of(stack).setDyeColor(color.getEntityColor()).build();
        }

        return stack;
    }

    public static Item getItemWithTeamColor(Item item, TeamManager manager, PlayerRef playerRef) {
        return getStackWithTeamColor(item.getDefaultStack(), manager, playerRef).getItem();
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
