package me.radus.hammer_enchant.event;

import me.radus.hammer_enchant.util.MiningShapeHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MiningShapeEvents {
    private static final Set<UUID> playersCurrentlyMining = new HashSet<>();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerId = player.getUUID();
        if (playersCurrentlyMining.contains(playerId)) {
            return;
        }

        if (!MiningShapeHelpers.hasMiningShapeModifiers(player)) {
            return;
        }

        BlockPos origin = event.getPos();
        Iterator<BlockPos> targetBlockPositions = MiningShapeHelpers.getBreakableBlockPositions(player, origin);
        ServerPlayerGameMode gameMode = player.gameMode;

        if (!targetBlockPositions.hasNext()) {
            return;
        }

        BlockPos pos;
        ItemStack tool = player.getMainHandItem();
        playersCurrentlyMining.add(playerId);

        int initialDamage = tool.getDamageValue();
        int blocksBroken = 0;

        do {
            pos = targetBlockPositions.next();
            gameMode.destroyBlock(pos);
            blocksBroken++;
        } while (targetBlockPositions.hasNext());

        int damagePenalty = (int) Math.ceil(Math.sqrt(blocksBroken));

        tool.setDamageValue(initialDamage + damagePenalty);

        playersCurrentlyMining.remove(playerId);
        event.setCanceled(true);
    }
}