package flash.npcmod.client.gui.behavior;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.core.behaviors.Behavior;
import flash.npcmod.core.node.BuilderNode;
import flash.npcmod.core.node.NodeData;
import flash.npcmod.client.gui.screen.BehaviorBuilderScreen;
import flash.npcmod.client.gui.screen.TreeBuilderScreen;
import flash.npcmod.core.behaviors.Trigger;
import flash.npcmod.core.client.behaviors.ClientBehaviorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.minecraft.client.gui.GuiComponent.blit;
import static net.minecraft.client.gui.GuiComponent.fill;

/**
 * The node that is visible on a builder screen. This class is in charge of drawing itself and tracking
 * its own gui elements.
 */
@OnlyIn(Dist.CLIENT)
public class BehaviorNode extends BuilderNode {
    protected static final int NUM_EXTRA_FIELDS = 2;
    protected Behavior nodeData;
    /**
     * The trigger index being edited.
     */
    private int editTriggerIndex = -1;

    public BehaviorNode(@Nullable BehaviorNode parent, BehaviorBuilderScreen screen, Minecraft minecraft, Behavior behavior) {
        this(parent, screen, minecraft, behavior, 2, 2);
    }

    public BehaviorNode(@Nullable BehaviorNode parent, BehaviorBuilderScreen screen, Minecraft minecraft, Behavior dialogue, int x, int y) {
        this(parent, screen, minecraft, dialogue, x, y, 50);
    }

    public BehaviorNode(@Nullable BehaviorNode parent, BehaviorBuilderScreen screen, Minecraft minecraft, Behavior behavior, int x, int y, int width) {
        super(screen, minecraft, x, y, width);
        this.nodeData = behavior;
        setParent(parent);
    }

