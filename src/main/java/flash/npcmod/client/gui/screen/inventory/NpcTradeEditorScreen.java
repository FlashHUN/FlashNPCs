package flash.npcmod.client.gui.screen.inventory;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.inventory.container.NpcTradeEditorContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NpcTradeEditorScreen extends ContainerScreen<NpcTradeEditorContainer> {

  private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID, "textures/gui/npc_trade_editor.png");

  NpcEntity npcEntity;

  public NpcTradeEditorScreen(NpcTradeEditorContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
    super(screenContainer, inv, titleIn);
    this.passEvents = true;

    this.npcEntity = this.container.getNpcEntity();
  }

  @Override
  protected void init() {
    if (!minecraft.player.hasPermissionLevel(4)) closeScreen();
    super.init();
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    this.minecraft.getTextureManager().bindTexture(TEXTURE);
    int i = this.guiLeft-72;
    int j = this.guiTop-36;
    this.blit(matrixStack, i, j, 0, 0, 320, 202, 512, 256);
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
    this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
  }

  @Override
  protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int x, int y) {}

  private boolean isMouseOverScrollIcon(double mouseX, double mouseY) {
    int i = this.guiLeft+164;
    int j = this.guiTop+8;
    return mouseX >= i && mouseX <= i+4 && mouseY >= j && mouseY <= j+70;
  }
}
