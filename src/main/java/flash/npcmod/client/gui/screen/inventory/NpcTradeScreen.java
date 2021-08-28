package flash.npcmod.client.gui.screen.inventory;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import flash.npcmod.Main;
import flash.npcmod.client.gui.widget.TradeWidget;
import flash.npcmod.core.trades.TradeOffer;
import flash.npcmod.core.trades.TradeOffers;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.inventory.container.NpcTradeContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.client.gui.screen.inventory.InventoryScreen.drawEntityOnScreen;

@OnlyIn(Dist.CLIENT)
public class NpcTradeScreen extends ContainerScreen<NpcTradeContainer> {

  public static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID, "textures/gui/npc_trades.png");

  private NpcEntity npcEntity;
  private TradeOffers notEmptyTradeOffers;
  private Map<TradeOffer, Integer> offerToIndexMap;

  private double scrollY;
  private boolean isScrolling;
  private int scrollOffset;
  private int maxScrollOffset;

  private TradeWidget[] tradeWidgets;

  public NpcTradeScreen(NpcTradeContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
    super(screenContainer, inv, titleIn);
    this.passEvents = true;
    this.titleX = 0;
    this.titleY = -12;

    this.scrollY = 0.0;
    this.isScrolling = false;
    this.scrollOffset = 0;
    this.tradeWidgets = new TradeWidget[3];

    this.npcEntity = this.container.getNpcEntity();
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
    return npcEntity.getEntityId();
  }

  public void renderItemTooltip(MatrixStack matrixStack, ItemStack itemStack, int mouseX, int mouseY) {
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
      this.tradeWidgets[j] = this.addButton(new TradeWidget(this, this.guiLeft+8, this.guiTop+8+j*20, 153, 20, notEmptyTradeOffers.get(j+scrollOffset)));
    }
    updateTradeOffers();
  }

  @Override
  public void tick() {
    super.tick();
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
  protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    this.minecraft.getTextureManager().bindTexture(TEXTURE);
    int i = this.guiLeft;
    int j = this.guiTop;
    this.blit(matrixStack, i, j, 0, 0, this.xSize, this.ySize);
    if (canScroll()) {
      matrixStack.push();
      int v = isMouseOverScrollBar(x, y) ? 7 : 0;
      matrixStack.translate(0, scrollY, 0);
      this.blit(matrixStack, i + 164, j + 8, xSize, v, 4, 7);
      matrixStack.pop();

      if (this.scrollOffset < maxScrollOffset)
        this.blit(matrixStack, i+8, j+68, 0, 166, 153, 10);
    }
    drawEntityOnScreen(i + xSize + 45, j + 140, 60, 40, -5, npcEntity);
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
    this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
  }

  @Override
  protected void drawGuiContainerForegroundLayer(MatrixStack matrixStack, int x, int y) {
    this.font.drawText(matrixStack, new TranslationTextComponent("screen.trades.title", this.title.getString()), (float)this.titleX, (float)this.titleY, 0xFFFFFF);
  }

  private boolean canScroll() {
    return this.notEmptyTradeOffers.size() > 3;
  }

  private boolean isMouseOverScrollBar(double mouseX, double mouseY) {
    int i = this.guiLeft+164;
    int j = this.guiTop+8;
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
      this.scrollY = MathHelper.clamp(scrollY + delta, 0, 63);
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
