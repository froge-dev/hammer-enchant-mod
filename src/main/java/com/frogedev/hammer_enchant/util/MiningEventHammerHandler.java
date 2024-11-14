package com.frogedev.hammer_enchant.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

abstract public class MiningEventHammerHandler extends GenericHammerHandler<MiningEventHammerHandler.MiningEventInfo> {
    public record MiningEventInfo(Player player, ItemStack tool, BlockPos pos, Direction direction){}

    public boolean tryPerformMine(
            MiningEventInfo eventInfo
    ) {
        Player player = eventInfo.player();
        ItemStack tool = eventInfo.tool();
        if (!doPlayerAndToolMeetRequirements(player, tool)) {
            return false;
        }

        // TODO: getCandidateBlockPositions should only need a direction, not a whole BlockHitResult
        Iterator<BlockPos> targetBlockPositionsIter = iterCandidateBlockPositions(
                player,
                tool,
                eventInfo.direction,
                eventInfo.pos,
                eventInfo
        );

        List<BlockPos> targetBlockPositions = new ArrayList<>();
        targetBlockPositionsIter.forEachRemaining(bp -> {
            // BlockPos.betweenClosed returns mutated references to the SAME BlockPos, so we collect copies into a list to avoid issues.
            targetBlockPositions.add(bp.immutable());
        });

        UUID playerUUID = player.getUUID();
        playerTracker.add(playerUUID);
        perform(player.level(), targetBlockPositions, eventInfo);
        playerTracker.remove(playerUUID);
        return true;
    }
}
