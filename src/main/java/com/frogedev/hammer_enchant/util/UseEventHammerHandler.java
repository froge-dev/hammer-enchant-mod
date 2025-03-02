package com.frogedev.hammer_enchant.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ToolAction;

public abstract class UseEventHammerHandler extends EventSpecificHammerHandler<UseEventHammerHandler.UseEventInfo> {
    public record UseEventInfo(UseOnContext useOnContext, ToolAction toolAction, Direction hitDirection) implements IEventInfo {
        @Override
        public Player player() {
            return useOnContext().getPlayer();
        }

        @Override
        public BlockPos originPos() {
            return useOnContext().getClickedPos();
        }
    }

    @Override
    public final UseEventInfo upgradeEventInfo(IEventInfo base) {
        UseOnContext useContext = new UseOnContext(base.player(), InteractionHand.MAIN_HAND, new BlockHitResult(base.originPos().getCenter(), base.hitDirection(), base.originPos(), false));
        return new UseEventInfo(useContext, base.toolAction(), base.hitDirection());
    }
}
