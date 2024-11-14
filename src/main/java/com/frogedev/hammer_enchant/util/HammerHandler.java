package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ToolAction;

import java.util.*;

import static com.frogedev.hammer_enchant.util.HammerShapeHelper.getCandidateBlockPositions;

public abstract class HammerHandler {
    public abstract boolean isToolCorrectType(ItemStack tool);

    public abstract boolean doesStartingBlockQualify(Level level, Player player, ItemStack tool, BlockPos pos);

    public abstract boolean doesNeighborBlockQualify(Level level, Player player, ItemStack tool, BlockPos originPos, BlockState originBlockState, BlockPos neighborPos, BlockState neighborBlockState);

    abstract void perform(Level level, ServerPlayer player, ItemStack tool, List<BlockPos> blocks);

    Set<UUID> playerTracker = new HashSet<>();

    public static boolean hasHammerModifiers(ItemStack tool) {
        int surfaceEnchantLevel = tool.getEnchantmentLevel(ModEnchantments.MINING_SHAPE_SURFACE_ENCHANTMENT.get());
        int depthEnchantLevel = tool.getEnchantmentLevel(ModEnchantments.MINING_SHAPE_DEPTH_ENCHANTMENT.get());
        return surfaceEnchantLevel > 0 || depthEnchantLevel > 0;
    }

    // Perform various checks to see if the hammer should be used.
    private boolean canHammer(
            Player player,
            ItemStack tool
    ) {
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

    public boolean tryPerformUse(
            UseOnContext useContext,
            ToolAction toolAction
    ) {
        ServerPlayer player = (ServerPlayer) useContext.getPlayer();
        if(player == null){
            return false;
        }

        ItemStack tool = useContext.getItemInHand();
        if (!canHammer(player, tool)) {
            return false;
        }

        Level level = player.level();
        BlockPos originPos = useContext.getClickedPos();
        if (!doesStartingBlockQualify(level, player, tool, originPos)) {
            return false;
        }

        // TODO: getCandidateBlockPositions should only need a direction, not a whole BlockHitResult
        BlockHitResult hitResult = new BlockHitResult(useContext.getClickLocation(), useContext.getClickedFace(), useContext.getClickedPos(), false);
        Iterator<BlockPos> targetBlockPositionsIter = getCandidateBlockPositions(
                player,
                tool,
                hitResult,
                originPos,
                this
        );

        List<BlockPos> targetBlockPositions = new ArrayList<>();
        targetBlockPositionsIter.forEachRemaining(bp -> {
            // BlockPos.betweenClosed returns mutated references to the SAME BlockPos, so we collect copies into a list to avoid issues.
            targetBlockPositions.add(bp.immutable());
        });

        UUID playerUUID = player.getUUID();
        playerTracker.add(playerUUID);
        perform(level, player, tool, targetBlockPositions);
        playerTracker.remove(playerUUID);
        return true;
    }
}
