package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.client.gui.screen.dialogue.DialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class TextButton extends Button {
  List<List<FormattedCharSequence>> trimmedTexts;

  public TextButton(int x, int y, int width, Component title, OnPress pressedAction) {
    super(x, y, width, 9, title, pressedAction);
    trimmedTexts = new ArrayList<>();
    String[] lines = DialogueScreen.splitTextIntoLines(title.getString());
    int height = 0;
    for (int i = lines.length - 1; i >= 0; i--) {
      String line = i == lines.length - 1 ? "> " + lines[i] : lines[i];
      List<FormattedCharSequence> trimmedText = Minecraft.getInstance().font.split(new TextComponent(line), width);
      trimmedTexts.add(trimmedText);
      height += 9 * trimmedText.size();
    }
    this.setHeight(height);
  }

  @Override
  public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    Minecraft minecraft = Minecraft.getInstance();
    matrixStack.pushPose();
    Font fontrenderer = minecraft.font;
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.enableDepthTest();
    int j = this.isHoveredOrFocused() ? 0xFFFF00 : getFGColor();

    drawMultilineText(matrixStack, fontrenderer, this.x, this.y, j | Mth.ceil(this.alpha * 255.0F) << 24);

    if (this.isHoveredOrFocused()) {
      this.renderToolTip(matrixStack, mouseX, mouseY);
    }
    matrixStack.popPose();
  }

  private void drawMultilineText(PoseStack matrixStack, Font font, int x, int y, int color) {
    int h = 0;
    for (int i = 0; i < trimmedTexts.size(); i++) {
      List<FormattedCharSequence> trimmedText = trimmedTexts.get(i);
      for (int j = 0; j < trimmedText.size(); j++) {
        FormattedCharSequence processor = trimmedText.get(j);
        int k = i != 0 || j > 0 ? 4 : 0;
        font.draw(matrixStack, processor, x+k, y+h, color);
        h += font.lineHeight + 1;
      }
    }
  }
}
