package flash.npcmod.client.gui.dialogue;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.core.node.BuilderNode;
import flash.npcmod.client.gui.screen.dialogue.DialogueBuilderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static net.minecraft.client.gui.GuiComponent.blit;
import static net.minecraft.client.gui.GuiComponent.fill;

/**
 * The node that is visible on a builder screen. This class is in charge of drawing itself and tracking
 * its own gui elements.
 */
@OnlyIn(Dist.CLIENT)
public class DialogueNode extends BuilderNode {
    protected static final int NUM_EXTRA_FIELDS = 3;
    protected Dialogue nodeData;

    public DialogueNode(@Nullable DialogueNode parent, DialogueBuilderScreen screen, Minecraft minecraft, Dialogue dialogue) {
        this(parent, screen, minecraft, dialogue, 2, 2);
    }

    public DialogueNode(@Nullable DialogueNode parent, DialogueBuilderScreen screen, Minecraft minecraft, Dialogue dialogue, int x, int y) {
        this(parent, screen, minecraft, dialogue, x, y, 50);
    }

    public DialogueNode(@Nullable DialogueNode parent, DialogueBuilderScreen screen, Minecraft minecraft, Dialogue dialogue, int x, int y, int width) {
        super(screen, minecraft, x, y, width);
        this.nodeData = dialogue;
        setParent(parent);
    }

    /**
     * Calculate the width and height of the background color.
     */
    public void calculateDimensions() {
        String text = getText();
        adjustWidth(text);
        String response = getResponse();
        adjustWidth(response);
        for (int i = 0; i < getConnectionNames().length; i++) {
            adjustWidth(getConnectionNames()[i]);
        }

        super.calculateDimensions();

        // Setting text bar height
        List<FormattedCharSequence> trimmedText = minecraft.font.split(new TextComponent(text), this.width - 4);
        int numOfTextLines = trimmedText.size() > 0 ? trimmedText.size() : 1;
        this.extraFieldsHeight[0] = defaultTextHeight * numOfTextLines;
        actualHeight += this.extraFieldsHeight[0] + 1;

        // Setting response bar height
        List<FormattedCharSequence> trimmedResponse = minecraft.font.split(new TextComponent(response), this.width - 4);
        int numOfResponseLines = trimmedResponse.size() > 0 ? trimmedResponse.size() : 1;
        this.extraFieldsHeight[1] = isStart() ? -1 : defaultTextHeight * numOfResponseLines;
        actualHeight += this.extraFieldsHeight[1] + 1;

        // Setting trigger bar height
        this.extraFieldsHeight[2] = defaultTextHeight;
        actualHeight += this.extraFieldsHeight[2] + 1;

    }


