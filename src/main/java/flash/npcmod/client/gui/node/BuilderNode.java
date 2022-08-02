package flash.npcmod.client.gui.node;

import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.client.gui.screen.TreeBuilderScreen;
import flash.npcmod.core.client.dialogues.ClientDialogueUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

import static net.minecraft.client.gui.GuiComponent.fill;

/**
 * Super Class of BehaviorNode and DialogueNode.
 * The node that is visible on a builder screen. This class is in charge of drawing itself and tracking
 * its own gui elements.
 */
@OnlyIn(Dist.CLIENT)
abstract public class BuilderNode {
    protected static final ResourceLocation TEXTURES = new ResourceLocation(Main.MODID, "textures/gui/edit_dialogue/textures.png");
    protected final TreeBuilderScreen builderScreen;
    protected final Minecraft minecraft;

    protected boolean dragging;
    protected boolean isQueuedForRemoval;

    protected int x, y, width;
    protected int nameBarHeight, functionBarHeight;
    protected int[] connectionBarsHeights, extraFieldsHeight;
    protected int actualHeight;

    protected static final int maxWidth = 120;
    protected static final int defaultTextHeight = 2+Minecraft.getInstance().font.lineHeight;

    public boolean hasConflictingName;

    @Nullable
    public BuilderNode parent;

    public BuilderNode(TreeBuilderScreen screen, Minecraft minecraft, int x, int y, int width) {
        this.builderScreen = screen;
        this.minecraft = minecraft;

        this.setPosition(x, y);
        this.setWidth(width);

        this.nameBarHeight = defaultTextHeight;
    }

    /**
     * Add a child to this node and create a new trigger for it.
     * @param nodeData The child data.
     */
    public void addChild(NodeData nodeData) {
        if (!this.getNodeData().isChild(nodeData)) {
            this.getNodeData().addChild(nodeData);
            calculateDimensions();
        }
    }

    /**
     * Add a child to the node data. The index
     * @param nodeData The child data.
     * @param index The index of the associated trigger.
     */
    public void addChild(NodeData nodeData, int index) {
        if (!this.getNodeData().isChild(nodeData)) {
            this.getNodeData().addChild(nodeData, index);
            calculateDimensions();
        }
    }

    /**
     * Adjust the width of this node to fit this text.
     * @param text The text to adjust width to.
     */
    protected void adjustWidth(String text) {
        int textWidth = minecraft.font.width(text);
        if (textWidth > width && textWidth < maxWidth) {
            setWidth(textWidth);
        } else {
            setWidth(maxWidth);
        }
    }

    /**
     * Calculate the width and height of the background color.
     */
    public void calculateDimensions() {
        String function = getFunction();
        adjustWidth(function);

        this.extraFieldsHeight = new int[this.getNumExtraFields()];

        // Setting the function bar height
        List<FormattedCharSequence> trimmedFunction = minecraft.font.split(new TextComponent(function), this.width-4);
        int numOfFunctionLines = trimmedFunction.size() > 0 ? trimmedFunction.size() : 1;
        this.functionBarHeight = defaultTextHeight * numOfFunctionLines;

        // Setting options bar height
        String[] connectionNames = getConnectionNames();
        this.connectionBarsHeights = new int[connectionNames.length+1];
        this.actualHeight = 0;
        for (int i = 0; i < connectionNames.length; i++) {
            List<FormattedCharSequence> trimmedOption = minecraft.font.split(new TextComponent(connectionNames[i]), this.width-4);
            connectionBarsHeights[i] = defaultTextHeight * Math.max(trimmedOption.size(), 1);
            actualHeight += connectionBarsHeights[i] + 1;
        }
        connectionBarsHeights[getConnectionNames().length] = defaultTextHeight;
        this.actualHeight += 1+nameBarHeight+1+functionBarHeight+1+defaultTextHeight+1;
    }

