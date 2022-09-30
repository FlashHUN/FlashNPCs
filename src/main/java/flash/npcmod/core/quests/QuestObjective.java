package flash.npcmod.core.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import flash.npcmod.config.ConfigHolder;
import flash.npcmod.core.pathing.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static flash.npcmod.core.ItemUtil.stackFromString;

public abstract class QuestObjective {

  private int id;
  private Quest quest;
  private String name;
  private ObjectiveType type;
  private int amount;
  private int progress;
  private boolean hidden;
  private boolean completed;
  private boolean forceComplete;
  private boolean displayProgress;
  private boolean optional;
  private List<String> onComplete;
  private boolean onCompleteRan;

  public QuestObjective(int id, String name, ObjectiveType type, int amount) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.amount = amount;
    this.displayProgress = true;
  }

  public int getId() {
    return id;
  }

  @OnlyIn(Dist.CLIENT)
  public void setId(int id) {
    this.id = id;
  }

  public Quest getQuest() {
    return quest;
  }

  public void setQuest(Quest quest) {
    this.quest = quest;
  }

  public String getName() {
    return name;
  }

  public ObjectiveType getType() {
    return type;
  }

  public int getAmount() {
    return amount;
  }

  public int getProgress() {
    return progress;
  }

  public void setProgress(int progress) {
    this.progress = Mth.clamp(progress, 0, amount);
  }

  public void progress(int amount) {
    this.progress = Mth.clamp(this.progress + amount, 0, this.amount);
  }

  public boolean isComplete() {
    if (forceComplete && !completed) progress = amount;
    this.completed = progress == amount;
    return completed;
  }

  public List<String> getRunOnComplete() {
    return onComplete;
  }

  public void setRunOnComplete(List<String> runOnComplete) {
    onComplete = runOnComplete;
  }

  public void onComplete(Player playerEntity) {
    if (onComplete != null) {
      if (isComplete()) {
        for (String s : onComplete) {
          if (s.startsWith("hide:")) {
            String objectiveName = s.substring(5);
            if (!objectiveName.isEmpty()) {
              QuestObjective objective = Quest.getObjectiveFromName(quest, objectiveName);
              if (objective != null)
                objective.setHidden(true);
            }
          } else if (s.startsWith("unhide:")) {
            String objectiveName = s.substring(7);
            if (!objectiveName.isEmpty()) {
              QuestObjective objective = Quest.getObjectiveFromName(quest, objectiveName);
              if (objective != null)
                objective.setHidden(false);
            }
          } else if (s.startsWith("forceComplete:")) {
            String objectiveName = s.substring(14);
            if (!objectiveName.isEmpty()) {
              QuestObjective objective = Quest.getObjectiveFromName(quest, objectiveName);
              if (objective != null)
                objective.forceComplete();
            }
          } else if (s.startsWith("/")) {
            if (!playerEntity.level.isClientSide && !onCompleteRan) {
              if (ConfigHolder.COMMON.isInvalidCommand(s)) continue;

              MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
              server.getCommands().performCommand(server.createCommandSourceStack().withSuppressedOutput(), s.replaceAll("@p", playerEntity.getName().getString()));
            }
          }
        }
        if (!onCompleteRan) {
          playerEntity.displayClientMessage(new TranslatableComponent("msg.flashnpcs.objectivecomplete").withStyle(ChatFormatting.YELLOW), true);
        }
        onCompleteRan = true;
      }
    }
  }

  public void setOnCompleteRan(boolean b) {
    this.onCompleteRan = b;
  }

  public boolean getOnCompleteRan() {
    return onCompleteRan;
  }

  public boolean isHidden() {
    return hidden;
  }

  public void setHidden(boolean b) {
    this.hidden = b;
  }

  public void forceComplete() {
    forceComplete = true;
  }

  public boolean isForceComplete() {
    return forceComplete;
  }

  public boolean shouldDisplayProgress() {
    return displayProgress;
  }

  public void setShouldDisplayProgress(boolean b) {
    displayProgress = b;
  }

  public boolean isOptional() {
    return optional;
  }

  public void setOptional(boolean b) {
    optional = b;
  }

  public abstract <T extends Object> T getObjective();

  @Nullable
  public <T extends Object> T getSecondaryObjective() {
    return null;
  }

  public abstract String primaryToString();

  @Nullable
  public String secondaryToString() {
    return null;
  }

  public abstract QuestObjective copy();

  public void copyTo(QuestObjective to) {
    to.setQuest(getQuest());
    to.setHidden(isHidden());
    to.setShouldDisplayProgress(shouldDisplayProgress());
    to.setProgress(getProgress());
    to.setOptional(isOptional());
    to.onCompleteRan = onCompleteRan;
    to.id = id;
    if(isForceComplete()) to.forceComplete();
    to.setRunOnComplete(getRunOnComplete());
  }

  public QuestObjective setItemStackObjective(ItemStack itemStack) {
    if (itemStack == null || itemStack.isEmpty()) return this;

    QuestObjective questObjective = this;
    if (type.equals(ObjectiveType.Gather))
      questObjective = new QuestObjectiveTypes.GatherObjective(id, name, itemStack, amount);
    else if (type.equals(ObjectiveType.DeliverToEntity))
      questObjective = new QuestObjectiveTypes.DeliverToEntityObjective(id, name, itemStack, getSecondaryObjective(), amount);
    else if (type.equals(ObjectiveType.DeliverToLocation))
      questObjective = new QuestObjectiveTypes.DeliverToLocationObjective(id, name, itemStack, getSecondaryObjective(), amount);
    else if (type.equals(ObjectiveType.UseOnEntity))
      questObjective = new QuestObjectiveTypes.UseOnEntityObjective(id, name, itemStack, getSecondaryObjective(), amount);
    else if (type.equals(ObjectiveType.UseOnBlock))
      questObjective = new QuestObjectiveTypes.UseOnBlockObjective(id, name, itemStack, getSecondaryObjective(), amount);
    else if (type.equals(ObjectiveType.Use))
      questObjective = new QuestObjectiveTypes.UseObjective(id, name, itemStack, amount);
    else if (type.equals(ObjectiveType.CraftItem))
      questObjective = new QuestObjectiveTypes.CraftItemObjective(id, name, itemStack, amount);

    if (questObjective != this) {
      questObjective.setQuest(this.getQuest());
      questObjective.setHidden(this.isHidden());
      questObjective.setOptional(this.isOptional());
      questObjective.setShouldDisplayProgress(this.shouldDisplayProgress());
      questObjective.setRunOnComplete(this.getRunOnComplete());
    }

    return questObjective;
  }

  public enum ObjectiveType implements net.minecraftforge.common.IExtensibleEnum {
    Gather,
    Kill,
    DeliverToEntity,
    DeliverToLocation,
    Escort,
    Talk,
    Find,
    UseOnEntity,
    UseOnBlock,
    Use,
    Scoreboard,
    CraftItem;

    ObjectiveType() {}

    public static ObjectiveType create(String name) {
      throw new IllegalStateException("Enum not extended");
    }
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("index", id);
    jsonObject.addProperty("name", getName());
    jsonObject.addProperty("type", getType().ordinal());
    jsonObject.addProperty("amount", getAmount());
    jsonObject.addProperty("primaryObjective", primaryToString());
    if (getSecondaryObjective() != null && !secondaryToString().isEmpty())
      jsonObject.addProperty("secondaryObjective", secondaryToString());

    if (isHidden())
      jsonObject.addProperty("isHidden", true);
    if (isOptional())
      jsonObject.addProperty("isOptional", true);
    if (!shouldDisplayProgress())
      jsonObject.addProperty("displayProgress", false);

    if (getRunOnComplete() != null && !getRunOnComplete().isEmpty()) {
      JsonArray runOnCompleteArray = new JsonArray();
      for (String s : getRunOnComplete()) {
        runOnCompleteArray.add(s);
      }
      jsonObject.add("objectiveRunOnComplete", runOnCompleteArray);
    }
    return jsonObject;
  }

  public static QuestObjective fromJson(JsonObject jsonObject) {
    int id = jsonObject.get("index").getAsInt();
    String objectiveName = jsonObject.get("name").getAsString();
    int type = Mth.clamp(jsonObject.get("type").getAsInt(), 0, QuestObjective.ObjectiveType.values().length);
    QuestObjective.ObjectiveType objectiveType = QuestObjective.ObjectiveType.values()[type];
    int objectiveAmount = jsonObject.get("amount").getAsInt();
    String primaryObjective = jsonObject.get("primaryObjective").getAsString();
    String secondaryObjective = "";
    if (jsonObject.has("secondaryObjective"))
      secondaryObjective = jsonObject.get("secondaryObjective").getAsString();
    QuestObjective questObjective = switch (objectiveType) {
      case Gather ->
              new QuestObjectiveTypes.GatherObjective(id, objectiveName, stackFromString(primaryObjective), objectiveAmount);
      case Kill -> new QuestObjectiveTypes.KillObjective(id, objectiveName, primaryObjective, objectiveAmount);
      case DeliverToEntity ->
              new QuestObjectiveTypes.DeliverToEntityObjective(id, objectiveName, stackFromString(primaryObjective), secondaryObjective, objectiveAmount);
      case DeliverToLocation ->
              new QuestObjectiveTypes.DeliverToLocationObjective(id, objectiveName, stackFromString(primaryObjective), QuestObjectiveTypes.areaFromString(secondaryObjective), objectiveAmount);
      case Escort ->
        // TODO figure out how this should work
              new QuestObjectiveTypes.EscortObjective(id, objectiveName, primaryObjective, Path.fromString(secondaryObjective));
      case Talk -> new QuestObjectiveTypes.TalkObjective(id, objectiveName, primaryObjective, secondaryObjective);
      case Find ->
              new QuestObjectiveTypes.FindObjective(id, objectiveName, QuestObjectiveTypes.areaFromString(primaryObjective));
      case UseOnEntity ->
              new QuestObjectiveTypes.UseOnEntityObjective(id, objectiveName, stackFromString(primaryObjective), secondaryObjective, objectiveAmount);
      case UseOnBlock ->
              new QuestObjectiveTypes.UseOnBlockObjective(id, objectiveName, stackFromString(primaryObjective), QuestObjectiveTypes.blockStateFromString(secondaryObjective), objectiveAmount);
      case Use ->
              new QuestObjectiveTypes.UseObjective(id, objectiveName, stackFromString(primaryObjective), objectiveAmount);
      case Scoreboard ->
              new QuestObjectiveTypes.ScoreboardObjective(id, objectiveName, primaryObjective, objectiveAmount);
      case CraftItem -> new QuestObjectiveTypes.CraftItemObjective(id, objectiveName, stackFromString(primaryObjective), objectiveAmount);
    };

    if (jsonObject.has("isHidden"))
      questObjective.setHidden(jsonObject.get("isHidden").getAsBoolean());
    if (jsonObject.has("isOptional"))
      questObjective.setOptional(jsonObject.get("isOptional").getAsBoolean());
    if (jsonObject.has("displayProgress"))
      questObjective.setShouldDisplayProgress(jsonObject.get("displayProgress").getAsBoolean());

    if (jsonObject.has("objectiveRunOnComplete")) {
      JsonArray runOnCompleteArray = jsonObject.getAsJsonArray("objectiveRunOnComplete");
      List<String> runOnCompleteList = new ArrayList<>();
      for (int j = 0; j < runOnCompleteArray.size(); j++) {
        String s = runOnCompleteArray.get(j).getAsString();
        runOnCompleteList.add(s);
      }

      questObjective.setRunOnComplete(runOnCompleteList);
    }

    return questObjective;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QuestObjective that = (QuestObjective) o;
    if ((quest != null && that.quest == null) || (quest == null && that.quest != null) ||
        (quest != null && that.quest != null && !quest.getName().equals(that.quest.getName()))) return false;
    if ((getSecondaryObjective() != null && that.getSecondaryObjective() == null) ||
        (getSecondaryObjective() == null && that.getSecondaryObjective() != null) ||
        (getSecondaryObjective() != null && that.getSecondaryObjective() != null
            && !secondaryToString().equals(that.secondaryToString()))) return false;
    return id == that.id && name.equals(that.name) && isComplete() == that.isComplete() && isHidden() == that.isHidden() && primaryToString().equals(that.primaryToString());
  }
}
