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
import flash.npcmod.client.gui.widget.DirectionalFrame;
import flash.npcmod.client.gui.widget.DropdownWidget;
import flash.npcmod.client.gui.widget.FunctionListWidget;
import flash.npcmod.client.gui.widget.TextWidget;
import flash.npcmod.core.client.behaviors.ClientBehaviorUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CEditBehavior;
import flash.npcmod.network.packets.client.CEditNpc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class BehaviorBuilderScreen extends TreeBuilderScreen {
    protected String newDialogueName = "", newActionName = "", newTriggerName = "", newTriggerChild = "";
    protected long[] waitingPath;
    /**
     * 0-2 Block Pos, 3 Radius, 4 Timer
     */
    protected int[] intArgs;
    private final NpcEntity npcEntity;
    private DropdownWidget<CEditNpc.NPCPose> poseDropdownWidget;
    private DropdownWidget<Action.ActionType> actionTypeDropdownWidget;
    private DropdownWidget<Trigger.TriggerType> triggerTypeDropdownWidget;
    private EditBox triggerChildField, triggerTimerField;
    /**
     * 0-2 Block Pos, 3 Radius
     */
    private final List<EditBox> actionFields;
    private Button getPathButton, setPathButton;
    @Nullable
    protected BehaviorNode editingNode, selectedNode;

    protected DirectionalFrame testFrame;

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
        this.waitingPath = new long[0];

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
                blockPos, intArgs[3], this.waitingPath);
        this.editingNode.getNodeData().setAction(action);
        this.editingNode.setEditTriggerIndex(-1);
    }

    @Override
    protected ResourceLocation getBackground() {
        return new ResourceLocation(Main.MODID, "textures/gui/edit_behavior/background.png");
    }

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

    @Override
    public @Nullable BehaviorNode getSelectedNode() {
        return this.selectedNode;
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
                        getWidgetHeight(-2, this.height, null),
                        120,
                        3,
                        null
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
                        100,
                        Action.ActionType.values().length,
                        dropdownWidget -> {
                            Action.ActionType actionType = (Action.ActionType) dropdownWidget.getSelectedOption();
                            switch (actionType) {
                                case MOVE_TO_BLOCK, STANDSTILL ->
                                    this.actionFields.get(3).visible = false;
                                case WANDER ->
                                    this.actionFields.get(3).visible = true;
                            }

                        }
                )
        );
        this.actionTypeDropdownWidget.visible = false;

        // Set up the block pos fields.
        int numCols = getNumWidgetCols(this.width, 30);
        colWidths = new int[numCols];
        Arrays.fill(colWidths, 32);
        int indexWidth = (numCols / 2);
        colWidths[indexWidth] = 34; //Reserve space for the block pos text.
        indexHeight = (numRows / 2) - 1;
        indexWidth -= 2;

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
                        getWidgetWidth(indexWidth--, this.width, colWidths, 5),
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

        if (this.npcEntity != null) {
            BlockPos blockPos = this.npcEntity.getOrigin();
            actionFields.get(0).setValue(String.valueOf(blockPos.getX()));
            actionFields.get(1).setValue(String.valueOf(blockPos.getX()));
            actionFields.get(2).setValue(String.valueOf(blockPos.getX()));
        }
        colWidths[Math.abs(indexWidth - 1)] = 50;
        colWidths[Math.abs(indexWidth - 2)] = 50;
        this.getPathButton = new Button(
            getWidgetWidth(indexWidth--, this.width, colWidths, 5),
            getWidgetHeight(indexHeight, this.height, null),
            50, 20, new TextComponent("Get Path"), btn -> {
                ItemStack behaviorEditorStack = getMinecraft().player.getItemInHand(InteractionHand.MAIN_HAND);
                if (this.waitingPath.length == 0) {
                    behaviorEditorStack.setTag(null);
                    return;
                }
                CompoundTag pathTag = new CompoundTag();
                pathTag.putLongArray("Path", this.waitingPath);
                Main.LOGGER.info("Loaded something;");
            }
        );
        this.getPathButton.visible = false;

        this.setPathButton = new Button(
            getWidgetWidth(indexWidth, this.width, colWidths, 5),
            getWidgetHeight(indexHeight, this.height, null),
            50, 20, new TextComponent("Set Path"), btn -> {
                ItemStack behaviorEditorStack = getMinecraft().player.getItemInHand(InteractionHand.MAIN_HAND);
                if (behaviorEditorStack.hasTag()) {
                    CompoundTag pathTag = behaviorEditorStack.getTag();
                    if (pathTag != null && pathTag.contains("Path")) {
                        this.waitingPath = behaviorEditorStack.getTag().getLongArray("Path");
                        return;
                    }
                }
                this.waitingPath = new long[0];
            }
        );

        this.setPathButton.visible = false;

        DirectionalFrame verticalFrame = this.addRenderableWidget(
                DirectionalFrame.createVerticalFrame(0, 0, this.height)
        );


        this.testFrame = DirectionalFrame.createHorizontalFrame(0, getWidgetHeight(indexHeight, this.height, null), this.width);
        this.testFrame.addWidget(new TextWidget(0, 0, minecraft.font.width("Target Block:"), "Target Block:"));
        this.testFrame.addWidget(this.actionFields.get(0));
        this.testFrame.addWidget(this.actionFields.get(1));
        this.testFrame.addWidget(this.actionFields.get(2));
        this.testFrame.addSpacer();
        this.testFrame.addWidget(this.setPathButton);
        this.testFrame.addWidget(this.getPathButton);
        this.testFrame.setVisible(false);
        verticalFrame.addWidget(this.testFrame);

        // Set up the radius field.

        indexWidth = (numCols / 2) - 3;
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
                        BlockPos blockPos = BlockPos.ZERO;
                        if (this.npcEntity != null) {
                            blockPos = this.npcEntity.blockPosition();
                        }

                        if (getSelectedNode() != null && getSelectedNodeIndex() == -2) {
                            newNode = new BehaviorNode(null, this, this.minecraft, Behavior.newBehavior(blockPos));
                            newNode.getNodeData().setName(name);
                            newNode.addChild(getSelectedNode().getNodeData());
                            getSelectedNode().setParent(newNode);
                        } else {
                            newNode = new BehaviorNode(this.selectedNode, this, this.minecraft, Behavior.newBehavior(blockPos));
                            newNode.getNodeData().setName(name);
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

    @Override
    public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        if (this.actionFields.size() > 0 && this.actionFields.get(0).isVisible()) {
            int numCols = getNumWidgetCols(this.width, 30);
            int[] colWidths = new int[numCols];
            Arrays.fill(colWidths, 32);
            colWidths[(numCols / 2) - 1] = 28;
            int numRows = getNumWidgetRows(this.height);
            numRows = (numRows / 2);
            int textHeightOffset = 5; // This lines up the text with the edit boxes a little better.
            if (actionTypeDropdownWidget.getSelectedOption() == Action.ActionType.WANDER) {
                drawString(
                        matrixStack,
                        font,
                        "Radius:",
                        getWidgetWidth((numCols / 2) - 1, this.width, colWidths, 0),
                        getWidgetHeight(numRows - 2, this.height, null) + textHeightOffset,
                        0xFFFFFF
                );
            }
            colWidths = new int[]{120, 30};
            drawString(
                    matrixStack,
                    font,
                    "Name:",
                    getWidgetWidth(1, this.width, colWidths, 0),
                    getWidgetHeight(0, this.height, null) + textHeightOffset,
                    0xFFFFFF
            );
        } else if (triggerChildField.isVisible()) {
            int[] colWidths = {120,22};
            int textHeightOffset = 5; // This lines up the text with the edit boxes a little better.
            drawString(
                    matrixStack,
                    font,
                    "Name:",
                    getWidgetWidth(1, this.width, colWidths, 0),
                    getWidgetHeight(2, this.height, null) + textHeightOffset,
                    0xFFFFFF
            );
            drawString(
                    matrixStack,
                    font,
                    "Timer:",
                    getWidgetWidth(1, this.width, colWidths, 0),
                    getWidgetHeight(1, this.height, null) + textHeightOffset,
                    0xFFFFFF
            );
            drawString(
                    matrixStack,
                    font,
                    "Node:",
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
        int id = -1000;
        if (this.npcEntity != null) {
            id = this.npcEntity.getId();
        }
        PacketDispatcher.sendToServer(new CEditBehavior(
                this.fileName, id, behaviorJson.toString(), behaviorEditorJSON.toString()));
    }

    @Override
    protected void setEditingNode(BuilderNode node) {
        this.editingNode = (BehaviorNode) node;
    }

    protected void setNewActionName(String s) {
        if (!s.isEmpty()) {
            this.newActionName = s;
            this.confirmButton.active = true;
        } else this.confirmButton.active = false;
    }

    protected void setNewDialogueName(String s) {
        if (!s.isEmpty()) {
            this.newDialogueName = s;
            this.confirmButton.active = true;
        } else this.confirmButton.active = false;
    }

    protected void setNewTriggerChild(String s) {
        if (!s.isEmpty()) {
            this.newTriggerChild = s;
        }
    }

    protected void setNewTriggerName(String s) {
        if (!s.isEmpty()) {
            this.newTriggerName = s;
            this.confirmButton.active = true;
        } else this.confirmButton.active = false;
    }

    @Override
    public void setNodeBeingEdited(@Nullable BuilderNode node, EditType editType) {
        super.setNodeBeingEdited(node, editType);
        boolean isActionType = editType == EditType.ACTION;
        this.poseDropdownWidget.visible = isActionType;
        this.actionTypeDropdownWidget.visible = isActionType;
        //for (EditBox editBox : actionFields) editBox.setVisible(isActionType);
        this.testFrame.setVisible(isActionType);
        //this.getPathButton.visible = isActionType;
        //this.setPathButton.visible = isActionType;
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
            this.waitingPath = action.getPath();
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
            this.waitingPath = new long[0];
        }

        boolean isTriggerType = editType == EditType.TRIGGER;
        this.triggerTypeDropdownWidget.visible = isTriggerType;
        this.triggerChildField.setVisible(isTriggerType);
        this.triggerTimerField.setVisible(isTriggerType);

        if (!isActionType && !isTriggerType && editType != EditType.FILE) {
            this.confirmButton.active = true;
        }
    }


    public void setSelectedNode(@Nullable BuilderNode node, int selectedNodeIndex) {
        this.selectedNode = (BehaviorNode) node;
        this.selectedNodeIndex = selectedNodeIndex;
    }

    @Override
    public void tick() {
        super.tick();
        for (EditBox editBox : actionFields) editBox.tick();
    }
}
