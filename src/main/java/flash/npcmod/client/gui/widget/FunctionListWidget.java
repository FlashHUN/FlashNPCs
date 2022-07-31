package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.client.gui.node.BuilderNode;
import flash.npcmod.client.gui.screen.TreeBuilderScreen;
import flash.npcmod.core.client.dialogues.ClientDialogueUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

import static net.minecraft.client.gui.GuiComponent.fill;

@OnlyIn(Dist.CLIENT)
public class FunctionListWidget<T extends BuilderNode, S extends TreeBuilderScreen> {

  private S screen;
  private Minecraft minecraft;
  @Nullable
  private T editingNode;

  private int x, y, width, height;
  private boolean visible;

  private int scrollY;
  private int maxSize = 7;

  private String selectedFunction;

  private static final int lineHeight = 2+Minecraft.getInstance().font.lineHeight;

  public FunctionListWidget(S screen, Minecraft minecraft) {
    this.screen = screen;
    this.minecraft = minecraft;

    this.visible = false;

    this.selectedFunction = "";

    calculatePositionAndDimensions();
  }

  public void setEditingNode(@Nullable T editingNode) {
    this.editingNode = editingNode;
  }

  public void calculatePositionAndDimensions() {
    width = 0;
    for (String name : ClientDialogueUtil.FUNCTION_NAMES) {
      width = Math.max(width, minecraft.font.width(name)+4);
    }
    x = screen.width/2-width/2;
    height = Math.min(ClientDialogueUtil.FUNCTION_NAMES.size(), maxSize)*(minecraft.font.lineHeight+2);
    y = screen.height/2-height/2;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
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

    calculatePositionAndDimensions();
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
    int white = 0xFFFFFFFF;
    int green = 0xFF00FF00;

    // Outlines
    fill(matrixStack, 0, 0, width, 1, black);
    fill(matrixStack, 0, 0, 1, height, black);
    fill(matrixStack, width - 1, 0, width, height, black);
    fill(matrixStack, 0, height, width, height+1, black);

    // Background
    fill(matrixStack, 1, 1, width-1, height, white);

    int minY = lineHeight;
    int size = ClientDialogueUtil.FUNCTION_NAMES.size();
    for (int i = 0; i < (size > maxSize ? maxSize : size); i++) {
      boolean isSelected = ClientDialogueUtil.FUNCTION_NAMES.get(Mth.clamp(i+scrollY, 0, ClientDialogueUtil.FUNCTION_NAMES.size()-1)).equals(this.selectedFunction);
      fill(matrixStack, 1, minY-(i==0 ? 0 : 1*i), width-1, minY+1-(i==0 ? 0 : 1*i), black);
      if (isSelected)
        fill(matrixStack, 1, minY-lineHeight+(i == 0 ? 1 : (i-1)*-1), width-1, minY-i, green);

      minY += 1+lineHeight;
    }
  }

  private void drawText(PoseStack matrixStack) {
    int size = ClientDialogueUtil.FUNCTION_NAMES.size();
    for (int i = 0; i < (size > maxSize ? maxSize : size); i++) {
      String name = ClientDialogueUtil.FUNCTION_NAMES.get(i+scrollY);
      int y = 2+i*lineHeight;
      minecraft.font.draw(matrixStack, name, width/2-minecraft.font.width(name)/2, y, 0x000000);
    }
  }

  public void clickedOn(double mouseX, double mouseY) {
    if (isVisible()){
      if (mouseX >= this.x && mouseX <= this.x + width && mouseY >= this.y && mouseY <= this.y + height) {
        if (ClientDialogueUtil.FUNCTION_NAMES.size() > 0) {
          int minY = this.y + 1;
          int size = ClientDialogueUtil.FUNCTION_NAMES.size();
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
    int index = Mth.clamp(i+scrollY, 0, ClientDialogueUtil.FUNCTION_NAMES.size()-1);
    String newSelection = ClientDialogueUtil.FUNCTION_NAMES.get(index);
    if (this.selectedFunction.equals(newSelection) || i == -1) {
      this.selectedFunction = "";
    } else {
      this.selectedFunction = newSelection;
    }
  }

  public void setFunction() {
    String function = this.selectedFunction.split("::")[0];
    if (!screen.getNewFunctionParams().isEmpty()) {
      function += "::"+screen.getNewFunctionParams();
    }
    this.editingNode.getNodeData().setFunction(function);
    this.selectedFunction = "";
  }

  public String getSelectedFunction() {
    return selectedFunction;
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
    int max = ClientDialogueUtil.FUNCTION_NAMES.size()-maxSize;
    if (max > 0) {
      return Mth.clamp(newScroll, 0, max);
    }
    else {
      return scrollY;
    }
  }

}
