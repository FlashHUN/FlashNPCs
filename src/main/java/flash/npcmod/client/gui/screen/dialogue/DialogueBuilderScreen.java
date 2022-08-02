package flash.npcmod.client.gui.screen.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.client.gui.dialogue.Dialogue;
import flash.npcmod.client.gui.dialogue.DialogueNode;
import flash.npcmod.client.gui.node.BuilderNode;
import flash.npcmod.client.gui.node.NodeData;
import flash.npcmod.client.gui.screen.TreeBuilderScreen;
import flash.npcmod.client.gui.widget.FunctionListWidget;
import flash.npcmod.client.gui.widget.ListWidget;
import flash.npcmod.core.client.dialogues.ClientDialogueUtil;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CEditDialogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class DialogueBuilderScreen extends TreeBuilderScreen {
    private String newText = "", newResponse = "", newTrigger = "";
    private ListWidget<DialogueBuilderScreen> createMenuWidget;
    @Nullable
    protected DialogueNode editingNode, selectedNode;


    public DialogueBuilderScreen(String name) {
        super(name);
    }

    /**
     * Confirm Edits and push their changes to the NodeData.
     */
    @Override
    protected void confirmEdits() {
        super.confirmEdits();
        assert this.editingNode != null;
        this.editingNode.getNodeData().setText(this.newText);
        this.editingNode.getNodeData().setResponse(this.newResponse);
        this.editingNode.getNodeData().setTrigger(this.newTrigger);
    }

    /**
     * Create the Node at the mouse.
     *
     * @param mouseX The mouse x position.
     * @param mouseY The mouse y position.
     */
    private void createNode(double mouseX, double mouseY) {
        DialogueNode newNode;
        if (getSelectedNode() != null && getSelectedNodeIndex() == -2) {
            newNode = new DialogueNode(null, this, this.minecraft, Dialogue.newDialogue());
            newNode.addChild(getSelectedNode().getNodeData());
            getSelectedNode().setParent(newNode);
        } else {
            newNode = new DialogueNode(getSelectedNode(), this, this.minecraft, Dialogue.newDialogue());
        }

        newNode.setPosition((int) (-this.scrollX + mouseX - 9), (int) (-this.scrollY + mouseY - 18));
        if (getSelectedNode() == null || !getSelectedNode().getName().equals(newNode.getName())) {
            this.allNodes.add(newNode);
        }
        if (this.getSelectedNode() != null) {
            this.selectedNode = null;
        }
    }

    /**
     * Get the background to draw.
     * @return The background.
     */
    @Override
    protected ResourceLocation getBackground() {
        return new ResourceLocation(Main.MODID, "textures/gui/edit_dialogue/background.png");
    }

    /**
     * Get the editing node.
     * @return The editing node.
     */
    @Override
    public @Nullable DialogueNode getEditingNode() {
        return this.editingNode;
    }

    /**
     * Get the entries of the dialogue editor.
     * @return The JSON array of dialogues nodes.
     */
    @Override
    protected JsonArray getEntries() {
        if (ClientDialogueUtil.currentDialogueEditor == null) {
            return new JsonArray();
        }
        return ClientDialogueUtil.currentDialogueEditor.getAsJsonArray("entries");
    }

    /**
     * Get the selected Dialogue node.
     * @return The selected node.
     */
    @Override
    public @Nullable DialogueNode getSelectedNode() {
        return this.selectedNode;
    }

    /**
     * Initialize the text fields that will be used for editing.
     */
    @Override
    protected void init() {
        super.init();
        // Initialize the function widget.
        this.functionListWidget = new FunctionListWidget<>(this, Minecraft.getInstance());
        this.functionListWidget.calculatePositionAndDimensions();

        //initialize right-click widget
        this.createMenuWidget = new ListWidget<>(this, Minecraft.getInstance());

        this.createMenuWidget.setOptions(List.of("Create Dialogue"));

        // Initialize our text field widgets
        EditBox textField = this.addRenderableWidget(
                new EditBox(
                        this.font,
                        this.width / 2 - 60,
                        this.height / 2 - 10,
                        120,
                        20,
                        TextComponent.EMPTY
                )
        );
        textField.setResponder(this::setNewText);
        textField.setMaxLength(500);
        textField.setVisible(false);

        textField.setCanLoseFocus(true);
        this.allFields.put(EditType.TEXT, textField);

        EditBox responseField = this.addRenderableWidget(new EditBox(this.font, this.width / 2 - 60, this.height / 2 - 10, 120, 20, TextComponent.EMPTY));
        responseField.setResponder(this::setNewResponse);
        responseField.setMaxLength(500);
        responseField.setVisible(false);
        responseField.setCanLoseFocus(true);
        this.allFields.put(EditType.RESPONSE, responseField);

        EditBox triggerField = this.addRenderableWidget(new EditBox(this.font, this.width / 2 - 60, this.height / 2 - 10, 120, 20, TextComponent.EMPTY));
        triggerField.setResponder(this::setNewTrigger);
        triggerField.setMaxLength(500);
        triggerField.setVisible(false);
        triggerField.setCanLoseFocus(true);
        this.allFields.put(EditType.ACTION, triggerField);
    }

    /**
     * Load this from the json file.
     */
    @Override
    protected void loadFromJsonObject() {
        ClientDialogueUtil.loadDialogue(fileName);
        ClientDialogueUtil.loadDialogueEditor(fileName);
        Dialogue[] dialogues;
        if (ClientDialogueUtil.currentDialogue != null) {
            dialogues = Dialogue.multipleFromJSONObject(ClientDialogueUtil.currentDialogue);
        } else {
            dialogues = new Dialogue[]{ new Dialogue("init") };
        }

        conflictingNodeDataNames = new ArrayList<>();

        for (Dialogue dialogue : dialogues) {
            populateNodeList(dialogue, null);
        }
    }

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
        if (!this.isAnyTextFieldVisible()) {
            if (button == 0 || button == 1) {
                boolean isMouseOverAnyNode = isMouseOverNode(mouseX, mouseY);
                if (!isMouseOverAnyNode) {
                    if (button == 0 && this.selectedNode != null) {
                        if (this.selectedNode.isStart()) {
                            this.selectedNode.getNodeData().setResponse("");
                        }
                        this.selectedNode.calculateDimensions();
                        this.selectedNode = null;
                    }
                    if (button == 1) {
                        this.createMenuWidget.setY(mouseY);
                        this.createMenuWidget.setX(mouseX);
                        this.createMenuWidget.setVisible(true);
                    }
                }
                if (button != 1 && this.createMenuWidget.isVisible()) {
                    this.createMenuWidget.clickedOn(mouseX, mouseY);
                    if (!this.createMenuWidget.getSelectedOption().isEmpty()) {
                        String option = this.createMenuWidget.getSelectedOption();
                        if (option.equals("Create Dialogue")) {
                            createNode(mouseX, mouseY);
                        }
                    }
                    this.createMenuWidget.setVisible(false);
                }
                // TODO make sure not to check during parent setting.
                this.allNodes.forEach(node -> node.clickedOn(mouseX, mouseY, button, 0, 0, this.scrollX, this.scrollY));
            }
        } else if (this.functionListWidget.isVisible()) {
            this.functionListWidget.clickedOn(mouseX, mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Populate allNodes.
     * @param dialogue The nodeData.
     * @param parent The parent of the nodeData.
     */
    @Override
    protected void populateNodeList(NodeData dialogue, @Nullable BuilderNode parent) {
        DialogueNode node = new DialogueNode((DialogueNode) parent, this, Minecraft.getInstance(), (Dialogue) dialogue);
        if (this.initNode == null && dialogue.isInitData()) {
            this.initNode = node;
        }
        allNodes.add(node);
        if (dialogue.getChildren().length > 0) {
            for (NodeData child : dialogue.getChildren()) {
                populateNodeList(child, node);
            }
        }
    }

    /**
     * Function from screen.
     *
     * @param matrixStack  The PoseStack.
     * @param mouseX       The mouse x coordinate.
     * @param mouseY       The mouse y coordinate.
     * @param partialTicks The float time.
     */
    @Override
    public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.createMenuWidget.draw(matrixStack);
    }

    /**
     * Send a Client Request to Edit the Dialogue of the npc being edited.
     */
    @Override
    protected void sendCEdit() {
        JsonObject dialogue = buildNodeDataJSON();
        JsonObject dialogueEditor = buildEditorJSON();
        PacketDispatcher.sendToServer(new CEditDialogue(this.fileName, dialogue.toString(), dialogueEditor.toString()));
    }

    /**
     * Set the editing node.
     * @param node The editing node.
     */
    @Override
    protected void setEditingNode(BuilderNode node) {
        this.editingNode = (DialogueNode) node;
    }

    /**
     * Set a new text.
     * @param s The new text.
     */
    private void setNewText(String s) {
        this.newText = s;
    }

    /**
     * Set a new response.
     * @param s The new response.
     */
    private void setNewResponse(String s) {
        this.newResponse = s;
    }

    /**
     * Set a new trigger.
     * @param s The new trigger.
     */
    private void setNewTrigger(String s) {
        this.newTrigger = s;
    }

    /**
     * Set the selected node and the selected option index of it.
     *
     * @param node              The node to select.
     * @param selectedNodeIndex The index of it.
     */
    @Override
    public void setSelectedNode(@Nullable BuilderNode node, int selectedNodeIndex) {
        this.selectedNode = (DialogueNode) node;
        this.selectedNodeIndex = selectedNodeIndex;
    }

    /**
     * Set `node` as being edited and set the edit type.
     * @param node The node.
     * @param editType The edit type.
     */
    @Override
    public void setNodeBeingEdited(@Nullable BuilderNode node, EditType editType) {
        super.setNodeBeingEdited(node, editType);
        if (node != null) {
            this.allFields.get(EditType.TEXT).setValue(((DialogueNode) node).getText());
            this.allFields.get(EditType.RESPONSE).setValue(((DialogueNode) node).getResponse());
            this.allFields.get(EditType.ACTION).setValue(((DialogueNode) node).getTrigger());
        }
    }

}
