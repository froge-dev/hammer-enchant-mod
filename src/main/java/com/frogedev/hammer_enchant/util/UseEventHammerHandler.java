package com.frogedev.hammer_enchant.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import java.util.*;

public abstract class UseEventHammerHandler extends EventSpecificHammerHandler<UseEventHammerHandler.UseEventInfo> {
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
}
