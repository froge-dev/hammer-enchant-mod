package com.frogedev.hammer_enchant.event;

import com.frogedev.hammer_enchant.ModConfig;
import com.frogedev.hammer_enchant.tag.ModTags;
import com.frogedev.hammer_enchant.util.MiningShapeHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MiningShapeEvents {
    public static boolean canTill(Level level, BlockPos blockPos) {
        return level.getBlockState(blockPos).is(ModTags.Blocks.TILLABLE_BLOCK_TAG) && level.getBlockState(blockPos.above()).isAir();
    }

    public static class TillingHandler implements MiningShapeHelpers.MiningShapeHandler {
        private static final Set<UUID> playerTracker = new HashSet<>();
        public static final TillingHandler INSTANCE = new TillingHandler();

        @Override
        public boolean shouldTryHandler(Player player, ItemStack tool) {
            return tool.getItem() instanceof HoeItem;
        }

        @Override
        public void perform(Level level, ServerPlayer player, ItemStack tool, List<BlockPos> blocks) {
            int blocksConverted = 0;

            for (BlockPos block : blocks) {
                level.setBlock(block, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                blocksConverted++;
            }

            int damagePenalty = ModConfig.DURABILITY_MODE.get().computeDamage(blocksConverted);
            player.getMainHandItem().hurtAndBreak(damagePenalty, player, (a) -> {
            });
        }

        @Override
        public boolean testNeighbor(Level level, Player player, ItemStack tool, BlockPos originPos, BlockState originBlockState, BlockPos neighborPos, BlockState neighborBlockState) {
            return canTill(level, neighborPos);
        }

        @Override
        public boolean testOrigin(Level level, Player player, ItemStack tool, BlockPos pos) {
            return canTill(level, pos);
        }

        @Override
        public Set<UUID> playerTracker() {
            return playerTracker;
        }
    }

    public static class MiningHandler implements MiningShapeHelpers.MiningShapeHandler {
        private static final Set<UUID> playerTracker = new HashSet<>();
        public static final MiningHandler INSTANCE = new MiningHandler();

        @Override
        public boolean shouldTryHandler(Player player, ItemStack tool) {
            return true;
        }

        @Override
        public void perform(Level level, ServerPlayer player, ItemStack tool, List<BlockPos> blocks) {
            // The damage calculation might decrease how much damage the tool takes.
            // As a precaution, temporarily set the tool to undamaged such that it doesn't break prematurely.
            int initialDamage = tool.getDamageValue();
            tool.setDamageValue(0);

            for (BlockPos block : blocks) {
                player.gameMode.destroyBlock(block);
            }

            int rawDamageTaken = tool.getDamageValue();
            int damagePenalty = ModConfig.DURABILITY_MODE.get().computeDamage(rawDamageTaken);

            int newDamage = initialDamage + damagePenalty;
            tool.setDamageValue(newDamage);

            // Make sure tool breaks if it's supposed to.
            if (newDamage >= tool.getMaxDamage()) {
                tool.hurtAndBreak(0, player, (a) -> {
                });
            }
        }

        @Override
        public boolean testNeighbor(Level level, Player player, ItemStack tool, BlockPos originPos, BlockState originBlockState, BlockPos neighborPos, BlockState neighborBlockState) {
            float originDestroySpeed = originBlockState.getDestroySpeed(level, originPos);
            float neighborDestroySpeed = neighborBlockState.getDestroySpeed(level, neighborPos);

            if (tool.getItem() instanceof HoeItem) {
                // Allow hoe to mine any instamineable block.
                return tool.isCorrectToolForDrops(neighborBlockState) || neighborDestroySpeed <= ModConfig.INSTAMINE_THRESHOLD.get();
            } else {
                if (!tool.isCorrectToolForDrops(neighborBlockState)) {
                    return false;
                }
                if (originDestroySpeed <= ModConfig.INSTAMINE_THRESHOLD.get()) {
                    // If origin is instamined, only mine other instamineable blocks.
                    return neighborDestroySpeed <= ModConfig.INSTAMINE_THRESHOLD.get();
                } else {
                    // If origin is not instamined, only mine blocks with destroy speed within cheat limit.
                    return neighborDestroySpeed <= originDestroySpeed + ModConfig.MINING_SPEED_CHEAT_CAP.get();
                }
            }
        }

        @Override
        public boolean testOrigin(Level level, Player player, ItemStack tool, BlockPos pos) {
            BlockState originBlockState = level.getBlockState(pos);

            Item toolItem = tool.getItem();

            if (toolItem instanceof HoeItem) {
                // Allow hoe to mine any instamineable block.
                return toolItem.isCorrectToolForDrops(originBlockState) || originBlockState.getDestroySpeed(level, pos) <= ModConfig.INSTAMINE_THRESHOLD.get();
            } else {
                return toolItem.isCorrectToolForDrops(originBlockState) && originBlockState.getDestroySpeed(level, pos) > ModConfig.INSTAMINE_THRESHOLD.get();
            }
        }

        @Override
        public Set<UUID> playerTracker() {
            return playerTracker;
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent someEvent) {
        if (!(someEvent.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (someEvent instanceof PlayerInteractEvent.RightClickBlock rightClickBlockEvent) {
            ItemStack tool = rightClickBlockEvent.getItemStack();

            if (player.getCooldowns().isOnCooldown(tool.getItem())) {
                return;
            }

            if (TillingHandler.INSTANCE.shouldTryHandler(player, tool)) {
                if (MiningShapeHelpers.handleMiningShapeEvent(
                        player,
                        tool,
                        rightClickBlockEvent.getPos(),
                        rightClickBlockEvent.getHitVec(),
                        TillingHandler.INSTANCE
                )) {
                    rightClickBlockEvent.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (MiningShapeHelpers.handleMiningShapeEvent(
                player,
                player.getMainHandItem(),
                event.getPos(),
                // Server-side raycast. For client, use Minecraft.instance.hitResult.
                event.getPlayer().pick(event.getPlayer().getBlockReach(), 0F, false),
                MiningHandler.INSTANCE
        )) {
            event.setCanceled(true);
        }
    }

    // Mining speed (s) is basically everything *but* block hardness (h).
    // Let (t) be time to break.
    //  normally: t=h/s
    //  we want: t' = f(h1,h2,...)/s
    //  we can only change (s), so we do: t' = h/s'
    //      f(h1,h2,...})/s = h/s'
    //      s' = s*h / f(h1,h2,...)
    @SubscribeEvent
    public static void onBlockBreakStart(PlayerEvent.BreakSpeed event) {
        if (event.getPosition().isEmpty()) {
            return;
        }

        Player player = event.getEntity();
        BlockPos breakPos = event.getPosition().get();

        ItemStack tool = player.getMainHandItem();
        if (!MiningShapeHelpers.hasMiningShapeModifiers(tool)) {
            return;
        }

        Level level = player.level();
        if (!MiningHandler.INSTANCE.testOrigin(level, player, tool, breakPos)) {
            return;
        }

        Iterator<BlockPos> blockPosIter = MiningShapeHelpers.getCandidateBlockPositions(
                player,
                tool,
                player.pick(player.getBlockReach(), 0F, false),
                breakPos,
                MiningHandler.INSTANCE
        );

        List<Float> allDestroyTimes = new ArrayList<>();
        if (blockPosIter.hasNext()) {
            while (blockPosIter.hasNext()) {
                BlockPos blockPos = blockPosIter.next();
                BlockState blockState = level.getBlockState(blockPos);
                allDestroyTimes.add(blockState.getBlock().defaultDestroyTime());
            }

            float centerDestroyTime = level.getBlockState(breakPos).getBlock().defaultDestroyTime();
            float totalDestroyTime = ModConfig.MINING_SPEED_MODE.get().computeDestroyTime(centerDestroyTime, allDestroyTimes);

            if(totalDestroyTime > 0){
                float newSpeed = event.getOriginalSpeed() * centerDestroyTime / totalDestroyTime;
                event.setNewSpeed(newSpeed);
            }
        }
    }
}
