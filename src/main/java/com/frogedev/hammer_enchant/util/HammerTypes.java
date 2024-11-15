package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.ModConfig;
import com.frogedev.hammer_enchant.event.client.ToolRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
        protected boolean doesStartingBlockQualify(UseEventInfo info, BlockPos originPos, BlockState blockState) {
            BlockState newState = blockState.getToolModifiedState(info.useOnContext(), info.toolAction(), true);
            return newState != null;
        }

        @Override
        protected boolean doesNeighborBlockQualify(UseEventInfo eventInfo, BlockPos originPos, BlockState originState, BlockPos neighborPos, BlockState neighborState) {
            return doesStartingBlockQualify(eventInfo, neighborPos, neighborState);
        }

        @Override
        protected void perform(UseEventInfo useEventInfo, List<BlockPos> blocks) {
            Level level = useEventInfo.player().level();
            for(BlockPos pos : blocks){
                level.getBlockState(pos).getToolModifiedState(useEventInfo.useOnContext(), useEventInfo.toolAction(), false);
            }
        }

        private static final ToolRenderEvents.FloatColor WIREFRAME_COLOR = new ToolRenderEvents.FloatColor(0.8f, 1.0f, 0.0f);
        @Override
        public ToolRenderEvents.FloatColor getWireframeColor() {
            return WIREFRAME_COLOR;
        }
    }

    public static class UniversalMiningHandler extends MiningEventHammerHandler {
        public static final UniversalMiningHandler INSTANCE = new UniversalMiningHandler();

        @Override
        public boolean isToolCorrectType(ItemStack tool) {
            return true;
        }


        @Override
        protected boolean doesStartingBlockQualify(MiningEventInfo miningEventInfo, BlockPos pos, BlockState blockState) {
            Item toolItem = miningEventInfo.tool().getItem();
            Level level = miningEventInfo.player().level();

            if (toolItem instanceof HoeItem) {
                // Allow hoe to mine any instamineable block.
                return toolItem.isCorrectToolForDrops(blockState) || blockState.getDestroySpeed(level, pos) <= ModConfig.INSTAMINE_THRESHOLD.get();
            } else {
                return toolItem.isCorrectToolForDrops(blockState) && blockState.getDestroySpeed(level, pos) > ModConfig.INSTAMINE_THRESHOLD.get();
            }
        }

        @Override
        protected boolean doesNeighborBlockQualify(MiningEventInfo miningEventInfo, BlockPos originPos, BlockState originState, BlockPos neighborPos, BlockState neighborState) {
            if(!this.doesStartingBlockQualify(miningEventInfo, neighborPos, neighborState)){
                return false;
            }

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
        protected void perform(MiningEventInfo miningEventInfo, List<BlockPos> blocks) {
            ItemStack tool = miningEventInfo.tool();

            if(miningEventInfo.player() instanceof ServerPlayer serverPlayer){
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

        private static final ToolRenderEvents.FloatColor WIREFRAME_COLOR = new ToolRenderEvents.FloatColor(0.4f, 0.4f, 1.0f);
        @Override
        public ToolRenderEvents.FloatColor getWireframeColor() {
            return WIREFRAME_COLOR;
        }
    }
}
