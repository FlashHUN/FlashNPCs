package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.client.gui.screen.NpcBuilderScreen;
import flash.npcmod.core.ColorUtil;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ColorSliderWidget extends AbstractWidget {

  @Override
  public void updateNarration(NarrationElementOutput p_169152_) {
    // TODO?
  }

  public enum Color {
    RED(0xFFFF0000) {
      @Override
      public int getMask(NpcBuilderScreen screen) {
        return ColorUtil.rgbToHex(0, screen.getG(), screen.getB());
      }

      @Override
      public void setColor(NpcBuilderScreen screen, int color) {
        screen.setR(color);
        screen.redField.setValue(String.valueOf(color));
      }

      @Override
      public int getColor(NpcBuilderScreen screen) {
        return screen.getR();
      }
    },
    GREEN(0xFF00FF00) {
      @Override
      public int getMask(NpcBuilderScreen screen) {
        return ColorUtil.rgbToHex(screen.getR(), 0, screen.getB());
      }

      @Override
      public void setColor(NpcBuilderScreen screen, int color) {
        screen.setG(color);
        screen.greenField.setValue(String.valueOf(color));
      }

      @Override
      public int getColor(NpcBuilderScreen screen) {
        return screen.getG();
      }
    },
    BLUE(0xFF0000FF) {
      @Override
      public int getMask(NpcBuilderScreen screen) {
        return ColorUtil.rgbToHex(screen.getR(), screen.getG(), 0);
      }

      @Override
      public void setColor(NpcBuilderScreen screen, int color) {
        screen.setB(color);
        screen.blueField.setValue(String.valueOf(color));
      }

      @Override
      public int getColor(NpcBuilderScreen screen) {
        return screen.getB();
      }
    };

    private int colorHex;

    Color(int colorHex) {
      this.colorHex = colorHex;
    }

    public int getColorHex() {
      return colorHex;
    }

    public int getMask(NpcBuilderScreen screen) { return 0xFF000000; }

    public void setColor(NpcBuilderScreen screen, int color) {}

    public int getColor(NpcBuilderScreen screen) { return 0; }
  }

  private Color color;
  private NpcBuilderScreen screen;
  private double colorY;

  public ColorSliderWidget(NpcBuilderScreen screen, int x, int y, int width, int height, Color color) {
    super(x, y, width, height, TextComponent.EMPTY);
    this.color = color;
    this.screen = screen;
    updateColorY();
  }

  public void updateColorY() {
    this.colorY = color.getColor(screen)/255.0;
  }

  @Override
  public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    fill(matrixStack, x-1, y-1, x+width+1, y+height+1, 0xFF000000);

    int mask = color.getMask(screen);
    fillGradient(matrixStack, x, y, x+width, y+height, color.getColorHex() | mask, 0xFF000000 | mask);

    matrixStack.pushPose();
    {
      matrixStack.translate(0, height - colorY*height, 0);
      hLine(matrixStack, x, x+width, y, 0xFF000000);
    }
    matrixStack.popPose();
  }

  @Override
  public void onClick(double mouseX, double mouseY) {
    getColorAtMouse(mouseY);
  }

  @Override
  protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
    getColorAtMouse(mouseY);
  }

  private void getColorAtMouse(double mouseY) {
    if (mouseY < y) {
      mouseY = y;
    } else if (mouseY > y+height) {
      mouseY = y+height;
    }
    colorY = (mouseY-y)/height;
    int color = 255 - (int) (colorY*255);
    this.color.setColor(screen, color);
  }
}
