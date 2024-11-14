package com.frogedev.hammer_enchant.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.ToolAction;

public abstract class UseEventHammerHandler extends GenericHammerHandler<UseEventHammerHandler.UseEventInfo> {
    public record UseEventInfo(UseOnContext useOnContext, ToolAction toolAction) implements IEventInfo {
        @Override
        public Player player() {
            return useOnContext().getPlayer();
        }

        @Override
        public BlockPos originPos() {
            return useOnContext().getClickedPos();
        }
}
}
