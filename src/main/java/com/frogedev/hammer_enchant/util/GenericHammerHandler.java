package com.frogedev.hammer_enchant.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.IFluidBlock;

import java.util.*;

import static com.frogedev.hammer_enchant.util.HammerShapeHelper.getAllBlockPositions;

public abstract class GenericHammerHandler<EventInfo extends IHammerHandler.IEventInfo>  implements IHammerHandler {
    Set<UUID> playersActivelyUsing = new HashSet<>();

    @Override
    public boolean isPlayerActivelyUsing(UUID playerUUID) {
        return playersActivelyUsing.contains(playerUUID);
    }

    public abstract boolean doesStartingBlockQualify(EventInfo info, BlockPos pos, BlockState state);

    public abstract boolean doesNeighborBlockQualify(EventInfo info, BlockPos originPos, BlockState originState, BlockPos neighborPos, BlockState neighborState);

    protected abstract void perform(EventInfo info, List<BlockPos> blocks);

    public final boolean tryPerform(
            EventInfo eventInfo
    ) {
        Player player = eventInfo.player();
        ItemStack tool = eventInfo.tool();
        if (!doPlayerAndToolMeetRequirements(player, tool)) {
            return false;
        }

        List<BlockPos> targetBlockPositions = getCandidateBlockPositions(eventInfo);

        UUID playerUUID = player.getUUID();
        playersActivelyUsing.add(playerUUID);
        perform(eventInfo, targetBlockPositions);
        playersActivelyUsing.remove(playerUUID);
        return true;
    }

    public final List<BlockPos> getCandidateBlockPositions(EventInfo eventInfo) {
        // BlockPos.betweenClosed returns mutated references to the SAME BlockPos, so we collect copies into a list to avoid issues.
        List<BlockPos> positions = new ArrayList<>();
        iterCandidateBlockPositions(eventInfo).forEachRemaining(bp -> {
            positions.add(bp.immutable());
        });
        return positions;
    }

    public final Iterator<BlockPos> iterCandidateBlockPositions(EventInfo eventInfo) {
        Level level = eventInfo.player().level();
        if (!doesStartingBlockQualify(eventInfo, eventInfo.originPos(), level.getBlockState(eventInfo.originPos()))) {
            return Collections.emptyIterator();
        }

        BlockState originBlockState = level.getBlockState(eventInfo.originPos());

        return new FilteredIterator<>(getAllBlockPositions(eventInfo), blockPos -> {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.isAir() || blockState.getBlock() instanceof LiquidBlock || blockState.getBlock() instanceof IFluidBlock) {
                return false;
            }
            if (eventInfo.altMode() && originBlockState.getBlock() != blockState.getBlock()) {
                return false;
            }

            return doesNeighborBlockQualify(eventInfo, eventInfo.originPos(), originBlockState, blockPos, blockState);
        });
    }
}
