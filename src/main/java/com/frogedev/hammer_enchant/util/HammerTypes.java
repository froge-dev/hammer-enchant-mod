package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.ModConfig;
import com.frogedev.hammer_enchant.event.client.ToolRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.List;


public class HammerTypes {
    public static class UniversalToolUseHandler extends UseEventHammerHandler {
        public static final UniversalToolUseHandler INSTANCE = new UniversalToolUseHandler();

        @Override
        public boolean isToolCorrectType(ItemStack tool) {
            return true;
        }

        @Override
        protected boolean shouldSkipBlockUniversal(UseEventInfo eventInfo, BlockPos pos, BlockState blockState) {
            if(!super.shouldSkipBlockUniversal(eventInfo, pos, blockState)){
                return true;
            }

            BlockState newState = blockState.getToolModifiedState(eventInfo.useOnContext(), eventInfo.toolAction(), true);
            eventInfo.player().sendSystemMessage(Component.literal(newState == null ? "none" : newState.toString()));
            return newState == null;
        }

        @Override
        protected boolean doesStartingBlockQualify(UseEventInfo info, BlockPos originPos, BlockState blockState) {
            return true;
        }

        @Override
        protected boolean doesNeighborBlockQualify(UseEventInfo eventInfo, BlockPos originPos, BlockState originState, BlockPos neighborPos, BlockState neighborState) {
            return true;
        }

        @Override
        protected void perform(UseEventInfo baseEvent, List<BlockPos> blocks) {
            if(baseEvent.player() instanceof ServerPlayer serverPlayer) {
                Level level = baseEvent.player().level();
                for (BlockPos pos : blocks) {
                    level.getBlockState(pos).getToolModifiedState(baseEvent.useOnContext(), baseEvent.toolAction(), false);
                }
            }
        }

        private static final ToolRenderEvents.FloatColor WIREFRAME_COLOR = new ToolRenderEvents.FloatColor(0.8f, 1.0f, 0.0f);
        @Override
        public ToolRenderEvents.FloatColor getWireframeColor() {
            return WIREFRAME_COLOR;
        }
    }

    private static boolean isBestToolForMiningBlock(Item toolItem, BlockState blockState){
        ITagManager<Block> tags = ForgeRegistries.BLOCKS.tags();
        if(tags == null){
            return false;
        }

        if(toolItem instanceof PickaxeItem){
            if(tags.getTag(BlockTags.MINEABLE_WITH_PICKAXE).contains(blockState.getBlock())){
                return true;
            }
        }
        if(toolItem instanceof AxeItem){
            if(tags.getTag(BlockTags.MINEABLE_WITH_AXE).contains(blockState.getBlock())){
                return true;
            }
        }
        if(toolItem instanceof ShovelItem){
            if(tags.getTag(BlockTags.MINEABLE_WITH_SHOVEL).contains(blockState.getBlock())){
                return true;
            }
        }
        if(toolItem instanceof HoeItem){
            if(tags.getTag(BlockTags.MINEABLE_WITH_HOE).contains(blockState.getBlock())){
                return true;
            }

            if(tags.getTag(BlockTags.REPLACEABLE).contains(blockState.getBlock()) || tags.getTag(BlockTags.FLOWERS).contains(blockState.getBlock())){
                return true;
            }
        }
        if(toolItem instanceof ShearsItem){
            if(tags.getTag(BlockTags.LEAVES).contains(blockState.getBlock())){
                return true;
            }

            if(tags.getTag(BlockTags.WOOL).contains(blockState.getBlock())){
                return true;
            }
        }

        return false;
    }

    public static class UniversalMiningHandler extends MiningEventHammerHandler {
        public static final UniversalMiningHandler INSTANCE = new UniversalMiningHandler();

        @Override
        public boolean isToolCorrectType(ItemStack tool) {
            return true;
        }

        @Override
        protected boolean shouldSkipBlockUniversal(MiningEventInfo eventInfo, BlockPos pos, BlockState blockState) {
            if(super.shouldSkipBlockUniversal(eventInfo, pos, blockState)){
                return true;
            }

            Item toolItem = eventInfo.tool().getItem();
            Level level = eventInfo.player().level();

            if (toolItem instanceof HoeItem) {
                // Skip if this is the wrong tool AND the block is not instamineable.
                if(!isBestToolForMiningBlock(toolItem,blockState) && blockState.getDestroySpeed(level, pos) > ModConfig.INSTAMINE_THRESHOLD.get()){
                    return true;
                }
                return false;
            } else {
                // Skip if this is the wrong tool.
                if(!isBestToolForMiningBlock(toolItem,blockState)){
                    return true;
                }
                // Skip instamineable blocks (eg torches).
                if(blockState.getDestroySpeed(level, pos) <= ModConfig.INSTAMINE_THRESHOLD.get()){
                    return true;
                }
                return false;
            }
        }

        @Override
        protected boolean doesStartingBlockQualify(MiningEventInfo miningEventInfo, BlockPos pos, BlockState blockState) {
            return true;
        }

        @Override
        protected boolean doesNeighborBlockQualify(MiningEventInfo miningEventInfo, BlockPos originPos, BlockState originState, BlockPos neighborPos, BlockState neighborState) {
            Level level = miningEventInfo.player().level();

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
        protected void perform(MiningEventInfo baseEvent, List<BlockPos> blocks) {
            ItemStack tool = baseEvent.tool();

            if(baseEvent.player() instanceof ServerPlayer serverPlayer){
                // The damage calculation might decrease how much damage the tool takes.
                // As a precaution, temporarily set the tool to undamaged such that it doesn't break prematurely.
                int initialDamage = tool.getDamageValue();
                tool.setDamageValue(0);

                for (BlockPos block : blocks) {
                    serverPlayer.gameMode.destroyBlock(block);
                }

                int rawDamageTaken = tool.getDamageValue();
                int damagePenalty = ModConfig.DURABILITY_MODE.get().computeDamage(rawDamageTaken);

                int newDamage = initialDamage + damagePenalty;
                tool.setDamageValue(newDamage);

                // Make sure tool breaks if it's supposed to.
                if (newDamage >= tool.getMaxDamage()) {
                    tool.hurtAndBreak(0, serverPlayer, (a) -> {
                    });
                }
            }

        }

        private static final ToolRenderEvents.FloatColor WIREFRAME_COLOR = new ToolRenderEvents.FloatColor(1.0f, 0.4f, 0.4f);
        @Override
        public ToolRenderEvents.FloatColor getWireframeColor() {
            return WIREFRAME_COLOR;
        }
    }
}
