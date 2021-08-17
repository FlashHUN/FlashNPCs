package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class TextButton extends Button {
  List<IReorderingProcessor> trimmedText;

  public TextButton(int x, int y, int width, ITextComponent title, IPressable pressedAction) {
    super(x, y, width, 9, title, pressedAction);
    trimmedText = Minecraft.getInstance().fontRenderer.trimStringToWidth(new StringTextComponent("> " + title.getString()), width);
    this.setHeight(9 * trimmedText.size());
  }

  @Override
  public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    Minecraft minecraft = Minecraft.getInstance();
    RenderSystem.pushMatrix();
    FontRenderer fontrenderer = minecraft.fontRenderer;
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.enableDepthTest();
    int j = this.isHovered() ? 0xFFFF00 : getFGColor();

    drawMultilineText(matrixStack, fontrenderer, this.x, this.y, j | MathHelper.ceil(this.alpha * 255.0F) << 24);

    if (this.isHovered()) {
      this.renderToolTip(matrixStack, mouseX, mouseY);
    }
    RenderSystem.popMatrix();
  }

  private void drawMultilineText(MatrixStack matrixStack, FontRenderer font, int x, int y, int color) {
    for (int i = 0; i < trimmedText.size(); i++) {
      IReorderingProcessor processor = trimmedText.get(i);
      int j = i > 0 ? 4 : 0;
      font.func_238422_b_(matrixStack, processor, x+j, y+((font.FONT_HEIGHT+1)*i), color);
    }
  }
}
