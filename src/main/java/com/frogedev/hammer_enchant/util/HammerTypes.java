package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.ModConfig;
import com.frogedev.hammer_enchant.tag.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HammerTypes {
    public static boolean canTill(Level level, BlockPos blockPos) {
        return level.getBlockState(blockPos).is(ModTags.Blocks.TILLABLE_BLOCK_TAG) && level.getBlockState(blockPos.above()).isAir();
    }

    public static class ToolUseHandler implements HammerShapeHelper.MiningShapeHandler {
        private static final Set<UUID> playerTracker = new HashSet<>();
        public static final ToolUseHandler INSTANCE = new ToolUseHandler();

        @Override
        public boolean isToolCorrectType(ItemStack tool) {
            return true;
        }

        @Override
        public void perform(Level level, ServerPlayer player, ItemStack tool, List<BlockPos> blocks) {

        }

        @Override
        public boolean testOrigin(Level level, Player player, ItemStack tool, BlockPos pos) {
            return true;
        }

        @Override
        public Set<UUID> playerTracker() {
            return playerTracker;
        }

        @Override
        public boolean testNeighbor(Level level, Player player, ItemStack tool, BlockPos originPos, BlockState originBlockState, BlockPos neighborPos, BlockState neighborBlockState) {
            return true;
        }
    }

    public static class TillingHandler implements HammerShapeHelper.MiningShapeHandler {
        private static final Set<UUID> playerTracker = new HashSet<>();
        public static final TillingHandler INSTANCE = new TillingHandler();

        @Override
        public boolean isToolCorrectType(ItemStack tool) {
            return tool.getItem() instanceof HoeItem;
        }

        @Override
        public void perform(Level level, ServerPlayer player, ItemStack tool, List<BlockPos> blocks) {
            int blocksConverted = 0;

            for (BlockPos block : blocks) {
                level.setBlock(block, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                blocksConverted++;
            }

            int damagePenalty = ModConfig.DURABILITY_MODE.get().computeDamage(blocksConverted);
            player.getMainHandItem().hurtAndBreak(damagePenalty, player, (a) -> {
            });
        }

        @Override
        public boolean testNeighbor(Level level, Player player, ItemStack tool, BlockPos originPos, BlockState originBlockState, BlockPos neighborPos, BlockState neighborBlockState) {
            return canTill(level, neighborPos);
        }

        @Override
        public boolean testOrigin(Level level, Player player, ItemStack tool, BlockPos pos) {
            return canTill(level, pos);
        }

        @Override
        public Set<UUID> playerTracker() {
            return playerTracker;
        }
    }

    public static class MiningHandler implements HammerShapeHelper.MiningShapeHandler {
        private static final Set<UUID> playerTracker = new HashSet<>();
        public static final MiningHandler INSTANCE = new MiningHandler();

        @Override
        public boolean isToolCorrectType(ItemStack tool) {
            return true;
        }

        @Override
        public void perform(Level level, ServerPlayer player, ItemStack tool, List<BlockPos> blocks) {
            // The damage calculation might decrease how much damage the tool takes.
            // As a precaution, temporarily set the tool to undamaged such that it doesn't break prematurely.
            int initialDamage = tool.getDamageValue();
            tool.setDamageValue(0);

            for (BlockPos block : blocks) {
                player.gameMode.destroyBlock(block);
            }

            int rawDamageTaken = tool.getDamageValue();
            int damagePenalty = ModConfig.DURABILITY_MODE.get().computeDamage(rawDamageTaken);

            int newDamage = initialDamage + damagePenalty;
            tool.setDamageValue(newDamage);

            // Make sure tool breaks if it's supposed to.
            if (newDamage >= tool.getMaxDamage()) {
                tool.hurtAndBreak(0, player, (a) -> {
                });
            }
        }

        @Override
        public boolean testNeighbor(Level level, Player player, ItemStack tool, BlockPos originPos, BlockState originBlockState, BlockPos neighborPos, BlockState neighborBlockState) {
            float originDestroySpeed = originBlockState.getDestroySpeed(level, originPos);
            float neighborDestroySpeed = neighborBlockState.getDestroySpeed(level, neighborPos);

            if (tool.getItem() instanceof HoeItem) {
                // Allow hoe to mine any instamineable block.
                return tool.isCorrectToolForDrops(neighborBlockState) || neighborDestroySpeed <= ModConfig.INSTAMINE_THRESHOLD.get();
            } else {
                if (!tool.isCorrectToolForDrops(neighborBlockState)) {
                    return false;
                }
                if (originDestroySpeed <= ModConfig.INSTAMINE_THRESHOLD.get()) {
                    // If origin is instamined, only mine other instamineable blocks.
                    return neighborDestroySpeed <= ModConfig.INSTAMINE_THRESHOLD.get();
                } else {
                    // If origin is not instamined, only mine blocks with destroy speed within cheat limit.
                    return neighborDestroySpeed <= originDestroySpeed + ModConfig.MINING_SPEED_CHEAT_CAP.get();
                }
            }
        }

        @Override
        public boolean testOrigin(Level level, Player player, ItemStack tool, BlockPos pos) {
            BlockState originBlockState = level.getBlockState(pos);

            Item toolItem = tool.getItem();

            if (toolItem instanceof HoeItem) {
                // Allow hoe to mine any instamineable block.
                return toolItem.isCorrectToolForDrops(originBlockState) || originBlockState.getDestroySpeed(level, pos) <= ModConfig.INSTAMINE_THRESHOLD.get();
            } else {
                return toolItem.isCorrectToolForDrops(originBlockState) && originBlockState.getDestroySpeed(level, pos) > ModConfig.INSTAMINE_THRESHOLD.get();
            }
        }

        @Override
        public Set<UUID> playerTracker() {
            return playerTracker;
        }
    }
}
