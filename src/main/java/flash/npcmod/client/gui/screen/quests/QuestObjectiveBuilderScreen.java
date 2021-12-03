package flash.npcmod.client.gui.screen.quests;

import com.mojang.blaze3d.matrix.MatrixStack;
import flash.npcmod.client.gui.widget.DropdownWidget;
import flash.npcmod.core.EntityUtil;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.core.quests.QuestObjectiveTypes;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CRequestContainer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public class QuestObjectiveBuilderScreen extends Screen {

  private DropdownWidget<QuestObjective.ObjectiveType> typeDropdown;
  private DropdownWidget<EntityUtil.LivingEntities> entityTypeDropdown;
  private TextFieldWidget nameField, amountField, primaryObjectiveField, secondaryObjectiveField, runOnCompleteField;
  private CheckboxButton optionalCheckbox, hiddenCheckbox, displayProgressCheckbox;
  private Button itemFromInventoryButton, plusRunOnCompleteButton;
  private Button[] removeRunOnCompletionButtons;

  private QuestEditorScreen questEditorScreen;
  private String originalObjectiveName = "";

  private ItemStack itemStackObjective;
  private EntityType<?> entityObjective;
  private BlockState blockStateObjective;

  private QuestObjective.ObjectiveType objectiveType;
  private String name, primaryObjective, secondaryObjective, currentRunOnComplete;
  private int amount;
  private List<String> runOnComplete;
  private boolean optional, isHidden, displayProgress;

  private int scrollY;

  private static final String TYPE = "Type: ", OPTIONAL = "Optional? ",
      NAME = "Name: ", AMOUNT = "Amount: ",
      HIDDEN = "Hidden by Default? ", DISPLAY_PROGRESS = "Display Progress? ",
      PRIMARY = "Primary Objective: ", SECONDARY = "Secondary Objective: ",
      RUNONCOMPLETE = "Run on completion: ", CANSAVE = "Changes will be saved on exit...",
      CANNOTSAVE = "Changes will NOT be saved on exit...";

  private String tip1, tip2;

  private final Predicate<String> amountFilter = (text) -> {
    if (text.isEmpty()) return true;
    try {
      Integer.parseInt(text);
      return true;
    } catch (NumberFormatException nfe) {
      return false;
    }
  };

  public QuestObjectiveBuilderScreen(QuestEditorScreen questEditorScreen, @Nullable QuestObjective originalObjective, String originalObjectiveName) {
    this(questEditorScreen, originalObjective);
    this.originalObjectiveName = originalObjectiveName;
  }

  public QuestObjectiveBuilderScreen(QuestEditorScreen questEditorScreen, QuestObjective questObjective) {
    super(StringTextComponent.EMPTY);
    this.questEditorScreen = questEditorScreen;
    this.objectiveType = QuestObjective.ObjectiveType.Gather;
    this.name = "";
    this.primaryObjective = "";
    this.secondaryObjective = "";
    this.currentRunOnComplete = "";
    this.itemStackObjective = ItemStack.EMPTY;
    this.displayProgress = true;
    this.runOnComplete = new ArrayList<>();
    this.amount = 1;
    this.tip1 = "";
    this.tip2 = "";

    if (questObjective != null) {
      this.name = questObjective.getName();
      this.amount = questObjective.getAmount();
      if (questObjective.getRunOnComplete() != null)
        this.runOnComplete = questObjective.getRunOnComplete();

      this.isHidden = questObjective.isHidden();
      this.displayProgress = questObjective.shouldDisplayProgress();
      this.optional = questObjective.isOptional();

      if (questObjective.getObjective() instanceof ItemStack) {
        itemStackObjective = questObjective.getObjective();
      } else if (questObjective.getObjective() instanceof LivingEntity) {
        entityObjective = ((LivingEntity) questObjective.getObjective()).getType();
      } else if (questObjective.getObjective() instanceof BlockState) {
        blockStateObjective = questObjective.getObjective();
      }
      primaryObjective = questObjective.primaryToString();

      if (questObjective.getSecondaryObjective() instanceof ItemStack) {
        itemStackObjective = questObjective.getSecondaryObjective();
      } else if (questObjective.getSecondaryObjective() instanceof LivingEntity) {
        entityObjective = ((LivingEntity) questObjective.getSecondaryObjective()).getType();
      } else if (questObjective.getSecondaryObjective() instanceof BlockState) {
        blockStateObjective = questObjective.getSecondaryObjective();
      }
      secondaryObjective = questObjective.secondaryToString();

      objectiveType = questObjective.getType();
    }
  }

  @Override
  protected void init() {
    this.optionalCheckbox = this.addButton(new CheckboxButton(5+font.getStringWidth(TYPE)+120+font.getStringWidth(OPTIONAL), 0, 20, 20, StringTextComponent.EMPTY, optional));


    this.nameField = this.addButton(new TextFieldWidget(font, 5+font.getStringWidth(NAME), 25-6, 100, 20, StringTextComponent.EMPTY));
    this.nameField.setText(name);
    this.nameField.setResponder(this::setName);
    this.nameField.setMaxStringLength(250);
    this.nameField.setCanLoseFocus(true);

    this.amountField = this.addButton(new TextFieldWidget(font, 5+font.getStringWidth(NAME)+105+font.getStringWidth(AMOUNT), 25-6, 100, 20, StringTextComponent.EMPTY));
    this.amountField.setText(String.valueOf(amount));
    this.amountField.setValidator(amountFilter);
    this.amountField.setResponder(this::setAmount);
    this.amountField.setMaxStringLength(3);
    this.amountField.setCanLoseFocus(true);


    this.hiddenCheckbox = this.addButton(new CheckboxButton(5+font.getStringWidth(HIDDEN), 52-6, 20, 20, StringTextComponent.EMPTY, isHidden));
    this.displayProgressCheckbox = this.addButton(new CheckboxButton(5+font.getStringWidth(HIDDEN)+25+font.getStringWidth(DISPLAY_PROGRESS), 52-6, 20, 20, StringTextComponent.EMPTY, displayProgress));

    this.primaryObjectiveField = this.addButton(new TextFieldWidget(font, 5+font.getStringWidth(PRIMARY), 82-6, 100, 20, StringTextComponent.EMPTY));
    this.primaryObjectiveField.setText(primaryObjective);
    this.primaryObjectiveField.setResponder(this::setPrimaryObjective);
    this.primaryObjectiveField.setMaxStringLength(600);
    this.primaryObjectiveField.setCanLoseFocus(true);

    this.secondaryObjectiveField = this.addButton(new TextFieldWidget(font, 5+font.getStringWidth(SECONDARY), 112-6, 100, 20, StringTextComponent.EMPTY));
    this.secondaryObjectiveField.setText(secondaryObjective);
    this.secondaryObjectiveField.setResponder(this::setSecondaryObjective);
    this.secondaryObjectiveField.setMaxStringLength(600);
    this.secondaryObjectiveField.setCanLoseFocus(true);

    this.itemFromInventoryButton = this.addButton(new Button(5+font.getStringWidth(PRIMARY), 82-6, 100, 20, new StringTextComponent("From Inventory"), btn -> {
      String jsons = create().toJson().toString() + "::::::::::" + questEditorScreen.build().toJson().toString() + (originalObjectiveName == null ? "" : "::::::::::" + originalObjectiveName);
      PacketDispatcher.sendToServer(new CRequestContainer(jsons, CRequestContainer.ContainerType.OBJECTIVE_STACK_SELECTOR));
    }));

    this.runOnCompleteField = this.addButton(new TextFieldWidget(font, 5+font.getStringWidth(RUNONCOMPLETE), 142-6, 100, 20, StringTextComponent.EMPTY));
    this.runOnCompleteField.setText(currentRunOnComplete);
    this.runOnCompleteField.setResponder(this::setRunOnComplete);
    this.runOnCompleteField.setMaxStringLength(500);
    this.runOnCompleteField.setCanLoseFocus(true);
    this.plusRunOnCompleteButton = this.addButton(new Button(5+font.getStringWidth(RUNONCOMPLETE)+105, 142-6, 20, 20, new StringTextComponent("+"), btn -> {
      if (canAddRunOnComplete()) {
        this.runOnComplete.add(currentRunOnComplete);
        runOnCompleteField.setText("");
      }
    }));

    EntityUtil.LivingEntities entity = entityObjective != null ? EntityUtil.LivingEntities.valueOf(EntityType.getKey(entityObjective).toString().replaceAll(":", "_")) : EntityUtil.LivingEntities.valueOf("minecraft_pig");
    this.entityTypeDropdown = this.addButton(new DropdownWidget<>(entity, 5+font.getStringWidth(PRIMARY), 82-2, 200, 10));
    this.typeDropdown = this.addButton(new DropdownWidget<>(objectiveType, 5+font.getStringWidth(TYPE), 3, 100));

    this.removeRunOnCompletionButtons = new Button[6];
    for (int i = 0; i < removeRunOnCompletionButtons.length; i++) {
      int j = i;
      this.removeRunOnCompletionButtons[i] = this.addButton(new Button(5, 161+i*12, 10, 10, new StringTextComponent("-"), btn -> {
        removeRunOnComplete(j);
      }));
    }

    this.addButton(new Button(width-100, 0, 100, 20, new StringTextComponent("Cancel"), btn -> {
      minecraft.displayGuiScreen(questEditorScreen);
    }));
  }

  private void removeRunOnComplete(int i) {
    if (i+scrollY < runOnComplete.size()) {
      runOnComplete.remove(i+scrollY);
      scrollY = clampScroll(scrollY-1);
    }
  }

  private boolean canAddRunOnComplete() {
    return currentRunOnComplete.length() > 0 || currentRunOnComplete.startsWith("hide:") || currentRunOnComplete.startsWith("unhide:") || currentRunOnComplete.startsWith("forceComplete");
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setAmount(String amount) {
    try {
      this.amount = Integer.parseInt(amount);
    } catch (NumberFormatException ignored) {
      this.amount = 0;
    }
  }

  public void setPrimaryObjective(String primaryObjective) {
    this.primaryObjective = primaryObjective;
  }

  public void setSecondaryObjective(String secondaryObjective) {
    this.secondaryObjective = secondaryObjective;
  }

  public void setRunOnComplete(String runOnComplete) {
    this.currentRunOnComplete = runOnComplete;
  }

  @Override
  public void tick() {
    this.nameField.tick();
    this.amountField.tick();
    this.primaryObjectiveField.tick();
    this.secondaryObjectiveField.tick();
    this.runOnCompleteField.tick();

    for (int i = 0; i < removeRunOnCompletionButtons.length; i++) {
      removeRunOnCompletionButtons[i].visible = runOnComplete.size() > i+scrollY;
    }

    this.objectiveType = typeDropdown.getSelectedOption();

    primaryObjectiveField.visible = !typeDropdown.isShowingOptions() && (
        typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Talk)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Find)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Scoreboard));
    secondaryObjectiveField.visible = !typeDropdown.isShowingOptions() && !entityTypeDropdown.isShowingOptions() && (
        typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.DeliverToLocation)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Talk)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Scoreboard)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.UseOnBlock));

    if (typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Talk)) {
      tip1 = "NPC Name";
      tip2 = "Dialouge Name";
    } else if (typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Scoreboard)) {
      tip1 = "Objective Name";
      tip2 = "min|max|exact";
    } else if (typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Find)) {
      tip1 = "x1;y1;z1,x2;y2;z2";
      tip2 = "";
    } else if (typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.DeliverToLocation)) {
      tip1 = "";
      tip2 = "x1;y1;z1,x2;y2;z2";
    } else if (typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.UseOnBlock)) {
      tip1 = "";
      tip2 = "Blockstate";
    } else { tip1 = ""; tip2 = ""; }

    if (typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.UseOnBlock)) {
      blockStateObjective = QuestObjectiveTypes.blockStateFromString(secondaryObjective);
    }

    boolean isPrimaryEntityObjective =
        typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Kill);
    boolean isSecondaryEntityObjective =
        typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.DeliverToEntity)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.UseOnEntity);
    entityTypeDropdown.active = !typeDropdown.isShowingOptions();
    entityTypeDropdown.visible = isPrimaryEntityObjective || isSecondaryEntityObjective;
    if (isPrimaryEntityObjective) {
      entityTypeDropdown.x = 5 + font.getStringWidth(PRIMARY);
      entityTypeDropdown.y = 82 - 2;
      entityObjective = entityTypeDropdown.getSelectedOption().entityType;
    } else if (isSecondaryEntityObjective) {
      entityTypeDropdown.x = 5 + font.getStringWidth(SECONDARY);
      entityTypeDropdown.y = 112 - 2;
      entityObjective = entityTypeDropdown.getSelectedOption().entityType;
    }

    boolean isPrimaryItemObjective =
        typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Gather)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.DeliverToEntity)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.DeliverToLocation)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.UseOnEntity)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.UseOnBlock)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Use);
    itemFromInventoryButton.visible = isPrimaryItemObjective;
    itemFromInventoryButton.active = !typeDropdown.isShowingOptions();


    boolean isNotEscortObjective = !objectiveType.equals(QuestObjective.ObjectiveType.Escort);

    nameField.visible = isNotEscortObjective && !typeDropdown.isShowingOptions();
    amountField.visible = isNotEscortObjective;
    runOnCompleteField.visible = isNotEscortObjective && !typeDropdown.isShowingOptions() && !entityTypeDropdown.isShowingOptions();
    plusRunOnCompleteButton.visible = isNotEscortObjective;
    plusRunOnCompleteButton.active = canAddRunOnComplete() && !typeDropdown.isShowingOptions() && !entityTypeDropdown.isShowingOptions();

    optionalCheckbox.visible = isNotEscortObjective;
    hiddenCheckbox.visible = isNotEscortObjective;
    displayProgressCheckbox.visible = isNotEscortObjective;

    this.optional = optionalCheckbox.isChecked();
    this.isHidden = hiddenCheckbox.isChecked();
    this.displayProgress = displayProgressCheckbox.isChecked();
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    renderBackground(matrixStack);

    drawString(matrixStack, font, TYPE, 5, 5, 0xFFFFFF);
    drawString(matrixStack, font, OPTIONAL, 5+font.getStringWidth(TYPE)+120, 5, 0xFFFFFF);

    drawString(matrixStack, font, NAME, 5, 25, 0xFFFFFF);
    drawString(matrixStack, font, AMOUNT, 5+font.getStringWidth(NAME)+105, 25, 0xFFFFFF);

    drawString(matrixStack, font, HIDDEN, 5, 52, 0xFFFFFF);
    drawString(matrixStack, font, DISPLAY_PROGRESS, 5+font.getStringWidth(HIDDEN)+25, 52, 0xFFFFFF);

    drawString(matrixStack, font, PRIMARY, 5, 82, 0xFFFFFF);
    drawString(matrixStack, font, SECONDARY, 5, 112, 0xFFFFFF);

    if (!tip1.isEmpty())
      drawString(matrixStack, font, tip1, 5 + font.getStringWidth(PRIMARY) + 105, 82, 0xFFFFFF);
    if (!tip2.isEmpty())
      drawString(matrixStack, font, tip2, 5 + font.getStringWidth(SECONDARY) + 105, 112, 0xFFFFFF);

    if (itemStackObjective != null && !itemStackObjective.isEmpty() && itemFromInventoryButton.visible) {
      int x = 5 + font.getStringWidth(PRIMARY) + 105;
      int y = 82 - 4;
      minecraft.getItemRenderer().renderItemAndEffectIntoGuiWithoutEntity(itemStackObjective, x, y);
      if (mouseX >= x && mouseX <= x+16 && mouseY >= y && mouseY <= y+16) {
        this.renderTooltip(matrixStack, itemStackObjective, mouseX, mouseY);
      }
    }

    drawString(matrixStack, font, RUNONCOMPLETE, 5, 142, 0xFFFFFF);
    for (int i = 0; i < Math.min(6, runOnComplete.size()); i++) {
      if (i+scrollY < runOnComplete.size())
        drawString(matrixStack, font, runOnComplete.get(i+scrollY), 20, 162+i*12, 0xFFFFFF);
    }

    boolean canSave = !name.isEmpty() && canCreateObjective();
    String save = canSave ? CANSAVE : CANNOTSAVE;
    int color = canSave ? 0x00FF00 : 0xFF0000;

    drawString(matrixStack, font, save, width-2-font.getStringWidth(save), height-font.FONT_HEIGHT-2, color);

    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }

  private boolean canCreateObjective() {
    switch (typeDropdown.getSelectedOption()) {
      case Gather:
      case Use:
        return itemStackObjective != null && !itemStackObjective.isEmpty() && amount > 0;
      case Kill:
        return entityObjective != null && amount > 0;
      case DeliverToEntity:
      case UseOnEntity:
        return itemStackObjective != null && !itemStackObjective.isEmpty() && entityObjective != null && amount > 0;
      case DeliverToLocation:
        return itemStackObjective != null && !itemStackObjective.isEmpty() && canConvertToArea(secondaryObjective) && amount > 0;
      case Escort:
        return false; // TODO
      case Talk:
        return !primaryObjective.isEmpty() && !secondaryObjective.isEmpty();
      case Find:
        return canConvertToArea(primaryObjective);
      case UseOnBlock:
        return itemStackObjective != null && !itemStackObjective.isEmpty() && blockStateObjective != null && !blockStateObjective.getBlock().equals(Blocks.AIR) && amount > 0;
      case Scoreboard:
        return !primaryObjective.isEmpty();
    }
    return false;
  }

  @Nullable
  private QuestObjective create() {
    QuestObjective questObjective = null;
    int id = questEditorScreen.objectives.size();
    if (originalObjectiveName != null && !originalObjectiveName.isEmpty()) {
      for (QuestObjective objective : questEditorScreen.objectives) {
        if (objective.getName().equals(originalObjectiveName)) {
          id = objective.getId();
          break;
        }
      }
    }

    switch (typeDropdown.getSelectedOption()) {
      case Gather:
        questObjective = new QuestObjectiveTypes.GatherObjective(id, name, itemStackObjective, amount);
        break;
      case Kill:
        questObjective = new QuestObjectiveTypes.KillObjective(id, name, EntityType.getKey(entityObjective).toString(), amount);
        break;
      case DeliverToEntity:
        questObjective = new QuestObjectiveTypes.DeliverToEntityObjective(id, name, itemStackObjective, EntityType.getKey(entityObjective).toString(), amount);
        break;
      case DeliverToLocation:
        questObjective = new QuestObjectiveTypes.DeliverToLocationObjective(id, name, itemStackObjective, QuestObjectiveTypes.areaFromString(secondaryObjective), amount);
        break;
      case Escort:
        // TODO questObjective = new QuestObjectiveTypes.EscortObjective(id, name, primaryObjective, Path.fromString(secondaryObjective));
        break;
      case Talk:
        questObjective = new QuestObjectiveTypes.TalkObjective(id, name, primaryObjective, secondaryObjective);
        break;
      case Find:
        questObjective = new QuestObjectiveTypes.FindObjective(id, name, QuestObjectiveTypes.areaFromString(primaryObjective));
        break;
      case UseOnEntity:
        questObjective = new QuestObjectiveTypes.UseOnEntityObjective(id, name, itemStackObjective, EntityType.getKey(entityObjective).toString(), amount);
        break;
      case UseOnBlock:
        questObjective = new QuestObjectiveTypes.UseOnBlockObjective(id, name, itemStackObjective, blockStateObjective, amount);
        break;
      case Use:
        questObjective = new QuestObjectiveTypes.UseObjective(id, name, itemStackObjective, amount);
        break;
      case Scoreboard:
        questObjective = new QuestObjectiveTypes.ScoreboardObjective(id, name, primaryObjective, amount);
        break;
    }
    if (questObjective != null) {
      questObjective.setRunOnComplete(this.runOnComplete);
      questObjective.setOptional(optional);
      questObjective.setHidden(isHidden);
      questObjective.setShouldDisplayProgress(displayProgress);
    }
    return questObjective;
  }

  @Override
  public void closeScreen() {
    if (!name.isEmpty() && canCreateObjective()) {
      QuestObjective questObjective = create();

      if (questObjective != null) {
        int j = -1;
        if (originalObjectiveName != null && !originalObjectiveName.isEmpty()) {
          for (int i = 0; i < questEditorScreen.objectives.size(); i++) {
            QuestObjective objective = questEditorScreen.objectives.get(i);
            if (objective.getName().equals(originalObjectiveName)) {
              j = i;
              break;
            }
          }
          if (j >= 0 && j < questEditorScreen.objectives.size())
            questEditorScreen.objectives.set(j, questObjective);
        } else {
          for (int i = 0; i < questEditorScreen.objectives.size(); i++) {
            QuestObjective objective = questEditorScreen.objectives.get(i);
            if (objective.getName().equals(originalObjectiveName)) {
              j = i;
              break;
            }
          }
          if (j >= 0 && j < questEditorScreen.objectives.size())
            questEditorScreen.objectives.set(j, questObjective);
          else
            questEditorScreen.objectives.add(questObjective);
        }
      }

    }

    minecraft.displayGuiScreen(questEditorScreen);
  }

  private boolean canConvertToArea(String s) {
    if (s.isEmpty() || !s.contains(",") || !s.contains(";")) return false;

    String[] split = s.split(",");
    if (split.length != 2 || !split[0].contains(";") || !split[1].contains(";")) return false;

    String[] firstSplit = split[0].split(";");
    String[] secondSplit = split[1].split(";");
    if (firstSplit.length != 3 || secondSplit.length != 3) return false;

    for (String coord : firstSplit) {
      try {
        Integer.parseInt(coord);
      } catch (NumberFormatException nfe) {
        return false;
      }
    }
    for (String coord : secondSplit) {
      try {
        Integer.parseInt(coord);
      } catch (NumberFormatException nfe) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta > 0) {
      this.scrollY = clampScroll(scrollY - 1);
    } else {
      this.scrollY = clampScroll(scrollY + 1);
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  public int clampScroll(int newScroll) {
    int max = runOnComplete.size()-6;
    if (max > 0)
      return MathHelper.clamp(newScroll, 0, max);
    else
      return 0;
  }
}
