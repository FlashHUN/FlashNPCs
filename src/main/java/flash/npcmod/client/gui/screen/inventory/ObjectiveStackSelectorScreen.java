package flash.npcmod.client.gui.screen.inventory;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import flash.npcmod.Main;
import flash.npcmod.client.gui.screen.quests.QuestEditorScreen;
import flash.npcmod.client.gui.screen.quests.QuestObjectiveBuilderScreen;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.inventory.container.ObjectiveStackSelectorContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ObjectiveStackSelectorScreen extends ContainerScreen<ObjectiveStackSelectorContainer> {

  private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID, "textures/gui/item_selector_inventory.png");

  public ObjectiveStackSelectorScreen(ObjectiveStackSelectorContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
    super(screenContainer, inv, titleIn);
    this.playerInventoryTitleY-=5;
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
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
    if (container.getSelectedSlot() != null) {
      RenderSystem.disableDepthTest();
      int j1 = this.guiLeft+container.getSelectedSlot().xPos;
      int k1 = this.guiTop+container.getSelectedSlot().yPos;
      RenderSystem.colorMask(true, true, true, false);
      int slotColor = 0x7300FF00;
      this.fillGradient(matrixStack, j1, k1, j1 + 16, k1 + 16, slotColor, slotColor);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.enableDepthTest();
    }

    this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
  }

  @Override
  protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int x, int y) {
    this.font.drawText(matrixStack, this.playerInventory.getDisplayName(), (float)this.playerInventoryTitleX, (float)this.playerInventoryTitleY, 0xFFFFFF);
  }

  @Override
  public void closeScreen() {
    QuestObjective newObjective = container.getSelectedSlot() != null ? container.getQuestObjective().setItemStackObjective(container.getSelectedSlot().getStack()) : container.getQuestObjective();
    QuestEditorScreen questEditorScreen = QuestEditorScreen.fromQuest(container.getQuest());
    questEditorScreen.updateObjectiveId();
    minecraft.displayGuiScreen(new QuestObjectiveBuilderScreen(questEditorScreen, newObjective, container.getOriginalName()));
  }
}
