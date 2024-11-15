package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.ModEnchantments;
import com.frogedev.hammer_enchant.event.client.ToolRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;

import javax.annotation.Nullable;
import java.util.UUID;

// Hammer logic that does not depend on a specific EventType.
public interface IHammerHandler {
    interface IEventInfo {
        Player player();
        BlockPos originPos();

        default ItemStack tool() {
            return player().getMainHandItem();
        }

        @Nullable
        default ToolAction toolAction() { return null; };

        default Direction planarDirection() {
            return player().getDirection();
        }

        default Direction direction() {
            float xRot = player().getXRot();
            if(xRot >= -45 && xRot <= 45){
                return planarDirection();
            }
            return xRot > 0 ? Direction.DOWN : Direction.UP;
        }

        // When true, only process blocks of the same type.
        default boolean altMode() {
            return player().isCrouching();
        }
    }

    ToolRenderEvents.FloatColor getWireframeColor();

    boolean isToolCorrectType(ItemStack tool);

    boolean isPlayerActivelyUsing(UUID playerUUID);

    static boolean hasHammerModifiers(ItemStack tool) {
        int surfaceEnchantLevel = tool.getEnchantmentLevel(ModEnchantments.MINING_SHAPE_SURFACE_ENCHANTMENT.get());
        int depthEnchantLevel = tool.getEnchantmentLevel(ModEnchantments.MINING_SHAPE_DEPTH_ENCHANTMENT.get());
        return surfaceEnchantLevel > 0 || depthEnchantLevel > 0;
    }

    // Perform various checks to see if the hammer should be used.
    default boolean doPlayerAndToolMeetRequirements(
            Player player,
            ItemStack tool
    ) {
        if(player == null){
            return false;
        }

        UUID playerUUID = player.getUUID();
        if (isPlayerActivelyUsing(playerUUID)) {
            return false;
        }

        if (!IHammerHandler.hasHammerModifiers(tool)) {
            return false;
        }

        if (!isToolCorrectType(tool)) {
            return false;
        }

        if (player.getCooldowns().isOnCooldown(tool.getItem())) {
            return false;
        }

        return true;
    }
}
