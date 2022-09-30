package flash.npcmod.client.gui.screen.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.client.gui.screen.quests.QuestEditorScreen;
import flash.npcmod.inventory.container.QuestStackSelectorContainer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class QuestStackSelectorScreen extends AbstractContainerScreen<QuestStackSelectorContainer> {

  private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID, "textures/gui/item_selector_inventory.png");

  public QuestStackSelectorScreen(QuestStackSelectorContainer screenContainer, Inventory inv, Component titleIn) {
    super(screenContainer, inv, titleIn);
    this.inventoryLabelY-=5;
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
  }

  @Override
  public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
    for (Slot slot : menu.getSelectedSlots()) {
      RenderSystem.disableDepthTest();
      int j1 = this.leftPos+slot.x;
      int k1 = this.topPos+slot.y;
      RenderSystem.colorMask(true, true, true, false);
      int slotColor = 0x7300FF00;
      this.fillGradient(matrixStack, j1, k1, j1 + 16, k1 + 16, slotColor, slotColor);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.enableDepthTest();
    }

    this.renderTooltip(matrixStack, mouseX, mouseY);
  }

  @Override
  protected void renderLabels(PoseStack matrixStack, int x, int y) {
    this.font.draw(matrixStack, this.playerInventoryTitle, (float)this.inventoryLabelX, (float)this.inventoryLabelY, 0xFFFFFF);
  }

  @Override
  public void onClose() {
    List<ItemStack> stackList = new ArrayList<>();
    for (Slot slot : menu.getSelectedSlots()) {
      stackList.add(slot.getItem());
    }
    menu.getQuest().setItemRewards(stackList);
    QuestEditorScreen questEditorScreen = QuestEditorScreen.fromQuest(menu.getQuest());
    questEditorScreen.updateObjectiveId();
    minecraft.setScreen(questEditorScreen);
  }
}