    /**
     * Check if this node was clicked on.
     *
     * @param mouseX  Mouse position.
     * @param mouseY  Mouse position.
     * @param button  The mouse button.
     * @param offsetX The screen offset.
     * @param offsetY The screen offset.
     * @param scrollX The screen scroll.
     * @param scrollY The screen scroll.
     */
    public void clickedOn(double mouseX, double mouseY, int button, int offsetX, int offsetY, double scrollX, double scrollY) {
        if (isVisible(scrollX, scrollY)) {
            double minX = offsetX + 9 + (scrollX + x);
            double maxX = minX + width;
            double minY = offsetY + 18 + 1 + (scrollY + y);
            double maxY = minY + actualHeight;
            if (mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY) {
                if (checkIfConnectingNodes()) return;
                if (mouseY >= minY && mouseY <= minY + nameBarHeight) {
                    clickedOnNameBar(button);
                    return;
                }
                minY += nameBarHeight + 1;
                if (mouseY >= minY && mouseY <= minY + extraFieldsHeight[0]) {
                    clickedOnTextBar();
                    return;
                }
                minY += extraFieldsHeight[0] + 1;
                if (!isStart() && mouseY >= minY && mouseY <= minY + extraFieldsHeight[1]) {
                    clickedOnResponseBar();
                    return;
                }
                minY += extraFieldsHeight[1] + 1;
                if (mouseY >= minY && mouseY <= minY + extraFieldsHeight[2]) {
                    clickedOnTriggerBar();
                    return;
                }
                minY += extraFieldsHeight[2] + 1;
                if (mouseY >= minY
                        && mouseY <= minY + functionBarHeight) {
                    clickedOnFunctionBar();
                    return;
                } else {
                    double optionMinY = minY + functionBarHeight + 1;
                    if (getOptionsText().length > 0) {
                        int optionIndex = -2;
                        for (int i = 0; i < getOptionsText().length + 1; i++) {
                            if (connectionBarsHeights.length > i) {
                                if (mouseY >= optionMinY && mouseY <= optionMinY + connectionBarsHeights[i] + (i == 0 ? 0 : 2)) {
                                    optionIndex = i;
                                    break;
                                }
                                optionMinY += connectionBarsHeights[i] + 1;
                            }
                        }

                        if (optionIndex == getOptionsText().length) {
                            optionIndex = -1;
                        }

                        if (optionIndex >= -1 && optionIndex < getOptionsText().length) {
                            clickedOnOptionBar(optionIndex, button);
                            return;
                        }
                    } else {
                        if (mouseY >= optionMinY && mouseY <= optionMinY + defaultTextHeight) {
                            clickedOnOptionBar(-1, button);
                            return;
                        }
                    }
                }
            }
            if (button == 0) {
                if (!isStart() && mouseX >= minX - 12 && mouseX <= minX - 12 + 8) {
                    int[] xy = getEndPointLocation();
                    minY = offsetY + 18 + 1 + (scrollY + y);
                    if (mouseY >= minY + xy[1] && mouseY <= minY + xy[1] + 8) {
                        clickedOnSelectIcon(-2);
                        return;
                    }
                }
                if (mouseX >= maxX + 4 && mouseX <= maxX + 4 + 8) {
                    minY = offsetY + 18 + 1 + (scrollY + y);
                    // Select current option
                    for (int i = 0; i < getConnectionNames().length; i++) {
                        int[] xy = getStartPointLocation(i);
                        if (mouseY >= minY + xy[1] && mouseY <= minY + xy[1] + 8) {
                            clickedOnSelectIcon(i);
                            return;
                        }
                    }
                    // Select new option
                    int[] xy = getStartPointLocation(getConnectionNames().length);
                    if (mouseY >= minY + xy[1] && mouseY <= minY + xy[1] + 8) {
                        clickedOnSelectIcon(-1);
                        return;
                    }
                }
                if (mouseX >= minX - 9 && mouseX <= minX - 2) {
                    minY = offsetY + 18 + 1 + (scrollY + y) - 9;
                    maxY = minY + 7;
                    if (mouseY >= minY && mouseY <= maxY) {
                        this.dragging = true;
                    }
                }
            }
        }
    }

    /**
     * Set the Response Field as being edited.
     */
    public void clickedOnResponseBar() {
        builderScreen.setNodeBeingEdited(this, DialogueBuilderScreen.EditType.RESPONSE);
    }

    /**
     * Set the Text Field as being edited.
     */
    public void clickedOnTextBar() {
        builderScreen.setNodeBeingEdited(this, DialogueBuilderScreen.EditType.TEXT);
    }

    /**
     * Set the Trigger Field as being edited.
     */
    public void clickedOnTriggerBar() {
        builderScreen.setNodeBeingEdited(this, DialogueBuilderScreen.EditType.ACTION);
    }

    /**
     * Draw the extra rectangles.
     * @param matrixStack The PoseStack.
     * @param minY The minimal y point.
     * @return The new minY.
     */
    @Override
    protected int drawExtraRectangles(PoseStack matrixStack, int minY) {
        int black = 0xFF000000;

        // Bar for text
        minY += 1 + extraFieldsHeight[0];
        fill(matrixStack, 1, minY, width - 1, minY + 1, black);

        // Bar for response (not needed for dialogue branch starts)
        minY += 1 + extraFieldsHeight[1];
        if (!isStart()) {
            fill(matrixStack, 1, minY, width - 1, minY + 1, black);
        }

        // Bar for triggers
        minY += 1 + extraFieldsHeight[2];
        fill(matrixStack, 1, minY, width - 1, minY + 1, black);

        return minY;
    }

    /**
     * Draw the extra text lines.
     * @param matrixStack The PoseStack.
     * @param minY The minimal y point.
     * @return The new minY.
     */
    @Override
    protected int drawExtraTexts(PoseStack matrixStack, int minY) {
        // Draw File
        String text = getText();
        String response = getResponse();
        String trigger = getTrigger();

        drawMultilineText(matrixStack, text.isEmpty() ? "Text" : text, minY, text.isEmpty());
        minY += extraFieldsHeight[0] + 1;

        if (!isStart()) {
            drawMultilineText(matrixStack, response.isEmpty() ? "Response" : response, minY, response.isEmpty());
        }
        minY += extraFieldsHeight[1] + 1;

        drawSingleLineText(matrixStack, trigger.isEmpty() ? "Trigger" : trigger, minY, trigger.isEmpty());
        minY += extraFieldsHeight[2] + 1;

        return minY;
    }

    /**
     * Draw the end and start point icons.
     * @param matrixStack PoseStack
     */
    @Override
    protected void drawIcons(PoseStack matrixStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURES);

