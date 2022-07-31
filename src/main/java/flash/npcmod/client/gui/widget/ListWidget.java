package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.client.gui.screen.TreeBuilderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.client.gui.GuiComponent.fill;

@OnlyIn(Dist.CLIENT)
public class ListWidget<S extends TreeBuilderScreen> {

  private S screen;
  private Minecraft minecraft;

  private double x;
  private double y;
  private int width, height;
  private boolean visible;

  private int scrollY;
  private int maxSize = 7;

  private String selectedOption;
  private int selectedOptionIndex;
  private List<String> options;

  private static final int lineHeight = 2+Minecraft.getInstance().font.lineHeight;

  public ListWidget(S screen, Minecraft minecraft) {
    this.screen = screen;
    this.minecraft = minecraft;

    this.visible = false;

    this.selectedOption = "";
    this.selectedOptionIndex = -1;
    this.options = new ArrayList<>();
  }

  public void calculatePositionAndDimensions() {
    width = 0;
    for (String name : options) {
      width = Math.max(width, minecraft.font.width(name)+4);
    }
    if (x > screen.width / 2)
      x = x - width;

    height = Math.min(options.size(), maxSize)*(minecraft.font.lineHeight+2);
    if (y > screen.height / 2)
      y = y - height;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
    this.selectedOption = "";
    this.selectedOptionIndex = -1;

    calculatePositionAndDimensions();
  }

  public void setX(double posX) {
    this.x = posX;
  }
  public void setY(double posY) {
    this.y = posY;
  }

  public void setOptions(List<String> options) {
    this.options = options;
  }

  public void draw(PoseStack matrixStack) {
    if (isVisible()) {
      matrixStack.pushPose();
      matrixStack.translate(x, y, 0);

      drawRectangles(matrixStack);
      drawText(matrixStack);

      matrixStack.popPose();
    }
  }

  private void drawRectangles(PoseStack matrixStack) {
    int black = 0xFF000000;
    int grey = 0xFFCCCCCC;

    // Outlines
    fill(matrixStack, 0, 0, width, 1, black);
    fill(matrixStack, 0, 0, 1, height, black);
    fill(matrixStack, width - 1, 0, width, height, black);
    fill(matrixStack, 0, height, width, height+1, black);

    // Background
    fill(matrixStack, 1, 1, width-1, height, grey);

    int minY = lineHeight;
    int size = options.size();
    for (int i = 0; i < (size > maxSize ? maxSize : size); i++) {
      fill(matrixStack, 1, minY-(i==0 ? 0 : 1*i), width-1, minY+1-(i==0 ? 0 : 1*i), black);
      minY += 1+lineHeight;
    }
  }

  private void drawText(PoseStack matrixStack) {
    int size = options.size();
    for (int i = 0; i < (size > maxSize ? maxSize : size); i++) {
      String name = options.get(i+scrollY);
      int y = 2+i*lineHeight;
      minecraft.font.draw(matrixStack, name, width/2-minecraft.font.width(name)/2, y, 0x000000);
    }
  }

  public void clickedOn(double mouseX, double mouseY) {
    if (isVisible()){
      if (mouseX >= this.x && mouseX <= this.x + width && mouseY >= this.y && mouseY <= this.y + height) {
        if (options.size() > 0) {
          double minY = this.y + 1;
          int size = options.size();
          for (int i = 0; i < (size > maxSize ? maxSize : size); i++) {
            if (mouseY >= minY && mouseY <= minY + lineHeight) {
              clickedOnFunction(i);
            }
            minY += lineHeight + 1;
          }
        }
      }
    }
  }

  public void clickedOnFunction(int i) {
    int index = Mth.clamp(i+scrollY, 0, options.size()-1);
    String newSelection = options.get(index);
    if (i == -1) {
      this.selectedOption = "";
      this.selectedOptionIndex = -1;
    } else {
      this.selectedOption = newSelection;
      this.selectedOptionIndex = i;
    }
  }

  public String getSelectedOption() {
    return selectedOption;
  }
  public int getSelectedOptionIndex() {
    return selectedOptionIndex;
  }

  public void onScrolled(double delta) {
    if (this.isVisible()) {
      if (delta > 0) {
        this.scrollY = clampScroll(scrollY - 1);
      } else {
        this.scrollY = clampScroll(scrollY + 1);
      }
    }
  }

  public int clampScroll(int newScroll) {
    int max = options.size()-maxSize;
    if (max > 0) {
      return Mth.clamp(newScroll, 0, max);
    }
    else {
      return scrollY;
    }
  }

}

