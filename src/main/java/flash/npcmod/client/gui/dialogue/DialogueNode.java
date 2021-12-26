package flash.npcmod.client.gui.dialogue;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.client.gui.screen.dialogue.DialogueBuilderScreen;
import flash.npcmod.core.client.dialogues.ClientDialogueUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static net.minecraft.client.gui.GuiComponent.blit;
import static net.minecraft.client.gui.GuiComponent.fill;

@OnlyIn(Dist.CLIENT)
public class DialogueNode {
  private static final ResourceLocation TEXTURES = new ResourceLocation(Main.MODID, "textures/gui/edit_dialogue/textures.png");

  private final DialogueBuilderScreen dialogueBuilderScreen;
  private final Minecraft minecraft;
  private final Dialogue dialogue;

  private boolean dragging;
  private boolean isQueuedForRemoval;

  private int x, y, width;
  private int nameBarHeight, textBarHeight, responseBarHeight, functionBarHeight;
  private int actualHeight;
  private int[] optionsBarHeight;

  private static final int maxWidth = 120;
  private static final int defaultTextHeight = 2+Minecraft.getInstance().font.lineHeight;

  public boolean hasConflictingName;

  @Nullable
  public DialogueNode parent;

  public DialogueNode(@Nullable DialogueNode parent, DialogueBuilderScreen screen, Minecraft minecraft, Dialogue dialogue) {
    this(parent, screen, minecraft, dialogue, 2, 2);
  }

  public DialogueNode(@Nullable DialogueNode parent, DialogueBuilderScreen screen, Minecraft minecraft, Dialogue dialogue, int x, int y) {
    this(parent, screen, minecraft, dialogue, x, y, 50);
  }

  public DialogueNode(@Nullable DialogueNode parent, DialogueBuilderScreen screen, Minecraft minecraft, Dialogue dialogue, int x, int y, int width) {
    this.dialogueBuilderScreen = screen;
    this.minecraft = minecraft;
    this.dialogue = dialogue;

    setParent(parent);
    this.parent = parent;

    this.setPosition(x, y);
    this.setWidth(width);

    this.nameBarHeight = defaultTextHeight;

    calculateDimensions();
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
    return actualHeight;
  }

  private void adjustWidth(String text) {
    int textWidth = minecraft.font.width(text);
    if (textWidth > width && textWidth < maxWidth) {
      setWidth(textWidth);
    } else {
      setWidth(maxWidth);
    }
  }

  public void calculateDimensions() {
    String text = getText();
    adjustWidth(text);
    String response = getResponse();
    adjustWidth(response);
    String function = getFunction();
    adjustWidth(function);
    for (int i = 0; i < getOptionsNames().length; i++) {
      adjustWidth(getOptionsNames()[i]);
    }

    // Setting text bar height
    List<FormattedCharSequence> trimmedText = minecraft.font.split(new TextComponent(text), this.width-4);
    int numOfTextLines = trimmedText.size() > 0 ? trimmedText.size() : 1;
    this.textBarHeight = defaultTextHeight * numOfTextLines;

    // Setting response bar height
    List<FormattedCharSequence> trimmedResponse = minecraft.font.split(new TextComponent(response), this.width-4);
    int numOfResponseLines = trimmedResponse.size() > 0 ? trimmedResponse.size() : 1;
    this.responseBarHeight = isDialogueStart() ? -1 : defaultTextHeight * numOfResponseLines;

    // Setting the function bar height
    List<FormattedCharSequence> trimmedFunction = minecraft.font.split(new TextComponent(function), this.width-4);
    int numOfFunctionLines = trimmedFunction.size() > 0 ? trimmedFunction.size() : 1;
    this.functionBarHeight = defaultTextHeight * numOfFunctionLines;

    // Setting options bar height
    this.optionsBarHeight = new int[getOptionsNames().length+1];
    for (int i = 0; i < getOptionsNames().length; i++) {
      List<FormattedCharSequence> trimmedOption = minecraft.font.split(new TextComponent(getOptionsNames()[i]), this.width-4);
      optionsBarHeight[i] = defaultTextHeight*trimmedOption.size();
    }
    optionsBarHeight[getOptionsNames().length] = defaultTextHeight;

    this.actualHeight = 1+nameBarHeight+1+textBarHeight+1+responseBarHeight+1+functionBarHeight+1;
    for (int optionHeight : optionsBarHeight) {
      actualHeight += optionHeight;
      actualHeight += 1;
    }
    actualHeight += 1;
  }

