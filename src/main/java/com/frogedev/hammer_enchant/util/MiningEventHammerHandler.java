package com.frogedev.hammer_enchant.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

abstract public class MiningEventHammerHandler extends EventSpecificHammerHandler<MiningEventHammerHandler.MiningEventInfo> {
    public record MiningEventInfo(Player player, BlockPos originPos, Direction hitDirection) implements IEventInfo {
    }

    @Override
    public final MiningEventInfo upgradeEventInfo(IEventInfo base) {
        return new MiningEventInfo(base.player(), base.originPos(), base.hitDirection());
    }
}
