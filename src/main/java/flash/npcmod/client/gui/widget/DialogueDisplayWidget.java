package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.client.gui.screen.dialogue.DialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class DialogueDisplayWidget extends AbstractWidget {
  private DialogueScreen screen;
  private int scrollY;

  public DialogueDisplayWidget(DialogueScreen screen, int x, int y, int width, int height) {
    super(x, y, width, height, TextComponent.EMPTY);
    this.screen = screen;
    this.scrollY = 0;
  }

  @Override
  public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    matrixStack.pushPose();
    Minecraft minecraft = Minecraft.getInstance();
    Font fontrenderer = minecraft.font;
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.enableDepthTest();
    int prevHeight = 0;
    for (int i = screen.displayedText.size()-1-scrollY; i >= 0; i--) {
      String[] lines = screen.displayedText.get(i);
      int textColor = lines[0].startsWith(screen.playerName) ? 0xFFFFFF : screen.getNpcTextColor();
      int lineHeight = fontrenderer.lineHeight+1;

      for (String line : lines) {
        if (line.isEmpty()) {
          prevHeight += lineHeight;
        } else {
          List<FormattedCharSequence> trimmedText = fontrenderer.split(new TextComponent(line), width);
          drawMultilineText(matrixStack, trimmedText, fontrenderer, x, y + height - prevHeight - lineHeight * trimmedText.size(), textColor);
          prevHeight += lineHeight * trimmedText.size();
        }
      }
      prevHeight += lineHeight;
    }
    matrixStack.popPose();
  }

  private void drawMultilineText(PoseStack matrixStack, List<FormattedCharSequence> trimmedText, Font font, int x, int y, int color) {
    for (int i = 0; i < trimmedText.size(); i++) {
      int y2 = y+((font.lineHeight+1)*i);
      if (y2 >= this.y) {
        FormattedCharSequence processor = trimmedText.get(i);
        font.draw(matrixStack, processor, x, y2, color);
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
    Font fontRenderer = Minecraft.getInstance().font;
    int lineHeight = fontRenderer.lineHeight+1;
    int numOfLines = 0;
    for (int i = 0; i < screen.displayedText.size(); i++) {
      String[] lines = screen.displayedText.get(i);
      for (String line : lines) {
        List<FormattedCharSequence> trimmedText = fontRenderer.split(new TextComponent(line), width);
        numOfLines += trimmedText.size();
      }
      numOfLines += 1;
    }
    int maxLinesInHeight = height/lineHeight;
    int max = numOfLines - maxLinesInHeight;
    if (max > 0) {
      return Mth.clamp(newScroll, 0, max/2);
    } else {
      return scrollY;
    }
  }

  @Override
  public void updateNarration(NarrationElementOutput p_169152_) {
    // TODO?
  }
}
