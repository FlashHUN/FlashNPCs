package flash.npcmod.client.gui.screen.quests;

import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.core.client.quests.ClientQuestUtil;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CBuildQuest;
import flash.npcmod.network.packets.client.CRequestContainer;
import flash.npcmod.network.packets.client.CRequestQuestInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public class QuestEditorScreen extends Screen {

  private EditBox nameField, displayNameField, xpRewardField, runOnCompleteField, loadQuestField;
  private Checkbox repeatableCheckbox;
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
    super(TextComponent.EMPTY);

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

    questEditorScreen.objectives.addAll(quest.getObjectives());

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
    this.nameField = this.addRenderableWidget(new EditBox(font, 5+font.width(NAME), 5, 120, 20, TextComponent.EMPTY));
    this.nameField.setFilter(nameFilter);
    this.nameField.setResponder(this::setName);
    this.nameField.setValue(name);
    this.nameField.setMaxLength(50);
    this.nameField.setCanLoseFocus(true);

    this.displayNameField = this.addRenderableWidget(new EditBox(font, 5+font.width(DISPLAYNAME), 40-6, 120, 20, TextComponent.EMPTY));
    this.displayNameField.setResponder(this::setDisplayName);
    this.displayNameField.setValue(displayName);
    this.displayNameField.setMaxLength(400);
    this.displayNameField.setCanLoseFocus(true);

    this.repeatableCheckbox = this.addRenderableWidget(new Checkbox(5+font.width(NAME)+130+font.width(REPEATABLE), 5, 20, 20, TextComponent.EMPTY, repeatable));

    this.objectiveBuilderButton = this.addRenderableWidget(new Button(5+font.width(OBJECTIVES), 70-6, 120, 20, new TextComponent("Objective Builder"), btn -> {
      minecraft.setScreen(new QuestObjectiveBuilderScreen(this, null));
    }));

    objectiveEditButton = new Button[objectiveDisplayLimit];
    objectiveRemoveButton = new Button[objectiveDisplayLimit];
    for (int i = 0; i < objectiveEditButton.length; i++) {
      int j = i;
      this.objectiveEditButton[i] = this.addRenderableWidget(new Button(10, 86+i*15, 40, 14,
          new TextComponent("Edit"), btn -> editObjective(j)));
      this.objectiveRemoveButton[i] = this.addRenderableWidget(new Button(51, 86+i*15, 40, 14,
          new TextComponent("Remove"), btn -> removeObjective(j)));
      this.objectiveEditButton[i].visible = false;
      this.objectiveRemoveButton[i].visible = false;
    }

    this.runOnCompleteField = this.addRenderableWidget(new EditBox(font, width/2, 70-6, 120, 20, TextComponent.EMPTY));
    this.runOnCompleteField.setResponder(this::setRunOnComplete);
    this.runOnCompleteField.setValue(currentRunOnComplete);
    this.runOnCompleteField.setMaxLength(400);
    this.runOnCompleteField.setCanLoseFocus(true);

    this.plusRunOnCompleteButton = this.addRenderableWidget(new Button(width/2+125, 70-6, 20, 20, new TextComponent("+"), btn -> {
      if (canAddRunOnComplete())
        this.runOnComplete.add(currentRunOnComplete);
      this.runOnCompleteField.setValue("");
    }));
    plusRunOnCompleteButton.active = canAddRunOnComplete();

    this.removeRunOnCompleteButton = new Button[runOnCompleteDisplayLimit];
    for (int i = 0; i < removeRunOnCompleteButton.length; i++) {
      int j = i;
      this.removeRunOnCompleteButton[i] = this.addRenderableWidget(new Button(width/2+10, 86+i*12, 10, 10, new TextComponent("-"), btn -> {
        this.removeRunOnComplete(j);
      }));
      this.removeRunOnCompleteButton[i].visible = false;
    }

    int xpRewardY = 86+(objectiveDisplayLimit*15)+2;
    this.xpRewardField = this.addRenderableWidget(new EditBox(font, 5+font.width(XPREWARD), xpRewardY, 100, 20, TextComponent.EMPTY));
    this.xpRewardField.setResponder(this::setXpReward);
    this.xpRewardField.setValue(String.valueOf(xpReward));
    this.xpRewardField.setMaxLength(10);
    this.xpRewardField.setCanLoseFocus(true);

    this.itemsFromInventoryButton = this.addRenderableWidget(new Button(5+font.width(ITEMREWARDS), xpRewardY+25, 100, 20, new TextComponent("From Inventory"), btn -> {
      String name = build().toJson().toString();
      PacketDispatcher.sendToServer(new CRequestContainer(name, CRequestContainer.ContainerType.QUEST_STACK_SELECTOR));
    }));

    this.confirmButton = this.addRenderableWidget(new Button(width-100, height-20, 100, 20, new TextComponent("Build"), btn -> {
      if (canBuild()) {
        PacketDispatcher.sendToServer(new CBuildQuest(build()));
        onClose();
      }
    }));

    this.loadQuestField = this.addRenderableWidget(new EditBox(font, width-105, 5, 100, 20, TextComponent.EMPTY));
    this.loadQuestField.setResponder(this::setQuestToLoad);
    this.loadQuestField.setValue(questToLoad);
    this.loadQuestField.setMaxLength(51);
    this.loadQuestField.setCanLoseFocus(true);

    this.loadButton = this.addRenderableWidget(new Button(width-105, 30, 100, 20, new TextComponent("Load"), btn -> {
      if (!questToLoad.isEmpty()) {
        PacketDispatcher.sendToServer(new CRequestQuestInfo(questToLoad));
        Quest quest = ClientQuestUtil.loadQuest(questToLoad);
        if (quest != null) {
          QuestEditorScreen questEditorScreen = QuestEditorScreen.fromQuest(quest);
          questEditorScreen.updateObjectiveId();
          minecraft.setScreen(questEditorScreen);
        }
      }
    }));
  }

  public void setQuestToLoad(String questToLoad) {
    this.questToLoad = questToLoad;
  }

  private boolean canAddRunOnComplete() {
    return this.currentRunOnComplete.startsWith("/") || this.currentRunOnComplete.startsWith("acceptQuest:");
  }

  private void editObjective(int i) {
    if (i+objectiveScrollY < objectives.size())
      minecraft.setScreen(new QuestObjectiveBuilderScreen(this, objectives.get(i+objectiveScrollY), objectives.get(i+objectiveScrollY).getName()));
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

    this.repeatable = repeatableCheckbox.selected();

    this.confirmButton.active = canBuild();
  }

  @Override
  public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);

    drawString(matrixStack, font, NAME, 5, 10, 0xFFFFFF);
    drawString(matrixStack, font, REPEATABLE, 5+font.width(NAME)+130, 10, 0xFFFFFF);
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
        int swidth = font.width(objective.getName());
        String name = swidth > maxObjectiveWidth ? font.plainSubstrByWidth(objective.getName(), maxObjectiveWidth-font.width(DOTS)) + DOTS : objective.getName();
        drawString(matrixStack, font, name, objXpos, 89 + 15 * i, 0xFFFFFF);
        if (mouseX >= objXpos && mouseX <= objXpos+font.width(name) && mouseY >= 86+i*15 && mouseY <= 86+14+i*15)
          this.renderTooltip(matrixStack, getObjectiveTooltip(objective), mouseX, mouseY);
      }
    }


    int runOnCompleteXpos = 10+this.removeRunOnCompleteButton[0].getWidth()+3;
    int maxRunOnCompleteWidth = width/2-runOnCompleteXpos-15;
    for (int i = 0; i < maxDisplayedRunOnComplete(); i++) {
      if (i+runOnCompleteScrollY < runOnComplete.size()) {
        String runnable = runOnComplete.get(i+runOnCompleteScrollY);
        int swidth = font.width(runnable);
        String name = swidth > maxRunOnCompleteWidth ? font.plainSubstrByWidth(runnable, maxRunOnCompleteWidth-font.width(DOTS)) + DOTS : runnable;
        drawString(matrixStack, font, name, width/2+runOnCompleteXpos, 87+i*12, 0xFFFFFF);
        if (mouseX >= width/2+runOnCompleteXpos && mouseX <= width/2+runOnCompleteXpos+font.width(name) && mouseY >= 86+i*12 && mouseY <= 86+10+i*12)
          this.renderTooltip(matrixStack, new TextComponent(runnable), mouseX, mouseY);
      }
    }

    for (int i = 0; i < itemRewards.size(); i++) {
      ItemStack stack = itemRewards.get(i);
      int minX = 10+font.width(ITEMREWARDS)+itemsFromInventoryButton.getWidth();
      int x = minX+17*i;
      int y = xpRewardY+21;
      if (x+16 > width) {
        int j = (width-minX)/17;
        int k = i/j;
        x = minX+17*(i-j*k);
        y += k*17;
      }
      minecraft.getItemRenderer().renderAndDecorateItem(stack, x, y);
      minecraft.getItemRenderer().renderGuiItemDecorations(font, stack, x, y);
      if (mouseX >= x && mouseX <= x+16 && mouseY >= y && mouseY <= y+16)
        this.renderTooltip(matrixStack, stack, mouseX, mouseY);
    }
  }

  private List<FormattedCharSequence> getObjectiveTooltip(QuestObjective objective) {
    List<FormattedCharSequence> tooltips = new ArrayList<>();
    tooltips.add(FormattedCharSequence.forward(objective.getName(), Style.EMPTY.applyFormat(ChatFormatting.WHITE)));
    // tooltips.add(IReorderingProcessor.fromString("DEBUG ID: " + objective.getId(), Style.EMPTY.applyFormatting(TextFormatting.LIGHT_PURPLE)));
    tooltips.add(FormattedCharSequence.forward("Type: " + objective.getType().name(), Style.EMPTY.applyFormat(ChatFormatting.GRAY)));

    String primary = objective.primaryToString();
    if (font.width(primary) > 100)
      primary = font.plainSubstrByWidth(primary, 100-font.width("...")) + "...";
    tooltips.add(FormattedCharSequence.forward("Objective: " + primary, Style.EMPTY.applyFormat(ChatFormatting.GRAY)));

    if (objective.getSecondaryObjective() != null) {
      String secondary = objective.secondaryToString();
      if (font.width(secondary) > 100)
        secondary = font.plainSubstrByWidth(secondary, 100-font.width("...")) + "...";
      tooltips.add(FormattedCharSequence.forward("Secondary Objective: " + secondary, Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
    }

    tooltips.add(FormattedCharSequence.forward("Amount: " + objective.getAmount(), Style.EMPTY.applyFormat(ChatFormatting.GRAY)));

    if (objective.isOptional())
      tooltips.add(FormattedCharSequence.forward("Optional", Style.EMPTY.applyFormat(ChatFormatting.GREEN)));
    if (objective.isHidden())
      tooltips.add(FormattedCharSequence.forward("Hidden by Default", Style.EMPTY.applyFormat(ChatFormatting.RED)));
    if (!objective.shouldDisplayProgress())
      tooltips.add(FormattedCharSequence.forward("Won't display progress", Style.EMPTY.applyFormat(ChatFormatting.YELLOW)));

    if (objective.getRunOnComplete() != null && !objective.getRunOnComplete().isEmpty()) {
      tooltips.add(FormattedCharSequence.forward("Run on Completion: ", Style.EMPTY.applyFormat(ChatFormatting.AQUA)));
      for (int i = 0; i < Math.min(3, objective.getRunOnComplete().size()); i++) {
        String runOnComplete = objective.getRunOnComplete().get(i);
        if (font.width(runOnComplete) > 120)
          runOnComplete = font.plainSubstrByWidth(runOnComplete, 120-font.width("...")) + "...";
        tooltips.add(FormattedCharSequence.forward(runOnComplete, Style.EMPTY.applyFormat(ChatFormatting.WHITE)));
        if (objective.getRunOnComplete().size() > 3 && i == 2)
          tooltips.add(FormattedCharSequence.forward("...", Style.EMPTY.applyFormat(ChatFormatting.WHITE)));
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
      return Mth.clamp(newScroll, 0, max);
    else
      return 0;
  }

  public int clampRunOnCompleteScroll(int newScroll) {
    int max = runOnComplete.size()-runOnCompleteDisplayLimit;
    if (max > 0)
      return Mth.clamp(newScroll, 0, max);
    else
      return 0;
  }
}
