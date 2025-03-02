package com.frogedev.hammer_enchant.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;

abstract public class MiningEventHammerHandler extends EventSpecificHammerHandler<MiningEventHammerHandler.MiningEventInfo> {
    public record MiningEventInfo(Player player, BlockPos originPos, Direction hitDirection) implements IEventInfo {
    }

    @Override
    public final Iterable<MiningEventInfo> expandBaseEventIntoCandidateEvents(Player player, BlockPos originPos, Direction hitDirection) {
        return Collections.singleton(new MiningEventInfo(player, originPos, hitDirection));
    }
}

