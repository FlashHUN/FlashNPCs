package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import flash.npcmod.client.gui.screen.DialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class DialogueDisplayWidget extends Widget {
  private DialogueScreen screen;
  private int scrollY;

  public DialogueDisplayWidget(DialogueScreen screen, int x, int y, int width, int height) {
    super(x, y, width, height, StringTextComponent.EMPTY);
    this.screen = screen;
    this.scrollY = 0;
  }

  @Override
  public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    RenderSystem.pushMatrix();
    Minecraft minecraft = Minecraft.getInstance();
    FontRenderer fontrenderer = minecraft.fontRenderer;
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.enableDepthTest();
    int prevHeight = 0;
    for (int i = screen.displayedText.size()-1-scrollY; i >= 0; i--) {
      List<IReorderingProcessor> trimmedText = fontrenderer.trimStringToWidth(new StringTextComponent(screen.displayedText.get(i)), width);

      int lineHeight = fontrenderer.FONT_HEIGHT+1;

      drawMultilineText(matrixStack, trimmedText, fontrenderer, x, y+height-prevHeight-lineHeight*trimmedText.size(), (i & 1) == 0 ? screen.getNpcTextColor() : 0xFFFFFF);

      prevHeight += lineHeight*trimmedText.size()+lineHeight;
    }
    RenderSystem.popMatrix();
  }

  private void drawMultilineText(MatrixStack matrixStack, List<IReorderingProcessor> trimmedText, FontRenderer font, int x, int y, int color) {
    for (int i = 0; i < trimmedText.size(); i++) {
      int y2 = y+((font.FONT_HEIGHT+1)*i);
      if (y2 >= this.y) {
        IReorderingProcessor processor = trimmedText.get(i);
        font.func_238422_b_(matrixStack, processor, x, y2, color);
      }
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta > 0) {
      this.scrollY = clampScroll(scrollY+1);
    } else {
      this.scrollY = clampScroll(scrollY-1);
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  public int clampScroll(int newScroll) {
    FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
    int lineHeight = fontRenderer.FONT_HEIGHT+1;
    int numOfLines = 0;
    for (int i = 0; i < screen.displayedText.size(); i++) {
      List<IReorderingProcessor> trimmedText = fontRenderer.trimStringToWidth(new StringTextComponent(screen.displayedText.get(i)), width);

      numOfLines += trimmedText.size()+1;
    }
    int maxLinesInHeight = height/lineHeight;
    int max = numOfLines - maxLinesInHeight;
    if (max > 0) {
      return MathHelper.clamp(newScroll, 0, max/2);
    } else {
      return scrollY;
    }
  }
}
