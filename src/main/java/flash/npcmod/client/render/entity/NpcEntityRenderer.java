package flash.npcmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import flash.npcmod.Main;
import flash.npcmod.core.client.SkinUtil;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class NpcEntityRenderer extends LivingEntityRenderer<NpcEntity, PlayerModel<NpcEntity>> {

  private static PlayerModel<NpcEntity> normalModel;
  private static PlayerModel<NpcEntity> slimModel;

  private LivingEntity currentEntityToRenderAs;
  private LivingEntityRenderer<?,?> currentRenderer;
  private Method currentGetBobMethod, currentScaleMethod, currentGetWhiteOverlayProgressMethod;
  private final Map<LivingEntityRenderer<?,?>, Method> getBobMethodMap = new HashMap<>();
  private final Map<LivingEntityRenderer<?,?>, Method> scaleMethodMap = new HashMap<>();
  private final Map<LivingEntityRenderer<?,?>, Method> getWhiteOverlayProgressMethodMap = new HashMap<>();

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

  private boolean shouldRenderAsNormalNpc(NpcEntity npcEntity) {
    return npcEntity.getRendererType().equals(npcEntity.getType()) || npcEntity.getEntityToRenderAs() == null;
  }

  private void setCurrentRenderer(NpcEntity npcEntity) {
    LivingEntity entityToRenderAs = npcEntity.getEntityToRenderAs();
    if (currentEntityToRenderAs != entityToRenderAs) {
      currentEntityToRenderAs = entityToRenderAs;
      currentRenderer = (LivingEntityRenderer<?, ?>) this.entityRenderDispatcher.getRenderer(entityToRenderAs);
      if (getBobMethodMap.containsKey(currentRenderer)) {
        currentGetBobMethod = getBobMethodMap.get(currentRenderer);
        currentScaleMethod = scaleMethodMap.get(currentRenderer);
        currentGetWhiteOverlayProgressMethod = getWhiteOverlayProgressMethodMap.get(currentRenderer);

        return;
      }

      Method getBobMethod = tryGetRendererMethod("m_6930_", float.class);
      if (getBobMethod != null) {
        getBobMethodMap.put(currentRenderer, getBobMethod);
        currentGetBobMethod = getBobMethod;
      }
      Method scaleMethod = tryGetRendererMethod("m_7546_", PoseStack.class, float.class);
      if (scaleMethod != null) {
        scaleMethodMap.put(currentRenderer, scaleMethod);
        currentScaleMethod = scaleMethod;
      }
      Method getWhiteOverlayProgressMethod = tryGetRendererMethod("m_6931_", float.class);
      if (getWhiteOverlayProgressMethod != null) {
        getWhiteOverlayProgressMethodMap.put(currentRenderer, getWhiteOverlayProgressMethod);
        currentGetWhiteOverlayProgressMethod = getWhiteOverlayProgressMethod;
      }
    }
  }

  @Nullable
  private Method tryGetRendererMethod(@NotNull final String srgName, @NotNull final Class<?>... parameterTypes) {
    return tryGetRendererMethod(currentRenderer.getClass(), srgName, parameterTypes);
  }

  @Nullable
  private Method tryGetRendererMethod(Class<?> clazz, @NotNull final String srgName, @NotNull final Class<?>... parameterTypes) {
    Class<?>[] actualParameterTypes = new Class<?>[parameterTypes.length+1];
    actualParameterTypes[0] = LivingEntity.class;
    System.arraycopy(parameterTypes, 0, actualParameterTypes, 1, parameterTypes.length);
    Method method = null;
    try {
      method = ObfuscationReflectionHelper.findMethod(clazz, srgName, actualParameterTypes);
    } catch (ObfuscationReflectionHelper.UnableToFindMethodException e1) {
      if (clazz.getSuperclass() != null && clazz.getSuperclass() != LivingEntityRenderer.class) {
        method = tryGetRendererMethod(clazz.getSuperclass(), srgName, parameterTypes);
      }
      Main.LOGGER.debug("Couldn't get method " + srgName + " for class " + currentRenderer.getClass().getSimpleName());
    }
    return method;
  }

  @Override
  public ResourceLocation getTextureLocation(NpcEntity entity) {
    if (shouldRenderAsNormalNpc(entity)) {
      try {
        return SkinUtil.loadSkin(entity.getTexture());
      } catch (Exception e) {
        return DefaultPlayerSkin.getDefaultSkin();
      }
    }
    else {
      LivingEntity entityToRenderAs = entity.getEntityToRenderAs();
      return this.entityRenderDispatcher.getRenderer(entityToRenderAs).getTextureLocation(entityToRenderAs);
    }
  }

  private void setModelProperties(NpcEntity npcEntity) {
    this.model.crouching = npcEntity.isCrouching();
  }

  @Override
  public void render(NpcEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn,
                     MultiBufferSource bufferIn, int packedLightIn) {
    if (shouldRenderAsNormalNpc(entityIn)) {
      if (entityIn.isSlim())
        this.model = slimModel;
      else
        this.model = normalModel;
      this.shadowRadius = 0.5f * Math.max(entityIn.getScaleX(), entityIn.getScaleZ());
      this.shadowStrength = 1.0f;

      setModelProperties(entityIn);
      renderPlayerModel(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }
    else {
      setCurrentRenderer(entityIn);
      this.shadowRadius = currentRenderer.shadowRadius * Math.max(entityIn.getScaleX(), entityIn.getScaleZ());
      this.shadowStrength = currentRenderer.shadowStrength;
      renderCustomModel(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }
  }

  private void renderCustomModel(NpcEntity npcEntity, float entityYaw, float partialTicks, PoseStack matrixStackIn,
                                 MultiBufferSource bufferIn, int packedLightIn) {
    if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Pre<>(npcEntity, this, partialTicks, matrixStackIn, bufferIn, packedLightIn))) return;
    EntityModel model = currentRenderer.getModel();

    matrixStackIn.pushPose();
    model.attackTime = this.getAttackAnim(npcEntity, partialTicks);

    boolean shouldSit = npcEntity.isPassenger() && (npcEntity.getVehicle() != null && npcEntity.getVehicle().shouldRiderSit());
    model.riding = shouldSit;
    model.young = npcEntity.isBaby();
    float f = Mth.rotLerp(partialTicks, npcEntity.yBodyRotO, npcEntity.yBodyRot);
    float f1 = Mth.rotLerp(partialTicks, npcEntity.yHeadRotO, npcEntity.yHeadRot);
    float f2 = f1 - f;
    if (shouldSit && npcEntity.getVehicle() instanceof LivingEntity livingentity) {
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

    float f6 = Mth.lerp(partialTicks, npcEntity.xRotO, npcEntity.getXRot());
    if (isEntityUpsideDown(npcEntity)) {
      f6 *= -1.0F;
      f2 *= -1.0F;
    }

    if (npcEntity.getPose() == Pose.SLEEPING) {
      Direction direction = npcEntity.getBedOrientation();
      if (direction != null) {
        float f4 = npcEntity.getEyeHeight(Pose.STANDING) - 0.1F;
        matrixStackIn.translate((double)((float)(-direction.getStepX()) * f4), 0.0D, (double)((float)(-direction.getStepZ()) * f4));
      }
    }

    float f7;
    try {
      f7 = (Float) currentGetBobMethod.invoke(currentRenderer, currentEntityToRenderAs, partialTicks);
    } catch (Exception e) {
      f7 = this.getBob(npcEntity, partialTicks);
    }

    this.setupRotations(npcEntity, matrixStackIn, f7, f, partialTicks);
    matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
    try {
      this.currentScaleMethod.invoke(currentRenderer, currentEntityToRenderAs, matrixStackIn, partialTicks);
    } catch (Exception e) {
      this.scale(npcEntity, matrixStackIn, partialTicks);
    }
    matrixStackIn.scale(npcEntity.getScaleX(), npcEntity.getScaleY(), npcEntity.getScaleZ());
    matrixStackIn.translate(0.0D, (double)-1.501F, 0.0D);
    float f8 = 0.0F;
    float f5 = 0.0F;
    if (!shouldSit && npcEntity.isAlive()) {
      f8 = Mth.lerp(partialTicks, npcEntity.animationSpeedOld, npcEntity.animationSpeed);
      f5 = npcEntity.animationPosition - npcEntity.animationSpeed * (1.0F - partialTicks);
      if (npcEntity.isBaby()) {
        f5 *= 3.0F;
      }

      if (f8 > 1.0F) {
        f8 = 1.0F;
      }
    }

    model.prepareMobModel(currentEntityToRenderAs, f5, f8, partialTicks);
    model.setupAnim(currentEntityToRenderAs, f5, f8, f7, f2, f6);
    Minecraft minecraft = Minecraft.getInstance();
    boolean flag = this.isBodyVisible(npcEntity);
    boolean flag1 = !flag && !npcEntity.isInvisibleTo(minecraft.player);
    boolean flag2 = minecraft.shouldEntityAppearGlowing(npcEntity);
    RenderType rendertype = this.getRenderType(npcEntity, flag, flag1, flag2);
    if (rendertype != null) {
      VertexConsumer vertexconsumer = bufferIn.getBuffer(rendertype);
      float overlayProgress;
      try {
        overlayProgress = (Float) this.currentGetWhiteOverlayProgressMethod.invoke(currentRenderer, currentEntityToRenderAs, partialTicks);
      }
      catch (Exception e) {
        overlayProgress = this.getWhiteOverlayProgress(npcEntity, partialTicks);
      }
      int i = getOverlayCoords(npcEntity, overlayProgress);
      model.renderToBuffer(matrixStackIn, vertexconsumer, packedLightIn, i, 1.0F, 1.0F, 1.0F, flag1 ? 0.15F : 1.0F);
    }

    if (!currentEntityToRenderAs.isSpectator()) {
      for (RenderLayer renderlayer : currentRenderer.layers) {
        renderlayer.render(matrixStackIn, bufferIn, packedLightIn, currentEntityToRenderAs, f5, f8, partialTicks, f7, f2, f6);
      }
    }

    matrixStackIn.popPose();
    net.minecraftforge.client.event.RenderNameplateEvent renderNameplateEvent = new net.minecraftforge.client.event.RenderNameplateEvent(npcEntity, npcEntity.getDisplayName(), this, matrixStackIn, bufferIn, packedLightIn, partialTicks);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(renderNameplateEvent);
    if (renderNameplateEvent.getResult() != net.minecraftforge.eventbus.api.Event.Result.DENY && (renderNameplateEvent.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || this.shouldShowName(npcEntity))) {
      this.renderNameTag(npcEntity, renderNameplateEvent.getContent(), matrixStackIn, bufferIn, packedLightIn);
    }
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<>(npcEntity, this, partialTicks, matrixStackIn, bufferIn, packedLightIn));
  }

  @Nullable
  @Override
  protected RenderType getRenderType(NpcEntity npcEntity, boolean p_115323_, boolean p_115324_, boolean p_115325_) {
    ResourceLocation resourcelocation = this.getTextureLocation(npcEntity);
    if (p_115324_) {
      return RenderType.itemEntityTranslucentCull(resourcelocation);
    } else if (p_115323_) {
      if (shouldRenderAsNormalNpc(npcEntity) || currentRenderer == null) {
        return this.model.renderType(resourcelocation);
      }
      else {
        return currentRenderer.getModel().renderType(resourcelocation);
      }
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
    if (shouldRenderAsNormalNpc(npcEntity)) {
      return npcEntity.isCrouching() ? new Vec3(0.0D, -0.125D, 0.0D) : npcEntity.isSitting() ? new Vec3(0.0D, -0.55D, 0.0D) : super.getRenderOffset(npcEntity, p_117786_);
    }
    return Vec3.ZERO;
  }

  @Override
  protected void scale(NpcEntity npcEntity, PoseStack matrixStackIn, float partialTickTime) {
    float scale = 0.9375F;
    matrixStackIn.scale(scale, scale, scale);
    super.scale(npcEntity, matrixStackIn, partialTickTime);
  }

  @Override
  protected void renderNameTag(NpcEntity entityIn, Component displayNameIn, PoseStack matrixStackIn,
                            MultiBufferSource bufferIn, int packedLightIn) {
    if (entityIn.isCustomNameVisible()) super.renderNameTag(entityIn, displayNameIn, matrixStackIn, bufferIn, packedLightIn);
  }
}
