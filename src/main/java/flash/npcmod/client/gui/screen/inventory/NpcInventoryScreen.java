package flash.npcmod.client.gui.screen.inventory;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.inventory.container.NpcInventoryContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static net.minecraft.client.gui.screen.inventory.InventoryScreen.drawEntityOnScreen;

@OnlyIn(Dist.CLIENT)
public class NpcInventoryScreen extends ContainerScreen<NpcInventoryContainer> {

  private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID, "textures/gui/npc_inventory.png");
  public static final ResourceLocation EMPTY_ARMOR_SLOT_SWORD = new ResourceLocation(Main.MODID, "textures/item/empty_armor_slot_sword.png");

  private NpcEntity npcEntity;

  /** The old x position of the mouse pointer */
  private float oldMouseX;
  /** The old y position of the mouse pointer */
  private float oldMouseY;

  public NpcInventoryScreen(NpcInventoryContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
    super(screenContainer, inv, titleIn);
    this.passEvents = true;
    this.titleX = 0;
    this.titleY = -12;

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
    int i = this.guiLeft;
    int j = this.guiTop;
    this.blit(matrixStack, i, j, 0, 0, this.xSize, this.ySize);
    drawEntityOnScreen(i + 89, j + 75, 30, (float)(i + 89) - this.oldMouseX, (float)(j + 75 - 50) - this.oldMouseY, npcEntity);
    minecraft.textureManager.bindTexture(EMPTY_ARMOR_SLOT_SWORD);
    blit(matrixStack, i+115, j+44, 16, 16, 0, 0, 16, 16, 16, 16);
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {

    this.renderBackground(matrixStack);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
    this.renderHoveredTooltip(matrixStack, mouseX, mouseY);

    this.oldMouseX = (float)mouseX;
    this.oldMouseY = (float)mouseY;
  }

  @Override
  protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int x, int y) {
    this.font.drawText(matrixStack, this.title, (float)this.titleX, (float)this.titleY, 0xFFFFFF);
  }
}
