package flash.npcmod.client.gui.screen.quests;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.StringReader;
import flash.npcmod.Main;
import flash.npcmod.client.gui.widget.EntityDropdownWidget;
import flash.npcmod.client.gui.widget.EnumDropdownWidget;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.core.quests.QuestObjectiveTypes;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CRequestContainer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public class QuestObjectiveBuilderScreen extends Screen {

  private EnumDropdownWidget<QuestObjective.ObjectiveType> typeDropdown;
  private EntityDropdownWidget entityDropdown;
  private EditBox nameField, amountField, primaryObjectiveField, secondaryObjectiveField, entityObjectiveTagField, runOnCompleteField;
  private Checkbox optionalCheckbox, hiddenCheckbox, displayProgressCheckbox;
  private Button itemFromInventoryButton, plusRunOnCompleteButton;
  private Button[] removeRunOnCompletionButtons;

  private QuestEditorScreen questEditorScreen;
  private String originalObjectiveName = "";

  private ItemStack itemStackObjective;
  private EntityType<?> entityObjective;
  private CompoundTag entityObjectiveTag;
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
    super(TextComponent.EMPTY);
    this.questEditorScreen = questEditorScreen;
    this.objectiveType = QuestObjective.ObjectiveType.Gather;
    this.name = "";
    this.primaryObjective = "";
    this.secondaryObjective = "";
    this.currentRunOnComplete = "";
    this.itemStackObjective = ItemStack.EMPTY;
    this.displayProgress = true;
    this.runOnComplete = new ArrayList<>();
    this.entityObjectiveTag = new CompoundTag();
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
      } else if (questObjective.getObjective() instanceof BlockState) {
        blockStateObjective = questObjective.getObjective();
      }
      primaryObjective = questObjective.primaryToString();

      if (questObjective.getSecondaryObjective() instanceof ItemStack) {
        itemStackObjective = questObjective.getSecondaryObjective();
      } else if (questObjective.getSecondaryObjective() instanceof BlockState) {
        blockStateObjective = questObjective.getSecondaryObjective();
      }
      secondaryObjective = questObjective.secondaryToString();

      if (isEntityObjective(questObjective)) {
        // TODO class EntityQuestObjective extends QuestObjective
        switch (questObjective.getType()) {
          case Kill -> {
            QuestObjectiveTypes.KillObjective killObjective = (QuestObjectiveTypes.KillObjective) questObjective;
            try {
              entityObjective = EntityType.byString(killObjective.getEntityKey()).get();
            } catch (Exception e) {
              entityObjective = EntityType.PIG;
            }
            entityObjectiveTag = killObjective.getEntityTag();
          }
          case UseOnEntity -> {
            QuestObjectiveTypes.UseOnEntityObjective useOnEntityObjective = (QuestObjectiveTypes.UseOnEntityObjective) questObjective;
            try {
              entityObjective = EntityType.byString(useOnEntityObjective.getEntityKey()).get();
            } catch (Exception e) {
              entityObjective = EntityType.PIG;
            }
            entityObjectiveTag = useOnEntityObjective.getEntityTag();
          }
          case DeliverToEntity -> {
            QuestObjectiveTypes.DeliverToEntityObjective deliverToEntityObjective = (QuestObjectiveTypes.DeliverToEntityObjective) questObjective;
            try {
              entityObjective = EntityType.byString(deliverToEntityObjective.getEntityKey()).get();
            } catch (Exception e) {
              entityObjective = EntityType.PIG;
            }
            entityObjectiveTag = deliverToEntityObjective.getEntityTag();
          }
        }
      }

      objectiveType = questObjective.getType();
    }
  }

  private boolean isEntityObjective(QuestObjective objective) {
    return switch (objective.getType()) {
      case Kill, UseOnEntity, DeliverToEntity -> true;
      default -> false;
    };
  }

  @Override
  protected void init() {
    this.optionalCheckbox = this.addRenderableWidget(new Checkbox(5+font.width(TYPE)+120+font.width(OPTIONAL), 0, 20, 20, TextComponent.EMPTY, optional));


    this.nameField = this.addRenderableWidget(new EditBox(font, 5+font.width(NAME), 25-6, 100, 20, TextComponent.EMPTY));
    this.nameField.setValue(name);
    this.nameField.setResponder(this::setName);
    this.nameField.setMaxLength(250);
    this.nameField.setCanLoseFocus(true);

    this.amountField = this.addRenderableWidget(new EditBox(font, 5+font.width(NAME)+105+font.width(AMOUNT), 25-6, 100, 20, TextComponent.EMPTY));
    this.amountField.setValue(String.valueOf(amount));
    this.amountField.setFilter(amountFilter);
    this.amountField.setResponder(this::setAmount);
    this.amountField.setMaxLength(3);
    this.amountField.setCanLoseFocus(true);


    this.hiddenCheckbox = this.addRenderableWidget(new Checkbox(5+font.width(HIDDEN), 52-6, 20, 20, TextComponent.EMPTY, isHidden));
    this.displayProgressCheckbox = this.addRenderableWidget(new Checkbox(5+font.width(HIDDEN)+25+font.width(DISPLAY_PROGRESS), 52-6, 20, 20, TextComponent.EMPTY, displayProgress));

    this.primaryObjectiveField = this.addRenderableWidget(new EditBox(font, 5+font.width(PRIMARY), 82-6, 100, 20, TextComponent.EMPTY));
    this.primaryObjectiveField.setResponder(this::setPrimaryObjective);
    this.primaryObjectiveField.setValue(primaryObjective);
    this.primaryObjectiveField.setMaxLength(600);
    this.primaryObjectiveField.setCanLoseFocus(true);

    this.secondaryObjectiveField = this.addRenderableWidget(new EditBox(font, 5+font.width(SECONDARY), 112-6, 100, 20, TextComponent.EMPTY));
    this.secondaryObjectiveField.setResponder(this::setSecondaryObjective);
    this.secondaryObjectiveField.setValue(secondaryObjective);
    this.secondaryObjectiveField.setMaxLength(600);
    this.secondaryObjectiveField.setCanLoseFocus(true);

    this.entityObjectiveTagField = this.addRenderableWidget(new EditBox(font, 5+font.width(PRIMARY) + 200, 82-6, 100, 20, TextComponent.EMPTY));
    this.entityObjectiveTagField.setResponder(this::setEntityObjectiveTag);
    if (this.entityObjectiveTag.contains("id"))
      entityObjectiveTag.remove("id");
    this.entityObjectiveTagField.setValue(entityObjectiveTag.getAsString());
    this.secondaryObjectiveField.setMaxLength(1000);
    this.secondaryObjectiveField.setCanLoseFocus(true);

    this.itemFromInventoryButton = this.addRenderableWidget(new Button(5+font.width(PRIMARY), 82-6, 100, 20, new TextComponent("From Inventory"), btn -> {
      String jsons = create().toJson().toString() + "::::::::::" + questEditorScreen.build().toJson().toString() + (originalObjectiveName == null ? "" : "::::::::::" + originalObjectiveName);
      PacketDispatcher.sendToServer(new CRequestContainer(jsons, CRequestContainer.ContainerType.OBJECTIVE_STACK_SELECTOR));
    }));

    this.runOnCompleteField = this.addRenderableWidget(new EditBox(font, 5+font.width(RUNONCOMPLETE), 142-6, 100, 20, TextComponent.EMPTY));
    this.runOnCompleteField.setValue(currentRunOnComplete);
    this.runOnCompleteField.setResponder(this::setRunOnComplete);
    this.runOnCompleteField.setMaxLength(500);
    this.runOnCompleteField.setCanLoseFocus(true);
    this.plusRunOnCompleteButton = this.addRenderableWidget(new Button(5+font.width(RUNONCOMPLETE)+105, 142-6, 20, 20, new TextComponent("+"), btn -> {
      if (canAddRunOnComplete()) {
        this.runOnComplete.add(currentRunOnComplete);
        runOnCompleteField.setValue("");
      }
    }));

    EntityType<?> entity = entityObjective != null ? entityObjective : EntityType.PIG;
    this.entityDropdown = this.addRenderableWidget(new EntityDropdownWidget(entity, 5+font.width(PRIMARY), 82-2, 200, 10, false));
    this.typeDropdown = this.addRenderableWidget(new EnumDropdownWidget<>(objectiveType, 5+font.width(TYPE), 3, 100));

    this.removeRunOnCompletionButtons = new Button[6];
    for (int i = 0; i < removeRunOnCompletionButtons.length; i++) {
      int j = i;
      this.removeRunOnCompletionButtons[i] = this.addRenderableWidget(new Button(5, 161+i*12, 10, 10, new TextComponent("-"), btn -> {
        removeRunOnComplete(j);
      }));
    }

    this.addRenderableWidget(new Button(width-100, 0, 100, 20, new TextComponent("Cancel"), btn -> {
      minecraft.setScreen(questEditorScreen);
    }));
  }

  private void removeRunOnComplete(int i) {
    if (i+scrollY < runOnComplete.size()) {
      runOnComplete.remove(i+scrollY);
      scrollY = clampScroll(scrollY-1);
    }
  }

  private boolean canAddRunOnComplete() {
    return currentRunOnComplete.startsWith("/") || currentRunOnComplete.startsWith("hide:") || currentRunOnComplete.startsWith("unhide:") || currentRunOnComplete.startsWith("forceComplete");
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

  public void setEntityObjectiveTag(String s) {
    if (s.isEmpty()) {
      entityObjectiveTag = new CompoundTag();
    } else {
      try {
        entityObjectiveTag = new TagParser(new StringReader(s)).readStruct();
      } catch (Exception ignored) {
        entityObjectiveTag = new CompoundTag();
      }
    }
  }

  private String getActualEntityObjective() {
    String actualEntityObjective = EntityType.getKey(entityObjective) + "::" + entityObjectiveTag.getAsString();
    return actualEntityObjective;
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
    this.entityObjectiveTagField.tick();
    this.runOnCompleteField.tick();

    for (int i = 0; i < removeRunOnCompletionButtons.length; i++) {
      removeRunOnCompletionButtons[i].visible = runOnComplete.size() > i+scrollY;
    }

    this.objectiveType = typeDropdown.getSelectedOption();

    primaryObjectiveField.visible = !typeDropdown.isShowingOptions() && (
        typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Talk)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Find)
            || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Scoreboard));
    secondaryObjectiveField.visible = !typeDropdown.isShowingOptions() && !entityDropdown.isShowingOptions() && (
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
    entityDropdown.active = !typeDropdown.isShowingOptions();
    entityDropdown.visible = isPrimaryEntityObjective || isSecondaryEntityObjective;
    entityObjectiveTagField.visible = isPrimaryEntityObjective || isSecondaryEntityObjective;
    if (isPrimaryEntityObjective) {
      entityDropdown.x = 5 + font.width(PRIMARY);
      entityDropdown.y = 82 - 2;
      entityObjective = entityDropdown.getSelectedType();
      entityObjectiveTagField.x = entityDropdown.x + 210;
      entityObjectiveTagField.y = entityDropdown.y - 4;
    } else if (isSecondaryEntityObjective) {
      entityDropdown.x = 5 + font.width(SECONDARY);
      entityDropdown.y = 112 - 2;
      entityObjective = entityDropdown.getSelectedType();
      entityObjectiveTagField.x = entityDropdown.x + 210;
      entityObjectiveTagField.y = entityDropdown.y - 4;
    }

    itemFromInventoryButton.visible = typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Gather)
        || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.DeliverToEntity)
        || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.DeliverToLocation)
        || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.UseOnEntity)
        || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.UseOnBlock)
        || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.Use)
        || typeDropdown.getSelectedOption().equals(QuestObjective.ObjectiveType.CraftItem);
    itemFromInventoryButton.active = !typeDropdown.isShowingOptions();


    boolean isNotEscortObjective = !objectiveType.equals(QuestObjective.ObjectiveType.Escort);

    nameField.visible = isNotEscortObjective && !typeDropdown.isShowingOptions();
    amountField.visible = isNotEscortObjective;
    runOnCompleteField.visible = isNotEscortObjective && !typeDropdown.isShowingOptions() && !entityDropdown.isShowingOptions();
    plusRunOnCompleteButton.visible = isNotEscortObjective;
    plusRunOnCompleteButton.active = canAddRunOnComplete() && !typeDropdown.isShowingOptions() && !entityDropdown.isShowingOptions();

    optionalCheckbox.visible = isNotEscortObjective;
    hiddenCheckbox.visible = isNotEscortObjective;
    displayProgressCheckbox.visible = isNotEscortObjective;

    this.optional = optionalCheckbox.selected();
    this.isHidden = hiddenCheckbox.selected();
    this.displayProgress = displayProgressCheckbox.selected();
  }

  @Override
  public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    renderBackground(matrixStack);

    drawString(matrixStack, font, TYPE, 5, 5, 0xFFFFFF);
    drawString(matrixStack, font, OPTIONAL, 5+font.width(TYPE)+120, 5, 0xFFFFFF);

    drawString(matrixStack, font, NAME, 5, 25, 0xFFFFFF);
    drawString(matrixStack, font, AMOUNT, 5+font.width(NAME)+105, 25, 0xFFFFFF);

    drawString(matrixStack, font, HIDDEN, 5, 52, 0xFFFFFF);
    drawString(matrixStack, font, DISPLAY_PROGRESS, 5+font.width(HIDDEN)+25, 52, 0xFFFFFF);

    drawString(matrixStack, font, PRIMARY, 5, 82, 0xFFFFFF);
    drawString(matrixStack, font, SECONDARY, 5, 112, 0xFFFFFF);

    if (!tip1.isEmpty())
      drawString(matrixStack, font, tip1, 5 + font.width(PRIMARY) + 105, 82, 0xFFFFFF);
    if (!tip2.isEmpty())
      drawString(matrixStack, font, tip2, 5 + font.width(SECONDARY) + 105, 112, 0xFFFFFF);

    if (itemStackObjective != null && !itemStackObjective.isEmpty() && itemFromInventoryButton.visible) {
      int x = 5 + font.width(PRIMARY) + 105;
      int y = 82 - 4;
      minecraft.getItemRenderer().renderAndDecorateFakeItem(itemStackObjective, x, y);
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

    drawString(matrixStack, font, save, width-2-font.width(save), height-font.lineHeight-2, color);

    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }

  private boolean canCreateObjective() {
    return switch (typeDropdown.getSelectedOption()) {
      case Gather, Use, CraftItem -> itemStackObjective != null && !itemStackObjective.isEmpty() && amount > 0;
      case Kill -> entityObjective != null && amount > 0;
      case DeliverToEntity, UseOnEntity ->
              itemStackObjective != null && !itemStackObjective.isEmpty() && entityObjective != null && amount > 0;
      case DeliverToLocation ->
              itemStackObjective != null && !itemStackObjective.isEmpty() && canConvertToArea(secondaryObjective) && amount > 0;
      case Escort -> false; // TODO
      case Talk -> !primaryObjective.isEmpty() && !secondaryObjective.isEmpty();
      case Find -> canConvertToArea(primaryObjective);
      case UseOnBlock ->
              itemStackObjective != null && !itemStackObjective.isEmpty() && blockStateObjective != null && !blockStateObjective.getBlock().equals(Blocks.AIR) && amount > 0;
      case Scoreboard -> !primaryObjective.isEmpty();
    };
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
        questObjective = new QuestObjectiveTypes.KillObjective(id, name, getActualEntityObjective(), amount);
        break;
      case DeliverToEntity:
        questObjective = new QuestObjectiveTypes.DeliverToEntityObjective(id, name, itemStackObjective, getActualEntityObjective(), amount);
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
        questObjective = new QuestObjectiveTypes.UseOnEntityObjective(id, name, itemStackObjective, getActualEntityObjective(), amount);
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
      case CraftItem:
        questObjective = new QuestObjectiveTypes.CraftItemObjective(id, name, itemStackObjective, amount);
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
  public void onClose() {
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

    minecraft.setScreen(questEditorScreen);
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
      return Mth.clamp(newScroll, 0, max);
    else
      return 0;
  }
}
