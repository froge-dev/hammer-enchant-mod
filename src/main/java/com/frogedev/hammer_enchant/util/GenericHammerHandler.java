package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.ModEnchantments;
import com.frogedev.hammer_enchant.event.client.ToolRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.IFluidBlock;

import java.util.*;

import static com.frogedev.hammer_enchant.util.HammerShapeHelper.getAllBlockPositions;

public abstract class GenericHammerHandler<EventInfo> {
    public abstract boolean isToolCorrectType(ItemStack tool);

    public abstract boolean doesStartingBlockQualify(Level level, BlockPos pos, BlockState blockState, EventInfo info);

    public abstract boolean doesNeighborBlockQualify(Level level, BlockPos originPos, BlockState originState, BlockPos neighborPos, BlockState neighborState, EventInfo info);

    abstract void perform(Level level, List<BlockPos> blocks, EventInfo info);

    public abstract ToolRenderEvents.FloatColor getWireframeColor();

    Set<UUID> playerTracker = new HashSet<>();

    public static boolean hasHammerModifiers(ItemStack tool) {
        int surfaceEnchantLevel = tool.getEnchantmentLevel(ModEnchantments.MINING_SHAPE_SURFACE_ENCHANTMENT.get());
        int depthEnchantLevel = tool.getEnchantmentLevel(ModEnchantments.MINING_SHAPE_DEPTH_ENCHANTMENT.get());
        return surfaceEnchantLevel > 0 || depthEnchantLevel > 0;
    }

    // Perform various checks to see if the hammer should be used.
    protected final boolean doPlayerAndToolMeetRequirements(
            Player player,
            ItemStack tool
    ) {
        if(player == null){
            return false;
        }

        UUID playerUUID = player.getUUID();
        if (playerTracker.contains(playerUUID)) {
            return false;
        }

        if (!hasHammerModifiers(tool)) {
            return false;
        }

        if (!isToolCorrectType(tool)) {
            return false;
        }

        if (player.getCooldowns().isOnCooldown(tool.getItem())) {
            return false;
        }

        return true;
    }

    public final List<BlockPos> getCandidateBlockPositions(Player player, ItemStack tool, Direction direction, BlockPos originPos, EventInfo eventInfo) {
        // BlockPos.betweenClosed returns mutated references to the SAME BlockPos, so we collect copies into a list to avoid issues.
        List<BlockPos> positions = new ArrayList<>();
        iterCandidateBlockPositions(player, tool, direction, originPos, eventInfo).forEachRemaining(bp -> {
            positions.add(bp.immutable());
        });
        return positions;
    }

    public final Iterator<BlockPos> iterCandidateBlockPositions(Player player, ItemStack tool, Direction direction, BlockPos originPos, EventInfo eventInfo) {
        Level level = player.level();
        if (!doesStartingBlockQualify(level, originPos, level.getBlockState(originPos), eventInfo)) {
            return Collections.emptyIterator();
        }

        BlockState originBlockState = level.getBlockState(originPos);

        return new FilteredIterator<>(getAllBlockPositions(player, tool, direction, originPos), blockPos -> {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.isAir() || blockState.getBlock() instanceof LiquidBlock || blockState.getBlock() instanceof IFluidBlock) {
                return false;
            }
            if (player.isCrouching() && originBlockState.getBlock() != blockState.getBlock()) {
                return false;
            }

            return doesNeighborBlockQualify(level, originPos, originBlockState, blockPos, blockState, eventInfo);
        });
    }
}
