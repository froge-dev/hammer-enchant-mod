package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.fluids.IFluidBlock;

import java.util.*;

public class HammerShapeHelper {
    public interface MiningShapeHandler extends HammerShapeNeighborPredicate {
        boolean shouldTryHandler(Player player, ItemStack tool);

        void perform(Level level, ServerPlayer player, ItemStack tool, List<BlockPos> blocks);

        boolean testOrigin(Level level, Player player, ItemStack tool, BlockPos pos);

        Set<UUID> playerTracker();
    }

    public interface HammerShapeNeighborPredicate {
        boolean testNeighbor(Level level, Player player, ItemStack tool, BlockPos originPos, BlockState originBlockState, BlockPos neighborPos, BlockState neighborBlockState);
    }

    public static boolean perform(
            ServerPlayer player,
            ItemStack tool,
            BlockPos originPos,
            HitResult hitResult,
            MiningShapeHandler handler
    ) {
        UUID playerUUID = player.getUUID();
        if (handler.playerTracker().contains(playerUUID)) {
            return false;
        }

        if (!HammerShapeHelper.hasHammerModifiers(tool)) {
            return false;
        }

        if (player.getCooldowns().isOnCooldown(tool.getItem())) {
            return false;
        }

        Level level = player.level();
        if (!handler.testOrigin(level, player, tool, originPos)) {
            return false;
        }

        Iterator<BlockPos> targetBlockPositionsIter = getCandidateBlockPositions(
                player,
                tool,
                hitResult,
                originPos,
                handler
        );

        List<BlockPos> targetBlockPositions = new ArrayList<>();
        targetBlockPositionsIter.forEachRemaining(bp -> {
            // BlockPos.betweenClosed returns mutated references to the SAME BlockPos, so we collect copies into a list to avoid issues.
            targetBlockPositions.add(bp.immutable());
        });

        if (targetBlockPositions.size() < 2) {
            return false;
        }

        handler.playerTracker().add(playerUUID);
        handler.perform(level, player, tool, targetBlockPositions);
        handler.playerTracker().remove(playerUUID);
        return true;
    }

    public static Iterator<BlockPos> getCandidateBlockPositions(Player player, ItemStack tool, HitResult hitResult, BlockPos origin, HammerShapeNeighborPredicate neighborPredicate) {
        Level level = player.level();
        BlockState originBlockState = level.getBlockState(origin);

        return new FilteredIterator<>(getAllBlockPositions(player, tool, hitResult, origin), blockPos -> {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.isAir() || blockState.getBlock() instanceof LiquidBlock || blockState.getBlock() instanceof IFluidBlock) {
                return false;
            }
            if (player.isCrouching() && originBlockState.getBlock() != blockState.getBlock()) {
                return false;
            }

            return neighborPredicate.testNeighbor(level, player, tool, origin, originBlockState, blockPos, blockState);
        });
    }

    public static Iterator<BlockPos> getAllBlockPositions(Player player, ItemStack tool, HitResult hitResult, BlockPos origin) {
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return Collections.emptyIterator();
        }

        BlockHitResult blockHitResult = (BlockHitResult) hitResult;

        boolean facingEastWest = Math.floorMod(Math.round(player.getYRot() + 45), 180) > 90;

        // Get the world axes that correspond to the mining shape's depth/width/height.
        Direction depthDir = blockHitResult.getDirection().getOpposite();
        Direction heightDir;
        Direction widthDir;
        if (depthDir.getAxis().isVertical()) {
            if (facingEastWest) {
                heightDir = Direction.EAST;
                widthDir = Direction.SOUTH;
            } else {
                heightDir = Direction.SOUTH;
                widthDir = Direction.EAST;
            }
        } else {
            heightDir = Direction.UP;
            widthDir = depthDir.getClockWise();
        }

        // Get the corners of the mining shape.
        Vec3i selectionSize = getMiningSize(tool);
        BlockPos minCorner = origin
                .relative(heightDir, -selectionSize.getY())
                .relative(widthDir, -selectionSize.getZ());
        BlockPos maxCorner = origin
                .relative(heightDir, selectionSize.getY())
                .relative(widthDir, selectionSize.getZ())
                .relative(depthDir, selectionSize.getX());

        return BlockPos.betweenClosed(minCorner, maxCorner).iterator();
    }

    private static Vec3i getMiningSize(ItemStack itemStack) {
        int surfaceEnchantLevel = itemStack.getEnchantmentLevel(ModEnchantments.MINING_SHAPE_SURFACE_ENCHANTMENT.get());
        int depthEnchantLevel = itemStack.getEnchantmentLevel(ModEnchantments.MINING_SHAPE_DEPTH_ENCHANTMENT.get());

        int width = 0;
        int height = 0;

        if (surfaceEnchantLevel <= 4) {
            width = (int) Math.ceil(surfaceEnchantLevel / 2.0);
            height = (int) Math.floor(surfaceEnchantLevel / 2.0);
        } else {
            width = surfaceEnchantLevel - 2;
            height = surfaceEnchantLevel - 2;
        }

        return new Vec3i(depthEnchantLevel, height, width);
    }

    public static boolean hasHammerModifiers(ItemStack tool) {
        Vec3i size = getMiningSize(tool);
        return size.getX() > 0 || size.getY() > 0 || size.getZ() > 0;
    }
}
