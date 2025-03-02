package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.event.client.ToolRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ToolUseHandler extends UseEventHammerHandler {
    public static final ToolUseHandler INSTANCE = new ToolUseHandler();

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