package flash.npcmod.client.gui.screen.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.inventory.container.NpcInventoryContainer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory;

@OnlyIn(Dist.CLIENT)
public class NpcInventoryScreen extends AbstractContainerScreen<NpcInventoryContainer> {

  public static final ResourceLocation EMPTY_ARMOR_SLOT_SWORD = new ResourceLocation(Main.MODID, "textures/item/empty_armor_slot_sword.png");
  private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID, "textures/gui/npc_inventory.png");

  private NpcEntity npcEntity;

  /** The old x position of the mouse pointer */
  private float oldMouseX;
  /** The old y position of the mouse pointer */
  private float oldMouseY;

  public NpcInventoryScreen(NpcInventoryContainer screenContainer, Inventory inv, Component titleIn) {
    super(screenContainer, inv, titleIn);
    this.passEvents = true;
    this.titleLabelX = 0;
    this.titleLabelY = -12;

    this.npcEntity = this.menu.getNpcEntity();
  }

  @Override
  protected void init() {
    if (!minecraft.player.hasPermissions(4)) onClose();
    super.init();
  }

  @Override
  protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, TEXTURE);
    int i = this.leftPos;
    int j = this.topPos;
    this.blit(matrixStack, i, j, 0, 0, this.imageWidth, this.imageHeight);
    float bbHeight = Math.max(npcEntity.getBbHeight(), 1f);
    int scale = (int) (54 / bbHeight);
    renderEntityInInventory(i + 89, j + 75, scale, (float) (i + 89) - this.oldMouseX, (float) (j + 75 - 50) - this.oldMouseY, npcEntity);
    if (this.menu.slots.get(0).getItem().isEmpty()) { // have to do this because for some reason Slot#getNoItemIcon doesn't want to work
      RenderSystem.setShaderTexture(0, EMPTY_ARMOR_SLOT_SWORD);
      blit(matrixStack, i + 115, j + 44, 16, 16, 0, 0, 16, 16, 16, 16);
    }
  }

  @Override
  public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {

    this.renderBackground(matrixStack);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
    this.renderTooltip(matrixStack, mouseX, mouseY);

    this.oldMouseX = (float)mouseX;
    this.oldMouseY = (float)mouseY;
  }

  @Override
  protected void renderLabels(PoseStack matrixStack, int x, int y) {
    this.font.draw(matrixStack, this.title, (float)this.titleLabelX, (float)this.titleLabelY, 0xFFFFFF);
  }
}
