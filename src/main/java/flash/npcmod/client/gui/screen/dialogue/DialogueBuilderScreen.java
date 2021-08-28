package flash.npcmod.client.gui.screen.dialogue;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import flash.npcmod.Main;
import flash.npcmod.client.gui.dialogue.Dialogue;
import flash.npcmod.client.gui.dialogue.DialogueNode;
import flash.npcmod.client.gui.widget.FunctionListWidget;
import flash.npcmod.core.client.dialogues.ClientDialogueUtil;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CEditDialogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public class DialogueBuilderScreen extends Screen {

  private static final ResourceLocation BACKGROUND = new ResourceLocation(Main.MODID, "textures/gui/edit_dialogue/background.png");

  private String dialogueName;

  private double scrollX, scrollY;
  private boolean isScrolling;
  public int maxX = 10000;
  public int maxY = 10000;

  private DialogueNode initDialogueNode;
  public final List<DialogueNode> allDialogueNodes;
  public List<String> conflictingDialogueNames;
  @Nullable
  private DialogueNode selectedNode;
  private int selectedNodeIndex;
  @Nullable
  private DialogueNode editingNode;
  private FunctionListWidget functionListWidget;

  private TextFieldWidget nameField, textField, responseField, functionParamsField;
  private Button saveButton, confirmButton, cancelButton;
  private String newName = "", newText = "", newResponse = "", newFunctionParams = "";

  private final Predicate<String> nameFilter = (text) -> {
    Pattern pattern = Pattern.compile("\\s");
    Matcher matcher = pattern.matcher(text);
    return !matcher.find();
  };

  public DialogueBuilderScreen(String name) {
    super(StringTextComponent.EMPTY);
    this.dialogueName = name;

    ClientDialogueUtil.loadDialogue(name);
    ClientDialogueUtil.loadDialogueEditor(name);
    Dialogue[] dialogues = Dialogue.multipleFromJSONObject(ClientDialogueUtil.currentDialogue);

    conflictingDialogueNames = new ArrayList<>();

    this.functionListWidget = new FunctionListWidget(this, Minecraft.getInstance());
    this.functionListWidget.calculatePositionAndDimensions();

    allDialogueNodes = new ArrayList<>();
    for (Dialogue dialogue : dialogues) {
      populateDialogueNodeList(dialogue, null);
    }

    updateNodePositionsFromJson();
  }

  private void populateDialogueNodeList(Dialogue dialogue, @Nullable DialogueNode parent) {
    DialogueNode node = new DialogueNode(parent, this, Minecraft.getInstance(), dialogue);
    if (this.initDialogueNode == null && dialogue.isInitDialogue()) {
      this.initDialogueNode = node;
    }
    allDialogueNodes.add(node);
    if (dialogue.getChildren().length > 0) {
      for (Dialogue child : dialogue.getChildren()) {
        populateDialogueNodeList(child, node);
      }
    }
  }

  private void updateNodePositionsFromJson() {
    JSONArray entries = ClientDialogueUtil.currentDialogueEditor.getJSONArray("entries");
    allDialogueNodes.forEach(node -> {
      for (int i = 0; i < entries.length(); i++) {
        JSONObject entry = entries.getJSONObject(i);
        if (entry.has("name") && entry.has("x") && entry.has("y")) {
          if (node.getDialogue().getName().equals(entry.getString("name"))) {
            node.setPosition(entry.getInt("x"), entry.getInt("y"));
          }
        }
      }
    });
  }

  @Override
  protected void init() {
    this.editingNode = null;

    // Initialize our button widgets
    this.saveButton = this.addButton(new Button(width/2-50, height-25, 100, 20, new StringTextComponent("Save"), btn -> {
      if (this.editingNode == null) {
        if (conflictingDialogueNames.size() == 0) {
          JSONObject dialogue = buildDialogueJSON();
          JSONObject dialogueEditor = buildDialogueEditorJSON();
          PacketDispatcher.sendToServer(new CEditDialogue(this.dialogueName, dialogue.toString(), dialogueEditor.toString()));
        }
      }
    }));
    this.saveButton.active = conflictingDialogueNames.size() == 0;
    this.confirmButton = this.addButton(new Button(width/2-60, height/2+15, 50, 20, new StringTextComponent("Confirm"), btn -> {
      if (this.editingNode != null) {
        if (!this.newName.equals(ClientDialogueUtil.INIT_DIALOGUE_NAME)) {
          editingNode.getDialogue().setName(this.newName);
        }
        editingNode.getDialogue().setText(this.newText);
        editingNode.getDialogue().setResponse(this.newResponse);
        if (functionListWidget.isVisible()) {
          functionListWidget.setFunction();
        }
        editingNode.calculateDimensions();
      }
      this.setNodeBeingEdited(null, EditType.NONE);
    }));
    this.confirmButton.visible = false;
    this.cancelButton = this.addButton(new Button(width/2+10, height/2+15, 50, 20, new StringTextComponent("Cancel"), btn -> {
      this.setNodeBeingEdited(null, EditType.NONE);
    }));
    this.cancelButton.visible = false;

    // Initialize our text field widgets
    this.nameField = this.addButton(new TextFieldWidget(this.font, this.width/2-60, this.height/2-10, 120, 20, StringTextComponent.EMPTY));
    this.nameField.setResponder(this::setNewName);
    this.nameField.setValidator(this.nameFilter);
    this.nameField.setMaxStringLength(50);
    this.nameField.setVisible(false);
    this.nameField.setCanLoseFocus(true);

    this.textField = this.addButton(new TextFieldWidget(this.font, this.width/2-60, this.height/2-10, 120, 20, StringTextComponent.EMPTY));
    this.textField.setResponder(this::setNewText);
    this.textField.setMaxStringLength(500);
    this.textField.setVisible(false);
    this.textField.setCanLoseFocus(true);

    this.responseField = this.addButton(new TextFieldWidget(this.font, this.width/2-60, this.height/2-10, 120, 20, StringTextComponent.EMPTY));
    this.responseField.setResponder(this::setNewResponse);
    this.responseField.setMaxStringLength(500);
    this.responseField.setVisible(false);
    this.responseField.setCanLoseFocus(true);

    this.functionListWidget.calculatePositionAndDimensions();
    this.functionParamsField = this.addButton(new TextFieldWidget(this.font, width/2-60, height-44, 120, 20, StringTextComponent.EMPTY));
    this.functionParamsField.setResponder(this::setNewFunctionParams);
    this.functionParamsField.setValidator(this.nameFilter);
    this.functionParamsField.setMaxStringLength(100);
    this.functionParamsField.setVisible(false);
    this.functionParamsField.setCanLoseFocus(true);
    this.functionListWidget.setVisible(false);
  }

  private boolean isAnyTextFieldVisible() {
    return nameField.getVisible() || textField.getVisible() || responseField.getVisible() || this.functionListWidget.isVisible();
  }

  private void setNewName(String s) {
    if (!s.isEmpty()) {
      this.newName = s;
    }
  }

  private void setNewText(String s) {
    this.newText = s;
  }

  private void setNewResponse(String s) {
    this.newResponse = s;
  }

  private void setNewFunctionParams(String s) {
    this.newFunctionParams = s;
  }

  public String getNewFunctionParams() {
    return this.newFunctionParams;
  }

  public void setNodeBeingEdited(@Nullable DialogueNode node, EditType editType) {
    if (node != null) {
      this.nameField.setText(node.getName());
      this.textField.setText(node.getText());
      this.responseField.setText(node.getResponse());
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
          this.functionParamsField.setText(splitFunction[1]);
      }
    } else {
      this.nameField.setText("");
      this.textField.setText("");
      this.responseField.setText("");
      this.functionParamsField.setText("");
    }

    this.nameField.setVisible(editType == EditType.NAME);
    this.textField.setVisible(editType == EditType.TEXT);
    this.responseField.setVisible(editType == EditType.RESPONSE);

    this.functionListWidget.calculatePositionAndDimensions();
    this.functionListWidget.setEditingNode(node);
    this.functionListWidget.setVisible(editType == EditType.FUNCTION);

    this.confirmButton.visible = editType != EditType.NONE;
    this.cancelButton.visible = editType != EditType.NONE;
    if (editType == EditType.FUNCTION) {
      confirmButton.x = this.functionParamsField.x;
      confirmButton.y = this.functionParamsField.y+22;
      cancelButton.x = this.functionParamsField.x+functionParamsField.getWidth()-confirmButton.getWidth();
      cancelButton.y = this.functionParamsField.y+22;
    } else {
      confirmButton.x = width/2-60;
      confirmButton.y = height/2+15;
      cancelButton.x = width/2+10;
      cancelButton.y = height/2+15;
    }

    this.editingNode = node;
  }

  public enum EditType {
    NONE,
    NAME,
    TEXT,
    RESPONSE,
    FUNCTION
  }

  @Override
  public void tick() {
    // Tick Text Field Widgets
    this.nameField.tick();
    this.textField.tick();
    this.responseField.tick();
    this.functionParamsField.tick();

    // Button visibility
    boolean isAnyTextFieldVisible = this.isAnyTextFieldVisible();
    this.saveButton.visible = !isAnyTextFieldVisible;
    this.confirmButton.visible = isAnyTextFieldVisible;
    this.cancelButton.visible = isAnyTextFieldVisible;

    // Save button should only be active if we have no conflicting dialogue names
    this.saveButton.active = conflictingDialogueNames.size() == 0;

    // Remove all nodes queued for removal
    List<DialogueNode> queuedForRemoval = new ArrayList<>();
    for (DialogueNode node : allDialogueNodes) {
      if (node.isQueuedForRemoval()) {
        queuedForRemoval.add(node);
      }
    }
    for (DialogueNode node : queuedForRemoval) {
      if (node.getDialogue().getChildren().length > 0) {
        List<DialogueNode> queuedForParentRemoval = new ArrayList<>();
        allDialogueNodes.forEach(dialogueNode -> {
          for (int i = 0; i < node.getDialogue().getChildren().length; i++) {
            if (node.getDialogue().getChildren()[i].equals(dialogueNode.getDialogue())) {
              queuedForParentRemoval.add(dialogueNode);
            }
          }
        });
        queuedForParentRemoval.forEach(dialogueNode -> dialogueNode.setParent(null));
      }
      node.remove();
    }

    // See which dialogues have conflicting names and store them in a list
    conflictingDialogueNames.clear();
    for (DialogueNode base : allDialogueNodes) {
      String name = base.getName();
      for (DialogueNode other : allDialogueNodes) {
        if (!other.equals(base)) {
          if (other.getName().equals(name)) {
            if (!conflictingDialogueNames.contains(name)) {
              conflictingDialogueNames.add(name);
            }
            break;
          }
        }
      }
    }

    for (DialogueNode node : allDialogueNodes) {
      if (conflictingDialogueNames.contains(node.getName())) {
        node.hasConflictingName = true;
      } else {
        node.hasConflictingName = false;
      }
    }

    functionParamsField.visible = functionListWidget.isVisible() && functionListWidget.getSelectedFunction().contains("::");
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.drawWindowBackground(matrixStack, mouseX, mouseY);
    this.functionListWidget.draw(matrixStack);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }

  private void drawWindowBackground(MatrixStack matrixStack, int mouseX, int mouseY) {
    RenderSystem.pushMatrix();
    RenderSystem.translatef(9, 18, 0.0F);
    drawBackground(matrixStack, mouseX, mouseY);
    RenderSystem.depthFunc(GL11.GL_LEQUAL);
    RenderSystem.disableDepthTest();
    RenderSystem.popMatrix();
  }

  private void drawBackground(MatrixStack matrixStack, int mouseX, int mouseY) {
    RenderSystem.pushMatrix();
    this.minecraft.getTextureManager().bindTexture(BACKGROUND);

    int i = MathHelper.floor(scrollX);
    int j = MathHelper.floor(scrollY);
    int k = i % 16;
    int l = j % 16;

    int x = width/16;
    int y = height/16;

    for(int i1 = -1; i1 <= x; ++i1) {
      for(int j1 = -2; j1 <= y; ++j1) {
        blit(matrixStack, k + 16 * i1, l + 16 * j1, 0.0F, 0.0F, 16, 16, 16, 16);
      }
    }

    if (getSelectedNode() != null) {
      drawLinesToMouse(matrixStack, mouseX, mouseY);
    }
    allDialogueNodes.forEach(dialogueNode -> dialogueNode.draw(matrixStack, scrollX, scrollY, mouseX, mouseY));
    RenderSystem.popMatrix();
  }



  private void drawLinesToMouse(MatrixStack matrixStack, double mouseX, double mouseY) {
    if (getSelectedNode() != null) {
      int index = getSelectedNodeIndex();
      DialogueNode selectedNode = getSelectedNode();
      if (index >= 0 && index < selectedNode.getOptionsNames().length+2) {
        int[] nodeXY = index == 0 ? selectedNode.getTextIconLocation() : selectedNode.getOptionIconLocation(index-1);

        int nodeX = selectedNode.getX()+nodeXY[0];
        int nodeY = selectedNode.getY()+nodeXY[1];

        int color = 0xFFFFFF00;

        matrixStack.push();
        {
          matrixStack.translate(selectedNode.getX()+scrollX, selectedNode.getY()+scrollY, 0);
          matrixStack.translate(nodeXY[0], nodeXY[1], 0);
          matrixStack.translate(3.5, 3.5, 0);

          int lineLength = (int) -(nodeX+scrollX-mouseX+9);
          int lineHeight = (int) -(nodeY+scrollY-mouseY+18);

          fill(matrixStack, 0, 0, lineLength / 2, 1, color);
          fill(matrixStack, lineLength / 2, 0, lineLength / 2 + 1, lineHeight, color);
          fill(matrixStack, lineLength / 2 + 1, lineHeight, lineLength, lineHeight + 1, color);
        }
        matrixStack.pop();
      }
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    this.functionListWidget.onScrolled(delta);
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    if (button != 0) {
      this.isScrolling = false;
      return false;
    } else {
      if (!this.isScrolling) {
        this.isScrolling = true;
      } else {
        boolean draggingNode = false;
        for (DialogueNode node : allDialogueNodes) {
          if (node.isDragging()) {
            draggingNode = true;
            break;
          }
        }
        if (!draggingNode) {
          dragGui(dragX, dragY);
        } else {
          allDialogueNodes.forEach(node -> {
            if (node.isDragging()) {
              node.setPosition((int) (-scrollX+mouseX), (int)(-scrollY+mouseY-9));
            }
          });
        }
      }

      return true;
    }
  }

  public void dragGui(double dragX, double dragY) {
    this.scrollX = MathHelper.clamp(this.scrollX + dragX, -(maxX - width), 0.0D);

    this.scrollY = MathHelper.clamp(this.scrollY + dragY, -(maxY - height), 0.0D);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (!this.isAnyTextFieldVisible()) {
      if (button == 0 || button == 1) {
        boolean isMouseOverAnyNode = isMouseOverNode(mouseX, mouseY);
        if (!isMouseOverAnyNode) {
          if (button == 0 && this.getSelectedNode() != null) {
            if (this.selectedNode.isDialogueStart()) {
              this.selectedNode.getDialogue().setResponse("");
            }
            this.selectedNode.calculateDimensions();
            this.selectedNode = null;
          }
          if (button == 1) {
            DialogueNode newNode;
            if (getSelectedNode() != null && getSelectedNodeIndex() == 0) {
              newNode = new DialogueNode(null, this, minecraft, Dialogue.newDialogue());
              newNode.addChild(getSelectedNode().getDialogue());
              getSelectedNode().setParent(newNode);
            } else {
              newNode = new DialogueNode(getSelectedNode(), this, minecraft, Dialogue.newDialogue());
            }
            newNode.setPosition((int) (-scrollX + mouseX - 9), (int) (-scrollY + mouseY - 18));
            if (getSelectedNode() == null || !getSelectedNode().getName().equals(newNode.getName())) {
              this.allDialogueNodes.add(newNode);
            }
            if (this.getSelectedNode() != null) {
              this.selectedNode = null;
            }
          }
        }
        allDialogueNodes.forEach(node -> node.clickedOn(mouseX, mouseY, button, 0, 0, scrollX, scrollY));
      }
    } else if (this.functionListWidget.isVisible()) {
      this.functionListWidget.clickedOn(mouseX, mouseY);
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  private boolean isMouseOverNode(double mouseX, double mouseY) {
    for (DialogueNode node : allDialogueNodes) {
      double minX = 9 + scrollX + node.getX() - 12;
      double maxX = minX + node.getWidth() + 12*2;
      double minY = 18 + 1 + (scrollY + node.getY()) - 9;
      double maxY = minY + node.getHeight() + 9;
      if (mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (!this.isAnyTextFieldVisible()) {
      if (button == 0 || button == 1) {
        allDialogueNodes.forEach(DialogueNode::stopDragging);
      } else if (button == 2) {
        centerScreenOnNode(this.initDialogueNode);
      }
    }
    return super.mouseReleased(mouseX, mouseY, button);
  }

  public void centerScreenOnNode(DialogueNode node) {
    scrollX = -MathHelper.clamp(node.getX() + node.getWidth() / 2 - width / 2, 0, maxX - width);
    scrollY = -MathHelper.clamp(node.getY() + node.getHeight() / 2 - height / 2, 0, maxY - height);
  }

  public int getSelectedNodeIndex() {
    return selectedNodeIndex;
  }

  @Nullable
  public DialogueNode getSelectedNode() {
    return this.selectedNode;
  }

  public void setSelectedNode(@Nullable DialogueNode node, int selectedNodeIndex) {
    this.selectedNode = node;
    this.selectedNodeIndex = selectedNodeIndex;
  }

  public JSONObject buildDialogueEditorJSON() {
    JSONArray entries = new JSONArray();
    // Build an entry out of each node and put it in the array
    for (DialogueNode node : allDialogueNodes) {
      JSONObject entry = new JSONObject();
      entry.put("name", node.getName());
      entry.put("x", node.getX());
      entry.put("y", node.getY());

      entries.put(entry);
    }

    JSONObject object = new JSONObject();
    object.put("entries", entries);

    return object;
  }

  public JSONObject buildDialogueJSON() {
    // Build our array of dialogue entries
    JSONArray entries = new JSONArray();
    for (DialogueNode node : allDialogueNodes) {
      if (node.isDialogueStart()) {
        entries.put(buildDialogueToJSON(node.getDialogue()));
      }
    }

    // Build JSONObject out of the entries array.
    JSONObject object = new JSONObject();
    object.put("entries", entries);

    return object;
  }

  public JSONObject buildDialogueToJSON(Dialogue dialogue) {
    // Dialogue Properties
    String name = dialogue.getName();
    String text = dialogue.getText();
    if (name.length() == 0) {
      throw new InvalidParameterException("This node has no name. How did you even manage to do that?");
    }
    String response = dialogue.getResponse();
    String function = dialogue.getFunction();
    Dialogue[] children = dialogue.getChildren();
    JSONArray childrenAsObjects = new JSONArray();
    for (Dialogue child : children) {
      childrenAsObjects.put(buildDialogueToJSON(child));
    }

    // Build our JSON Object from the properties
    JSONObject dialogueObject = new JSONObject();
    dialogueObject.put("name", name);
    dialogueObject.put("text", text);
    dialogueObject.put("response", response);
    dialogueObject.put("function", function);
    dialogueObject.put("children", childrenAsObjects);

    return dialogueObject;
  }
}
