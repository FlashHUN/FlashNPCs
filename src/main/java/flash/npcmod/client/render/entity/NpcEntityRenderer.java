package flash.npcmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import flash.npcmod.core.client.SkinUtil;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NpcEntityRenderer extends LivingEntityRenderer<NpcEntity, PlayerModel<NpcEntity>> {

  private static PlayerModel<NpcEntity> normalModel;
  private static PlayerModel<NpcEntity> slimModel;

  public NpcEntityRenderer(EntityRendererProvider.Context context) {
    this(context, false);
  }

  public NpcEntityRenderer(EntityRendererProvider.Context context, boolean useSmallArms) {
    super(context, new PlayerModel<>(context.bakeLayer(useSmallArms ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), useSmallArms), 0.5F);
    this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(useSmallArms ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(useSmallArms ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR))));
    this.addLayer(new ItemInHandLayer<>(this));
    this.addLayer(new ArrowLayer<>(context, this));
    this.addLayer(new CustomHeadLayer<>(this, context.getModelSet()));
    this.addLayer(new ElytraLayer<>(this, context.getModelSet()));
    this.addLayer(new SpinAttackEffectLayer<>(this, context.getModelSet()));
    this.addLayer(new BeeStingerLayer<>(this));

    normalModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
    slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
  }

  @Override
  public ResourceLocation getTextureLocation(NpcEntity entity) {
    try {
      return SkinUtil.loadSkin(entity.getTexture());
    } catch (Exception e) {
      return DefaultPlayerSkin.getDefaultSkin();
    }
  }

  private void setModelProperties(NpcEntity npcEntity) {
    this.model.crouching = npcEntity.isCrouching();
  }

  @Override
  public void render(NpcEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn,
                     MultiBufferSource bufferIn, int packedLightIn) {
    if (entityIn.isSlim())
      this.model = slimModel;
    else
      this.model = normalModel;

    setModelProperties(entityIn);

    render2(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
  }
  
  private void render2(NpcEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn,
                       MultiBufferSource bufferIn, int packedLightIn) {
    if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Pre<>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn))) return;
    matrixStackIn.pushPose();
    this.model.attackTime = this.getAttackAnim(entityIn, partialTicks);

    boolean shouldSit = entityIn.isSitting() || (entityIn.isPassenger() && entityIn.getVehicle() != null && entityIn.getVehicle().shouldRiderSit());
    this.model.riding = shouldSit;
    this.model.young = entityIn.isBaby();
    float f = Mth.rotLerp(partialTicks, entityIn.yBodyRotO, entityIn.yBodyRot);
    float f1 = Mth.rotLerp(partialTicks, entityIn.yHeadRotO, entityIn.yHeadRot);
    float f2 = f1 - f;
    if (shouldSit && entityIn.getVehicle() instanceof LivingEntity livingentity) {
      f = Mth.rotLerp(partialTicks, livingentity.yBodyRotO, livingentity.yBodyRot);
      f2 = f1 - f;
      float f3 = Mth.wrapDegrees(f2);
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

    float f6 = Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot());
    if (isEntityUpsideDown(entityIn)) {
      f6 *= -1.0F;
      f2 *= -1.0F;
    }

    if (entityIn.getPose() == Pose.SLEEPING) {
      Direction direction = entityIn.getBedOrientation();
      if (direction != null) {
        float f4 = entityIn.getEyeHeight(Pose.STANDING) - 0.1F;
        matrixStackIn.translate((float)(-direction.getStepX()) * f4, 0.0D, (float)(-direction.getStepZ()) * f4);
      }
    }

    float f7 = this.getBob(entityIn, partialTicks);
    this.setupRotations(entityIn, matrixStackIn, f7, f, partialTicks);
    matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
    this.scale(entityIn, matrixStackIn, partialTicks);
    matrixStackIn.translate(0.0D, -1.501F, 0.0D);
    float f8 = 0.0F;
    float f5 = 0.0F;
    if (!shouldSit && entityIn.isAlive()) {
      f8 = Mth.lerp(partialTicks, entityIn.animationSpeedOld, entityIn.animationSpeed);
      f5 = entityIn.animationPosition - entityIn.animationSpeed * (1.0F - partialTicks);
      if (entityIn.isBaby()) {
        f5 *= 3.0F;
      }

      if (f8 > 1.0F) {
        f8 = 1.0F;
      }
    }

    this.model.prepareMobModel(entityIn, f5, f8, partialTicks);
    this.model.setupAnim(entityIn, f5, f8, f7, f2, f6);
    Minecraft minecraft = Minecraft.getInstance();
    boolean flag = this.isBodyVisible(entityIn);
    boolean flag1 = !flag && !entityIn.isInvisibleTo(minecraft.player);
    boolean flag2 = minecraft.shouldEntityAppearGlowing(entityIn);
    RenderType rendertype = this.getRenderType(entityIn, flag, flag1, flag2);
    if (rendertype != null) {
      VertexConsumer vertexconsumer = bufferIn.getBuffer(rendertype);
      int i = getOverlayCoords(entityIn, this.getWhiteOverlayProgress(entityIn, partialTicks));
      this.model.renderToBuffer(matrixStackIn, vertexconsumer, packedLightIn, i, 1.0F, 1.0F, 1.0F, flag1 ? 0.15F : 1.0F);
    }

    if (!entityIn.isSpectator()) {
      for(RenderLayer<NpcEntity, PlayerModel<NpcEntity>> renderlayer : this.layers) {
        renderlayer.render(matrixStackIn, bufferIn, packedLightIn, entityIn, f5, f8, partialTicks, f7, f2, f6);
      }
    }

    matrixStackIn.popPose();
    render3(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn));
  }

  private void render3(NpcEntity entityIn, float entityYaw, float partialTicksIn, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
    net.minecraftforge.client.event.RenderNameplateEvent renderNameplateEvent = new net.minecraftforge.client.event.RenderNameplateEvent(entityIn, entityIn.getDisplayName(), this, matrixStackIn, bufferIn, packedLightIn, partialTicksIn);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(renderNameplateEvent);
    if (renderNameplateEvent.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY && (renderNameplateEvent.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || this.shouldShowName(entityIn))) {
      this.renderNameTag(entityIn, renderNameplateEvent.getContent(), matrixStackIn, bufferIn, packedLightIn);
    }
  }

  @Override
  public Vec3 getRenderOffset(NpcEntity p_117785_, float p_117786_) {
    return p_117785_.isCrouching() ? new Vec3(0.0D, -0.125D, 0.0D) : p_117785_.isSitting() ? new Vec3(0.0D, -0.55D, 0.0D) : super.getRenderOffset(p_117785_, p_117786_);
  }

  @Override
  protected void scale(NpcEntity entitylivingbaseIn, PoseStack matrixStackIn, float partialTickTime) {
    float scale = 0.9375F;
    matrixStackIn.scale(scale, scale, scale);
    super.scale(entitylivingbaseIn, matrixStackIn, partialTickTime);
  }

  @Override
  protected void renderNameTag(NpcEntity entityIn, Component displayNameIn, PoseStack matrixStackIn,
                            MultiBufferSource bufferIn, int packedLightIn) {
    if (entityIn.isCustomNameVisible()) super.renderNameTag(entityIn, displayNameIn, matrixStackIn, bufferIn, packedLightIn);
  }
}
