package com.frogedev.hammer_enchant.event;

import com.frogedev.hammer_enchant.ModConfig;
import com.frogedev.hammer_enchant.util.HammerTypes;
import com.frogedev.hammer_enchant.util.MiningEventHammerHandler;
import com.frogedev.hammer_enchant.util.UseEventHammerHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
    public static void onToolModifyBlock(BlockEvent.BlockToolModificationEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer)) {
            return;
        }

        UseEventHammerHandler.UseEventInfo eventInfo = new UseEventHammerHandler.UseEventInfo(event.getContext(), event.getToolAction());
        if (HammerTypes.UniversalToolUseHandler.INSTANCE.tryPerform(eventInfo)) {
            event.setCanceled(true);
        }
    }

    // On conclusion of block broken.
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer)) {
            return;
        }

        MiningEventHammerHandler.MiningEventInfo eventInfo = new MiningEventHammerHandler.MiningEventInfo(event.getPlayer(), event.getPos());
        if (HammerTypes.UniversalMiningHandler.INSTANCE.tryPerform(eventInfo)) {
            event.setCanceled(true);
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
        Level level = player.level();

        MiningEventHammerHandler.MiningEventInfo eventInfo = new MiningEventHammerHandler.MiningEventInfo(player, breakPos);
        Iterator<BlockPos> blockPosIter = HammerTypes.UniversalMiningHandler.INSTANCE.iterCandidateBlockPositions(eventInfo);

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
