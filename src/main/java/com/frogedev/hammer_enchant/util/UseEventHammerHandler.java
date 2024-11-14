package com.frogedev.hammer_enchant.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.event.level.BlockEvent;

import java.util.List;
import java.util.UUID;

public abstract class UseEventHammerHandler extends GenericHammerHandler<UseEventHammerHandler.UseEventInfo> {
    public record UseEventInfo(BlockPos pos, Direction direction, UseOnContext useOnContext, ToolAction toolAction) {
    }

    public boolean tryPerformUse(
            BlockEvent.BlockToolModificationEvent event
    ) {
        UseEventInfo eventInfo = new UseEventInfo(event.getContext().getClickedPos(), event.getPlayer().getDirection(), event.getContext(), event.getToolAction());

        ServerPlayer player = (ServerPlayer) eventInfo.useOnContext().getPlayer();
        ItemStack tool = eventInfo.useOnContext().getItemInHand();
        if (!doPlayerAndToolMeetRequirements(player, tool)) {
            return false;
        }

        // TODO: getCandidateBlockPositions should only need a direction, not a whole BlockHitResult
        List<BlockPos> targetBlockPositions = getCandidateBlockPositions(
                player,
                tool,
                eventInfo.direction(),
                eventInfo.pos(),
                eventInfo
        );

        UUID playerUUID = player.getUUID();
        playerTracker.add(playerUUID);
        perform(player.level(), targetBlockPositions, eventInfo);
        playerTracker.remove(playerUUID);
        return true;
    }
}
