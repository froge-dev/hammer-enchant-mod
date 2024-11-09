package com.frogedev.hammer_enchant.event;

import com.frogedev.hammer_enchant.util.HammerTypes;
import com.frogedev.hammer_enchant.ModConfig;
import com.frogedev.hammer_enchant.util.HammerShapeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HammerEvents {
    // Called on right-click.
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

            if (HammerTypes.TillingHandler.INSTANCE.shouldTryHandler(player, tool)) {
                if (HammerShapeHelper.perform(
                        player,
                        tool,
                        rightClickBlockEvent.getPos(),
                        rightClickBlockEvent.getHitVec(),
                        HammerTypes.TillingHandler.INSTANCE
                )) {
                    rightClickBlockEvent.setCanceled(true);
                }
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
        BlockPos breakPos = event.getPosition().get();

        ItemStack tool = player.getMainHandItem();
        if (!HammerShapeHelper.hasHammerModifiers(tool)) {
            return;
        }

        Level level = player.level();
        if (!HammerTypes.MiningHandler.INSTANCE.testOrigin(level, player, tool, breakPos)) {
            return;
        }

        Iterator<BlockPos> blockPosIter = HammerShapeHelper.getCandidateBlockPositions(
                player,
                tool,
                player.pick(player.getBlockReach(), 0F, false),
                breakPos,
                HammerTypes.MiningHandler.INSTANCE
        );


        if (blockPosIter.hasNext()) {
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

            if(totalDestroyTime > 0){
                float newSpeed = event.getOriginalSpeed() * centerDestroyTime / totalDestroyTime;
                event.setNewSpeed(newSpeed);
            }
        }
    }

    // On conclusion of block broken.
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (HammerShapeHelper.perform(
                player,
                player.getMainHandItem(),
                event.getPos(),
                // Server-side raycast. For client, use Minecraft.instance.hitResult.
                event.getPlayer().pick(event.getPlayer().getBlockReach(), 0F, false),
                HammerTypes.MiningHandler.INSTANCE
        )) {
            event.setCanceled(true);
        }
    }
}
