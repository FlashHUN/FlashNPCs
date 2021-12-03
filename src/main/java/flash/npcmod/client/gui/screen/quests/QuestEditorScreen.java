package flash.npcmod.client.gui.screen.quests;

import com.mojang.blaze3d.matrix.MatrixStack;
import flash.npcmod.core.client.quests.ClientQuestUtil;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CBuildQuest;
import flash.npcmod.network.packets.client.CRequestContainer;
import flash.npcmod.network.packets.client.CRequestQuestInfo;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public class QuestEditorScreen extends Screen {

  private TextFieldWidget nameField, displayNameField, xpRewardField, runOnCompleteField, loadQuestField;
  private CheckboxButton repeatableCheckbox;
  private Button objectiveBuilderButton, itemsFromInventoryButton, plusRunOnCompleteButton, confirmButton, loadButton;
  private Button[] objectiveEditButton, objectiveRemoveButton, removeRunOnCompleteButton;

  private int objectiveId = 0;

  private String name;
  private String displayName;
  protected List<QuestObjective> objectives;
  private int xpReward;
  private List<ItemStack> itemRewards;
  private boolean repeatable;
  private List<String> runOnComplete;
  private String currentRunOnComplete;

  private String questToLoad;

  private int objectiveScrollY, runOnCompleteScrollY;
  private static final int objectiveDisplayLimit = 6, runOnCompleteDisplayLimit = 8;

  private static final String NAME = "Name: ", DISPLAYNAME = "Display name: ",
      OBJECTIVES = "Objectives: ", XPREWARD = "XP Reward: ", ITEMREWARDS = "Item Rewards: ",
      REPEATABLE = "Repeatable? ", RUNONCOMPLETE = "Run on completion: ", DOTS = "... ";

  private final Predicate<String> nameFilter = (text) -> {
    Pattern pattern = Pattern.compile("\\s");
    Matcher matcher = pattern.matcher(text);
    return !matcher.find();
  };

  public QuestEditorScreen() {
    super(StringTextComponent.EMPTY);

    questToLoad = "";

    this.name = "";
    this.displayName = "";
    this.objectives = new ArrayList<>();
    this.xpReward = 0;
    this.itemRewards = new ArrayList<>();
    this.repeatable = true;
    this.runOnComplete = new ArrayList<>();
    this.currentRunOnComplete = "";
  }

  public int nextID() {
    return objectiveId++;
  }

  public static QuestEditorScreen fromQuest(Quest quest) {
    QuestEditorScreen questEditorScreen = new QuestEditorScreen();

    questEditorScreen.name = quest.getName();
    questEditorScreen.displayName = quest.getDisplayName();

    for (QuestObjective questObjective : quest.getObjectives()) {
      questEditorScreen.objectives.add(questObjective);
    }

    questEditorScreen.xpReward = quest.getXpReward();
    questEditorScreen.itemRewards = quest.getItemRewards();

    questEditorScreen.repeatable = quest.isRepeatable();

    questEditorScreen.runOnComplete = quest.getRunOnComplete();

    return questEditorScreen;
  }

  public void updateObjectiveId() {
    objectiveId = objectives.size();
  }

  @Override
  protected void init() {
    this.nameField = this.addButton(new TextFieldWidget(font, 5+font.getStringWidth(NAME), 5, 120, 20, StringTextComponent.EMPTY));
    this.nameField.setValidator(nameFilter);
    this.nameField.setResponder(this::setName);
    this.nameField.setText(name);
    this.nameField.setMaxStringLength(50);
    this.nameField.setCanLoseFocus(true);

    this.displayNameField = this.addButton(new TextFieldWidget(font, 5+font.getStringWidth(DISPLAYNAME), 40-6, 120, 20, StringTextComponent.EMPTY));
    this.displayNameField.setResponder(this::setDisplayName);
    this.displayNameField.setText(displayName);
    this.displayNameField.setMaxStringLength(400);
    this.displayNameField.setCanLoseFocus(true);

    this.repeatableCheckbox = this.addButton(new CheckboxButton(5+font.getStringWidth(NAME)+130+font.getStringWidth(REPEATABLE), 5, 20, 20, StringTextComponent.EMPTY, repeatable));

    this.objectiveBuilderButton = this.addButton(new Button(5+font.getStringWidth(OBJECTIVES), 70-6, 120, 20, new StringTextComponent("Objective Builder"), btn -> {
      minecraft.displayGuiScreen(new QuestObjectiveBuilderScreen(this, null));
    }));

    objectiveEditButton = new Button[objectiveDisplayLimit];
    objectiveRemoveButton = new Button[objectiveDisplayLimit];
    for (int i = 0; i < objectiveEditButton.length; i++) {
      int j = i;
      this.objectiveEditButton[i] = this.addButton(new Button(10, 86+i*15, 40, 14,
          new StringTextComponent("Edit"), btn -> editObjective(j)));
      this.objectiveRemoveButton[i] = this.addButton(new Button(51, 86+i*15, 40, 14,
          new StringTextComponent("Remove"), btn -> removeObjective(j)));
      this.objectiveEditButton[i].visible = false;
      this.objectiveRemoveButton[i].visible = false;
    }

    this.runOnCompleteField = this.addButton(new TextFieldWidget(font, width/2, 70-6, 120, 20, StringTextComponent.EMPTY));
    this.runOnCompleteField.setResponder(this::setRunOnComplete);
    this.runOnCompleteField.setText(currentRunOnComplete);
    this.runOnCompleteField.setMaxStringLength(400);
    this.runOnCompleteField.setCanLoseFocus(true);

    this.plusRunOnCompleteButton = this.addButton(new Button(width/2+125, 70-6, 20, 20, new StringTextComponent("+"), btn -> {
      if (canAddRunOnComplete())
        this.runOnComplete.add(currentRunOnComplete);
      this.runOnCompleteField.setText("");
    }));
    plusRunOnCompleteButton.active = canAddRunOnComplete();

    this.removeRunOnCompleteButton = new Button[runOnCompleteDisplayLimit];
    for (int i = 0; i < removeRunOnCompleteButton.length; i++) {
      int j = i;
      this.removeRunOnCompleteButton[i] = this.addButton(new Button(width/2+10, 86+i*12, 10, 10, new StringTextComponent("-"), btn -> {
        this.removeRunOnComplete(j);
      }));
      this.removeRunOnCompleteButton[i].visible = false;
    }

    int xpRewardY = 86+(objectiveDisplayLimit*15)+2;
    this.xpRewardField = this.addButton(new TextFieldWidget(font, 5+font.getStringWidth(XPREWARD), xpRewardY, 100, 20, StringTextComponent.EMPTY));
    this.xpRewardField.setResponder(this::setXpReward);
    this.xpRewardField.setText(String.valueOf(xpReward));
    this.xpRewardField.setMaxStringLength(10);
    this.xpRewardField.setCanLoseFocus(true);

    this.itemsFromInventoryButton = this.addButton(new Button(5+font.getStringWidth(ITEMREWARDS), xpRewardY+25, 100, 20, new StringTextComponent("From Inventory"), btn -> {
      String name = build().toJson().toString();
      PacketDispatcher.sendToServer(new CRequestContainer(name, CRequestContainer.ContainerType.QUEST_STACK_SELECTOR));
    }));

    this.confirmButton = this.addButton(new Button(width-100, height-20, 100, 20, new StringTextComponent("Build"), btn -> {
      if (canBuild()) {
        PacketDispatcher.sendToServer(new CBuildQuest(build()));
        closeScreen();
      }
    }));

    this.loadQuestField = this.addButton(new TextFieldWidget(font, width-105, 5, 100, 20, StringTextComponent.EMPTY));
    this.loadQuestField.setResponder(this::setQuestToLoad);
    this.loadQuestField.setText(questToLoad);
    this.loadQuestField.setMaxStringLength(51);
    this.loadQuestField.setCanLoseFocus(true);

    this.loadButton = this.addButton(new Button(width-105, 30, 100, 20, new StringTextComponent("Load"), btn -> {
      if (!questToLoad.isEmpty()) {
        PacketDispatcher.sendToServer(new CRequestQuestInfo(questToLoad));
        Quest quest = ClientQuestUtil.fromName(questToLoad);
        if (quest != null) {
          QuestEditorScreen questEditorScreen = QuestEditorScreen.fromQuest(quest);
          questEditorScreen.updateObjectiveId();
          minecraft.displayGuiScreen(questEditorScreen);
        }
      }
    }));
  }

  public void setQuestToLoad(String questToLoad) {
    this.questToLoad = questToLoad;
  }

  private boolean canAddRunOnComplete() {
    return this.currentRunOnComplete.length() > 0;
  }

  private void editObjective(int i) {
    if (i+objectiveScrollY < objectives.size())
      minecraft.displayGuiScreen(new QuestObjectiveBuilderScreen(this, objectives.get(i+objectiveScrollY), objectives.get(i+objectiveScrollY).getName()));
  }

  private void removeObjective(int i) {
    if (i+objectiveScrollY < objectives.size()) {
      this.objectives.remove(i+objectiveScrollY);
      objectiveScrollY = clampObjectiveScroll(objectiveScrollY-1);
      for (int j = i+objectiveScrollY; j < objectives.size(); j++) {
        objectives.get(j).setId(j);
      }
    }
  }

  private void removeRunOnComplete(int i) {
    if (i+runOnCompleteScrollY < runOnComplete.size()) {
      this.runOnComplete.remove(i+runOnCompleteScrollY);
      runOnCompleteScrollY = clampObjectiveScroll(runOnCompleteScrollY-1);
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void setXpReward(String xpReward) {
    try {
      this.xpReward = Integer.parseInt(xpReward);
    } catch (NumberFormatException nfe) {
      this.xpReward = 0;
    }
  }

  public void setRunOnComplete(String runOnComplete) {
    this.currentRunOnComplete = runOnComplete;
  }

  private int maxDisplayedObjectives() {
    return Math.min(objectiveDisplayLimit, objectives.size());
  }

  private int maxDisplayedRunOnComplete() {
    return Math.min(runOnCompleteDisplayLimit, runOnComplete.size());
  }

  @Override
  public void tick() {
    this.nameField.tick();
    this.displayNameField.tick();
    this.runOnCompleteField.tick();
    this.xpRewardField.tick();

    for (int i = 0; i < objectiveEditButton.length; i++) {
      objectiveEditButton[i].visible = objectives.size() > i+objectiveScrollY;
      objectiveRemoveButton[i].visible = objectives.size() > i+objectiveScrollY;
    }
    for (int i = 0; i < removeRunOnCompleteButton.length; i++) {
      removeRunOnCompleteButton[i].visible = runOnComplete.size() > i+runOnCompleteScrollY;
    }
    plusRunOnCompleteButton.active = canAddRunOnComplete();

    this.repeatable = repeatableCheckbox.isChecked();

    this.confirmButton.active = canBuild();
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);

    drawString(matrixStack, font, NAME, 5, 10, 0xFFFFFF);
    drawString(matrixStack, font, REPEATABLE, 5+font.getStringWidth(NAME)+130, 10, 0xFFFFFF);
    drawString(matrixStack, font, DISPLAYNAME, 5, 40, 0xFFFFFF);
    drawString(matrixStack, font, OBJECTIVES, 5, 70, 0xFFFFFF);
    drawString(matrixStack, font, RUNONCOMPLETE, width/2, 52, 0xFFFFFF);
    int xpRewardY = 86+(objectiveDisplayLimit*15)+8;
    drawString(matrixStack, font, XPREWARD, 5, xpRewardY, 0xFFFFFF);
    drawString(matrixStack, font, ITEMREWARDS, 5, xpRewardY+25, 0xFFFFFF);

    super.render(matrixStack, mouseX, mouseY, partialTicks);

    int objXpos = 10+this.objectiveEditButton[0].getWidth()+1+this.objectiveRemoveButton[0].getWidth()+3;
    int maxObjectiveWidth = width/2-objXpos-15;
    for (int i = 0; i < maxDisplayedObjectives(); i++) {
      if (i+objectiveScrollY < objectives.size()) {
        QuestObjective objective = objectives.get(i+objectiveScrollY);
        int swidth = font.getStringWidth(objective.getName());
        String name = swidth > maxObjectiveWidth ? font.trimStringToWidth(objective.getName(), maxObjectiveWidth-font.getStringWidth(DOTS)) + DOTS : objective.getName();
        drawString(matrixStack, font, name, objXpos, 89 + 15 * i, 0xFFFFFF);
        if (mouseX >= objXpos && mouseX <= objXpos+font.getStringWidth(name) && mouseY >= 86+i*15 && mouseY <= 86+14+i*15)
          this.renderTooltip(matrixStack, getObjectiveTooltip(objective), mouseX, mouseY);
      }
    }


    int runOnCompleteXpos = 10+this.removeRunOnCompleteButton[0].getWidth()+3;
    int maxRunOnCompleteWidth = width/2-runOnCompleteXpos-15;
    for (int i = 0; i < maxDisplayedRunOnComplete(); i++) {
      if (i+runOnCompleteScrollY < runOnComplete.size()) {
        String runnable = runOnComplete.get(i+runOnCompleteScrollY);
        int swidth = font.getStringWidth(runnable);
        String name = swidth > maxRunOnCompleteWidth ? font.trimStringToWidth(runnable, maxRunOnCompleteWidth-font.getStringWidth(DOTS)) + DOTS : runnable;
        drawString(matrixStack, font, name, width/2+runOnCompleteXpos, 87+i*12, 0xFFFFFF);
        if (mouseX >= width/2+runOnCompleteXpos && mouseX <= width/2+runOnCompleteXpos+font.getStringWidth(name) && mouseY >= 86+i*12 && mouseY <= 86+10+i*12)
          this.renderTooltip(matrixStack, new StringTextComponent(runnable), mouseX, mouseY);
      }
    }

    for (int i = 0; i < itemRewards.size(); i++) {
      ItemStack stack = itemRewards.get(i);
      int minX = 10+font.getStringWidth(ITEMREWARDS)+itemsFromInventoryButton.getWidth();
      int x = minX+17*i;
      int y = xpRewardY+21;
      if (x+16 > width) {
        int j = (width-minX)/17;
        int k = i/j;
        x = minX+17*(i-j*k);
        y += k*17;
      }
      minecraft.getItemRenderer().renderItemAndEffectIntoGUI(stack, x, y);
      minecraft.getItemRenderer().renderItemOverlays(font, stack, x, y);
      if (mouseX >= x && mouseX <= x+16 && mouseY >= y && mouseY <= y+16)
        this.renderTooltip(matrixStack, stack, mouseX, mouseY);
    }
  }

  private List<IReorderingProcessor> getObjectiveTooltip(QuestObjective objective) {
    List<IReorderingProcessor> tooltips = new ArrayList<>();
    tooltips.add(IReorderingProcessor.fromString(objective.getName(), Style.EMPTY.applyFormatting(TextFormatting.WHITE)));
    // tooltips.add(IReorderingProcessor.fromString("DEBUG ID: " + objective.getId(), Style.EMPTY.applyFormatting(TextFormatting.LIGHT_PURPLE)));
    tooltips.add(IReorderingProcessor.fromString("Type: " + objective.getType().name(), Style.EMPTY.applyFormatting(TextFormatting.GRAY)));

    String primary = objective.primaryToString();
    if (font.getStringWidth(primary) > 100)
      primary = font.trimStringToWidth(primary, 100-font.getStringWidth("...")) + "...";
    tooltips.add(IReorderingProcessor.fromString("Objective: " + primary, Style.EMPTY.applyFormatting(TextFormatting.GRAY)));

    if (objective.getSecondaryObjective() != null) {
      String secondary = objective.secondaryToString();
      if (font.getStringWidth(secondary) > 100)
        secondary = font.trimStringToWidth(secondary, 100-font.getStringWidth("...")) + "...";
      tooltips.add(IReorderingProcessor.fromString("Secondary Objective: " + secondary, Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
    }

    tooltips.add(IReorderingProcessor.fromString("Amount: " + objective.getAmount(), Style.EMPTY.applyFormatting(TextFormatting.GRAY)));

    if (objective.isOptional())
      tooltips.add(IReorderingProcessor.fromString("Optional", Style.EMPTY.applyFormatting(TextFormatting.GREEN)));
    if (objective.isHidden())
      tooltips.add(IReorderingProcessor.fromString("Hidden by Default", Style.EMPTY.applyFormatting(TextFormatting.RED)));
    if (!objective.shouldDisplayProgress())
      tooltips.add(IReorderingProcessor.fromString("Won't display progress", Style.EMPTY.applyFormatting(TextFormatting.YELLOW)));

    if (objective.getRunOnComplete() != null && !objective.getRunOnComplete().isEmpty()) {
      tooltips.add(IReorderingProcessor.fromString("Run on Completion: ", Style.EMPTY.applyFormatting(TextFormatting.AQUA)));
      for (int i = 0; i < Math.min(3, objective.getRunOnComplete().size()); i++) {
        String runOnComplete = objective.getRunOnComplete().get(i);
        if (font.getStringWidth(runOnComplete) > 120)
          runOnComplete = font.trimStringToWidth(runOnComplete, 120-font.getStringWidth("...")) + "...";
        tooltips.add(IReorderingProcessor.fromString(runOnComplete, Style.EMPTY.applyFormatting(TextFormatting.WHITE)));
        if (objective.getRunOnComplete().size() > 3 && i == 2)
          tooltips.add(IReorderingProcessor.fromString("...", Style.EMPTY.applyFormatting(TextFormatting.WHITE)));
      }
    }

    return tooltips;
  }

  private boolean canBuild() {
    return !this.name.isEmpty() && !this.displayName.isEmpty() && !objectives.isEmpty();
  }

  public Quest build() {
    return new Quest(name, displayName, objectives, xpReward, itemRewards, repeatable, runOnComplete);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta > 0) {
      if (mouseX < width/2)
        this.objectiveScrollY = clampObjectiveScroll(objectiveScrollY - 1);
      else
        this.runOnCompleteScrollY = clampRunOnCompleteScroll(runOnCompleteScrollY - 1);
    } else {
      if (mouseX < width/2)
        this.objectiveScrollY = clampObjectiveScroll(objectiveScrollY + 1);
      else
        this.runOnCompleteScrollY = clampRunOnCompleteScroll(runOnCompleteScrollY + 1);
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  public int clampObjectiveScroll(int newScroll) {
    int max = objectives.size()-objectiveDisplayLimit;
    if (max > 0)
      return MathHelper.clamp(newScroll, 0, max);
    else
      return 0;
  }

  public int clampRunOnCompleteScroll(int newScroll) {
    int max = runOnComplete.size()-runOnCompleteDisplayLimit;
    if (max > 0)
      return MathHelper.clamp(newScroll, 0, max);
    else
      return 0;
  }
}
