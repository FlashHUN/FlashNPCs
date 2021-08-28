package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import flash.npcmod.client.gui.screen.inventory.NpcTradeScreen;
import flash.npcmod.core.trades.TradeOffer;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CTradeWithNpc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TradeWidget extends Widget {

  private static final Minecraft minecraft = Minecraft.getInstance();

  private NpcTradeScreen tradeScreen;
  private TradeOffer tradeOffer;

  public TradeWidget(NpcTradeScreen tradeScreen, int x, int y, int width, int height, TradeOffer tradeOffer) {
    super(x, y, width, height, StringTextComponent.EMPTY);
    this.tradeScreen = tradeScreen;
    this.tradeOffer = tradeOffer;
  }

  public void setTradeOffer(TradeOffer tradeOffer) {
    this.tradeOffer = tradeOffer;
  }

  public void activeCheck() {
    this.active = this.tradeOffer.canDoTransaction(minecraft.player);
  }

  @Override
  public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    minecraft.textureManager.bindTexture(NpcTradeScreen.TEXTURE);
    int j = getYImage(isHovered());
    blit(matrixStack, x, y, 0, 166+j*20, 153, 20);
    for (int i = 0; i < tradeOffer.getBuyingStacks().length; i++) {
      ItemStack stack = tradeOffer.getBuyingStacks()[i];
      if (stack.isEmpty()) break;

      int x = this.x+2+i*17;
      minecraft.getItemRenderer().renderItemAndEffectIntoGuiWithoutEntity(stack, x, y+2);
      minecraft.getItemRenderer().renderItemOverlays(minecraft.fontRenderer, stack, x, y+2);
      if (isMouseOverItem(x, mouseX, mouseY))
        tradeScreen.renderItemTooltip(matrixStack, stack, mouseX, mouseY);
    }
    for (int i = 0; i < tradeOffer.getSellingStacks().length; i++) {
      ItemStack stack = tradeOffer.getSellingStacks()[i];
      if (stack.isEmpty()) break;

      int x = this.x+width-18-i*17;
      minecraft.getItemRenderer().renderItemAndEffectIntoGuiWithoutEntity(stack, x, y+2);
      minecraft.getItemRenderer().renderItemOverlays(minecraft.fontRenderer, stack, x, y+2);
      if (isMouseOverItem(x, mouseX, mouseY))
        tradeScreen.renderItemTooltip(matrixStack, stack, mouseX, mouseY);
    }
  }

  private boolean isMouseOverItem(int x, int mouseX, int mouseY) {
    return mouseX >= x && mouseX <= x+16 && mouseY >= y+2 && mouseY <= y+18;
  }

  public void onPress() {
    if (this.active && this.visible) {
      int entityid = this.tradeScreen.getNpcId();
      int tradeid = this.tradeScreen.getOfferToIndexMap().get(tradeOffer);
      PacketDispatcher.sendToServer(new CTradeWithNpc(entityid, tradeid));
    }
  }

  public void onClick(double mouseX, double mouseY) {
    this.onPress();
  }

  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (this.active && this.visible) {
      if (keyCode != 257 && keyCode != 32 && keyCode != 335) {
        return false;
      } else {
        this.playDownSound(Minecraft.getInstance().getSoundHandler());
        this.onPress();
        return true;
      }
    } else {
      return false;
    }
  }
}
