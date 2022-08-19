package flash.npcmod.client.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import flash.npcmod.Main;
import flash.npcmod.client.gui.behavior.Action;
import flash.npcmod.client.gui.behavior.Behavior;
import flash.npcmod.client.gui.behavior.BehaviorNode;
import flash.npcmod.client.gui.behavior.Trigger;
import flash.npcmod.client.gui.node.BuilderNode;
import flash.npcmod.client.gui.node.NodeData;
import flash.npcmod.client.gui.widget.DirectionalFrame;
import flash.npcmod.client.gui.widget.DropdownWidget;
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
    @Nullable
    protected BehaviorNode editingNode, selectedNode;

    protected DirectionalFrame actionRadiusFrame, actionTargetAndPathFrame, actionTargetBlockFrame, actionPathFrame;
    protected DirectionalFrame actionVFrame, dialogueFrame, triggerFrame;

    protected final Predicate<String> numberFilter = (text) -> {
        Pattern pattern = Pattern.compile("~?-?\\d*");
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
        if (this.editingNode.isEditingTrigger()) {
            this.editingNode.setEditTrigger(
                    new Trigger(
                            this.newTriggerName,
                            this.triggerTypeDropdownWidget.getSelectedOption(),
                            this.intArgs[4],
                            this.newTriggerChild)
            );
        }
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

        // Initialize our text field widgets
        EditBox dialogueField = this.addWidget(
                new EditBox(this.font,0,0,120,20,TextComponent.EMPTY)
        );
        dialogueField.setResponder(this::setNewDialogueName);
        dialogueField.setFilter(this.nameFilter);
        dialogueField.setMaxLength(50);
        dialogueField.setCanLoseFocus(true);
        this.allNameFields.put(EditType.FILE, dialogueField);

        this.dialogueFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.CENTERED);
        this.dialogueFrame.addWidget(new TextWidget("Dialogue Name:"));
        this.dialogueFrame.addWidget(dialogueField);
        this.dialogueFrame.setVisible(false);
        this.mainVFrame.insertWidget(this.dialogueFrame, 0, 20);
        this.allTopLevelFrames.put(EditType.FILE, this.dialogueFrame);

        // Set up the trigger fields.
        EditBox triggerField = this.addWidget(
                new EditBox(this.font,0,0,120,20,TextComponent.EMPTY)
        );
        triggerField.setResponder(this::setNewTriggerName);
        triggerField.setMaxLength(50);
        triggerField.setCanLoseFocus(true);
        allNameFields.put(EditType.TRIGGER, triggerField);

        this.triggerTimerField = this.addWidget(
                new EditBox(this.font, 0, 0,120,20,TextComponent.EMPTY)
        );
        this.triggerTimerField.setFilter(numberFilter);
        this.triggerTimerField.setResponder((String s) -> {
            if (s.isEmpty() || s.equals("-")) s = "0";
            this.intArgs[4] = Integer.parseInt(s);
        });
        this.triggerTimerField.setMaxLength(10);
        this.triggerTimerField.setCanLoseFocus(true);

        this.triggerChildField = this.addWidget(
                new EditBox(this.font, 0, 0, 120, 20, TextComponent.EMPTY)
        );
        this.triggerChildField.setResponder(this::setNewTriggerChild);
        this.triggerChildField.setMaxLength(50);
        this.triggerChildField.setCanLoseFocus(true);

        this.triggerFrame = DirectionalFrame.createVerticalFrame(this.height, DirectionalFrame.Alignment.CENTERED);
        DirectionalFrame triggerNameFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.CENTERED);
        triggerNameFrame.addWidget(new TextWidget("Trigger:"));
        triggerNameFrame.addWidget(allNameFields.get(EditType.TRIGGER));
        this.triggerFrame.addWidget(triggerNameFrame);
        DirectionalFrame triggerTimerFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.CENTERED);
        triggerTimerFrame.addWidget(new TextWidget("Timer:"));
        triggerTimerFrame.addWidget(triggerTimerField);
        triggerTimerFrame.setVisible(false);
        this.triggerFrame.addWidget(triggerTimerFrame);
        this.triggerTypeDropdownWidget = this.addWidget(
            new DropdownWidget<>(Trigger.TriggerType.DIALOGUE_TRIGGER,0,0,120,3, dropdownWidget -> {
                Trigger.TriggerType triggerType = (Trigger.TriggerType) dropdownWidget.getSelectedOption();
                Main.LOGGER.info("Timer type changed");
                switch (triggerType) {
                    case ACTION_FINISH_TRIGGER, DIALOGUE_TRIGGER ->
                            triggerTimerFrame.setVisible(false);
                    case TIMER_TRIGGER ->
                            triggerTimerFrame.setVisible(true);
                }
            })
        );
        DirectionalFrame triggerChildFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.CENTERED);
        triggerChildFrame.addWidget(new TextWidget("Next:"));
        triggerChildFrame.addWidget(triggerChildField);
        this.triggerFrame.addWidget(triggerChildFrame);
        this.triggerFrame.addWidget(triggerTypeDropdownWidget);
        this.triggerFrame.setVisible(false);
        this.mainVFrame.insertWidget(triggerFrame, 0, 20);
        this.allTopLevelFrames.put(EditType.TRIGGER, this.triggerFrame);

        // Set up the action fields.
        EditBox actionField = this.addWidget(
                new EditBox(this.font,0,0, 120,20,TextComponent.EMPTY)
        );
        actionField.setResponder(this::setNewActionName);
        actionField.setMaxLength(50);
        actionField.setCanLoseFocus(true);
        allNameFields.put(EditType.ACTION, actionField);

        //  Set up the dropdown widgets
        this.poseDropdownWidget = this.addWidget(
                new DropdownWidget<>(CEditNpc.NPCPose.STANDING,0,0,80)
        );

        this.actionTypeDropdownWidget = this.addWidget(
                new DropdownWidget<>(
                        Action.ActionType.STANDSTILL,0,0,100, Action.ActionType.values().length, dropdownWidget -> {
                            Action.ActionType actionType = (Action.ActionType) dropdownWidget.getSelectedOption();
                            switch (actionType) {
                                case FOLLOW_PATH -> {
                                    actionRadiusFrame.setVisible(false);
                                    actionTargetAndPathFrame.setVisible(true);
                                    actionTargetBlockFrame.setVisible(false);
                                    actionPathFrame.setVisible(true);
                                    this.actionVFrame.recalculateSize();
                                }
                                case WANDER -> {
                                    actionRadiusFrame.setVisible(true);
                                    actionTargetAndPathFrame.setVisible(true);
                                    actionPathFrame.setVisible(false);
                                    actionTargetBlockFrame.setVisible(true);
                                    this.actionVFrame.recalculateSize();
                                }
                                case INTERACT_WITH, STANDSTILL -> {
                                    actionRadiusFrame.setVisible(false);
                                    actionTargetAndPathFrame.setVisible(true);
                                    actionPathFrame.setVisible(false);
                                    actionTargetBlockFrame.setVisible(true);
                                    this.actionVFrame.recalculateSize();
                                }
                            }

                        }
                )
        );

        EditBox targetBlockXField = this.addWidget(
                new EditBox(this.font,0,0,32,20,TextComponent.EMPTY)
        );
        targetBlockXField.setFilter(numberFilter);
        targetBlockXField.setResponder((String s) -> {
            if (s.isEmpty() || s.equals("-")) s = "0";
            else if (s.startsWith("~")) {
                if (s.length() == 1) {
                    this.intArgs[0] = npcEntity.blockPosition().getX();
                }
                else {
                    this.intArgs[0] = npcEntity.blockPosition().getX() + Integer.parseInt(s.substring(1));
                }
                return;
            }
            this.intArgs[0] = Integer.parseInt(s);
        });
        targetBlockXField.setMaxLength(10);
        targetBlockXField.setCanLoseFocus(true);
        this.actionFields.add(targetBlockXField);

        EditBox targetBlockYField = this.addWidget(
                new EditBox(this.font,0,0,32,20,TextComponent.EMPTY)
        );
        targetBlockYField.setFilter(numberFilter);
        targetBlockYField.setResponder((String s) -> {
            if (s.isEmpty() || s.equals("-")) s = "0";
            else if (s.startsWith("~")) {
                if (s.length() == 1) {
                    this.intArgs[1] = npcEntity.blockPosition().getY();
                }
                else {
                    this.intArgs[1] = npcEntity.blockPosition().getY() + Integer.parseInt(s.substring(1));
                }
                return;
            }
            this.intArgs[1] = Integer.parseInt(s);
        });
        targetBlockYField.setMaxLength(10);
        targetBlockYField.setCanLoseFocus(true);
        this.actionFields.add(targetBlockYField);

        EditBox targetBlockZField = this.addWidget(
                new EditBox(this.font,0,0,32,20,TextComponent.EMPTY)
        );
        targetBlockZField.setFilter(numberFilter);
        targetBlockZField.setResponder((String s) -> {
            if (s.isEmpty() || s.equals("-")) s = "0";
            else if (s.startsWith("~")) {
                if (s.length() == 1) {
                    this.intArgs[2] = npcEntity.blockPosition().getZ();
                }
                else {
                    this.intArgs[2] = npcEntity.blockPosition().getZ() + Integer.parseInt(s.substring(1));
                }
                return;
            }
            this.intArgs[2] = Integer.parseInt(s);
        });
        targetBlockZField.setMaxLength(10);
        targetBlockZField.setCanLoseFocus(true);
        this.actionFields.add(targetBlockZField);

        if (this.npcEntity != null) {
            BlockPos blockPos = this.npcEntity.getOrigin();
            actionFields.get(0).setValue(String.valueOf(blockPos.getX()));
            actionFields.get(1).setValue(String.valueOf(blockPos.getX()));
            actionFields.get(2).setValue(String.valueOf(blockPos.getX()));
        }
        Button getPathButton = this.addWidget(new Button(
                0,0, 50, 20, new TextComponent("Get Path"), btn -> {
            assert Minecraft.getInstance().player != null;
            ItemStack behaviorEditorStack = Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
            if (this.waitingPath.length == 0 && this.getEditingNode() != null) {
                CompoundTag pathTag = new CompoundTag();
                pathTag.putLongArray("Path", this.getEditingNode().getNodeData().getAction().getPath());
                behaviorEditorStack.setTag(pathTag);
                return;
            }
            CompoundTag pathTag = new CompoundTag();
            pathTag.putLongArray("Path", this.waitingPath);
            behaviorEditorStack.setTag(pathTag);
        }));

        Button setPathButton = this.addWidget(new Button(
                0,0, 50, 20, new TextComponent("Set Path"), btn -> {
            assert Minecraft.getInstance().player != null;
            ItemStack behaviorEditorStack = Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
            if (behaviorEditorStack.hasTag()) {
                CompoundTag pathTag = behaviorEditorStack.getTag();
                if (pathTag != null && pathTag.contains("Path")) {
                    this.waitingPath = behaviorEditorStack.getTag().getLongArray("Path");
                    return;
                }
            }
            this.waitingPath = new long[0];
        }));

        this.actionVFrame = DirectionalFrame.createVerticalFrame(
                this.height, DirectionalFrame.Alignment.START_ALIGNED);

        this.actionTargetAndPathFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.EQUALLY_SPACED);
        this.actionTargetBlockFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.START_ALIGNED);
        this.actionTargetBlockFrame.addWidget(new TextWidget(0, 0, "Target Block:"));
        this.actionTargetBlockFrame.addWidget(this.actionFields.get(0));
        this.actionTargetBlockFrame.addWidget(this.actionFields.get(1));
        this.actionTargetBlockFrame.addWidget(this.actionFields.get(2));
        this.actionTargetAndPathFrame.addWidget(this.actionTargetBlockFrame);
        this.actionTargetAndPathFrame.addSpacer();
        this.actionPathFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.START_ALIGNED);
        this.actionPathFrame.addWidget(setPathButton);
        this.actionPathFrame.addWidget(getPathButton);
        this.actionTargetAndPathFrame.addWidget(actionPathFrame);
        this.actionVFrame.addWidget(this.actionTargetAndPathFrame, 5);

        // Set up the radius field.
        EditBox radiusField = this.addWidget(
                new EditBox(this.font, 0, 0,30,20,TextComponent.EMPTY)
        );
        radiusField.setFilter(numberFilter);
        radiusField.setResponder((String s) -> {
            if (s.isEmpty() || s.equals("-")) s = "0";
            this.intArgs[3] = Integer.parseInt(s);
        });
        radiusField.setMaxLength(10);
        radiusField.setCanLoseFocus(true);
        this.actionFields.add(radiusField);

        this.actionRadiusFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.START_ALIGNED);
        this.actionRadiusFrame.addWidget(new TextWidget(0,0, "Radius:"));
        this.actionRadiusFrame.addWidget(this.actionFields.get(3));
        this.actionRadiusFrame.setVisible(false);
        this.actionVFrame.addWidget(this.actionRadiusFrame);

        DirectionalFrame nameFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.CENTERED);
        nameFrame.addWidget(new TextWidget("Name:"));
        nameFrame.addWidget(this.allNameFields.get(EditType.ACTION));
        this.actionVFrame.addWidget(nameFrame);
        DirectionalFrame dropdownFrame = DirectionalFrame.createHorizontalFrame(this.width, DirectionalFrame.Alignment.CENTERED);
        dropdownFrame.addWidget(this.actionTypeDropdownWidget, 20);
        dropdownFrame.addWidget(this.poseDropdownWidget, 20);
        this.actionVFrame.addWidget(dropdownFrame);
        this.actionVFrame.setVisible(false);
        this.mainVFrame.insertWidget(this.actionVFrame, 0, 20);
        this.allTopLevelFrames.put(EditType.ACTION, this.actionVFrame);
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
        if (node != null) {
            this.allNameFields.get(EditType.FILE).setValue(((BehaviorNode) node).getDialogueName());
            Trigger trigger = ((BehaviorNode) node).getEditTrigger();
            if (trigger != null) {
                this.allNameFields.get(EditType.TRIGGER).setValue(trigger.getName());
                this.triggerChildField.setValue(trigger.getNextBehaviorName());
                this.triggerTimerField.setValue(String.valueOf(trigger.getTimer()));
                this.triggerTypeDropdownWidget.selectOption(trigger.getType());
            } else {
                this.allNameFields.get(EditType.TRIGGER).setValue("");
                this.triggerChildField.setValue("");
                this.triggerTimerField.setValue("0");
                this.triggerTypeDropdownWidget.selectOption(Trigger.TriggerType.DIALOGUE_TRIGGER);
            }

            Action action = ((BehaviorNode) node).getNodeData().getAction();
            this.poseDropdownWidget.selectOption(action.getPose());
            this.actionTypeDropdownWidget.selectOption(action.getActionType());
            this.allNameFields.get(EditType.ACTION).setValue(action.getName());
            this.waitingPath = action.getPath();
            if (action.getName().isEmpty()) this.confirmButton.active = false;
            BlockPos blockPos = action.getTargetBlockPos();
            this.actionFields.get(0).setValue(String.valueOf(blockPos.getX()));
            this.actionFields.get(1).setValue(String.valueOf(blockPos.getY()));
            this.actionFields.get(2).setValue(String.valueOf(blockPos.getZ()));
            this.actionFields.get(3).setValue(String.valueOf(action.getRadius()));

        } else {
            this.triggerChildField.setValue("");
            this.triggerTimerField.setValue("0");
            this.actionTypeDropdownWidget.selectOption(Action.ActionType.STANDSTILL);

            this.actionFields.get(0).setValue("0");
            this.actionFields.get(1).setValue("0");
            this.actionFields.get(2).setValue("0");
            this.actionFields.get(3).setValue("0");
            this.waitingPath = new long[0];
        }

        if (editType != EditType.ACTION && editType != EditType.TRIGGER && editType != EditType.FILE) {
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
