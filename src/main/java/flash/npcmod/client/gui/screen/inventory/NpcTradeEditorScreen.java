package flash.npcmod.client.gui.screen.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.inventory.container.NpcTradeEditorContainer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NpcTradeEditorScreen extends AbstractContainerScreen<NpcTradeEditorContainer> {

  private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID, "textures/gui/npc_trade_editor.png");

  NpcEntity npcEntity;

  public NpcTradeEditorScreen(NpcTradeEditorContainer screenContainer, Inventory inv, Component titleIn) {
    super(screenContainer, inv, titleIn);
    this.passEvents = true;

    this.npcEntity = this.menu.getNpcEntity();
  }

  @Override
  protected void init() {
    super.init();
  }

  @Override
  protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, TEXTURE);
    int i = this.leftPos-72;
    int j = this.topPos-36;
    this.blit(matrixStack, i, j, 0, 0, 320, 202, 512, 256);
  }

  @Override
  public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
    this.renderTooltip(matrixStack, mouseX, mouseY);
  }

  @Override
  protected void renderLabels(PoseStack matrixStack, int x, int y) {}

  private boolean isMouseOverScrollIcon(double mouseX, double mouseY) {
    int i = this.leftPos+164;
    int j = this.topPos+8;
    return mouseX >= i && mouseX <= i+4 && mouseY >= j && mouseY <= j+70;
  }
}
