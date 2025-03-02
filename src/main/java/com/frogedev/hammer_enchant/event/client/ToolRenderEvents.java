package com.frogedev.hammer_enchant.event.client;

import com.frogedev.hammer_enchant.HammerEnchantMod;
import com.frogedev.hammer_enchant.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
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
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.Iterator;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = HammerEnchantMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ToolRenderEvents {
    /**
     * Maximum number of blocks from the iterator to render
     */
    private static final int MAX_BLOCKS = 60;

    public record FloatColor(float r, float g, float b){};

    private static Field field_LevelRenderer_DestroyingBlocks;

    static {
        field_LevelRenderer_DestroyingBlocks = ObfuscationReflectionHelper.findField(LevelRenderer.class, "destroyingBlocks");
        field_LevelRenderer_DestroyingBlocks.setAccessible(true);
    }

    private static Int2ObjectMap<BlockDestructionProgress> getBlockDestructionProgress(LevelRenderer levelRenderer) {
        try {
            return (Int2ObjectMap<BlockDestructionProgress>) field_LevelRenderer_DestroyingBlocks.get(levelRenderer);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static final UseEventHammerHandler[] USE_HANDLERS = {
            ToolUseHandler.INSTANCE,
    };
    private static final MiningEventHammerHandler[] MINING_HANDLERS = {
            MiningHandler.INSTANCE,
    };

    /**
     * Renders the outline on the extra blocks
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

        record HandlerResult(Iterator<BlockPos> blocks, FloatColor wireframeColor){}
        HandlerResult handlerResult = null;

        // See if any tool-use handlers qualify.
        for(UseEventHammerHandler handler : USE_HANDLERS){
            Iterator<BlockPos> blocks = handler.computeCandidatePositionsForFirstQualifyingEvent(handler.expandBaseEventIntoCandidateEvents(player, origin, blockTrace.getDirection()).iterator());
            if(blocks.hasNext()){
                handlerResult = new HandlerResult(blocks, handler.getWireframeColor());
            }
        }

//        // See if any mining handlers qualify.
//        if(handlerResult == null){
//            for(MiningEventHammerHandler handler : MINING_HANDLERS){
//                Iterator<BlockPos> blocks = handler.computeCandidatePositionsForFirstQualifyingEvent(handler.expandBaseEventIntoCandidateEvents(player, origin, blockTrace.getDirection()).iterator());
//                if(blocks.hasNext()){
//                    handlerResult = new HandlerResult(blocks, handler.getWireframeColor());
//                }
//            }
//        }

        // If no handlers qualify, don't render anything.
        if (handlerResult == null) {
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
        int rendered = 0;

        CollisionContext collisionContext = CollisionContext.of(viewEntity);

        do {
            BlockPos pos = handlerResult.blocks.next();

            if (level.getWorldBorder().isWithinBounds(pos)) {
                Vec3 camPos = camera.getPosition();
                rendered++;
                highlightBlock(pos, matrices, level, camPos, buffers, 0.0f, handlerResult.wireframeColor.r, handlerResult.wireframeColor.g, handlerResult.wireframeColor.b);
            }
        } while (rendered < MAX_BLOCKS && handlerResult.blocks.hasNext());

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

//    /**
//     * Renders the block damage process on the extra blocks
//     */
//    @SubscribeEvent
//    public static void onRenderLevelStage(RenderLevelStageEvent event) {
//        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
//            return;
//        }
//
//        Player player = Minecraft.getInstance().player;
//        Level level = player.level();
//
//        ItemStack tool = player.getMainHandItem();
//        if (!HammerHelper.hasHammerModifiers(tool)) {
//            return;
//        }
//
//        if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult blockTrace)) {
//            return;
//        }
//
//        BlockPos origin = blockTrace.getBlockPos();
//        ToolMode activeMode = ToolMode.None;
//
//        // Find the active tool mode.
//        for (ToolMode candidateMode : MODE_ATTEMPT_ORDER) {
//            if (candidateMode.handler.isToolCorrectType(tool) && candidateMode.handler.testOrigin(level, player, tool, origin)) {
//                activeMode = candidateMode;
//                break;
//            }
//        }
//
//        // If no tool mode qualifies, do nothing.
//        if (activeMode == ToolMode.None) {
//            return;
//        }
//
//        Iterator<BlockPos> breakableBlocks = HammerShapeHelper.getCandidateBlockPositions(
//                player,
//                tool,
//                Minecraft.getInstance().hitResult,
//                origin,
//                activeMode.handler
//        );
//
//        LevelRenderer levelRenderer = Minecraft.getInstance().levelRenderer;
//        Int2ObjectMap<BlockDestructionProgress> destroyingBlocks = getBlockDestructionProgress(levelRenderer);
//
//        if (destroyingBlocks == null) {
//            return;
//        }
//
//        BlockDestructionProgress destroyProgress = null;
//        for (Int2ObjectMap.Entry<BlockDestructionProgress> entry : destroyingBlocks.int2ObjectEntrySet()) {
//            if (entry.getValue().getPos().equals(origin)) {
//                destroyProgress = entry.getValue();
//                break;
//            }
//        }
//        if (destroyProgress == null) {
//            return;
//        }
//
//        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
//        PoseStack matrices = event.getPoseStack();
//        PoseStack poseStack = event.getPoseStack();
//        RenderBuffers renderBuffers = Minecraft.getInstance().renderBuffers();
//        MultiBufferSource.BufferSource breakBufferSource = renderBuffers.crumblingBufferSource();
//        RenderType destroyRenderType = ModelBakery.DESTROY_TYPES.get(destroyProgress.getProgress());
//
//        // Translate back to origin
//        Camera camera = event.getCamera();
//        double x = camera.getPosition().x;
//        double y = camera.getPosition().y;
//        double z = camera.getPosition().z;
//        poseStack.pushPose();
//        poseStack.translate(-x, -y, -z);
//
//        while (breakableBlocks.hasNext()) {
//            BlockPos blockPos = breakableBlocks.next();
//            BlockState blockState = level.getBlockState(blockPos);
//
//            poseStack.pushPose();
//            poseStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
//            PoseStack.Pose lastPose = poseStack.last();
//
//            VertexConsumer vertexConsumer = new SheetedDecalTextureGenerator(breakBufferSource.getBuffer(destroyRenderType), lastPose.pose(), lastPose.normal(), 1.0F);
//            blockRenderer.renderBreakingTexture(blockState, blockPos, level, poseStack, vertexConsumer, ModelData.EMPTY);
//
//            poseStack.popPose();
//        }
//
//        poseStack.popPose();
//    }
}