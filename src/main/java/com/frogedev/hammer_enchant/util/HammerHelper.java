package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.frogedev.hammer_enchant.util.HammerShapeHelper.getCandidateBlockPositions;
import static com.frogedev.hammer_enchant.util.HammerShapeHelper.getMiningSize;

public class HammerHelper {
    public static boolean hasHammerModifiers(ItemStack tool) {
        int surfaceEnchantLevel = tool.getEnchantmentLevel(ModEnchantments.MINING_SHAPE_SURFACE_ENCHANTMENT.get());
        int depthEnchantLevel = tool.getEnchantmentLevel(ModEnchantments.MINING_SHAPE_DEPTH_ENCHANTMENT.get());
        return surfaceEnchantLevel > 0 || depthEnchantLevel > 0;
    }

    public static boolean tryPerform(
            ServerPlayer player,
            ItemStack tool,
            BlockPos originPos,
            HitResult hitResult,
            HammerShapeHelper.MiningShapeHandler handler
    ) {
        UUID playerUUID = player.getUUID();

        // Perform various checks to see if the hammer should be used.
        if (handler.playerTracker().contains(playerUUID)) {
            return false;
        }

        if (!HammerHelper.hasHammerModifiers(tool)) {
            return false;
        }

        if(!handler.isToolCorrectType(tool)){
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

        handler.playerTracker().add(playerUUID);
        handler.perform(level, player, tool, targetBlockPositions);
        handler.playerTracker().remove(playerUUID);
        return true;
    }
}