    public void calculateDimensions() {
        String dialogueName = getDialogueName();
        adjustWidth(dialogueName);
        super.calculateDimensions();

        // Setting dialogue bar height
        this.extraFieldsHeight[0] = defaultTextHeight;
        actualHeight += this.extraFieldsHeight[0] + 1;
        this.extraFieldsHeight[1] = defaultTextHeight;
        actualHeight += this.extraFieldsHeight[1] + 1;
    }

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
                // Extra Fields Bounding
                if (mouseY >= minY
                        && mouseY <= minY + extraFieldsHeight[0]) {
                    clickedOnDialogueBar();
                    return;
                }
                minY += extraFieldsHeight[0] + 1;
                if (mouseY >= minY
                        && mouseY <= minY + extraFieldsHeight[1]) {
                    clickedOnActionBar();
                    return;
                }
                minY += extraFieldsHeight[1] + 1;
                // Back to normal Bounding
                if (mouseY >= minY
                        && mouseY <= minY + functionBarHeight) {
                    clickedOnFunctionBar();
                    return;
                } else {
                    double optionMinY = minY + functionBarHeight + 1;
                    if (getConnectionNames().length > 0) {
                        int optionIndex = -2;
                        for (int i = 0; i < getConnectionNames().length + 1; i++) {
                            if (connectionBarsHeights.length > i) {
                                if (mouseY >= optionMinY && mouseY <= optionMinY + connectionBarsHeights[i] + (i == 0 ? 0 : 2)) {
                                    optionIndex = i;
                                    break;
                                }
                                optionMinY += connectionBarsHeights[i] + 1;
                            }
                        }

                        if (optionIndex == getConnectionNames().length) {
                            optionIndex = -1;
                        }

                        if (optionIndex >= -1 && optionIndex < getConnectionNames().length) {
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
     * Set the Action Field as being edited.
     */
    public void clickedOnActionBar() {
        builderScreen.setNodeBeingEdited(this, TreeBuilderScreen.EditType.ACTION);
    }

    /**
     * Set the Dialogue Field as being edited.
     */
    public void clickedOnDialogueBar() {
        builderScreen.setNodeBeingEdited(this, TreeBuilderScreen.EditType.FILE);
    }

    /**
     * Center on the Option bar or remove the connection if right-clicked.
     *
     * @param optionIndex The index of the option.
     * @param button      The mouse button.
     */
    public void clickedOnOptionBar(int optionIndex, int button) {
        if (optionIndex >= 0 && optionIndex < this.nodeData.getTriggers().length) {
            if (button == 0) {
                this.editTriggerIndex = optionIndex;
                this.builderScreen.setNodeBeingEdited(this, TreeBuilderScreen.EditType.TRIGGER);
            } else if (button == 1) {
                // Otherwise set the child's parent to null
                Trigger trigger = this.nodeData.getTriggers()[optionIndex];
                for (BuilderNode child : this.builderScreen.allNodes) {
                    if (child.getName().equals(trigger.getNextBehaviorName())) {
                        child.setParent(null);
                        break;
                    }
                }
                this.nodeData.removeTrigger(optionIndex);
                this.calculateDimensions();
            }
        }
    }

    /**
     * When clicked on a select icon (the icon in front of the text or the icons next to the children options),
     * select this node in the behavior builder screen,
     */
    @Override
    public void clickedOnSelectIcon(int index) {
        if (builderScreen.allNodes.contains(this)) {
            if (index == -2) { // end point select icon
                if (this.builderScreen.getSelectedNode() != null) {
                    this.setParent(this.builderScreen.getSelectedNode(), this.builderScreen.getSelectedNodeIndex());
                    this.builderScreen.setSelectedNode(null, 0);
                } else {
                    this.builderScreen.setSelectedNode(this, index);
                    this.setParent(null);

                }
            }
            else if (index == -1) { // last select icon
                if (this.builderScreen.getSelectedNode() != null) {
                    ((BehaviorNode) this.builderScreen.getSelectedNode()).setParent(this, this.nodeData.getTriggers().length);
                    this.builderScreen.setSelectedNode(null, 0);
                } else {
                    this.builderScreen.setSelectedNode(this, getConnectionNames().length);
                }
            }
            else {
                // Disconnect the child and avoid using setParent otherwise the trigger being edited will be removed.
                for (BuilderNode builderNode : builderScreen.allNodes) {
                    if (builderNode.getName().equals(this.getConnectionNames()[index])) {
                        if (builderNode.parent != null && builderNode.parent.equals(this)) {
                            builderNode.setParent(null);
                            break;
                        }
                    }
                }
                if (this.builderScreen.getSelectedNode() != null) {
                    ((BehaviorNode) this.builderScreen.getSelectedNode()).setParent(this, index);
                    this.builderScreen.setSelectedNode(null, 0);
                } else {
                    this.builderScreen.setSelectedNode(this, index);
                }
            }
        }
    }

    /**
     * Draw the extra texts added by this subclass.
     * @param matrixStack The PoseStack.
     * @param minY The minimal y point.
     * @return The new minY.
     */
    @Override
    protected int drawExtraTexts(PoseStack matrixStack, int minY) {
        // Draw File
        String file = getDialogueName();
        drawSingleLineText(matrixStack, file.isEmpty() ? "Dialogue" : file, minY, file.isEmpty());
        minY += extraFieldsHeight[0] + 1;
        // Draw Action
        String action = getActionName();
        drawSingleLineText(matrixStack, action.isEmpty() ? "Action" : action, minY, action.isEmpty());
        minY += extraFieldsHeight[1] + 1;

        return minY;
    }

    /**
     * Draw the rectangles for the extra texts added by this subclass.
     * @param matrixStack The PoseStack.
     * @param minY The minimal y point.
     * @return The new minY.
     */
    @Override
    protected int drawExtraRectangles(PoseStack matrixStack, int minY) {
        int black = 0xFF000000;

        // Bar for extraFields
        minY += extraFieldsHeight[0] + 1;
        fill(matrixStack, 1, minY, width - 1, minY + 1, black);
        minY += extraFieldsHeight[1] + 1;
        fill(matrixStack, 1, minY, width - 1, minY + 1, black);

        return minY;
    }

    @Override
    protected void drawIcons(PoseStack matrixStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURES);

        // Drag icon
        blit(matrixStack, -9, -9, 7, 7, 8, 0, 7, 7, 256, 256);

        // Text icon
        if (!isStart() || (this.equals(builderScreen.getSelectedNode()) && builderScreen.getSelectedNodeIndex() == -2)) {
            int[] xy = getEndPointLocation();
            blit(matrixStack, xy[0], xy[1], 8, 8, 0, 0, 8, 8, 256, 256);
        }

        // Option Icon on Parent, so the line we draw from this to the parent isn't over said icon
        if (parent != null) {
            matrixStack.pushPose();

            int x = this.x - parent.getX();
            int y = this.y - parent.getY();

            matrixStack.translate(-x, -y, 0);

            Trigger[] triggers = ((Behavior) parent.getNodeData()).getTriggers();

            int index = Arrays.stream(triggers).map(Trigger::getNextBehaviorName).collect(Collectors.toList()).indexOf(this.getNodeData());
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
     * Draws the options text.
     *
     * @param matrixStack The matrix stack.
     * @param minY        The height to start drawing from.
     */
    @Override
    protected void drawOptionsText(PoseStack matrixStack, int minY) {
        Trigger[] triggers = this.getNodeData().getTriggers();
        List<String> triggerNames = Arrays.stream(triggers).map(Trigger::getName).collect(Collectors.toList());

        for (int i = 0; i < triggerNames.size(); i++) {
            minY += (i > 0 ? connectionBarsHeights[i - 1] : 0) + 1;
            drawMultilineText(matrixStack, triggerNames.get(i), minY, false);
        }
        if (triggerNames.size() > 0) {
            minY += connectionBarsHeights[connectionBarsHeights.length - 1] + 1;
        } else {
            minY += 1;
        }

        drawMultilineText(matrixStack, "New Trigger", minY, true);
    }

    /**
     * Test for equality.
     * @param o The object to compare to.
     * @return boolean.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BehaviorNode that = (BehaviorNode) o;
        return x == that.x && y == that.y && width == that.width && actualHeight == that.actualHeight && nodeData.equals(that.nodeData);
    }

    public String getActionName() {
        return nodeData.getAction().getName();
    }

    public String getDialogueName() {
        return nodeData.getDialogueName();
    }

    public Trigger getEditTrigger() {
        if (this.editTriggerIndex == -1) return null;
        return nodeData.getTriggers()[this.editTriggerIndex];
    }

    @Override
    public Behavior getNodeData() {
        return nodeData;
    }

    public int getNumExtraFields() {
        return NUM_EXTRA_FIELDS;
    }

    @Override
    public String[] getConnectionNames() {
        Trigger[] triggers = this.getNodeData().getTriggers();
        String [] options = new String[triggers.length];
        Arrays.stream(triggers).map(Trigger::getNextBehaviorName).collect(Collectors.toList()).toArray(options);
        return options;
    }

    public boolean isStart() {
        return getName().equals(ClientBehaviorUtil.INIT_BEHAVIOR_NAME);
    }

    public boolean isEditingTrigger() {
        return this.editTriggerIndex != -1;
    }

    @Override
    public void removeChild(NodeData childData) {
        if (this.nodeData.isChild(childData)) {
            this.getNodeData().removeChild(childData);
            for (Trigger trigger : this.nodeData.getTriggers()) {
                if (trigger.getNextBehaviorName().equals(childData.getName())) {
                    trigger.setNextBehaviorName("");
                }
            }
        }
        calculateDimensions();
    }

    @Override
    public void setParent(@Nullable BuilderNode node) {
        if (!this.equals(node)) {
            if (parent != null) {
                parent.removeChild(this.getNodeData());
            }
            if (node != null) {
                if (this.builderScreen.getSelectedNode() == null) {
                    node.addChild(this.getNodeData());
                } else {
                    node.addChild(this.getNodeData(), this.builderScreen.getSelectedNodeIndex());
                }
            }
            this.parent = node;
            this.calculateDimensions();
        }
    }

    public void setParent(@Nullable BuilderNode node, int index) {
        if (!this.equals(node)) {
            if (parent != null) {
                parent.removeChild(this.getNodeData());
            }
            if (node != null) {
                node.addChild(this.getNodeData(), index);
            }
            this.parent = node;
            this.calculateDimensions();
        }
    }

    public void setEditTrigger(Trigger newTrigger) {
        this.getNodeData().setTrigger(this.editTriggerIndex, newTrigger);
    }

    public void setEditTriggerIndex(int index) {
        this.editTriggerIndex = index;
    }

    public void updateTriggerChild(String oldName, String newName) {
        for (Trigger trigger: this.getNodeData().getTriggers()) {
            if (trigger.getNextBehaviorName().equals(oldName)) {
                trigger.setNextBehaviorName(newName);
            }
        }
    }
}