  public Dialogue getDialogue() {
    return dialogue;
  }

  public String getName() {
    return dialogue.getName();
  }

  public String getText() {
    return dialogue.getText();
  }

  public String getResponse() {
    return dialogue.getResponse();
  }

  public String getFunction() {
    return dialogue.getFunction();
  }

  private String[] getOptionsText() {
    String[] options = new String[dialogue.getChildren().length];
    for (int i = 0; i < options.length; i++) {
      options[i] = dialogue.getChildren()[i].getText();
    }
    return options;
  }

  public String[] getOptionsNames() {
    String[] options = new String[dialogue.getChildren().length];
    for (int i = 0; i < options.length; i++) {
      options[i] = dialogue.getChildren()[i].getName();
    }
    return options;
  }

  public void setPosition(int x, int y) {
    this.x = Mth.clamp(x, 0, dialogueBuilderScreen.maxX);
    this.y = Mth.clamp(y, 0, dialogueBuilderScreen.maxY);
  }

  private void setWidth(int width) {
    this.width = Math.min(Math.max(50, width), maxWidth);
  }

  public void draw(PoseStack matrixStack, double scrollX, double scrollY, double mouseX, double mouseY) {
    matrixStack.pushPose();
    matrixStack.translate(scrollX, scrollY, 0);
    matrixStack.translate(x, y, 0);

    // We need to render these regardless of this being visible or not, otherwise we couldn't see the lines and the icons on the parent
    drawLinesToParent(matrixStack);
    drawIcons(matrixStack);

    if (isVisible(scrollX, scrollY)) {

      drawRectangles(matrixStack);

      minecraft.font.draw(matrixStack, getName(), 3, 3, 0x000000);

      String text = getText();
      String response = getResponse();
      String function = getFunction();

      int minY = 2 + nameBarHeight;

      drawMultilineText(matrixStack, text.isEmpty() ? "Text" : text, minY, text.isEmpty());
      minY += textBarHeight + 1;

      if (!isDialogueStart()) {
        drawMultilineText(matrixStack, response.isEmpty() ? "Response" : response, minY, response.isEmpty());
      }
      minY += responseBarHeight + 1;

      drawMultilineText(matrixStack, function.isEmpty() ? "Function" : function, minY, function.isEmpty());
      minY += functionBarHeight;
      for (int i = 0; i < getOptionsNames().length; i++) {
        minY += (i > 0 ? optionsBarHeight[i - 1] : 0) + 1;

        drawMultilineText(matrixStack, getOptionsNames()[i],
            minY, false);
      }
      if (getOptionsNames().length > 0) {
        minY += optionsBarHeight[optionsBarHeight.length - 1] + 1;
      } else {
        minY += 1;
      }

      drawMultilineText(matrixStack, "New Option", minY, true);

    }

    matrixStack.popPose();
  }

  private void drawMultilineText(PoseStack matrixStack, String text, int y, boolean isEmpty) {
    List<FormattedCharSequence> lines = minecraft.font.split(new TextComponent(text), width-4);
    for (int i = 0; i < lines.size(); i++) {
      FormattedCharSequence processor = lines.get(i);
      minecraft.font.draw(matrixStack, processor, 3, y+2+(defaultTextHeight*i), isEmpty ? 0xFFBBBBBB : 0x000000);
    }
  }

  private void drawRectangles(PoseStack matrixStack) {
    int black = 0xFF000000;
    int gray = 0xFFCCCCCC;
    int nameBar = hasConflictingName ? 0xFFFF0000 : 0xFF00FFFF;

    // Black box outlines
    fill(matrixStack, 0, 0, width, 1, black);
    fill(matrixStack, 0, 0, 1, actualHeight, black);
    fill(matrixStack, width - 1, 0, width, actualHeight, black);
    fill(matrixStack, 0, actualHeight - 1, width, actualHeight, black);

    // Inside
    fill(matrixStack, 1, 1, width-1, actualHeight-1, gray);

    // Bar for dialogue name
    int minY = 1+nameBarHeight;
    fill(matrixStack, 1, 1, width-1, minY, nameBar);
    fill(matrixStack, 1, minY, width-1, minY+1, black);

    // Bar for text
    minY += 1+textBarHeight;
    fill(matrixStack, 1, minY, width-1, minY+1, black);

    // Bar for response (not needed for dialogue branch starts)
    minY += 1+responseBarHeight;
    if (!isDialogueStart()) {
      fill(matrixStack, 1, minY, width-1, minY+1, black);
    }

    // Bar for function
    minY += 1+functionBarHeight;
    fill(matrixStack, 1, minY, width-1, minY+1, black);

    // Options bars
    for (int i = 0; i < getOptionsNames().length; i++) {
      minY += 1+optionsBarHeight[i];
      fill(matrixStack, 1, minY,
          width-1, minY+1, black);
    }
  }

