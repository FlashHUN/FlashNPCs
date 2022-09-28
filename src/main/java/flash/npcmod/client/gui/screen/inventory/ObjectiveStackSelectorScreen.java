package flash.npcmod.client.gui.screen.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.client.gui.screen.quests.QuestEditorScreen;
import flash.npcmod.client.gui.screen.quests.QuestObjectiveBuilderScreen;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.inventory.container.ObjectiveStackSelectorContainer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ObjectiveStackSelectorScreen extends AbstractContainerScreen<ObjectiveStackSelectorContainer> {

  private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID, "textures/gui/item_selector_inventory.png");

  public ObjectiveStackSelectorScreen(ObjectiveStackSelectorContainer screenContainer, Inventory inv, Component titleIn) {
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
    if (menu.getSelectedSlot() != null) {
      RenderSystem.disableDepthTest();
      int j1 = this.leftPos+menu.getSelectedSlot().x;
      int k1 = this.topPos+menu.getSelectedSlot().y;
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
    this.font.draw(matrixStack, playerInventoryTitle, (float)this.inventoryLabelX, (float)this.inventoryLabelY, 0xFFFFFF);
  }

  @Override
  public void onClose() {
    QuestObjective newObjective = menu.getSelectedSlot() != null ? menu.getQuestObjective().setItemStackObjective(menu.getSelectedSlot().getItem()) : menu.getQuestObjective();
    QuestEditorScreen questEditorScreen = QuestEditorScreen.fromQuest(menu.getQuest());
    questEditorScreen.updateObjectiveId();
    minecraft.setScreen(new QuestObjectiveBuilderScreen(questEditorScreen, newObjective, menu.getOriginalName()));
  }
}
