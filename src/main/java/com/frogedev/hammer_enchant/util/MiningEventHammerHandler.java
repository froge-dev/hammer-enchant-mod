package com.frogedev.hammer_enchant.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

abstract public class MiningEventHammerHandler extends GenericHammerHandler<MiningEventHammerHandler.MiningEventInfo> {
    public record MiningEventInfo(Player player, BlockPos originPos) implements IEventInfo {
    }

    @Override
    public MiningEventInfo upgradeEventInfo(IEventInfo base) {
        return new MiningEventInfo(base.player(), base.originPos());
    }
}
