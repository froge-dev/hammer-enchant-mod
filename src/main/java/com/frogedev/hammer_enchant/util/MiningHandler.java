package com.frogedev.hammer_enchant.util;

import com.frogedev.hammer_enchant.ModConfig;
import com.frogedev.hammer_enchant.event.client.ToolRenderEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.Collections;
import java.util.List;

public class MiningHandler extends EventSpecificHammerHandler<MiningHandler.MiningEventInfo> {
    public record MiningEventInfo(Player player, BlockPos originPos, Direction hitDirection) implements BaseHammerHandler.IEventInfo {
    }

    @Override
    public final Iterable<MiningEventInfo> expandBaseEventIntoCandidateEvents(Player player, BlockPos originPos, Direction hitDirection) {
        return Collections.singleton(new MiningEventInfo(player, originPos, hitDirection));
    }

    public static final MiningHandler INSTANCE = new MiningHandler();

    private static boolean isBestToolForMiningBlock(Item toolItem, BlockState blockState){
        ITagManager<Block> tags = ForgeRegistries.BLOCKS.tags();
        if(tags == null){
            return false;
        }

        if(toolItem instanceof PickaxeItem){
            if(tags.getTag(BlockTags.MINEABLE_WITH_PICKAXE).contains(blockState.getBlock())){
                return true;
            }
        }
        if(toolItem instanceof AxeItem){
            if(tags.getTag(BlockTags.MINEABLE_WITH_AXE).contains(blockState.getBlock())){
                return true;
            }
        }
        if(toolItem instanceof ShovelItem){
            if(tags.getTag(BlockTags.MINEABLE_WITH_SHOVEL).contains(blockState.getBlock())){
                return true;
            }
        }
        if(toolItem instanceof HoeItem){
            if(tags.getTag(BlockTags.MINEABLE_WITH_HOE).contains(blockState.getBlock())){
                return true;
            }

            if(tags.getTag(BlockTags.REPLACEABLE).contains(blockState.getBlock()) || tags.getTag(BlockTags.FLOWERS).contains(blockState.getBlock())){
                return true;
            }
        }
        if(toolItem instanceof ShearsItem){
            if(tags.getTag(BlockTags.LEAVES).contains(blockState.getBlock())){
                return true;
            }

            if(tags.getTag(BlockTags.WOOL).contains(blockState.getBlock())){
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isToolCorrectType(ItemStack tool) {
        return true;
    }

    @Override
    protected boolean shouldSkipBlockUniversal(MiningEventInfo eventInfo, BlockPos pos, BlockState blockState) {
        if(super.shouldSkipBlockUniversal(eventInfo, pos, blockState)){
            return true;
        }

        Item toolItem = eventInfo.tool().getItem();
        Level level = eventInfo.player().level();

        if (toolItem instanceof HoeItem) {
            // Skip if this is the wrong tool AND the block is not instamineable.
            if(!isBestToolForMiningBlock(toolItem,blockState) && blockState.getDestroySpeed(level, pos) > ModConfig.INSTAMINE_THRESHOLD.get()){
                return true;
            }
            return false;
        } else {
            // Skip if this is the wrong tool.
            if(!isBestToolForMiningBlock(toolItem,blockState)){
                return true;
            }
            // Skip instamineable blocksIterator (eg torches).
            if(blockState.getDestroySpeed(level, pos) <= ModConfig.INSTAMINE_THRESHOLD.get()){
                return true;
            }
            return false;
        }
    }

    @Override
    protected boolean doesStartingBlockQualify(MiningEventInfo miningEventInfo, BlockPos pos, BlockState blockState) {
        return true;
    }

    @Override
    protected boolean doesNeighborBlockQualify(MiningEventInfo miningEventInfo, BlockPos originPos, BlockState originState, BlockPos neighborPos, BlockState neighborState) {
        Level level = miningEventInfo.player().level();

        float originDestroySpeed = originState.getDestroySpeed(level, originPos);
        float neighborDestroySpeed = neighborState.getDestroySpeed(level, neighborPos);
        if (originDestroySpeed <= ModConfig.INSTAMINE_THRESHOLD.get()) {
            // If origin is instamined, only mine other instamineable blocksIterator.
            return neighborDestroySpeed <= ModConfig.INSTAMINE_THRESHOLD.get();
        } else {
            // If origin is not instamined, only mine blocksIterator with destroy speed within cheat limit.
            return neighborDestroySpeed <= originDestroySpeed + ModConfig.MINING_SPEED_CHEAT_CAP.get();
        }
    }

    @Override
    protected void perform(MiningEventInfo event, List<BlockPos> blocks) {
        ItemStack tool = event.tool();

        if(event.player() instanceof ServerPlayer serverPlayer){
            // The damage calculation might decrease how much damage the tool takes.
            // As a precaution, temporarily set the tool to undamaged such that it doesn't break prematurely.
            int initialDamage = tool.getDamageValue();
            tool.setDamageValue(0);

            for (BlockPos block : blocks) {
                serverPlayer.gameMode.destroyBlock(block);
            }

            int rawDamageTaken = tool.getDamageValue();
            int damagePenalty = ModConfig.DURABILITY_MODE.get().computeDamage(rawDamageTaken);

            int newDamage = initialDamage + damagePenalty;
            tool.setDamageValue(newDamage);

            // Make sure tool breaks if it's supposed to.
            if (newDamage >= tool.getMaxDamage()) {
                tool.hurtAndBreak(0, serverPlayer, (a) -> {
                });
            }
        }

    }

    private static final ToolRenderEvents.FloatColor WIREFRAME_COLOR = new ToolRenderEvents.FloatColor(1.0f, 0.4f, 0.4f);
    @Override
    protected ToolRenderEvents.FloatColor getWireframeColorForEvent(MiningEventInfo event) {
        return WIREFRAME_COLOR;
    }
}