package com.frogedev.hammer_enchant.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.IFluidBlock;

import javax.annotation.Nullable;
import java.util.*;

import static com.frogedev.hammer_enchant.util.HammerShapeHelper.getAllBlockPositions;

public abstract class EventSpecificHammerHandler<EventInfo extends BaseHammerHandler.IEventInfo> extends BaseHammerHandler {
    /// Used for both starting block and neighbor block.
    protected boolean shouldSkipBlockUniversal(EventInfo eventInfo, BlockPos pos, BlockState blockState){
        if(blockState.isAir()){
            return true;
        }

        if(blockState.getBlock() instanceof LiquidBlock || blockState.getBlock() instanceof IFluidBlock){
            return true;
        }

        return false;
    }

    protected abstract boolean doesStartingBlockQualify(EventInfo info, BlockPos pos, BlockState state);

    protected abstract boolean doesNeighborBlockQualify(EventInfo info, BlockPos originPos, BlockState originState, BlockPos neighborPos, BlockState neighborState);

    protected abstract void perform(EventInfo baseEvent, List<BlockPos> blocks);

    public final boolean tryPerform(
        Player player,
        BlockPos originPos,
        Direction hitDirection
    ) {
        List<BlockPos> targetBlockPositions = new ArrayList<>();

        // One event may need to be attempted as multiple different events. E.g.: Right-clicking an Axe may invoke AXE_STRIP, AXE_SCRAPE, or AXE_WAX_OFF.
        Iterable<EventInfo> candidateEvents = this.expandBaseEventIntoCandidateEvents(player, originPos, hitDirection);

        Pair<EventInfo, Iterator<BlockPos>> qualifyingPair = getFirstQualifyingEventAndBlocks(candidateEvents.iterator());

        qualifyingPair.u.forEachRemaining(bp -> {
            targetBlockPositions.add(bp.immutable());
        });

        if(targetBlockPositions.isEmpty()){
            return false;
        }

        UUID playerUUID = player.getUUID();
        playersActivelyUsing.add(playerUUID);
        perform(qualifyingPair.t, targetBlockPositions);
        playersActivelyUsing.remove(playerUUID);
        return true;
    }

    public Iterator<BlockPos> computeTargetBlocksForBaseEvent(Player player, BlockPos origin, Direction hitDirection){
        return this.getFirstQualifyingEventAndBlocks(this.expandBaseEventIntoCandidateEvents(player, origin, hitDirection).iterator()).u;
    }

    private Pair<EventInfo, Iterator<BlockPos>> getFirstQualifyingEventAndBlocks(Iterator<EventInfo> events) {
        while(events.hasNext()){
            EventInfo event = events.next();
            Iterator<BlockPos> blocks = this.computeTargetBlocksForSingleEvent(event);
            if(blocks.hasNext()){
                return new Pair<>(event, blocks);
            }
        }
        return new Pair<>(null, Collections.emptyIterator());
    }

    private Iterator<BlockPos> computeTargetBlocksForSingleEvent(EventInfo eventInfo) {
        Player player = eventInfo.player();
        ItemStack tool = eventInfo.tool();
        if (!doPlayerAndToolMeetRequirements(player, tool)) {
            return Collections.emptyIterator();
        }

        Level level = eventInfo.player().level();
        BlockState originBlockState = level.getBlockState(eventInfo.originPos());
        if (shouldSkipBlockUniversal(eventInfo, eventInfo.originPos(), originBlockState)) {
            return Collections.emptyIterator();
        }

        if(!doesStartingBlockQualify(eventInfo, eventInfo.originPos(), originBlockState)){
            return Collections.emptyIterator();
        }


        return new FilteredIterator<>(getAllBlockPositions(eventInfo), blockPos -> {
            BlockState blockState = level.getBlockState(blockPos);
            if (eventInfo.shouldUseAltAction() && originBlockState.getBlock() != blockState.getBlock()) {
                return false;
            }

            if (shouldSkipBlockUniversal(eventInfo, blockPos, blockState)) {
                return false;
            }

            return doesNeighborBlockQualify(eventInfo, eventInfo.originPos(), originBlockState, blockPos, blockState);
        });
    }

    public abstract Iterable<EventInfo> expandBaseEventIntoCandidateEvents(Player player, BlockPos originPos, Direction hitDirection);
}
