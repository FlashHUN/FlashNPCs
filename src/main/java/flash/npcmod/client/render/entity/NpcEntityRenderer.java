package flash.npcmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import flash.npcmod.core.client.SkinUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.EntityInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class NpcEntityRenderer extends LivingEntityRenderer<NpcEntity, PlayerModel<NpcEntity>> {

  private static PlayerModel<NpcEntity> STEVE_MODEL;
  private static PlayerModel<NpcEntity> ALEX_MODEL;

  private LivingEntity currentRenderedEntity;
  private EntityRenderer currentRenderer;
  private Map<String, EntityRenderer> renderers = new HashMap<>();

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

    STEVE_MODEL = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
    ALEX_MODEL = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
  }

  @Override
  public void render(NpcEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn,
                     MultiBufferSource bufferIn, int packedLightIn) {
    if (shouldRenderAsPlayer(entityIn)) {
      if (entityIn.isSlim())
        this.model = ALEX_MODEL;
      else
        this.model = STEVE_MODEL;
      this.shadowRadius = 0.5f * Math.max(entityIn.getScaleX(), entityIn.getScaleZ());
      this.shadowStrength = 1.0f;

      setModelProperties(entityIn);
      renderPlayerModel(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }
    else {
      setRenderer(entityIn);
      this.currentRenderedEntity.tickCount = entityIn.tickCount;
      copyRotationsAndAnim(entityIn);
      this.shadowRadius = currentRenderer.shadowRadius * Math.max(entityIn.getScaleX(), entityIn.getScaleZ());
      this.shadowStrength = currentRenderer.shadowStrength;
      matrixStackIn.pushPose();
      matrixStackIn.scale(entityIn.getScaleX(), entityIn.getScaleY(), entityIn.getScaleZ());
      currentRenderer.render(currentRenderedEntity, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
      matrixStackIn.popPose();
    }
  }

  private void copyRotationsAndAnim(NpcEntity npcEntity) {
    if (currentRenderedEntity != null) {
      currentRenderedEntity.setYRot(npcEntity.getYRot());
      currentRenderedEntity.yRotO = npcEntity.yRotO;
      currentRenderedEntity.setYBodyRot(npcEntity.yBodyRot);
      currentRenderedEntity.yBodyRotO = npcEntity.yBodyRotO;

      currentRenderedEntity.setYHeadRot(npcEntity.getYHeadRot());
      currentRenderedEntity.yHeadRotO = npcEntity.yHeadRotO;

      currentRenderedEntity.setXRot(npcEntity.getXRot());
      currentRenderedEntity.xRotO = npcEntity.xRotO;


      currentRenderedEntity.animationPosition = npcEntity.animationPosition;
      currentRenderedEntity.animationSpeedOld = npcEntity.animationSpeedOld;
      currentRenderedEntity.animationSpeed = npcEntity.animationSpeed;
    }
  }

  private void setRenderer(NpcEntity npcEntity) {
    LivingEntity renderedEntity = npcEntity.getRenderedEntity();
    if (currentRenderedEntity != renderedEntity) {
      String key = npcEntity.getRenderedEntityTypeKey();
      if (renderers.containsKey(key)) {
        currentRenderer = renderers.get(key);
        currentRenderedEntity = renderedEntity;
      } else {
        var renderer = this.entityRenderDispatcher.getRenderer(renderedEntity);
        if (renderer instanceof LivingEntityRenderer) {
          currentRenderedEntity = renderedEntity;
          currentRenderer = renderer;
          renderers.put(key, renderer);
        } else {
          renderers.put(key, null);
          npcEntity.clearRenderedEntity();
          currentRenderedEntity = null;
          currentRenderer = null;
        }
      }
    }
  }

  private boolean shouldRenderAsPlayer(NpcEntity npcEntity) {
    return npcEntity.getRenderedEntity() == null || npcEntity.getRenderedEntityType().equals(npcEntity.getType());
  }

  @Override
  public ResourceLocation getTextureLocation(NpcEntity entity) {
    try {
      if (shouldRenderAsPlayer(entity)) {
        if (entity.isTextureResourceLocation()) {
          ResourceLocation texture = ResourceLocation.tryParse(entity.getTexture());
          return texture == null ? DefaultPlayerSkin.getDefaultSkin() : texture;
        } else {
          return SkinUtil.loadSkin(entity.getTexture(), DefaultPlayerSkin.getDefaultSkin(), true);
        }
      }
      return DefaultPlayerSkin.getDefaultSkin();
    } catch (Exception ignored) {
      return DefaultPlayerSkin.getDefaultSkin();
    }
  }

  private void setModelProperties(NpcEntity npcEntity) {
    var playermodel = this.getModel();
    if (npcEntity.isSpectator()) {
      playermodel.setAllVisible(false);
      playermodel.head.visible = true;
      playermodel.hat.visible = true;
    } else {
      playermodel.setAllVisible(true);
      playermodel.hat.visible = npcEntity.isModelPartShown(PlayerModelPart.HAT);
      playermodel.jacket.visible = npcEntity.isModelPartShown(PlayerModelPart.JACKET);
      playermodel.leftPants.visible = npcEntity.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
      playermodel.rightPants.visible = npcEntity.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
      playermodel.leftSleeve.visible = npcEntity.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
      playermodel.rightSleeve.visible = npcEntity.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
      playermodel.crouching = npcEntity.isCrouching();
      var mainHand = getArmPose(npcEntity, InteractionHand.MAIN_HAND);
      var offHand = getArmPose(npcEntity, InteractionHand.OFF_HAND);
      if (mainHand.isTwoHanded()) {
        offHand = npcEntity.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
      }

      if (npcEntity.getMainArm() == HumanoidArm.RIGHT) {
        playermodel.rightArmPose = mainHand;
        playermodel.leftArmPose = offHand;
      } else {
        playermodel.rightArmPose = offHand;
        playermodel.leftArmPose = mainHand;
      }
    }
  }

  private static HumanoidModel.ArmPose getArmPose(NpcEntity player, InteractionHand hand) {
    ItemStack itemstack = player.getItemInHand(hand);
    if (itemstack.isEmpty()) {
      return HumanoidModel.ArmPose.EMPTY;
    } else {
      if (player.getUsedItemHand() == hand && player.getUseItemRemainingTicks() > 0) {
        UseAnim useanim = itemstack.getUseAnimation();
        if (useanim == UseAnim.BLOCK) {
          return HumanoidModel.ArmPose.BLOCK;
        }

        if (useanim == UseAnim.BOW) {
          return HumanoidModel.ArmPose.BOW_AND_ARROW;
        }

        if (useanim == UseAnim.SPEAR) {
          return HumanoidModel.ArmPose.THROW_SPEAR;
        }

        if (useanim == UseAnim.CROSSBOW && hand == player.getUsedItemHand()) {
          return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
        }

        if (useanim == UseAnim.SPYGLASS) {
          return HumanoidModel.ArmPose.SPYGLASS;
        }
      } else if (!player.swinging && itemstack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(itemstack)) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
      }

      return HumanoidModel.ArmPose.ITEM;
    }
  }

  @Nullable
  @Override
  protected RenderType getRenderType(NpcEntity npcEntity, boolean p_115323_, boolean p_115324_, boolean p_115325_) {
    ResourceLocation resourcelocation = this.getTextureLocation(npcEntity);
    if (p_115324_) {
      return RenderType.itemEntityTranslucentCull(resourcelocation);
    } else if (p_115323_ && (shouldRenderAsPlayer(npcEntity) || currentRenderer == null)) {
      return this.model.renderType(resourcelocation);
    } else {
      return p_115325_ ? RenderType.outline(resourcelocation) : null;
    }
  }

  private void renderPlayerModel(NpcEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn,
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
    matrixStackIn.scale(entityIn.getScaleX(), entityIn.getScaleY(), entityIn.getScaleZ());
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
    net.minecraftforge.client.event.RenderNameplateEvent renderNameplateEvent = new net.minecraftforge.client.event.RenderNameplateEvent(entityIn, entityIn.getDisplayName(), this, matrixStackIn, bufferIn, packedLightIn, partialTicks);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(renderNameplateEvent);
    if (renderNameplateEvent.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY && (renderNameplateEvent.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || this.shouldShowName(entityIn))) {
      this.renderNameTag(entityIn, renderNameplateEvent.getContent(), matrixStackIn, bufferIn, packedLightIn);
    }
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<>(entityIn, this, partialTicks, matrixStackIn, bufferIn, packedLightIn));
  }

  @Override
  public Vec3 getRenderOffset(NpcEntity npcEntity, float p_117786_) {
    if (shouldRenderAsPlayer(npcEntity)) {
      return npcEntity.isCrouching() ? new Vec3(0.0D, -0.125D, 0.0D) : npcEntity.isSitting() ? new Vec3(0.0D, -0.55D, 0.0D) : super.getRenderOffset(npcEntity, p_117786_);
    }
    return Vec3.ZERO;
  }

  @Override
  protected void scale(NpcEntity npcEntity, PoseStack matrixStackIn, float partialTickTime) {
    float scale = 0.9375F;
    matrixStackIn.scale(scale, scale, scale);
  }

  @Override
  protected void renderNameTag(NpcEntity entityIn, Component displayNameIn, PoseStack matrixStackIn,
                            MultiBufferSource bufferIn, int packedLightIn) {
    if (entityIn.isCustomNameVisible()) {
      double d0 = this.entityRenderDispatcher.distanceToSqr(entityIn);
      if (net.minecraftforge.client.ForgeHooksClient.isNameplateInRenderDistance(entityIn, d0)) {
        boolean flag = !entityIn.isDiscrete();
        boolean isTitleVisible = entityIn.isTitleVisible();
        float f = entityIn.getBbHeight() + (entityIn.isSitting() ? 1.0F : 0.5F);
        int i = isTitleVisible ? -10 : 0;
        matrixStackIn.pushPose();
        matrixStackIn.translate(0.0D, (double)f, 0.0D);
        matrixStackIn.mulPose(this.entityRenderDispatcher.cameraOrientation());
        matrixStackIn.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrixStackIn.last().pose();
        float f1 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int j = (int)(f1 * 255.0F) << 24;
        Font font = this.getFont();
        float f2 = (float)(-font.width(displayNameIn) / 2);
        font.drawInBatch(displayNameIn, f2, (float)i, 553648127, false, matrix4f, bufferIn, flag, j, packedLightIn);
        if (flag) {
          font.drawInBatch(displayNameIn, f2, (float)i, -1, false, matrix4f, bufferIn, false, 0, packedLightIn);
        }
        if (isTitleVisible) {
          Component title = entityIn.getTitleComponent();
          float f3 = (float)(-font.width(title) / 2);
          font.drawInBatch(title, f3, 0f, 553648127, false, matrix4f, bufferIn, flag, j, packedLightIn);
          if (flag) {
            font.drawInBatch(title, f3, 0f, -1, false, matrix4f, bufferIn, false, 0, packedLightIn);
          }
        }

        matrixStackIn.popPose();
      }
    }
  }
}
