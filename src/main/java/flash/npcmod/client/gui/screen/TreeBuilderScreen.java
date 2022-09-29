package flash.npcmod.client.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.core.node.BuilderNode;
import flash.npcmod.core.node.NodeData;
import flash.npcmod.client.gui.widget.DirectionalFrame;
import flash.npcmod.client.gui.widget.FunctionListWidget;
import flash.npcmod.client.gui.widget.TextWidget;
import flash.npcmod.core.client.behaviors.ClientBehaviorUtil;
import flash.npcmod.core.client.dialogues.ClientDialogueUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The abstract class for Dialogue and Behavior builder screens.
 *
 */
@OnlyIn(Dist.CLIENT)
abstract public class TreeBuilderScreen extends Screen {
    protected String fileName;
    protected double scrollX, scrollY;
    protected boolean isScrolling;
    public int maxX = 10000;
    public int maxY = 10000;

    protected BuilderNode initNode;
    public final List<BuilderNode> allNodes;
    public List<String> conflictingNodeDataNames;
    protected int selectedNodeIndex;
    protected FunctionListWidget<BuilderNode> functionListWidget;
    /**
     * The EditBoxes opened for editing.
     */
    protected EditBox functionParamsField;
    protected HashMap<EditType, EditBox> allNameFields;
    protected HashMap<EditType, DirectionalFrame> allTopLevelFrames;
    protected Button saveButton, confirmButton, cancelButton;
    /**
     * The main Vertical Frame that all gui elements are added to. This is initialized
     * with the `buttonFrame` already added.
     */
    protected DirectionalFrame mainVFrame;
    protected DirectionalFrame buttonFrame, fileNameFrame, nodeNameFrame;
    protected String newName = "", newFunctionParams = "";


    /**
     * The Edit Types that correspond to clicking on a part of a Node to edit.
     */
    public enum EditType {
        NONE,
        FILENAME,
        NAME,
        FILE,
        TEXT,
        RESPONSE,
        ACTION,
        FUNCTION,
        TRIGGER

    }

    protected final Predicate<String> nameFilter = (text) -> {
        Pattern pattern = Pattern.compile("\\s|/|\\|:|\\*|\\?|\\||<|>");
        Matcher matcher = pattern.matcher(text);
        return !matcher.find();
    };

    public TreeBuilderScreen(String name) {
        super(TextComponent.EMPTY);
        this.fileName = name;
        conflictingNodeDataNames = new ArrayList<>();

        // Initialize allNodes.
        this.allNodes = new ArrayList<>();
        loadFromJsonObject();
        if (!allNodes.isEmpty())
            updateNodePositionsFromJson();

        // Initialize allFields. Does not need to handle functionParamsField.
        this.allNameFields = new HashMap<>();
        this.allTopLevelFrames = new HashMap<>();
    }

    /**
     * Build the Editor JSON.
     *
     * @return The entries of nodes and positions.
     */
    public JsonObject buildEditorJSON() {
        JsonArray entries = new JsonArray();
        // Build an entry out of each node and put it in the array
        for (BuilderNode node : this.allNodes) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", node.getName());
            entry.addProperty("x", node.getX());
            entry.addProperty("y", node.getY());

            entries.add(entry);
        }

        JsonObject object = new JsonObject();
        object.add("entries", entries);

