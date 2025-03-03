package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.event.client.ToolRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import java.util.ArrayList;
import java.util.List;

public class ToolUseHandler extends EventSpecificHammerHandler<ToolUseHandler.UseEventInfo> {
    public static final ToolUseHandler INSTANCE = new ToolUseHandler();

    public record UseEventInfo(UseOnContext useOnContext, ToolAction toolAction, Direction hitDirection) implements IEventInfo {
        @Override
        public Player player() {
            return useOnContext().getPlayer();
        }

        @Override
        public BlockPos originPos() {
            return useOnContext().getClickedPos();
        }
    }

    public final Iterable<UseEventInfo> expandBaseEventIntoCandidateEvents(Player player, BlockPos originPos, Direction hitDirection) {
        UseOnContext useContext = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(originPos.getCenter(), hitDirection, originPos, false));

        List<ToolAction> toolActions = new ArrayList<>();
        Item toolItem = player.getMainHandItem().getItem();
        if(toolItem instanceof AxeItem) toolActions.addAll(ToolActions.DEFAULT_AXE_ACTIONS);
        if(toolItem instanceof HoeItem) toolActions.addAll(ToolActions.DEFAULT_HOE_ACTIONS);
        if(toolItem instanceof ShovelItem) toolActions.addAll(ToolActions.DEFAULT_SHOVEL_ACTIONS);
        if(toolItem instanceof PickaxeItem) toolActions.addAll(ToolActions.DEFAULT_PICKAXE_ACTIONS);
        if(toolItem instanceof SwordItem) toolActions.addAll(ToolActions.DEFAULT_SWORD_ACTIONS);
        if(toolItem instanceof ShearsItem) toolActions.addAll(ToolActions.DEFAULT_SHEARS_ACTIONS);
        if(toolItem instanceof ShieldItem) toolActions.addAll(ToolActions.DEFAULT_SHIELD_ACTIONS);
        if(toolItem instanceof FishingRodItem) toolActions.addAll(ToolActions.DEFAULT_FISHING_ROD_ACTIONS);

        List<UseEventInfo> events = new ArrayList<>();
        for(ToolAction toolAction : toolActions){
            events.add(new UseEventInfo(useContext, toolAction, hitDirection));
        }
        return events;
    }

    @Override
    public boolean isToolCorrectType(ItemStack tool) {
        return true;
    }

    @Override
    protected boolean shouldSkipBlockUniversal(UseEventInfo eventInfo, BlockPos pos, BlockState blockState) {
        if(super.shouldSkipBlockUniversal(eventInfo, pos, blockState)){
            return true;
        }

        BlockState newState = blockState.getToolModifiedState(eventInfo.useOnContext(), eventInfo.toolAction(), true);
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
    protected void perform(UseEventInfo event, List<BlockPos> blocks) {
//        if(event.player() instanceof ServerPlayer) {
            Level level = event.player().level();
            for (BlockPos pos : blocks) {
                BlockState modifiedState = level.getBlockState(pos).getToolModifiedState(event.useOnContext(), event.toolAction(), false);
                if(modifiedState != null){
                    event.player().sendSystemMessage(Component.literal(level.setBlockAndUpdate(pos, modifiedState) ? "true" : "false"));
                }
            }
            event.player().sendSystemMessage(Component.literal("Modifying " + blocks.size() + " blocks"));
//        }
    }

    private static final ToolRenderEvents.FloatColor WIREFRAME_COLOR = new ToolRenderEvents.FloatColor(0.4f, 0.7f, 1.0f);
    @Override
    public ToolRenderEvents.FloatColor getWireframeColor() {
        return WIREFRAME_COLOR;
    }
}