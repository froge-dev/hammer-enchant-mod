package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.ModConfig;
import com.frogedev.hammer_enchant.event.client.ToolRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;


public class HammerTypes {
    public static class UniversalToolUseHandler extends UseEventHammerHandler {
        public static final UniversalToolUseHandler INSTANCE = new UniversalToolUseHandler();

        @Override
        public boolean isToolCorrectType(ItemStack tool) {
            return true;
        }

        @Override
        public boolean doesStartingBlockQualify(Level level, BlockPos pos, BlockState blockState, UseEventInfo info) {
            BlockState newState = blockState.getToolModifiedState(info.useOnContext(), info.toolAction(), true);
            return newState != null;
        }

        @Override
        public boolean doesNeighborBlockQualify(Level level, BlockPos originPos, BlockState originState, BlockPos neighborPos, BlockState neighborState, UseEventInfo eventInfo) {
            return doesStartingBlockQualify(level, neighborPos, neighborState, eventInfo);
        }

        @Override
        void perform(Level level, List<BlockPos> blocks, UseEventInfo useEventInfo) {
            for(BlockPos pos : blocks){
                level.getBlockState(pos).getToolModifiedState(useEventInfo.useOnContext(), useEventInfo.toolAction(), false);
            }
        }

        private static final ToolRenderEvents.FloatColor WIREFRAME_COLOR = new ToolRenderEvents.FloatColor(0.8f, 1.0f, 0.0f);
        @Override
        ToolRenderEvents.FloatColor getWireframeColor() {
            return WIREFRAME_COLOR;
        }
    }

//    public static class TillingHandler extends GenericHammerHandler {
//        public static final TillingHandler INSTANCE = new TillingHandler();
//
//        @Override
//        public boolean isToolCorrectType(ItemStack tool) {
//            return tool.getItem() instanceof HoeItem;
//        }
//
//        @Override
//        public boolean doesStartingBlockQualify(Level level, Player player, ItemStack tool, BlockPos pos) {
//            return level.getBlockState(pos).is(ModTags.Blocks.TILLABLE_BLOCK_TAG) && level.getBlockState(pos.above()).isAir();
//        }
//
//        @Override
//        public boolean doesNeighborBlockQualify(Level level, Player player, ItemStack tool, BlockPos originPos, BlockState originBlockState, BlockPos neighborPos, BlockState neighborBlockState) {
//            return doesStartingBlockQualify(level, player, tool, neighborPos);
//        }
//
//        @Override
//        public void perform(Level level, ServerPlayer player, ItemStack tool, List<BlockPos> blocks) {
//            int blocksConverted = 0;
//
//            for (BlockPos block : blocks) {
//                level.setBlock(block, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
//                blocksConverted++;
//            }
//
//            int damagePenalty = ModConfig.DURABILITY_MODE.get().computeDamage(blocksConverted);
//            player.getMainHandItem().hurtAndBreak(damagePenalty, player, (a) -> {
//            });
//        }
//    }

    public static class UniversalMiningHandler extends MiningEventHammerHandler {
        public static final UniversalMiningHandler INSTANCE = new UniversalMiningHandler();

        @Override
        public boolean isToolCorrectType(ItemStack tool) {
            return true;
        }

        @Override
        public boolean doesStartingBlockQualify(Level level, BlockPos pos, BlockState blockState, MiningEventInfo miningEventInfo) {
            Item toolItem = miningEventInfo.tool().getItem();

            if (toolItem instanceof HoeItem) {
                // Allow hoe to mine any instamineable block.
                return toolItem.isCorrectToolForDrops(blockState) || blockState.getDestroySpeed(level, pos) <= ModConfig.INSTAMINE_THRESHOLD.get();
            } else {
                return toolItem.isCorrectToolForDrops(blockState) && blockState.getDestroySpeed(level, pos) > ModConfig.INSTAMINE_THRESHOLD.get();
            }
        }

        @Override
        public boolean doesNeighborBlockQualify(Level level, BlockPos originPos, BlockState originState, BlockPos neighborPos, BlockState neighborState, MiningEventInfo miningEventInfo) {
            if(!this.doesStartingBlockQualify(level, neighborPos, neighborState, miningEventInfo)){
                return false;
            }

            float originDestroySpeed = originState.getDestroySpeed(level, originPos);
            float neighborDestroySpeed = neighborState.getDestroySpeed(level, neighborPos);
            if (originDestroySpeed <= ModConfig.INSTAMINE_THRESHOLD.get()) {
                // If origin is instamined, only mine other instamineable blocks.
                return neighborDestroySpeed <= ModConfig.INSTAMINE_THRESHOLD.get();
            } else {
                // If origin is not instamined, only mine blocks with destroy speed within cheat limit.
                return neighborDestroySpeed <= originDestroySpeed + ModConfig.MINING_SPEED_CHEAT_CAP.get();
            }
        }

        @Override
        void perform(Level level, List<BlockPos> blocks, MiningEventInfo miningEventInfo) {
            ItemStack tool = miningEventInfo.tool();
            ServerPlayer player = miningEventInfo.player();

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

        private static final ToolRenderEvents.FloatColor WIREFRAME_COLOR = new ToolRenderEvents.FloatColor(0.4f, 0.4f, 1.0f);
        @Override
        ToolRenderEvents.FloatColor getWireframeColor() {
            return WIREFRAME_COLOR;
        }
    }
}
