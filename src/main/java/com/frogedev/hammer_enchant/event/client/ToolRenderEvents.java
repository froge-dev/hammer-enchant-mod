package com.frogedev.hammer_enchant.event.client;

import com.frogedev.hammer_enchant.HammerEnchantMod;
import com.frogedev.hammer_enchant.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = HammerEnchantMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ToolRenderEvents {
    /**
     * Maximum number of blocksIterator from the iterator to render
     */
    private static final int MAX_BLOCKS = 75;

    public record FloatColor(float r, float g, float b){};

    private static final BaseHammerHandler[] HANDLERS = {ToolUseHandler.INSTANCE, MiningHandler.INSTANCE};

    /**
     * Renders the outline on the extra blocksIterator
     *
     * @param event the highlight event
     */
    @SubscribeEvent
    public static void renderBlockHighlights(RenderHighlightEvent.Block event) {
        Level level = Minecraft.getInstance().level;
        Player player = Minecraft.getInstance().player;
        if (level == null || player == null) {
            return;
        }

        ItemStack tool = player.getMainHandItem();
        BlockHitResult blockTrace = event.getTarget();
        BlockPos origin = blockTrace.getBlockPos();

        BaseHammerHandler.HammerTarget hammerTarget = null;

        // See if any handlers qualify.
        for(BaseHammerHandler handler : HANDLERS){
            BaseHammerHandler.HammerTarget target = handler.computeTargetForBaseEvent(player, origin, blockTrace.getDirection());
            if(target != null){
                hammerTarget = target;
                break;
            }
        }

        // If no handlers qualify, don't render anything.
        if (hammerTarget == null) {
            return;
        }

        // set up renderer
        LevelRenderer worldRender = event.getLevelRenderer();
        PoseStack matrices = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        VertexConsumer vertexBuilder = buffers.getBuffer(RenderType.lines());
        matrices.pushPose();

        // start drawing
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Entity viewEntity = camera.getEntity();

        CollisionContext collisionContext = CollisionContext.of(viewEntity);

        int rendered = 0;
        while (rendered < MAX_BLOCKS && hammerTarget.blocksIterator().hasNext()) {
            BlockPos pos = hammerTarget.blocksIterator().next();

            if (level.getWorldBorder().isWithinBounds(pos)) {
                Vec3 camPos = camera.getPosition();
                rendered++;
                highlightBlock(pos, matrices, level, camPos, buffers, 0.0f, hammerTarget.wireframeColor().r, hammerTarget.wireframeColor().g, hammerTarget.wireframeColor().b);
            }
        }

        matrices.popPose();
        event.setCanceled(true);
    }

    // From SupportBlockRenderer:highlightPosition
    private static void highlightBlock(BlockPos pos, PoseStack poseStack, Level level, Vec3 camPos, MultiBufferSource bufferSource, double pBias, float pRed, float pGreen, float pBlue) {
        VertexConsumer vertexBuilder = bufferSource.getBuffer(RenderType.lines());
        VoxelShape shape = level
                .getBlockState(pos)
                .getShape(level, pos)
                .move(pos.getX(), pos.getY(), pos.getZ());

        LevelRenderer.renderVoxelShape(poseStack, vertexBuilder, shape, -camPos.x, -camPos.y, -camPos.z, pRed, pGreen, pBlue, 1.0F, false);
    }
}