        return object;
    }

    /**
     * Build all node data into a JSON format.
     *
     * @return The entries of node data.
     */
    public JsonObject buildNodeDataJSON() {
        // Build our array of dialogue entries
        JsonArray entries = new JsonArray();
        for (BuilderNode node : this.allNodes) {
            if (node.isStart()) entries.add(node.getNodeData().toJSON());
        }

        // Build JSONObject out of the entries array.
        JsonObject object = new JsonObject();
        object.add("entries", entries);
        return object;
    }

    /**
     * Move screen to node.
     *
     * @param node The node to center on.
     */
    public void centerScreenOnNode(BuilderNode node) {
        this.scrollX = -Mth.clamp(
                node.getX() + node.getWidth() / 2 - this.width / 2, 0, this.maxX - this.width
        );
        this.scrollY = -Mth.clamp(
                node.getY() + node.getHeight() / 2 - this.height / 2, 0, this.maxY - this.height
        );
    }

    /**
     * Confirm Edits and push their changes to the NodeData.
     */
    protected void confirmEdits() {
        if (!this.newName.equals(ClientBehaviorUtil.INIT_BEHAVIOR_NAME) && !this.newName.isEmpty()) {
            assert this.getEditingNode() != null;
            if (!this.getEditingNode().getName().equals(ClientBehaviorUtil.INIT_BEHAVIOR_NAME))
                this.getEditingNode().getNodeData().setName(this.newName);
        }
        if (functionListWidget.isVisible()) {
            this.functionListWidget.setFunction();
        }
    }

    /**
     * Draw the background of the tree.
     *
     * @param matrixStack The PoseStack.
     * @param mouseX      The mouse x position.
     * @param mouseY      The mouse y position.
     */
    private void drawBackground(PoseStack matrixStack, int mouseX, int mouseY) {
        matrixStack.pushPose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, getBackground());

        int i = Mth.floor(this.scrollX);
        int j = Mth.floor(this.scrollY);
        int k = i % 16;
        int l = j % 16;

        int x = this.width / 16;
        int y = this.height / 16;

        for (int i1 = -1; i1 <= x; ++i1) {
            for (int j1 = -2; j1 <= y; ++j1) {
                blit(matrixStack, k + 16 * i1, l + 16 * j1, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        }

        if (getSelectedNode() != null) {
            drawLinesToMouse(matrixStack, mouseX, mouseY);
        }

        this.allNodes.forEach(node -> node.draw(matrixStack, this.scrollX, this.scrollY));
        if (isAnyTextFieldVisible())
            this.fillGradient(matrixStack, -20, -20, this.width, this.height, -1072689136, -804253680);
        matrixStack.popPose();
    }

    /**
     * Track the dragged amount.
     *
     * @param dragX The dragged position.
     * @param dragY The dragged position.
     */
    public void dragGui(double dragX, double dragY) {
        this.scrollX = Mth.clamp(this.scrollX + dragX, -(this.maxX - this.width), 0.0D);

        this.scrollY = Mth.clamp(this.scrollY + dragY, -(this.maxY - this.height), 0.0D);
    }

    /**
     * Draw lines to the mouse.
     *
     * @param matrixStack The PoseStack.
     * @param mouseX      The mouse x pos.
     * @param mouseY      The mouse y pos.
     */
    protected void drawLinesToMouse(PoseStack matrixStack, double mouseX, double mouseY) {
        if (getSelectedNode() != null) {
            int index = getSelectedNodeIndex();
            BuilderNode selectedNode = getSelectedNode();
            if (index >= -2 && index < selectedNode.getConnectionNames().length + 1) {
                int[] nodeXY = index == -2 ? selectedNode.getEndPointLocation() : selectedNode.getStartPointLocation(index);

                int nodeX = selectedNode.getX() + nodeXY[0];
                int nodeY = selectedNode.getY() + nodeXY[1];

                int color = 0xFFFFFF00;

                matrixStack.pushPose();
                {
                    matrixStack.translate(selectedNode.getX() + this.scrollX, selectedNode.getY() + this.scrollY, 0);
                    matrixStack.translate(nodeXY[0], nodeXY[1], 0);
                    matrixStack.translate(3.5, 3.5, 0);

                    int lineLength = (int) -(nodeX + this.scrollX - mouseX + 9);
                    int lineHeight = (int) -(nodeY + this.scrollY - mouseY + 18);

                    fill(matrixStack, 0, 0, lineLength / 2, 1, color);
                    fill(matrixStack, lineLength / 2, 0, lineLength / 2 + 1, lineHeight, color);
                    fill(matrixStack, lineLength / 2 + 1, lineHeight, lineLength, lineHeight + 1, color);
                }
                matrixStack.popPose();
            }
        }
    }

    /**
     * Move PoseStackMatrix before calling drawBackground. Ready RenderSystem.
     *
     * @param matrixStack the PoseStack.
     * @param mouseX      The mouse x coordinate.
     * @param mouseY      The mouse y coordinate.
     */
    private void drawWindowBackground(PoseStack matrixStack, int mouseX, int mouseY) {
        matrixStack.pushPose();
        matrixStack.translate(9, 18, 0.0F);
        drawBackground(matrixStack, mouseX, mouseY);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableDepthTest();
        matrixStack.popPose();
    }

    abstract protected ResourceLocation getBackground();

    abstract protected BuilderNode getEditingNode();

    abstract protected JsonArray getEntries();

    /**
     * Calculate the maximum number of widget rows that can fit in a height.
     * @param height The max height.
     * @return The max num of rows.
     */
    public static int getNumWidgetRows(int height) {
        // 20 = row height. 10 = row padding.
        return height / (20 + 10);
    }

    /**
     * Calculate the maximum number of widget columns that can fit in a width.
     * @param indexSize The index size.
     * @return The max num of rows.
     */
    public static int getNumWidgetCols(int width, int indexSize) {
        // 20 = row height. 10 = row padding.
        return width / (indexSize + 10);
    }

    public String getNewFunctionParams() {
        return this.newFunctionParams;
    }

    /**
     * The index of the selected connection of the selected node.
     *
     * @return The selected index,
     */
    public int getSelectedNodeIndex() {
        return this.selectedNodeIndex;
    }

    @Nullable
    abstract public BuilderNode getSelectedNode();

    /**
     * Function to calculate the height of each widget based off of the row number. The middle of the screen is row 0.
     * Rows going up the screen are positive.
     * @param rowNum The row number. 0-based.
     * @param screenHeight The height of the screen.
     * @param rowHeights Nullable field of row heights.
     * @return The height of the widget.
     */
    public static int getWidgetHeight(int rowNum, int screenHeight, @Nullable int[] rowHeights) {
        int defaultHeight = 20; // the default.
        int rowPadding = 5; // the vertical spacing between widgets.
        return get1DWidgetCoordinate(rowNum, screenHeight, defaultHeight, rowPadding, rowHeights);
    }

    /**
     * Function to calculate the width of each widget based off of the column number. The middle of the screen is row 0.
     * columns going left of the screen are positive.
     * @param colNum The row number. 0-based.
     * @param screenWidth The height of the screen.
     * @param colWidths Nullable field of row heights.
     * @return The height of the widget.
     */
    public static int getWidgetWidth(int colNum, int screenWidth, @Nullable int[] colWidths, int padding) {
        int defaultWidth = 120; // the default.
        int colPadding = 10;
        if (padding > 0)
            colPadding = padding; // the vertical spacing between widgets.
        return get1DWidgetCoordinate(colNum, screenWidth, defaultWidth, colPadding, colWidths);
    }

    /**
     * Function to calculate the position of an index in a certain space size given a default index size and padding.
     * If indexSizes is used, then it will be used to replace defaultIndexSize. Padding is assumed to be constant.
     * Index 0 is in the center of the space. Negative Indices result in smaller coordinates. Positive, larger.
     * @param indexNum The index of the position. Essentially the row/col number.
     * @param size The size of the space.
     * @param defaultIndexSize The default size taken by an index.
     * @param padding The size of the padding around an index.
     * @param indexSizes Int array of actual sizes used. Replaces defaultIndexSize. Contains the sizes starting from
     *                   index 0 in the desired direction.
     * @return Return the coordinate of the index.
     */
    public static int get1DWidgetCoordinate(int indexNum, int size, int defaultIndexSize, int padding, @Nullable int[] indexSizes) {
        int space = size / 2 - defaultIndexSize / 2;
        int direction = indexNum > 0 ? -1 : 1;
        indexNum = Mth.abs(indexNum);
        if (indexSizes != null) {
            // If indexSize of this object is known, use that.
            if (indexSizes.length > 0) space = size / 2 - indexSizes[0] / 2;
            int i = 1;
            for(; i <= indexNum && i < indexSizes.length; i++) {
                space += (indexSizes[i] + padding) * direction;
            }
            // In case the row heights provided is too small.
            for (; i < indexNum; i++) {
                space += (defaultIndexSize + padding) * direction;
            }
        }else {
            space += ((defaultIndexSize + padding) * indexNum) * direction;
        }
        return space;
    }

    @Override
    protected void init() {
        this.setEditingNode(null);

        // Initialize our button widgets
        this.saveButton = this.addRenderableWidget(new Button(width / 2 - 50, height - 25, 100, 20, new TextComponent("Save"), btn -> {
            if (this.fileName.isEmpty()) {
                this.setNodeBeingEdited(null, EditType.FILENAME);
                return;
            }
            if (this.getEditingNode() == null) {
                if (conflictingNodeDataNames.size() == 0) {
                    // This overwrites the npc's dialogue even if there is no npc attached.
                    sendCEdit();
                }
            }
        }));
        this.saveButton.active = conflictingNodeDataNames.size() == 0;
        this.confirmButton = this.addWidget(new Button(width / 2 - 60, height / 2 + 15, 50, 20, new TextComponent("Confirm"), btn -> {
            if (this.getEditingNode() != null) {
                confirmEdits();
                this.getEditingNode().calculateDimensions();
            }
            Main.LOGGER.info("Confirm hit");
            this.setNodeBeingEdited(null, EditType.NONE);
        }));
        this.cancelButton = this.addWidget(new Button(width / 2 + 10, height / 2 + 15, 50, 20, new TextComponent("Cancel"),
                btn -> this.setNodeBeingEdited(null, EditType.NONE)
        ));

        this.mainVFrame = this.addRenderableWidget(
                DirectionalFrame.createVerticalFrame(this.height, DirectionalFrame.Alignment.CENTERED)
        );

        this.buttonFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.CENTERED);
        this.buttonFrame.addWidget(this.confirmButton, 20);
        this.buttonFrame.addWidget(this.cancelButton, 20);
        this.buttonFrame.setVisible(false);
        this.mainVFrame.addSpacer();
        this.mainVFrame.addWidget(this.buttonFrame);

        // Initialize our text field widgets
        this.nodeNameFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.CENTERED);
        EditBox nameField = this.addWidget(new EditBox(this.font, this.width / 2 - 60, this.height / 2 - 10, 120, 20, TextComponent.EMPTY));
        nameField.setResponder(this::setNewName);
        nameField.setFilter(this.nameFilter);
        nameField.setMaxLength(50);
        nameField.setCanLoseFocus(true);
        allNameFields.put(EditType.NAME, nameField);
        this.nodeNameFrame.addWidget(new TextWidget("Name:"));
        this.nodeNameFrame.addWidget(nameField);
        this.nodeNameFrame.setVisible(false);
        this.allTopLevelFrames.put(EditType.NAME, this.nodeNameFrame);
        this.mainVFrame.insertWidget(nodeNameFrame, 0, 20);

        this.fileNameFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.CENTERED);
        EditBox fileNameField = this.addWidget(new EditBox(this.font, this.width / 2 - 60, this.height / 2 - 10, 120, 20, TextComponent.EMPTY));
        fileNameField.setResponder(this::setNewFileName);
        fileNameField.setFilter(this.nameFilter);
        fileNameField.setMaxLength(50);
        fileNameField.setCanLoseFocus(true);
        allNameFields.put(EditType.FILENAME, fileNameField);
        this.fileNameFrame.addWidget(new TextWidget("File Name:"));
        this.fileNameFrame.addWidget(fileNameField);
        this.fileNameFrame.setVisible(false);
        this.mainVFrame.insertWidget(fileNameFrame, 0, 20);
        this.allTopLevelFrames.put(EditType.FILENAME, this.fileNameFrame);
        // Initialize the function list widget.
        this.functionListWidget = new FunctionListWidget<>(this, Minecraft.getInstance());
        this.functionListWidget.calculatePositionAndDimensions();
        this.functionParamsField = this.addRenderableWidget(new EditBox(this.font, width / 2 - 60, height - 44, 120, 20, TextComponent.EMPTY));
        this.functionParamsField.setResponder(this::setNewFunctionParams);
        this.functionParamsField.setFilter(this.nameFilter);
        this.functionParamsField.setMaxLength(100);
        this.functionParamsField.setVisible(false);
        this.functionParamsField.setCanLoseFocus(true);
        this.functionListWidget.setVisible(false);
    }

    /**
     * Iterate through all fields and checks visibility.
     *
     * @return True if any fields are visible.
     */
    protected boolean isAnyTextFieldVisible() {
        if (this.mainVFrame.isAnyVisible()) {
            return true;
        }
        return functionListWidget.isVisible();
    }

    /**
     * Check if the mouse is over a node.
     *
     * @param mouseX The mouse x position.
     * @param mouseY The mouse y position.
     * @return True if over node.
     */
    protected boolean isMouseOverNode(double mouseX, double mouseY) {
        for (BuilderNode node : this.allNodes) {
            double minX = 9 + this.scrollX + node.getX() - 12;
            double maxX = minX + node.getWidth() + 12 * 2;
            double minY = 18 + 1 + (this.scrollY + node.getY()) - 9;
            double maxY = minY + node.getHeight() + 9;
            if (mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Load the Editor from a Json Object.
     */
    abstract protected void loadFromJsonObject();

    /**
     * Handle mouse click event.
     *
     * @param mouseX The mouse x position.
     * @param mouseY The mouse y position.
     * @param button The button pressed.
     * @return True if handled.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Mouse dragged event.
     *
     * @param mouseX The mouse x position.
     * @param mouseY The mouse y position.
     * @param button The mouse button.
     * @param dragX  The dragged position.
     * @param dragY  The dragged position.
     * @return True if handled.
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) {
            this.isScrolling = false;
            return false;
        } else {
            if (!this.isScrolling) {
                this.isScrolling = true;
            } else {
                boolean draggingNode = false;
                for (BuilderNode node : this.allNodes) {
                    if (node.isDragging()) {
                        draggingNode = true;
                        break;
                    }
                }
                if (!draggingNode) {
                    dragGui(dragX, dragY);
                } else {
                    this.allNodes.forEach(node -> {
                        if (node.isDragging()) {
                            node.setPosition((int) (-this.scrollX + mouseX), (int) (-this.scrollY + mouseY - 9));
                        }
                    });
                }
            }

            return true;
        }
    }

    /**
     * Finish dragging or center on init node.
     *
     * @param mouseX The mouse x coordinate.
     * @param mouseY The mouse x coordinate.
     * @param button The button released.
     * @return The mouse released boolean.
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.isAnyTextFieldVisible()) {
            if (button == 0 || button == 1) {
                this.allNodes.forEach(BuilderNode::stopDragging);
            } else if (button == 2) {
                centerScreenOnNode(this.initNode);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Mouse scrolled event.
     *
     * @param mouseX The mouse x position.
     * @param mouseY The mouse y position.
     * @param delta  The amount scrolled.
     * @return True if handled.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.functionListWidget.onScrolled(delta);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /**
     * Add the nodeData to `allNodes`.
     *
     * @param nodeData the nodeData.
     * @param parent   the parent of the nodeData.
     */
    abstract protected void populateNodeList(NodeData nodeData, @Nullable BuilderNode parent);

    /**
     * Function from screen.
     *
     * @param matrixStack  The Pose Stack.
     * @param mouseX       The mouse x coordinate.
     * @param mouseY       The mouse y coordinate.
     * @param partialTicks float time.
     */
    @Override
    public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.drawWindowBackground(matrixStack, mouseX, mouseY);
        this.functionListWidget.draw(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    /**
     * Create and Send the CEdit[NAME] packet to the server.
     */
    abstract protected void sendCEdit();

    /**
     * Set the editing node.
     * @param node The new node.
     */
    abstract protected void setEditingNode(BuilderNode node);

    /**
     * Set a new file name to save this dialogue/ behavior. Then save.
     * @param s The file name.
     */
    protected void setNewFileName(String s) {
        if (!s.isEmpty()) {
            this.fileName = s;
            if (conflictingNodeDataNames.size() == 0) {
                sendCEdit();
            }
        }
    }

    /**
     * Set `newName` to `s`. This is an intermediary step before setting the NodeData on pressing Confirm.
     *
     * @param s The new name.
     */
    protected void setNewName(String s) {
        if (!s.isEmpty() && !s.equals(ClientBehaviorUtil.INIT_BEHAVIOR_NAME)) {
            this.newName = s;
        }
    }

    /**
     * Set `newFunctionParams` to `s`. This is an intermediary step before setting the NodeData on pressing Confirm.
     *
     * @param s The new name.
     */
    protected void setNewFunctionParams(String s) {
        this.newFunctionParams = s;
    }

    /**
     * Set `node` as being edited and set the edit type.
     *
     * @param node     The node.
     * @param editType The edit type.
     */
    public void setNodeBeingEdited(@Nullable BuilderNode node, EditType editType) {
        boolean isNodeNull = node == null;
        if (!isNodeNull) {
            this.allNameFields.get(EditType.NAME).setValue(node.getName());

            int i = -1;
            if (!node.getFunction().isEmpty()) {
                String[] splitFunction = node.getFunction().split("::");
                String currentFunctionName = splitFunction[0];
                for (int j = 0; j < ClientDialogueUtil.FUNCTION_NAMES.size(); j++) {
                    String function = ClientDialogueUtil.FUNCTION_NAMES.get(j);
                    if (function.startsWith(currentFunctionName)) {
                        i = j;
                        break;
                    }

                }
                this.functionListWidget.clickedOnFunction(i);
                if (splitFunction.length == 2)
                    this.functionParamsField.setValue(splitFunction[1]);
            }
        } else {
            this.functionParamsField.setValue("");
        }
        boolean check;
        this.fileNameFrame.setVisible(editType == EditType.FILENAME);
        this.nodeNameFrame.setVisible(editType == EditType.NAME);

        for (EditType key : this.allNameFields.keySet()) {
            check = editType == key;
            if (allNameFields.containsKey(key))
                this.allNameFields.get(key).setFocus(check);
            if (allTopLevelFrames.containsKey(key))
                this.allTopLevelFrames.get(key).setVisible(check);
            if (isNodeNull) this.allNameFields.get(key).setValue("");
        }

        this.functionListWidget.calculatePositionAndDimensions();
        this.functionListWidget.setEditingNode(node);
        this.functionListWidget.setVisible(editType == EditType.FUNCTION);

        this.buttonFrame.setVisible(editType != EditType.NONE);
        if (editType == EditType.FUNCTION) {
            this.confirmButton.x = this.functionParamsField.x;
            this.confirmButton.y = this.functionParamsField.y + 22;
            this.cancelButton.x = this.functionParamsField.x + this.functionParamsField.getWidth() - confirmButton.getWidth();
            this. cancelButton.y = this.functionParamsField.y + 22;
        }
        this.mainVFrame.recalculateSize();
        this.setEditingNode(node);
    }

    /**
     * Set the selected node and the selected option index of it.
     *
     * @param node              The node to select.
     * @param selectedNodeIndex The index of it.
     */
    abstract public void setSelectedNode(@Nullable BuilderNode node, int selectedNodeIndex);

    /**
     * Called every `tick`. Updates the screen and all fields. Clears the queued for removal nodes, and checks for
     * conflicting names.
     */
    @Override
    public void tick() {
        // Tick Text Field Widgets
        for (EditBox field : allNameFields.values()) field.tick();
        this.functionParamsField.tick();

        // Button visibility
        boolean isAnyTextFieldVisible = this.isAnyTextFieldVisible();
        this.saveButton.visible = !isAnyTextFieldVisible;
        this.buttonFrame.setVisible(isAnyTextFieldVisible);

        // Save button should only be active if we have no conflicting dialogue names
        this.saveButton.active = conflictingNodeDataNames.size() == 0;

        // Remove all nodes queued for removal
        List<BuilderNode> queuedForRemoval = new ArrayList<>();
        for (BuilderNode node : allNodes) {
            if (node.isQueuedForRemoval()) {
                queuedForRemoval.add(node);
            }
        }
        for (BuilderNode node : queuedForRemoval) {
            if (node.getNodeData().getChildren().length > 0) {
                List<BuilderNode> queuedForParentRemoval = new ArrayList<>();
                allNodes.forEach(node2 -> {
                    for (int i = 0; i < node.getNodeData().getChildren().length; i++) {
                        if (node.getNodeData().getChildren()[i].equals(node2.getNodeData())) {
                            queuedForParentRemoval.add(node2);
                        }
                    }
                });
                queuedForParentRemoval.forEach(behaviorNode -> behaviorNode.setParent(null));
            }
            node.remove();
        }

        // See which dialogues have conflicting names and store them in a list
        this.conflictingNodeDataNames.clear();
        for (BuilderNode base : this.allNodes) {
            String name = base.getName();
            for (BuilderNode other : this.allNodes) {
                if (!other.equals(base)) {
                    if (other.getName().equals(name)) {
                        if (!this.conflictingNodeDataNames.contains(name)) {
                            this.conflictingNodeDataNames.add(name);
                        }
                        break;
                    }
                }
            }
        }

        for (BuilderNode node : this.allNodes) {
            node.hasConflictingName = this.conflictingNodeDataNames.contains(node.getName());
        }

        this.functionParamsField.visible = (
                this.functionListWidget.isVisible() && this.functionListWidget.getSelectedFunction().contains("::")
        );
    }

    /**
     * Update the saved node positions from json file.
     */
    private void updateNodePositionsFromJson() {
        JsonArray entries = getEntries();
        this.allNodes.forEach(node -> {
            for (int i = 0; i < entries.size(); i++) {
                JsonObject entry = entries.get(i).getAsJsonObject();
                if (entry.has("name") && entry.has("x") && entry.has("y")) {
                    if (node.getNodeData().getName().equals(entry.get("name").getAsString())) {
                        node.setPosition(entry.get("x").getAsInt(), entry.get("y").getAsInt());
                    }
                }
            }
        });
    }
}


