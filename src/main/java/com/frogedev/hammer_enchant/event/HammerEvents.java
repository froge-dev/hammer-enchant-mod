package com.frogedev.hammer_enchant.event;

import com.frogedev.hammer_enchant.ModConfig;
import com.frogedev.hammer_enchant.util.MiningHandler;
import com.frogedev.hammer_enchant.util.ToolUseHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HammerEvents {
    @SubscribeEvent
    // Called on right-click.
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event){
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        if (ToolUseHandler.INSTANCE.tryPerform(event.getEntity(), event.getPos(), event.getFace())) {
            event.setCanceled(true);
        }
    }
    public static void onToolModifyBlock(BlockEvent.BlockToolModificationEvent event) {

    }

    // On conclusion of block broken.
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer)) {
            return;
        }

        HitResult hitResult =  event.getPlayer().pick(event.getPlayer().getBlockReach(), 0.0f, false);
        if(hitResult instanceof BlockHitResult blockHitResult){
            Direction hitDirection = blockHitResult.getDirection();

            if (MiningHandler.INSTANCE.tryPerform(event.getPlayer(), event.getPos(), hitDirection)) {
                event.setCanceled(true);
            }
        }
    }

    // This calls every tick a block is being broken!
    @SubscribeEvent
    public static void onBlockBreakStart(PlayerEvent.BreakSpeed event) {
        if (event.getPosition().isEmpty()) {
            return;
        }

        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        BlockPos breakPos = event.getPosition().get();
        Level level = player.level();


        HitResult hitResult =  player.pick(player.getBlockReach(), 0.0f, false);
        if(hitResult instanceof BlockHitResult blockHitResult) {
            Iterator<BlockPos> blockPosIter = MiningHandler.INSTANCE.computeTargetBlocksForBaseEvent(player, breakPos, blockHitResult.getDirection());

            if (!blockPosIter.hasNext()) {
                return;
            }

            List<Float> allDestroyTimes = new ArrayList<>();
            while (blockPosIter.hasNext()) {
                BlockPos blockPos = blockPosIter.next();
                BlockState blockState = level.getBlockState(blockPos);
                allDestroyTimes.add(blockState.getBlock().defaultDestroyTime());
            }

            // Mining speed (s) is basically everything *but* block hardness (h).
            // Let (t) be time to break.
            //  normally: t=h/s
            //  we want: t' = f(h1,h2,...)/s
            //  we can only change (s), so we do: t' = h/s'
            //      f(h1,h2,...})/s = h/s'
            //      s' = s*h / f(h1,h2,...)
            float centerDestroyTime = level.getBlockState(breakPos).getBlock().defaultDestroyTime();
            float totalDestroyTime = ModConfig.MINING_SPEED_MODE.get().computeDestroyTime(centerDestroyTime, allDestroyTimes);

            if (totalDestroyTime > 0) {
                float newSpeed = event.getOriginalSpeed() * centerDestroyTime / totalDestroyTime;
                event.setNewSpeed(newSpeed);
            }
        }
    }
}
