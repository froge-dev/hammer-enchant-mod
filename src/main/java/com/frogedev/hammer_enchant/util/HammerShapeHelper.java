package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class HammerShapeHelper {
    public static Iterator<BlockPos> getAllBlockPositions(BaseHammerHandler.IEventInfo info) {
        // Get the world axes that correspond to the mining shape's depth/width/height.
        Direction depthDir = info.hitDirection().getOpposite();
        Direction heightDir;
        Direction widthDir;
        if (depthDir.getAxis().isVertical()) {
            if (info.planarDirection().getAxis() == Direction.Axis.X) {
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
        Vec3i selectionSize = getMiningSize(info.tool());
        BlockPos minCorner = info.originPos()
                .relative(heightDir, -selectionSize.getY())
                .relative(widthDir, -selectionSize.getZ());
        BlockPos maxCorner = info.originPos()
                .relative(heightDir, selectionSize.getY())
                .relative(widthDir, selectionSize.getZ())
                .relative(depthDir, selectionSize.getX());

        return BlockPos.betweenClosed(minCorner, maxCorner).iterator();
    }

    public static Vec3i getMiningSize(ItemStack itemStack) {
        int surfaceEnchantLevel = itemStack.getEnchantmentLevel(ModEnchantments.WIDE_SHAPE_ENCHANTMENT.get());
        int depthEnchantLevel = itemStack.getEnchantmentLevel(ModEnchantments.DEEP_SHAPE_ENCHANTMENT.get());

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
}