    /**
     * Check if in the process of connecting nodes.
     * @return True if connecting nodes.
     */
    public boolean checkIfConnectingNodes() {
        BuilderNode selectedNode = this.builderScreen.getSelectedNode();
        if (selectedNode != null) {
            int index = this.builderScreen.getSelectedNodeIndex();
            if (index >= 0 || index == -1) {
                // If the index is -1, then set the selected node as the parent of the original node.
                if (index == -1) {
                    this.builderScreen.setSelectedNode(selectedNode, selectedNode.getConnectionNames().length);
                }
                this.setParent(selectedNode);
            }
            else if (index == -2){
                if (selectedNode.isInit())
                    return false;
                // if the index is -2,
                // we need to set the selected node's parent to this.
                this.builderScreen.setSelectedNode(selectedNode, selectedNode.getConnectionNames().length);
                selectedNode.setParent(this);
            }
            builderScreen.setSelectedNode(null, 0);
            return true;
        }
        return false;
    }

    abstract public void clickedOn(double mouseX, double mouseY, int button, int offsetX, int offsetY, double scrollX, double scrollY);


    /**
     * Edit Name or remove node if right-clicked.
     * @param button The mouse button.
     */
    public void clickedOnNameBar(int button) {
        if (!isInit()) {
            if (button == 1) {
                // If we are not an init dialogue option, when right-clicked on, queue for removal
                this.isQueuedForRemoval = true;
                return;
            }
        }
        if (button == 0) {
            // If this isn't an init dialogue node, then we can make our required widgets visible to rename this
            if (!isInit()) {
                builderScreen.setNodeBeingEdited(this, TreeBuilderScreen.EditType.NAME);
            }
        }
    }

    public void clickedOnFunctionBar() {
        builderScreen.setNodeBeingEdited(this, TreeBuilderScreen.EditType.FUNCTION);
    }

    /**
     * Center on the Option bar or remove the connection if right-clicked.
     * @param optionIndex The index of the option.
     * @param button The mouse button.
     */
    public void clickedOnOptionBar(int optionIndex, int button) {
        if (optionIndex >= 0 && optionIndex < this.getNodeData().getChildren().length) {
            BuilderNode child = findChild(optionIndex);
            if (child != null) {
                if (button == 0) {
                    // If left clicked, then center the screen on this node
                    builderScreen.centerScreenOnNode(child);
                } else if (button == 1) {
                    // Otherwise set the child's parent to null
                    child.setParent(null);
                }
            }
        }
    }

