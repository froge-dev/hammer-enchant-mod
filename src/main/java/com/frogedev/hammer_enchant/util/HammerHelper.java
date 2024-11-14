package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.HammerEnchantMod;
import com.frogedev.hammer_enchant.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ToolAction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.frogedev.hammer_enchant.util.HammerShapeHelper.getCandidateBlockPositions;

public class HammerHelper {
    public static boolean hasHammerModifiers(ItemStack tool) {
        int surfaceEnchantLevel = tool.getEnchantmentLevel(ModEnchantments.MINING_SHAPE_SURFACE_ENCHANTMENT.get());
        int depthEnchantLevel = tool.getEnchantmentLevel(ModEnchantments.MINING_SHAPE_DEPTH_ENCHANTMENT.get());
        return surfaceEnchantLevel > 0 || depthEnchantLevel > 0;
    }

    private static boolean canHammer(
            Player player,
            ItemStack tool,
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

        if (!handler.isToolCorrectType(tool)) {
            return false;
        }

        if (player.getCooldowns().isOnCooldown(tool.getItem())) {
            return false;
        }

        return true;
    }

    public static boolean tryPerformUse(
            UseOnContext useContext,
            ToolAction toolAction,
            HammerShapeHelper.MiningShapeHandler handler
    ) {
        if (!canHammer(useContext.getPlayer(), useContext.getItemInHand(), handler)) {
            return false;
        }

        ServerPlayer player = (ServerPlayer) useContext.getPlayer();
        ItemStack tool = useContext.getItemInHand();
        BlockPos originPos = useContext.getClickedPos();
        BlockHitResult hitResult = new BlockHitResult(useContext.getClickLocation(), useContext.getClickedFace(), useContext.getClickedPos(), false);

        UUID playerUUID = player.getUUID();
        handler.playerTracker().add(playerUUID);

        Level level = player.level();
        if (!handler.testOrigin(level, player, tool, originPos)) {
            return false;
        }

        // TODO: getCandidateBlockPositions should only need a direction, not a whole BlockHitResult
        Iterator<BlockPos> targetBlockPositionsIter = getCandidateBlockPositions(
                player,
                tool,
                hitResult,
                originPos,
                (level1, player1, tool1, originPos1, originBlockState, neighborPos, neighborBlockState) -> {
                    BlockState newState = neighborBlockState.getToolModifiedState(useContext, toolAction, false);
                    if (newState == null) {
                        return false;
                    }

                    return true;
                }
        );

        List<BlockPos> targetBlockPositions = new ArrayList<>();
        targetBlockPositionsIter.forEachRemaining(bp -> {
            // BlockPos.betweenClosed returns mutated references to the SAME BlockPos, so we collect copies into a list to avoid issues.
            targetBlockPositions.add(bp.immutable());
        });

//        handler.perform(level, player, tool, targetBlockPositions);

        for (BlockPos blockPos : targetBlockPositions) {
            BlockState blockState = level.getBlockState(blockPos);
            BlockState newBlockState = blockState.getToolModifiedState(useContext, toolAction, false);

            // TODO: why no update
            level.setBlock(blockPos, newBlockState, Block.UPDATE_ALL_IMMEDIATE);

            // TODO: deal durability damage
        }

        handler.playerTracker().remove(playerUUID);
        return true;
    }

    public static boolean tryPerform(
            ServerPlayer player,
            ItemStack tool,
            BlockPos originPos,
            HitResult hitResult,
            HammerShapeHelper.MiningShapeHandler handler
    ) {
        if (!canHammer(player, tool, handler)) {
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

        UUID playerUUID = player.getUUID();

        handler.playerTracker().add(playerUUID);
        handler.perform(level, player, tool, targetBlockPositions);
        handler.playerTracker().remove(playerUUID);
        return true;
    }
}
