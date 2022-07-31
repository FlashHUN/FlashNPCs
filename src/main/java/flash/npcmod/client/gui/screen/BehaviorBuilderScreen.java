package flash.npcmod.client.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.client.gui.behavior.Action;
import flash.npcmod.client.gui.behavior.Behavior;
import flash.npcmod.client.gui.behavior.BehaviorNode;
import flash.npcmod.client.gui.behavior.Trigger;
import flash.npcmod.client.gui.node.BuilderNode;
import flash.npcmod.client.gui.node.NodeData;
import flash.npcmod.client.gui.widget.DropdownWidget;
import flash.npcmod.client.gui.widget.FunctionListWidget;
import flash.npcmod.core.client.behaviors.ClientBehaviorUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CEditBehavior;
import flash.npcmod.network.packets.client.CEditNpc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class BehaviorBuilderScreen extends TreeBuilderScreen {
    protected String newDialogueName = "", newActionName = "", newTriggerName = "", newTriggerChild = "";
    /**
     * 0-2 Block Pos, 3 Radius, 4 Timer
     */
    protected int[] intArgs;
    private final NpcEntity npcEntity;
    private DropdownWidget<CEditNpc.NPCPose> poseDropdownWidget;
    private DropdownWidget<Action.ActionType> actionTypeDropdownWidget;
    private DropdownWidget<Trigger.TriggerType> triggerTypeDropdownWidget;
    private EditBox triggerChildField, triggerTimerField;
    private final List<EditBox> actionFields;
    @Nullable
    protected BehaviorNode editingNode, selectedNode;

    protected final Predicate<String> numberFilter = (text) -> {
        Pattern pattern = Pattern.compile("-?\\d*");
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    };

    public BehaviorBuilderScreen(String name, NpcEntity npcEntity) {
        super(name);
        this.npcEntity = npcEntity;
        actionFields = new ArrayList<>();
        this.intArgs = new int[5];
        Arrays.fill(this.intArgs, 0);
    }

    /**
     * Apply all waiting changes.
     */
    @Override
    protected void confirmEdits() {
        assert editingNode != null;
        if (!(this.newName.equals(this.editingNode.getName())) && this.editingNode.parent != null)
            ((BehaviorNode) this.editingNode.parent).updateTriggerChild(this.editingNode.getName(), this.newName);
        super.confirmEdits();

        this.editingNode.getNodeData().setDialogueName(this.newDialogueName);
        if (this.editingNode.isEditingTrigger())
            this.editingNode.setEditTrigger(
                    new Trigger(
                            this.newTriggerName,
                            this.triggerTypeDropdownWidget.getSelectedOption(),
                            this.intArgs[4],
                            this.newTriggerChild));

        BlockPos blockPos = new BlockPos(this.intArgs[0], this.intArgs[1], this.intArgs[2]);
        Action action = new Action(
                newActionName, poseDropdownWidget.getSelectedOption(), actionTypeDropdownWidget.getSelectedOption(),
                blockPos, intArgs[3]);
        this.editingNode.getNodeData().setAction(action);
        this.editingNode.setEditTriggerIndex(-1);
    }

    /**
     * Get the background to draw.
     * @return The background.
     */
    @Override
    protected ResourceLocation getBackground() {
        return new ResourceLocation(Main.MODID, "textures/gui/edit_behavior/background.png");
    }


    /**
     * Get the editing node.
     * @return The editing node.
     */
    @Override
    public @Nullable BehaviorNode getEditingNode() {
        return this.editingNode;
    }

    /**
     * Get the `entries` value from the current editor JSON.
     * @return The JsonArray of entries (position of nodes).
     */
    @Override
    protected JsonArray getEntries() {
        assert ClientBehaviorUtil.currentBehaviorEditor != null;
        return ClientBehaviorUtil.currentBehaviorEditor.getAsJsonArray("entries");
    }

    /**
     * Get the selected Behavior node.
     * @return The selected node.
     */
    @Override
    public @Nullable BehaviorNode getSelectedNode() {
        return this.selectedNode;
    }

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
     * Function from Screen. Called before drawing the screen.
     */
    @Override
    protected void init() {
        super.init();
        int numRows = getNumWidgetRows(this.height);
        //initialize function widget
        this.functionListWidget = new FunctionListWidget<>(this, Minecraft.getInstance());
        this.functionListWidget.calculatePositionAndDimensions();

        // Initialize our text field widgets
        EditBox dialogueField = this.addRenderableWidget(
                new EditBox(
                        this.font,
                        getWidgetWidth(0, this.width, null, 0),
                        getWidgetHeight(0, this.height, null),
                        120,
                        20,
                        TextComponent.EMPTY
                )
        );
        dialogueField.setResponder(this::setNewDialogueName);
        dialogueField.setFilter(this.nameFilter);
        dialogueField.setMaxLength(50);
        dialogueField.setVisible(false);
        dialogueField.setCanLoseFocus(true);
        allFields.put(EditType.FILE, dialogueField);

        // Set up the trigger fields.
        EditBox triggerField = this.addRenderableWidget(
                new EditBox(
                        this.font,
                        getWidgetWidth(0, this.width, null, 0),
                        getWidgetHeight(2, this.height, null),
                        120,
                        20,
                        TextComponent.EMPTY
                )
        );
        triggerField.setResponder(this::setNewTriggerName);
        triggerField.setMaxLength(50);
        triggerField.setVisible(false);
        triggerField.setCanLoseFocus(true);
        allFields.put(EditType.TRIGGER, triggerField);

        this.triggerTypeDropdownWidget = this.addRenderableWidget(
                new DropdownWidget<>(
                        Trigger.TriggerType.DIALOGUE_TRIGGER,
                        getWidgetWidth(0, this.width, null, 20),
                        getWidgetHeight((numRows / 2)-1, this.height, null),
                        120
                )
        );
        this.triggerTypeDropdownWidget.visible = false;

        this.triggerTimerField =  this.addRenderableWidget(
                new EditBox(
                        this.font,
                        getWidgetWidth(0, this.width, null, 0),
                        getWidgetHeight(1, this.height, null),
                        120,
                        20,
                        TextComponent.EMPTY
                )
        );
        this.triggerTimerField.setFilter(numberFilter);
        this.triggerTimerField.setResponder((String s) -> {
            if (s.isEmpty() || s.equals("-")) s = "0";
            this.intArgs[4] = Integer.parseInt(s);
        });
        this.triggerTimerField.setMaxLength(10);
        this.triggerTimerField.setVisible(false);
        this.triggerTimerField.setCanLoseFocus(true);

        this.triggerChildField = this.addRenderableWidget(
                new EditBox(
                        this.font,
                        getWidgetWidth(0, this.width, null, 0),
                        getWidgetHeight(0, this.height, null),
                        120,
                        20,
                        TextComponent.EMPTY
                )
        );
        this.triggerChildField.setResponder(this::setNewTriggerChild);
        this.triggerChildField.setMaxLength(50);
        this.triggerChildField.setVisible(false);
        this.triggerChildField.setCanLoseFocus(true);

        // Set up the action fields.
        EditBox actionField = this.addRenderableWidget(
                new EditBox(
                        this.font,
                        getWidgetWidth(0, this.width, null, 0),
                        getWidgetHeight(0, this.height, null),
                        120,
                        20,
                        TextComponent.EMPTY
                )
        );
        actionField.setResponder(this::setNewActionName);
        actionField.setMaxLength(50);
        actionField.setVisible(false);
        actionField.setCanLoseFocus(true);
        allFields.put(EditType.ACTION, actionField);

        //  Set up the dropdown widgets
        int[] colWidths = new int[]{80, 100};
        int indexHeight = (numRows / 2);
        this.poseDropdownWidget = this.addRenderableWidget(
                new DropdownWidget<>(
                        CEditNpc.NPCPose.STANDING,
                        getWidgetWidth(-1, this.width, colWidths, 20),
                        getWidgetHeight(indexHeight, this.height, null),
                        80
                )
        );
        this.poseDropdownWidget.visible = false;

        this.actionTypeDropdownWidget = this.addRenderableWidget(
                new DropdownWidget<>(
                        Action.ActionType.STANDSTILL,
                        getWidgetWidth(0, this.width, colWidths, 20),
                        getWidgetHeight(indexHeight, this.height, null),
                        100
                )
        );
        this.actionTypeDropdownWidget.visible = false;

        // Set up the block pos fields.
        int numCols = getNumWidgetCols(this.width, 30);
        colWidths = new int[numCols];
        Arrays.fill(colWidths, 32);
        int indexWidth = (numCols / 2) - 1;
        colWidths[indexWidth + 1] = 22; //Reserve space for the block pos text.
        indexHeight = (numRows / 2) - 1;
        EditBox targetBlockXField = this.addRenderableWidget(
                new EditBox(
                        this.font,
                        getWidgetWidth(indexWidth--, this.width, colWidths, 5),
                        getWidgetHeight(indexHeight, this.height, null),
                        32,
                        20,
                        TextComponent.EMPTY
                )
        );
        targetBlockXField.setFilter(numberFilter);
        targetBlockXField.setResponder((String s) -> {
            if (s.isEmpty() || s.equals("-")) s = "0";
            this.intArgs[0] = Integer.parseInt(s);
        });
        targetBlockXField.setMaxLength(10);
        targetBlockXField.setVisible(false);
        targetBlockXField.setCanLoseFocus(true);
        this.actionFields.add(targetBlockXField);

        EditBox targetBlockYField = this.addRenderableWidget(
                new EditBox(
                        this.font,
                        getWidgetWidth(indexWidth--, this.width, colWidths, 5),
                        getWidgetHeight(indexHeight, this.height, null),
                        32,
                        20,
                        TextComponent.EMPTY
                )
        );
        targetBlockYField.setFilter(numberFilter);
        targetBlockYField.setResponder((String s) -> {
            if (s.isEmpty() || s.equals("-")) s = "0";
            this.intArgs[1] = Integer.parseInt(s);
        });
        targetBlockYField.setMaxLength(10);
        targetBlockYField.setVisible(false);
        targetBlockYField.setCanLoseFocus(true);
        this.actionFields.add(targetBlockYField);

        EditBox targetBlockZField = this.addRenderableWidget(
                new EditBox(
                        this.font,
                        getWidgetWidth(indexWidth, this.width, colWidths, 5),
                        getWidgetHeight(indexHeight, this.height, null),
                        32,
                        20,
                        TextComponent.EMPTY
                )
        );
        targetBlockZField.setFilter(numberFilter);
        targetBlockZField.setResponder((String s) -> {
            if (s.isEmpty() || s.equals("-")) s = "0";
            this.intArgs[2] = Integer.parseInt(s);
        });
        targetBlockZField.setMaxLength(10);
        targetBlockZField.setVisible(false);
        targetBlockZField.setCanLoseFocus(true);
        this.actionFields.add(targetBlockZField);

        // Set up the radius field.

        indexWidth = (numCols / 2) - 1;
        EditBox radiusField = this.addRenderableWidget(
                new EditBox(
                        this.font,
                        getWidgetWidth(indexWidth, this.width, colWidths, 5),
                        getWidgetHeight(indexHeight-1, this.height, null),
                        30,
                        20,
                        TextComponent.EMPTY
                )
        );
        radiusField.setFilter(numberFilter);
        radiusField.setResponder((String s) -> {
            if (s.isEmpty() || s.equals("-")) s = "0";
            this.intArgs[3] = Integer.parseInt(s);
        });
        radiusField.setMaxLength(10);
        radiusField.setVisible(false);
        radiusField.setCanLoseFocus(true);
        this.actionFields.add(radiusField);
    }

    /**
     * Load the Editor from a Json Object.
     */
    @Override
    protected void loadFromJsonObject() {
        //load from fileName.
        ClientBehaviorUtil.loadBehavior(fileName);
        ClientBehaviorUtil.loadBehaviorEditor(fileName);
        Behavior[] behaviors;
        if (ClientBehaviorUtil.currentBehavior != null)
            behaviors = Behavior.multipleFromJSONObject(ClientBehaviorUtil.currentBehavior);
        else
            behaviors = new Behavior[0];

        //Create nodes in `allNodes`.
        this.conflictingNodeDataNames = new ArrayList<>();
        for (Behavior behavior : behaviors) {
            populateNodeList(behavior, null);
        }
    }

    /**
     * Handle mouse click event.
     * @param mouseX The mouse x position.
     * @param mouseY The mouse y position.
     * @param button The button pressed.
     * @return True if handled.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isAnyTextFieldVisible()) {
            if (button == 0 || button == 1) {
                boolean isMouseOverAnyNode = isMouseOverNode(mouseX, mouseY);
                if (!isMouseOverAnyNode) {
                    if (button == 0 && this.selectedNode != null) {
                        this.selectedNode.calculateDimensions();
                        this.selectedNode = null;
                    }
                    if (button == 1) {
                        BehaviorNode newNode;
                        String name = "newBehaviorNode" + this.allNodes.size();
                        Set<String> allNames = allNodes.stream()
                                .map(BuilderNode::getName)
                                .collect(Collectors.toSet());
                        int count = this.allNodes.size() + 1;
                        while (allNames.contains(name)) {
                            name = "newBehaviorNode" + count++;
                        }
                        if (getSelectedNode() != null && getSelectedNodeIndex() == 0) {
                            newNode = new BehaviorNode(null, this, this.minecraft, Behavior.newBehavior());
                            newNode.addChild(getSelectedNode().getNodeData());
                            getSelectedNode().setParent(newNode);
                        } else {
                            newNode = new BehaviorNode(this.selectedNode, this, this.minecraft, Behavior.newBehavior());
                        }
                        newNode.setPosition((int) (-this.scrollX + mouseX - 9), (int) (-this.scrollY + mouseY - 18));
                        if (getSelectedNode() == null || !getSelectedNode().getName().equals(newNode.getName())) {
                            this.allNodes.add(newNode);
                        }
                        if (this.getSelectedNode() != null) {
                            this.selectedNode = null;
                        }
                    }
                }
                this.allNodes.forEach(node -> node.clickedOn(mouseX, mouseY, button, 0, 0, scrollX, scrollY));
            }
        } else if (this.functionListWidget.isVisible()) {
            this.functionListWidget.clickedOn(mouseX, mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Populate the node list `allNodes`, using the NodeData `behavior`.
     *
     * @param behavior The node data.
     * @param parent   The parent node.
     */
    @Override
    protected void populateNodeList(NodeData behavior, @Nullable BuilderNode parent) {
        BehaviorNode node = new BehaviorNode((BehaviorNode) parent, this, Minecraft.getInstance(), (Behavior) behavior);
        if (this.initNode == null && behavior.isInitData()) {
            this.initNode = node;
        }
        this.allNodes.add(node);
        if (behavior.getChildren().length > 0) {
            for (NodeData child : behavior.getChildren()) {
                boolean childAlreadyExists = false;
                for (BuilderNode childNode : allNodes) {
                    if (childNode.getName().equals(child.getName())) {
                        childNode.setParent(node);
                        childAlreadyExists = true;
                        break;
                    }
                }
                if (childAlreadyExists) {
                    continue;
                }
                populateNodeList(child, node);
            }
        }
    }

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
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        if (this.actionFields.size() > 0 && this.actionFields.get(0).isVisible()) {
            int numCols = getNumWidgetCols(this.width, 30);
            int[] colWidths = new int[numCols];
            Arrays.fill(colWidths, 32);
            colWidths[(numCols / 2) - 1] = 20;
            int numRows = getNumWidgetRows(this.height);
            numRows = (numRows / 2);
            int textHeightOffset = 5; // This lines up the text with the edit boxes a little better.
            drawString(
                    matrixStack,
                    font,
                    "Block Pos:",
                    getWidgetWidth(numCols / 2, this.width, colWidths, 0),
                    getWidgetHeight(numRows - 1, this.height, null) + textHeightOffset,
                    0xFFFFFF
            );
            drawString(
                    matrixStack,
                    font,
                    "Radius:",
                    getWidgetWidth(numCols / 2, this.width, colWidths, 0),
                    getWidgetHeight(numRows - 2, this.height, null) + textHeightOffset,
                    0xFFFFFF
            );
            colWidths = new int[]{120, 30};
            drawString(
                    matrixStack,
                    font,
                    "Name:",
                    getWidgetWidth(1, this.width, colWidths, 0),
                    getWidgetHeight(0, this.height, null) + textHeightOffset,
                    0xFFFFFF
            );
        }
    }

    /**
     * Send a Client Request to Edit the Behavior of the npc being edited.
     */
    @Override
    protected void sendCEdit() {
        JsonObject behaviorJson = buildNodeDataJSON();
        JsonObject behaviorEditorJSON = buildEditorJSON();
        PacketDispatcher.sendToServer(new CEditBehavior(
                this.fileName, this.npcEntity.getId(), behaviorJson.toString(), behaviorEditorJSON.toString()));
    }

    /**
     * Set the editing node.
     * @param node The editing node.
     */
    @Override
    protected void setEditingNode(BuilderNode node) {
        this.editingNode = (BehaviorNode) node;
    }

    /**
     * Set a new Action name.
     * @param s The new name.
     */
    protected void setNewActionName(String s) {
        if (!s.isEmpty()) {
            this.newActionName = s;
            this.confirmButton.active = true;
        } else this.confirmButton.active = false;
    }

    /**
     * Set a new Dialogue name.
     * @param s The new dialogue name.
     */
    protected void setNewDialogueName(String s) {
        if (!s.isEmpty()) {
            this.newDialogueName = s;
            this.confirmButton.active = true;
        } else this.confirmButton.active = false;
    }

    /**
     * Set a new Trigger child.
     * @param s The new trigger child.
     */
    protected void setNewTriggerChild(String s) {
        if (!s.isEmpty()) {
            this.newTriggerChild = s;
        }
    }

    /**
     * Set a new Trigger name.
     * @param s The new trigger name.
     */
    protected void setNewTriggerName(String s) {
        if (!s.isEmpty()) {
            this.newTriggerName = s;
            this.confirmButton.active = true;
        } else this.confirmButton.active = false;
    }

    /**
     * Set `node` as being edited and set the edit type.
     * @param node The node.
     * @param editType The edit type.
     */
    @Override
    public void setNodeBeingEdited(@Nullable BuilderNode node, EditType editType) {
        super.setNodeBeingEdited(node, editType);
        boolean isActionType = editType == EditType.ACTION;
        if (node != null) {
            this.allFields.get(EditType.FILE).setValue(((BehaviorNode) node).getDialogueName());
            Trigger trigger = ((BehaviorNode) node).getEditTrigger();
            if (trigger != null) {
                this.allFields.get(EditType.TRIGGER).setValue(trigger.getName());
                this.triggerChildField.setValue(trigger.getNextBehaviorName());
                this.triggerTimerField.setValue(String.valueOf(trigger.getTimer()));
                this.triggerTypeDropdownWidget.selectOption(trigger.getType());
            } else {
                this.allFields.get(EditType.TRIGGER).setValue("");
                this.triggerChildField.setValue("");
                this.triggerTimerField.setValue("0");
                this.triggerTypeDropdownWidget.selectOption(Trigger.TriggerType.DIALOGUE_TRIGGER);
            }

            Action action = ((BehaviorNode) node).getNodeData().getAction();
            this.poseDropdownWidget.selectOption(action.getPose());
            this.actionTypeDropdownWidget.selectOption(action.getActionType());
            this.allFields.get(EditType.ACTION).setValue(action.getName());
            if (action.getName().isEmpty()) this.confirmButton.active = false;
            BlockPos blockPos = action.getTargetBlockPos();
            actionFields.get(0).setValue(String.valueOf(blockPos.getX()));
            actionFields.get(1).setValue(String.valueOf(blockPos.getY()));
            actionFields.get(2).setValue(String.valueOf(blockPos.getZ()));
            actionFields.get(3).setValue(String.valueOf(action.getRadius()));

        } else {
            this.triggerChildField.setValue("");
            this.triggerTimerField.setValue("0");
            actionFields.get(0).setValue("0");
            actionFields.get(1).setValue("0");
            actionFields.get(2).setValue("0");
            actionFields.get(3).setValue("0");
        }

        this.poseDropdownWidget.visible = isActionType;
        this.actionTypeDropdownWidget.visible = isActionType;
        for (EditBox editBox : actionFields) editBox.setVisible(isActionType);

        boolean isTriggerType = editType == EditType.TRIGGER;
        this.triggerTypeDropdownWidget.visible = isTriggerType;
        this.triggerChildField.setVisible(isTriggerType);
        this.triggerTimerField.setVisible(isTriggerType);

        if (!isActionType && !isTriggerType && editType != EditType.FILE) {
            this.confirmButton.active = true;
        }
    }
    /**
     * Set the selected node and the selected option index of it.
     *
     * @param node              The node to select.
     * @param selectedNodeIndex The index of it.
     */
    public void setSelectedNode(@Nullable BuilderNode node, int selectedNodeIndex) {
        this.selectedNode = (BehaviorNode) node;
        this.selectedNodeIndex = selectedNodeIndex;
    }


    /**
     * Called every `tick`. Updates the screen and all fields. Clears the queued for removal nodes, and checks for
     * conflicting names.
     */
    @Override
    public void tick() {
        super.tick();
    }
}