        // Drag icon
        blit(matrixStack, -9, -9, 7, 7, 8, 0, 7, 7, 256, 256);

        // Text icon
        if (!isStart() || (this.equals(builderScreen.getSelectedNode()) && builderScreen.getSelectedNodeIndex() == 0)) {
            int[] xy = getEndPointLocation();
            blit(matrixStack, xy[0], xy[1], 8, 8, 0, 0, 8, 8, 256, 256);
        }

        // Option Icon on Parent, so the line we draw from this to the parent isn't over said icon
        if (parent != null) {
            matrixStack.pushPose();

            int x = this.x - parent.getX();
            int y = this.y - parent.getY();

            matrixStack.translate(-x, -y, 0);

            int index = Arrays.asList(parent.getNodeData().getChildren()).indexOf(this.getNodeData());
            if (index >= 0 && index < parent.getConnectionBarsHeights().length) {
                int[] xy = parent.getStartPointLocation(index);
                blit(matrixStack, xy[0], xy[1], 8, 8, 0, 0, 8, 8, 256, 256);
            }
            matrixStack.popPose();
        }

        // Option Icons on this
        matrixStack.pushPose();
        {
            for (int i = 0; i < connectionBarsHeights.length; i++) {
                int[] xy = getStartPointLocation(i);
                blit(matrixStack, xy[0], xy[1], 8, 8, 0, 0, 8, 8, 256, 256);
            }
        }
        matrixStack.popPose();
    }

    /**
     * Draw the lines to the parent.
     * @param matrixStack PoseStack.
     */
    @Override
    protected void drawLinesToParent(PoseStack matrixStack) {
        if (parent != null) {
            int index = 0;
            for (int i = 0; i < parent.getConnectionNames().length; i++) {
                if (parent.getConnectionNames()[i].equals(getName())) {
                    index = i + 1;
                    break;
                }
            }

            if (index > 0 && index < parent.getConnectionNames().length + 1) {
                int[] thisXY = getEndPointLocation();
                int[] parentXY = parent.getStartPointLocation(index - 1);

                int thisX = x + thisXY[0];
                int thisY = y + thisXY[1];
                int parentX = parent.getX() + parentXY[0];
                int parentY = parent.getY() + parentXY[1];

                int color = 0xFFFFFF00;

                matrixStack.pushPose();
                {
                    matrixStack.translate(thisXY[0], thisXY[1], 0);
                    matrixStack.translate(3.5, 3.5, 0);

                    int lineLength = -(thisX - parentX);
                    int lineHeight = -(thisY - parentY);

                    fill(matrixStack, 0, 0, lineLength / 2, 1, color);
                    fill(matrixStack, lineLength / 2, 0, lineLength / 2 + 1, lineHeight, color);
                    fill(matrixStack, lineLength / 2 + 1, lineHeight, lineLength, lineHeight + 1, color);
                }
                matrixStack.popPose();
            }
        }
    }

    /**
     * Check the objects for equality.
     * @param o The object to compare to.
     * @return The equality.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DialogueNode that = (DialogueNode) o;
        return x == that.x && y == that.y && width == that.width && actualHeight == that.actualHeight && nodeData.equals(that.nodeData);
    }

    /**
     * Get the node data of the node.
     * @return The node data.
     */
    public Dialogue getNodeData(){
        return this.nodeData;
    }

    /**
     * Get the number of extra fields.
     * @return The number of extra fields.
     */
    public int getNumExtraFields() {
        return NUM_EXTRA_FIELDS;
    }

    /**
     * Get text of each child.
     * @return The text of each child.
     */
    private String[] getOptionsText() {
        String[] options = new String[nodeData.getChildren().length];
        for (int i = 0; i < options.length; i++) {
            options[i] = nodeData.getChildren()[i].getText();
        }
        return options;
    }

    /**
     * Get the response.
     * @return The response.
     */
    public String getResponse() {
        return nodeData.getResponse();
    }

    /**
     * Get the text.
     * @return The text.
     */
    public String getText() {
        return nodeData.getText();
    }

    /**
     * Get the trigger.
     * @return The trigger.
     */
    public String getTrigger() {
        return nodeData.getTrigger();
    }

    /**
     * Set the parent to a new node.
     * @param node The new parent.
     */
    @Override
    public void setParent(@Nullable BuilderNode node) {
        if (!this.equals(node)) {
            if (parent != null) {
                parent.removeChild(this.getNodeData());
            }
            if (node != null) {
                node.addChild(this.getNodeData());
            } else if (builderScreen.getSelectedNode() == null) {
                this.getNodeData().setResponse("");
            }
            this.parent = node;
            this.calculateDimensions();
        }
    }
}