  private void drawIcons(PoseStack matrixStack) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, TEXTURES);

    // Drag icon
    blit(matrixStack, -9, -9, 7, 7, 8, 0, 7, 7, 256, 256);

    // Text icon
    if (!isDialogueStart() || (this.equals(dialogueBuilderScreen.getSelectedNode()) && dialogueBuilderScreen.getSelectedNodeIndex() == 0)) {
      int[] xy = getTextIconLocation();
      blit(matrixStack, xy[0], xy[1], 8, 8, 0, 0, 8, 8, 256, 256);
    }

    // Option Icon on Parent, so the line we draw from this to the parent isn't over said icon
    if (parent != null) {
      matrixStack.pushPose();

      int x = this.x - parent.getX();
      int y = this.y - parent.getY();

      matrixStack.translate(-x, -y, 0);

      int index = Arrays.asList(parent.getDialogue().getChildren()).indexOf(this.getDialogue());
      if (index >= 0 && index < parent.optionsBarHeight.length) {
        int[] xy = parent.getOptionIconLocation(index);
        blit(matrixStack, xy[0], xy[1], 8, 8, 0, 0, 8, 8, 256, 256);
      }
      matrixStack.popPose();
    }

    // Option Icons on this
    matrixStack.pushPose();
    {
      for (int i = 0; i < optionsBarHeight.length; i++) {
        int[] xy = getOptionIconLocation(i);
        blit(matrixStack, xy[0], xy[1], 8, 8, 0, 0, 8, 8, 256, 256);
      }
    }
    matrixStack.popPose();
  }

  private void drawLinesToParent(PoseStack matrixStack) {
    if (parent != null) {
      int index = 0;
      for (int i = 0; i < parent.getOptionsNames().length; i++) {
        if (parent.getOptionsNames()[i].equals(getName())) {
          index = i+1;
          break;
        }
      }

      if (index > 0 && index < parent.getOptionsNames().length+1) {
        int[] thisXY = getTextIconLocation();
        int[] parentXY = parent.getOptionIconLocation(index-1);

        int thisX = x+thisXY[0];
        int thisY = y+thisXY[1];
        int parentX = parent.getX()+parentXY[0];
        int parentY = parent.getY()+parentXY[1];

        int color = 0xFFFFFF00;

        matrixStack.pushPose();
        {
          matrixStack.translate(thisXY[0], thisXY[1], 0);
          matrixStack.translate(3.5, 3.5, 0);

          int lineLength = -(thisX-parentX);
          int lineHeight = -(thisY-parentY);

          fill(matrixStack, 0, 0, lineLength / 2, 1, color);
          fill(matrixStack, lineLength / 2, 0, lineLength / 2 + 1, lineHeight, color);
          fill(matrixStack, lineLength / 2 + 1, lineHeight, lineLength, lineHeight + 1, color);
        }
        matrixStack.popPose();
      }
    }
  }

  public void addChild(Dialogue dialogue) {
    if (!this.dialogue.isChild(dialogue)) {
      this.getDialogue().addChild(dialogue);
      calculateDimensions();
    }
  }

  public void removeChild(Dialogue dialogue) {
    if (this.dialogue.isChild(dialogue)) {
      this.getDialogue().removeChild(dialogue);
      calculateDimensions();
    }
  }

  public void setParent(@Nullable DialogueNode node) {
    if (!this.equals(node)) {
      if (parent != null) {
        parent.removeChild(this.getDialogue());
      }
      if (node != null) {
        node.addChild(this.getDialogue());
      } else if (dialogueBuilderScreen.getSelectedNode() == null) {
        this.getDialogue().setResponse("");
      }
      this.parent = node;
      this.calculateDimensions();
    }
  }

  public void remove() {
    if (dialogueBuilderScreen.allDialogueNodes.contains(this)) {
      setParent(null);

      dialogueBuilderScreen.allDialogueNodes.remove(this);
    }
  }

  public int[] getTextIconLocation() {
    int x = -12;
    int y = 2+nameBarHeight+(textBarHeight/2)-4;
    return new int[]{x, y};
  }

  public int[] getOptionIconLocation(int option) {
    int x = width+4;
    int minY = 2+nameBarHeight+textBarHeight+1+responseBarHeight+1+functionBarHeight;
    for (int i = 0; i < option; i++) {
      minY += 1+optionsBarHeight[i];
    }
    int y = minY+1+(optionsBarHeight[option]/2)-4;
    return new int[]{x, y};
  }

  public void clickedOn(double mouseX, double mouseY, int button,  int offsetX, int offsetY, double scrollX, double scrollY) {
    if (isVisible(scrollX, scrollY)) {
      double minX = offsetX + 9 + (scrollX + x);
      double maxX = minX + width;
      if (mouseX >= minX && mouseX <= maxX) {
        double minY = offsetY + 18 + 1 + (scrollY + y);
        if (mouseY >= minY && mouseY <= minY + nameBarHeight) {
          clickedOnNameBar(button);
        }
        minY += nameBarHeight + 1;
        if (mouseY >= minY && mouseY <= minY + textBarHeight) {
          clickedOnTextBar();
        }
        minY += textBarHeight + 1;
        if (!isDialogueStart() && mouseY >= minY && mouseY <= minY + responseBarHeight) {
          clickedOnResponseBar();
        }
        minY += responseBarHeight + 1;
        if (mouseY >= minY
            && mouseY <= minY + functionBarHeight) {
          clickedOnFunctionBar();
        }
        else {
          double optionMinY = minY + functionBarHeight + 1;
          if (getOptionsText().length > 0) {
            int optionIndex = -2;
            for (int i = 0; i < getOptionsText().length+1; i++) {
              if (optionsBarHeight.length > i) {
                if (mouseY >= optionMinY && mouseY <= optionMinY + optionsBarHeight[i] + (i == 0 ? 0 : 2)) {
                  optionIndex = i;
                  break;
                }
                optionMinY += optionsBarHeight[i] + 1;
              }
            }

            if (optionIndex == getOptionsText().length) {
              optionIndex = -1;
            }

            if (optionIndex >= -1 && optionIndex < getOptionsText().length) {
              clickedOnOptionBar(optionIndex, button);
            }
          } else {
            if (mouseY >= optionMinY && mouseY <= optionMinY + defaultTextHeight) {
              clickedOnOptionBar(-1, button);
            }
          }
        }
      }
      if (button == 0) {
        if (!isDialogueStart() && mouseX >= minX - 12 && mouseX <= minX - 12 + 8) {
          int xy[] = getTextIconLocation();
          double minY = offsetY + 18 + 1 + (scrollY + y);
          if (mouseY >= minY + xy[1] && mouseY <= minY + xy[1] + 8) {
            clickedOnSelectIcon(0);
          }
        }
        if (mouseX >= maxX + 4 && mouseX <= maxX + 4 + 8) {
          double minY = offsetY + 18 + 1 + (scrollY + y);
          // Select current option
          for (int i = 0; i < getOptionsNames().length; i++) {
            int xy[] = getOptionIconLocation(i);
            if (mouseY >= minY + xy[1] && mouseY <= minY + xy[1] + 8) {
              clickedOnSelectIcon(i + 1);
            }
          }
          // Select new option
          int xy[] = getOptionIconLocation(getOptionsNames().length);
          if (mouseY >= minY + xy[1] && mouseY <= minY + xy[1] + 8) {
            clickedOnSelectIcon(-1);
          }
        }
        if (mouseX >= minX - 9 && mouseX <= minX - 2) {
          double minY = offsetY + 18 + 1 + (scrollY + y) - 9;
          double maxY = minY + 7;
          if (mouseY >= minY && mouseY <= maxY) {
            this.dragging = true;
          }
        }
      }
    }
  }

  public boolean isDragging() {
    return dragging;
  }

  public void stopDragging() {
    dragging = false;
  }

  public boolean isQueuedForRemoval() {
    return isQueuedForRemoval;
  }

  public void clickedOnNameBar(int button) {
    if (!isInitDialogue()) {
      if (button == 1) {
        // If we are not an init dialogue option, when right clicked on, queue for removal
        this.isQueuedForRemoval = true;
        return;
      }
    }

    if (button == 0) {
      // If we have a selected node
      if (dialogueBuilderScreen.getSelectedNode() != null) {
        if (dialogueBuilderScreen.getSelectedNodeIndex() == dialogueBuilderScreen.getSelectedNode().getOptionsNames().length+1) {
          // With a selection index of -1 in the dialogue builder screen,
          // that means we are setting this as its new option
          this.setParent(dialogueBuilderScreen.getSelectedNode());
          dialogueBuilderScreen.setSelectedNode(null, 0);
        }
        else if (dialogueBuilderScreen.getSelectedNodeIndex() == 0) {
          // Otherwise if the selection index is 0,
          // we need to set the selected node's parent to this.
          dialogueBuilderScreen.getSelectedNode().setParent(this);
          dialogueBuilderScreen.setSelectedNode(null, 0);
        }
      }
      // If that's not the case and this isn't an init dialogue node, then we can make our required widgets visible to rename this
      else if (!isInitDialogue()) {
        dialogueBuilderScreen.setNodeBeingEdited(this, DialogueBuilderScreen.EditType.NAME);
      }
    }
  }

  public void clickedOnTextBar() {
    dialogueBuilderScreen.setNodeBeingEdited(this, DialogueBuilderScreen.EditType.TEXT);
  }

  public void clickedOnResponseBar() {
    dialogueBuilderScreen.setNodeBeingEdited(this, DialogueBuilderScreen.EditType.RESPONSE);
  }

  public void clickedOnFunctionBar() {
    dialogueBuilderScreen.setNodeBeingEdited(this, DialogueBuilderScreen.EditType.FUNCTION);
  }

  public void clickedOnOptionBar(int optionIndex, int button) {
    if (optionIndex >= 0 && optionIndex < this.dialogue.getChildren().length) {
      DialogueNode child = findChild(optionIndex);
      if (child != null) {
        if (button == 0) {
          // If left clicked, then center the screen on this node
          dialogueBuilderScreen.centerScreenOnNode(child);
        } else if (button == 1) {
          // Otherwise set the child's parent to null
          child.setParent(null);
        }
      }
    }
  }

  @Nullable
  private DialogueNode findChild(int index) {
    Dialogue child = this.getDialogue().getChildren()[index];
    for (DialogueNode node : dialogueBuilderScreen.allDialogueNodes) {
      if (node.getDialogue().equals(child)) {
        return node;
      }
    }
    return null;
  }

  /*
   * When clicked on a select icon (the icon in front of the text or the icons next to the children options),
   * select this node in the dialogue builder screen,
   */
  public void clickedOnSelectIcon(int index) {
    if (dialogueBuilderScreen.allDialogueNodes.contains(this)) {
      if (index == 0) {
        this.dialogueBuilderScreen.setSelectedNode(this, index);
        this.setParent(null);
      }
      else if (index == -1) {
        this.dialogueBuilderScreen.setSelectedNode(this, getOptionsNames().length+1);
      }
      else {
        for (DialogueNode node : dialogueBuilderScreen.allDialogueNodes) {
          if (node.getName().equals(this.getOptionsNames()[index - 1])) {
            if (node.parent != null && node.parent.equals(this)) {
              node.setParent(null);
              this.dialogueBuilderScreen.setSelectedNode(this, getOptionsNames().length + 1);
              break;
            }
          }
        }
      }
    }
  }

  public boolean isVisible(double scrollX, double scrollY) {
    return -scrollX <= x+9+width+12 && -scrollY <= y+18+actualHeight && -scrollX + minecraft.getWindow().getGuiScaledWidth() >= x+9-12 && -scrollY + minecraft.getWindow().getGuiScaledHeight() >= y+18;
  }

  public boolean isDialogueStart() {
    return parent == null;
  }

  public boolean isInitDialogue() {
    return getName().equals(ClientDialogueUtil.INIT_DIALOGUE_NAME);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DialogueNode that = (DialogueNode) o;
    return x == that.x && y == that.y && width == that.width && actualHeight == that.actualHeight && dialogue.equals(that.dialogue);
  }
}
