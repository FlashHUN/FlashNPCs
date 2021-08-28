package flash.npcmod.client.render.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import flash.npcmod.core.client.SkinUtil;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;
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
    this.addLayer(new BipedArmorLayer<>(this, new BipedModel(0.5F), new BipedModel(1.0F)));
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

  @Override
  public void render(NpcEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn,
                     IRenderTypeBuffer bufferIn, int packedLightIn) {

    if (entityIn.isSlim())
      this.entityModel = slimModel;
    else
      this.entityModel = normalModel;

    super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
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
