package flash.npcmod.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.core.client.SkinUtil;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
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
    this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel(context.bakeLayer(useSmallArms ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(useSmallArms ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR))));
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

  @Override
  public void render(NpcEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn,
                     MultiBufferSource bufferIn, int packedLightIn) {

    if (entityIn.isSlim())
      this.model = slimModel;
    else
      this.model = normalModel;

    super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
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
