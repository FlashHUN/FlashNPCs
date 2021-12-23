package flash.npcmod.client.gui.screen.inventory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import flash.npcmod.Main;
import flash.npcmod.client.gui.widget.TradeWidget;
import flash.npcmod.core.trades.TradeOffer;
import flash.npcmod.core.trades.TradeOffers;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.inventory.container.NpcTradeContainer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory;

@OnlyIn(Dist.CLIENT)
public class NpcTradeScreen extends AbstractContainerScreen<NpcTradeContainer> {

  public static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID, "textures/gui/npc_trades.png");

  private NpcEntity npcEntity;
  private TradeOffers notEmptyTradeOffers;
  private Map<TradeOffer, Integer> offerToIndexMap;

  private double scrollY;
  private boolean isScrolling;
  private int scrollOffset;
  private int maxScrollOffset;

  private TradeWidget[] tradeWidgets;

  public NpcTradeScreen(NpcTradeContainer screenContainer, Inventory inv, Component titleIn) {
    super(screenContainer, inv, titleIn);
    this.passEvents = true;
    this.titleLabelX = 0;
    this.titleLabelY = -12;

    this.scrollY = 0.0;
    this.isScrolling = false;
    this.scrollOffset = 0;
    this.tradeWidgets = new TradeWidget[3];

    this.npcEntity = this.menu.getNpcEntity();
    notEmptyTradeOffers = new TradeOffers();
    offerToIndexMap = new HashMap<>();
    for (int i = 0; i < npcEntity.getOffers().size(); i++) {
      TradeOffer offer = npcEntity.getOffers().get(i);
      if (!offer.isEmpty()) {

        ItemStack[] buyingStacks = formatArray(offer.getBuyingStacks());
        ItemStack[] sellingStacks = formatArray(offer.getSellingStacks());

        TradeOffer formattedOffer = new TradeOffer(buyingStacks, sellingStacks);
        notEmptyTradeOffers.add(formattedOffer);
        offerToIndexMap.put(formattedOffer, i);
      }
    }
    maxScrollOffset = notEmptyTradeOffers.size() - 3;
  }

  public Map<TradeOffer, Integer> getOfferToIndexMap() {
    return offerToIndexMap;
  }

  public int getNpcId() {
    return npcEntity.getId();
  }

  public void renderItemTooltip(PoseStack matrixStack, ItemStack itemStack, int mouseX, int mouseY) {
    super.renderTooltip(matrixStack, itemStack, mouseX, mouseY);
  }

  private int getMaxTradeWidgets() {
    return canScroll() ? 3 : notEmptyTradeOffers.size();
  }

  @Override
  protected void init() {
    super.init();
    int i = getMaxTradeWidgets();
    for (int j = 0; j < i; j++) {
      this.tradeWidgets[j] = this.addRenderableWidget(new TradeWidget(this, this.leftPos+8, this.topPos+8+j*20, 153, 20, notEmptyTradeOffers.get(j+scrollOffset)));
    }
    updateTradeOffers();
  }

  @Override
  protected void containerTick() {
    int i = getMaxTradeWidgets();
    for (int j = 0; j < i; j++) {
      this.tradeWidgets[j].activeCheck();
    }
  }

  private ItemStack[] formatArray(ItemStack[] array) {
    // for all not empty stacks in the array, add it to a list
    List<ItemStack> notEmpty = new ArrayList<>();
    for (ItemStack stack : array) {
      if (!stack.isEmpty()) notEmpty.add(stack);
    }

    // then create a new array with the same length and start filling it with the not empty items, then fill the rest with ItemStack.EMPTY
    ItemStack[] newArray = new ItemStack[array.length];
    for (int i = 0; i < notEmpty.size(); i++) { newArray[i] = notEmpty.get(i); }
    for (int i = notEmpty.size(); i < newArray.length; i++) { newArray[i] = ItemStack.EMPTY; }

    return newArray;
  }

  @Override
  protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, TEXTURE);
    int i = this.leftPos;
    int j = this.topPos;
    this.blit(matrixStack, i, j, 0, 0, this.imageWidth, this.imageHeight);
    if (canScroll()) {
      matrixStack.pushPose();
      int v = isMouseOverScrollBar(x, y) ? 7 : 0;
      matrixStack.translate(0, scrollY, 0);
      this.blit(matrixStack, i + 164, j + 8, imageWidth, v, 4, 7);
      matrixStack.popPose();

      if (this.scrollOffset < maxScrollOffset)
        this.blit(matrixStack, i+8, j+68, 0, 166, 153, 10);
    }
    renderEntityInInventory(i + imageWidth + 45, j + 140, 60, 40, -5, npcEntity);
  }

  @Override
  public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
    this.renderTooltip(matrixStack, mouseX, mouseY);
  }

  @Override
  protected void renderLabels(PoseStack matrixStack, int x, int y) {
    this.font.draw(matrixStack, new TranslatableComponent("screen.trades.title", this.title.getString()), (float)this.titleLabelX, (float)this.titleLabelY, 0xFFFFFF);
  }

  private boolean canScroll() {
    return this.notEmptyTradeOffers.size() > 3;
  }

  private boolean isMouseOverScrollBar(double mouseX, double mouseY) {
    int i = this.leftPos+164;
    int j = this.topPos+8;
    return mouseX >= i && mouseX <= i+4 && mouseY >= j && mouseY <= j+70;
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button == 0 && isMouseOverScrollBar(mouseX, mouseY)) this.isScrolling = true;
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    if (button != 0) this.isScrolling = false;
    else if (isScrolling) updateScrollY(dragY);
    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (button == 0) this.isScrolling = false;
    return super.mouseReleased(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    updateScrollY(-delta*3);
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  private void updateScrollY(double delta) {
    if (canScroll()) {
      this.scrollY = Mth.clamp(scrollY + delta, 0, 63);
      this.scrollOffset = (int) (scrollY * maxScrollOffset / 63);
    } else {
      this.scrollY = 0.0;
      this.scrollOffset = 0;
    }
    updateTradeOffers();
  }

  private void updateTradeOffers() {
    if (notEmptyTradeOffers.size() > 0) {
      int i = getMaxTradeWidgets();
      for (int j = 0; j < i; j++) {
        this.tradeWidgets[j].setTradeOffer(notEmptyTradeOffers.get(j + scrollOffset));
      }
    }
  }
}
