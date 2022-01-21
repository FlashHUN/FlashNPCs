package flash.npcmod.client.render.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import flash.npcmod.core.client.SkinUtil;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NpcEntityRenderer extends LivingRenderer<NpcEntity, PlayerModel<NpcEntity>> {

  private static final PlayerModel<NpcEntity> normalModel = new PlayerModel<>(0f, false);
  private static final PlayerModel<NpcEntity> slimModel = new PlayerModel<>(0f, true);

  public NpcEntityRenderer(EntityRendererManager renderManager) {
    this(renderManager, false);
  }

  public NpcEntityRenderer(EntityRendererManager renderManager, boolean useSmallArms) {
    super(renderManager, new PlayerModel<>(0.0F, useSmallArms), 0.5F);
    this.addLayer(new BipedArmorLayer<>(this, new BipedModel<>(0.5F), new BipedModel<>(1.0F)));
    this.addLayer(new HeldItemLayer<>(this));
    this.addLayer(new ArrowLayer<>(this));
    this.addLayer(new HeadLayer<>(this));
    this.addLayer(new ElytraLayer<>(this));
    this.addLayer(new SpinAttackEffectLayer<>(this));
    this.addLayer(new BeeStingerLayer<>(this));
  }

  @Override
  public ResourceLocation getEntityTexture(NpcEntity entity) {
    return SkinUtil.loadSkin(entity.getTexture());
  }

  private void setModelProperties(NpcEntity npcEntity) {
    this.entityModel.isSneak = npcEntity.isCrouching();
  }

  @Override
  public void render(NpcEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn,
                     IRenderTypeBuffer bufferIn, int packedLightIn) {

    if (entityIn.isSlim())
      this.entityModel = slimModel;
    else
      this.entityModel = normalModel;

    setModelProperties(entityIn);

    render2(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
  }

  private void render2(NpcEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
    if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Pre<>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn))) return;
    matrixStackIn.push();
    this.entityModel.swingProgress = this.getSwingProgress(entityIn, partialTicks);

    boolean shouldSit = entityIn.isSitting() || (entityIn.isPassenger() && entityIn.getRidingEntity() != null && entityIn.getRidingEntity().shouldRiderSit());
    this.entityModel.isSitting = shouldSit;
    this.entityModel.isChild = entityIn.isChild();
    float f = MathHelper.interpolateAngle(partialTicks, entityIn.prevRenderYawOffset, entityIn.renderYawOffset);
    float f1 = MathHelper.interpolateAngle(partialTicks, entityIn.prevRotationYawHead, entityIn.rotationYawHead);
    float f2 = f1 - f;
    if (shouldSit && entityIn.getRidingEntity() instanceof LivingEntity) {
      LivingEntity livingentity = (LivingEntity)entityIn.getRidingEntity();
      f = MathHelper.interpolateAngle(partialTicks, livingentity.prevRenderYawOffset, livingentity.renderYawOffset);
      f2 = f1 - f;
      float f3 = MathHelper.wrapDegrees(f2);
      if (f3 < -85.0F) {
        f3 = -85.0F;
      }

      if (f3 >= 85.0F) {
        f3 = 85.0F;
      }

      f = f1 - f3;
      if (f3 * f3 > 2500.0F) {
        f += f3 * 0.2F;
      }

      f2 = f1 - f;
    }

    float f6 = MathHelper.lerp(partialTicks, entityIn.prevRotationPitch, entityIn.rotationPitch);
    if (entityIn.getPose() == Pose.SLEEPING) {
      Direction direction = entityIn.getBedDirection();
      if (direction != null) {
        float f4 = entityIn.getEyeHeight(Pose.STANDING) - 0.1F;
        matrixStackIn.translate((double)((float)(-direction.getXOffset()) * f4), 0.0D, (double)((float)(-direction.getZOffset()) * f4));
      }
    }

    float f7 = this.handleRotationFloat(entityIn, partialTicks);
    this.applyRotations(entityIn, matrixStackIn, f7, f, partialTicks);
    matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
    this.preRenderCallback(entityIn, matrixStackIn, partialTicks);
    matrixStackIn.translate(0.0D, (double)-1.501F, 0.0D);
    float f8 = 0.0F;
    float f5 = 0.0F;
    if (!shouldSit && entityIn.isAlive()) {
      f8 = MathHelper.lerp(partialTicks, entityIn.prevLimbSwingAmount, entityIn.limbSwingAmount);
      f5 = entityIn.limbSwing - entityIn.limbSwingAmount * (1.0F - partialTicks);
      if (entityIn.isChild()) {
        f5 *= 3.0F;
      }

      if (f8 > 1.0F) {
        f8 = 1.0F;
      }
    }

    this.entityModel.setLivingAnimations(entityIn, f5, f8, partialTicks);
    this.entityModel.setRotationAngles(entityIn, f5, f8, f7, f2, f6);
    Minecraft minecraft = Minecraft.getInstance();
    boolean flag = this.isVisible(entityIn);
    boolean flag1 = !flag && !entityIn.isInvisibleToPlayer(minecraft.player);
    boolean flag2 = minecraft.isEntityGlowing(entityIn);
    RenderType rendertype = this.func_230496_a_(entityIn, flag, flag1, flag2);
    if (rendertype != null) {
      IVertexBuilder ivertexbuilder = bufferIn.getBuffer(rendertype);
      int i = getPackedOverlay(entityIn, this.getOverlayProgress(entityIn, partialTicks));
      this.entityModel.render(matrixStackIn, ivertexbuilder, packedLightIn, i, 1.0F, 1.0F, 1.0F, flag1 ? 0.15F : 1.0F);
    }

    if (!entityIn.isSpectator()) {
      for(LayerRenderer<NpcEntity, PlayerModel<NpcEntity>> layerrenderer : this.layerRenderers) {
        layerrenderer.render(matrixStackIn, bufferIn, packedLightIn, entityIn, f5, f8, partialTicks, f7, f2, f6);
      }
    }

    matrixStackIn.pop();
    render3(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn));
  }

  private void render3(NpcEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
    net.minecraftforge.client.event.RenderNameplateEvent renderNameplateEvent = new net.minecraftforge.client.event.RenderNameplateEvent(entityIn, entityIn.getDisplayName(), this, matrixStackIn, bufferIn, packedLightIn, partialTicks);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(renderNameplateEvent);
    if (renderNameplateEvent.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY && (renderNameplateEvent.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || this.canRenderName(entityIn))) {
      this.renderName(entityIn, renderNameplateEvent.getContent(), matrixStackIn, bufferIn, packedLightIn);
    }
  }

  @Override
  public Vector3d getRenderOffset(NpcEntity entityIn, float partialTicks) {
    return entityIn.isCrouching() ? new Vector3d(0.0D, -0.125D, 0.0D) : entityIn.isSitting() ? new Vector3d(0.0D, -0.55D, 0.0D) : super.getRenderOffset(entityIn, partialTicks);
  }

  @Override
  protected void preRenderCallback(NpcEntity entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
    float scale = 0.9375F;
    matrixStackIn.scale(scale, scale, scale);
    super.preRenderCallback(entitylivingbaseIn, matrixStackIn, partialTickTime);
  }

  @Override
  protected void renderName(NpcEntity entityIn, ITextComponent displayNameIn, MatrixStack matrixStackIn,
                            IRenderTypeBuffer bufferIn, int packedLightIn) {
    if (entityIn.isCustomNameVisible()) super.renderName(entityIn, displayNameIn, matrixStackIn, bufferIn, packedLightIn);
  }
}