    /**
     * When clicked on a select icon (the icon in front of the text or the icons next to the children options),
     * select this node in the dialogue builder screen,
     */
    public void clickedOnSelectIcon(int index) {
        if (builderScreen.allNodes.contains(this)) {
            if (index == -2) {
                this.builderScreen.setSelectedNode(this, index);
                this.setParent(null);
            }
            else if (index == -1) {
                this.builderScreen.setSelectedNode(this, getConnectionNames().length);
            }
            else {
                for (BuilderNode builderNode : builderScreen.allNodes) {
                    if (builderNode.getName().equals(this.getConnectionNames()[index - 1])) {
                        if (builderNode.parent != null && builderNode.parent.equals(this)) {
                            builderNode.setParent(null);
                            this.builderScreen.setSelectedNode(this, getConnectionNames().length);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void draw(PoseStack matrixStack, double scrollX, double scrollY) {
        matrixStack.pushPose();
        matrixStack.translate(scrollX, scrollY, 0);
        matrixStack.translate(x, y, 0);

        // We need to render these regardless of this being visible or not, otherwise we couldn't see the lines and the icons on the parent
        drawLinesToParent(matrixStack);
        drawIcons(matrixStack);

        if (isVisible(scrollX, scrollY)) {

            drawRectangles(matrixStack);

            // Draw Name
            minecraft.font.draw(matrixStack, getName(), 3, 3, 0x000000);
            int minY = 2 + nameBarHeight;

            minY = drawExtraTexts(matrixStack, minY);

            // Draw Function
            String function = getFunction();
            drawMultilineText(matrixStack, function.isEmpty() ? "Function" : function, minY, function.isEmpty());
            minY += functionBarHeight;

            drawOptionsText(matrixStack, minY);

        }

        matrixStack.popPose();
    }

    /**
     * Draw the extra rectangles for a specific subclass.
     * @param matrixStack The PoseStack.
     * @param minY The minimal y point.
     * @return The new minY.
     */
    abstract protected int drawExtraRectangles(PoseStack matrixStack, int minY);

    /**
     * Draw the extra text lines for a subclass.
     * @param matrixStack The PoseStack.
     * @param minY The minimal y point.
     * @return The new minY.
     */
    abstract protected int drawExtraTexts(PoseStack matrixStack, int minY);

    /**
     * Draw the end and start point icons.
     * @param matrixStack PoseStack
     */
    abstract protected void drawIcons(PoseStack matrixStack);

    /**
     * Draw the lines to the parent.
     * @param matrixStack PoseStack.
     */
    abstract protected void drawLinesToParent(PoseStack matrixStack);

    /**
     * Draw multiple lines of text.
     * @param matrixStack The PoseStack.
     * @param text The text.
     * @param y The y position.
     * @param isEmpty If the text is empty.
     */
    protected void drawMultilineText(PoseStack matrixStack, String text, int y, boolean isEmpty) {
        List<FormattedCharSequence> lines = minecraft.font.split(new TextComponent(text), width-4);
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence processor = lines.get(i);
            minecraft.font.draw(matrixStack, processor, 3, y+2+(defaultTextHeight*i), isEmpty ? 0xFFBBBBBB : 0x000000);
        }
    }

    /**
     * Draws the options text.
     * @param matrixStack The matrix stack.
     * @param minY The height to start drawing from.
     */
    protected void drawOptionsText(PoseStack matrixStack, int minY) {
        for (int i = 0; i < getConnectionNames().length; i++) {
            minY += (i > 0 ? connectionBarsHeights[i - 1] : 0) + 1;

            drawMultilineText(matrixStack, getConnectionNames()[i], minY, false);
        }
        if (getConnectionNames().length > 0) {
            minY += connectionBarsHeights[connectionBarsHeights.length - 1] + 1;
        } else {
            minY += 1;
        }

        drawMultilineText(matrixStack, "New Option", minY, true);
    }

    /**
     * Draw the rectangles.
     * @param matrixStack The PoseStack.
     */
    protected void drawRectangles(PoseStack matrixStack) {
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

        // Bar for behavior name
        int minY = 1+nameBarHeight;
        fill(matrixStack, 1, 1, width-1, minY, nameBar);
        fill(matrixStack, 1, minY, width-1, minY+1, black);

        minY = drawExtraRectangles(matrixStack, minY);

        // Bar for function
        minY += 1+functionBarHeight;
        fill(matrixStack, 1, minY, width-1, minY+1, black);

        // Options bars
        for (int i = 0; i < getConnectionNames().length; i++) {
            minY += 1+ connectionBarsHeights[i];
            fill(matrixStack, 1, minY,
                    width-1, minY+1, black);
        }
    }

    /**
     * Draw a single line of text.
     * @param matrixStack The PoseStack.
     * @param text The text.
     * @param y The y position.
     * @param isEmpty If the text is empty.
     */
    protected void drawSingleLineText(PoseStack matrixStack, String text, int y, boolean isEmpty) {
        minecraft.font.draw(matrixStack, text, 3, y+2, isEmpty ? 0xFFBBBBBB : 0x000000);
    }

    @Override
    abstract public boolean equals(Object o);

    @Nullable
    protected BuilderNode findChild(int index){
        NodeData child = this.getNodeData().getChildren()[index];
        for (BuilderNode node : builderScreen.allNodes) {
            if (node.getNodeData().equals(child)) {
                return node;
            }
        }
        return null;
    }

    public int[] getConnectionBarsHeights() {
        return this.connectionBarsHeights;
    }

    /**
     * The connection bars are the last bars in a node. Each one represents the connection name to another
     * node. In Dialogues, the name of the connection matches the name of the child dialogue. In Behaviors,
     * the name of the connection is entirely separate, as not each connection requires a behavior.
     * @return The string array of connection names.
     */
    public String[] getConnectionNames() {
        String[] options = new String[this.getNodeData().getChildren().length];
        for (int i = 0; i < options.length; i++) {
            options[i] = this.getNodeData().getChildren()[i].getName();
        }
        return options;
    }

    /**
     * Calculate the point for this Node's end point icon.
     * @return the end point of parent lines to the right of the name.
     */
    public int[] getEndPointLocation() {
        int x = -12;
        int y = 2+nameBarHeight;
        if (extraFieldsHeight.length > 0) {
            y +=(extraFieldsHeight[0]/2)-4;
        }
        return new int[]{x, y};
    }

    public String getFunction() {
        return this.getNodeData().getFunction();
    }

    public int getHeight() {
        return actualHeight;
    }

    public String getName() {
        return this.getNodeData().getName();
    }

    abstract public NodeData getNodeData();

    abstract public int getNumExtraFields();

    /**
     * Calculate the point for this Node's nth start point icon.
     * @param index the index of the connection.
     * @return the starting point to the right of the connection index.
     */
    public int[] getStartPointLocation(int index) {
        int x = width+4;
        int minY = 2+nameBarHeight+1+functionBarHeight;
        for (int height : extraFieldsHeight) {
            minY += height + 1;
        }
        for (int i = 0; i < index; i++) {
            minY += 1+ connectionBarsHeights[i];
        }
        int y = minY+1+(connectionBarsHeights[index]/2)-4;
        return new int[]{x, y};
    }

    public int getWidth() {
        return width;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean isQueuedForRemoval() {
        return isQueuedForRemoval;
    }

    /**
     * Check if this node is a top level node.
     * @return True if no parent.
     */
    public boolean isStart() {
        return parent == null;
    }

    /**
     * Check if this node is the init node.
     * @return True if init node.
     */
    public boolean isInit() { return getName().equals(ClientDialogueUtil.INIT_DIALOGUE_NAME); }

    /**
     * Check if this node is visible.
     * @param scrollX The scroll x position.
     * @param scrollY The scroll y position.
     * @return True if visible.
     */
    public boolean isVisible(double scrollX, double scrollY) {
        return -scrollX <= x+9+width+12 && -scrollY <= y+18+actualHeight && -scrollX + minecraft.getWindow().getGuiScaledWidth() >= x+9-12 && -scrollY + minecraft.getWindow().getGuiScaledHeight() >= y+18;
    }

    /**
     * Remove this node from the screen.
     */
    public void remove() {
        if (builderScreen.allNodes.contains(this)) {
            setParent(null);
            builderScreen.allNodes.remove(this);
        }
    }

    /**
     * Remove child from this node.
     * @param childData The child data.
     */
    public void removeChild(NodeData childData) {
        if (this.getNodeData().isChild(childData)) {
            this.getNodeData().removeChild(childData);
            calculateDimensions();
        }
    }

    /**
     * Set the position of the node.
     * @param x The x position.
     * @param y The y position.
     */
    public void setPosition(int x, int y) {
        this.x = Mth.clamp(x, 0, builderScreen.maxX);
        this.y = Mth.clamp(y, 0, builderScreen.maxY);
    }

    private void setWidth(int width) {
        this.width = Math.min(Math.max(50, width), maxWidth);
    }

    abstract public void setParent(@Nullable BuilderNode node);

    public void stopDragging() {
        dragging = false;
    }

    @Override
    public String toString() {
        return "BuilderNode{" +
                "nodeData=" + this.getNodeData() +
                '}';
    }
}
