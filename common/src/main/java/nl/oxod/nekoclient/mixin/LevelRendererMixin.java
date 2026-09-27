/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import nl.oxod.nekoclient.mixininterface.IEntityRenderState;
import nl.oxod.nekoclient.mixininterface.ILevelRenderer;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.render.BlockSelection;
import nl.oxod.nekoclient.systems.modules.render.ESP;
import nl.oxod.nekoclient.systems.modules.render.NoRender;
import nl.oxod.nekoclient.utils.OutlineRenderCommandQueue;
import nl.oxod.nekoclient.utils.render.color.Color;
import nl.oxod.nekoclient.utils.render.postprocess.EntityShader;
import nl.oxod.nekoclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

import static nl.oxod.nekoclient.NekoClient.mc;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements ILevelRenderer {
    @Unique
    private FeatureRenderDispatcher renderDispatcher;
    @Unique
    private final OutlineRenderCommandQueue outlineRenderCommandQueue = new OutlineRenderCommandQueue();

    @Inject(method = "checkPoseStack", at = @At("HEAD"), cancellable = true)
    private void onCheckPoseStack(PoseStack poseStack, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedHitOutline(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (Modules.get().isActive(BlockSelection.class)) ci.cancel();
    }

    // No Render

    // The wildcard is load-bearing: NeoForge adds a deprecated 3-arg `addSkyPass` overload that
    // just delegates to the real one, so a bare method name resolves to the overload that does
    // not read the field and the injection silently matches nothing. Matching all overloads
    // lets @At pick the one that actually performs the field read on each loader.
    @ModifyExpressionValue(method = "addSkyPass*", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/CameraEntityRenderState;doesMobEffectBlockSky:Z", opcode = Opcodes.GETFIELD))
    private boolean modifyMobEffectBlocksSky(boolean original) {
        NoRender noRender = Modules.get().get(NoRender.class);
        if (noRender.noBlindness() || noRender.noDarkness()) return false;
        return original;
    }

    // Entity Shaders

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderLevelHead(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        PostProcessShaders.beginRender();
    }

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void onSubmitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output, CallbackInfo ci) {
        if (renderDispatcher == null) {
            renderDispatcher = new FeatureRenderDispatcher(
                renderBuffers,
                mc.getModelManager(),
                mc.getAtlasManager(),
                mc.font,
                mc.gameRenderer.gameRenderState()
            );
        }

        draw(levelRenderState, poseStack, PostProcessShaders.CHAMS, _ -> Color.WHITE);
        draw(levelRenderState, poseStack, PostProcessShaders.ENTITY_OUTLINE, entity -> Modules.get().get(ESP.class).getColor(entity));
    }

    @Unique
    private void draw(LevelRenderState worldState, PoseStack matrices, EntityShader shader, Function<Entity, Color> colorGetter) {
        var camera = worldState.cameraRenderState.pos;
        var empty = true;

        for (var state : worldState.entityRenderStates) {
            Entity entity = ((IEntityRenderState) state).neko$getEntity();
            if (entity == null) continue;

            if (!shader.shouldDraw(entity)) continue;

            var color = colorGetter.apply(entity);
            if (color == null) continue;
            outlineRenderCommandQueue.setColor(color);

            var renderer = entityRenderDispatcher.getRenderer(state);
            var offset = renderer.getRenderOffset(state);

            matrices.pushPose();
            matrices.translate(state.x - camera.x + offset.x, state.y - camera.y + offset.y, state.z - camera.z + offset.z);
            renderer.submit(state, matrices, outlineRenderCommandQueue, worldState.cameraRenderState);
            matrices.popPose();

            empty = false;
        }

        if (empty)
            return;

        neko$pushEntityOutlineFramebuffer(shader.framebuffer);
        try {
            try (var frame = renderDispatcher.prepareFrame(outlineRenderCommandQueue)) {
                frame.executeOutline();
            }
        } finally {
            outlineRenderCommandQueue.submitsPerOrder.clear();
            neko$popEntityOutlineFramebuffer();
        }
    }

    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeOutline()V", shift = At.Shift.AFTER))
    private void addMainPass$submitEntityVertices(CallbackInfo ci) {
        PostProcessShaders.submitEntityVertices();
    }

    @Inject(method = "resize", at = @At("HEAD"))
    private void onResize(int width, int height, CallbackInfo ci) {
        PostProcessShaders.onResized(width, height);
    }

    // ILevelRenderer

    @Shadow
    @Final
    @Mutable
    private RenderTarget entityOutlineTarget;

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow
    @Final
    private RenderBuffers renderBuffers;
    @Unique
    private Stack<RenderTarget> framebufferStack;

    @Unique
    private Stack<ResourceHandle<RenderTarget>> framebufferHandleStack;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init$IWorldRenderer(CallbackInfo ci) {
        framebufferStack = new ObjectArrayList<>();
        framebufferHandleStack = new ObjectArrayList<>();
    }

    @Override
    public void neko$pushEntityOutlineFramebuffer(RenderTarget framebuffer) {
        framebufferStack.push(this.entityOutlineTarget);
        this.entityOutlineTarget = framebuffer;

        framebufferHandleStack.push(this.targets.entityOutline);
        this.targets.entityOutline = () -> framebuffer;
    }

    @Override
    public void neko$popEntityOutlineFramebuffer() {
        this.entityOutlineTarget = framebufferStack.pop();
        this.targets.entityOutline = framebufferHandleStack.pop();
    }
}
