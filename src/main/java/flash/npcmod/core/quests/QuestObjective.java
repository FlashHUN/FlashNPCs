package flash.npcmod.core.quests;

import flash.npcmod.config.ConfigHolder;
import flash.npcmod.core.pathing.Path;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.json.JSONArray;
import org.json.JSONObject;

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
    this.progress = MathHelper.clamp(progress, 0, amount);
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

  public void onComplete(PlayerEntity playerEntity) {
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
            if (!playerEntity.world.isRemote && !onCompleteRan) {
              if (ConfigHolder.COMMON.isInvalidCommand(s)) continue;

              MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
              server.getCommandManager().handleCommand(server.getCommandSource().withFeedbackDisabled(), s.replaceAll("@p", playerEntity.getName().getString()));
            }
          }
        }
        if (!onCompleteRan) {
          playerEntity.sendStatusMessage(new TranslationTextComponent("msg.flashnpcs.objectivecomplete").mergeStyle(TextFormatting.YELLOW), true);
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
    Scoreboard;

    ObjectiveType() {}

    public static ObjectiveType create(String name) {
      throw new IllegalStateException("Enum not extended");
    }
  }

  public JSONObject toJson() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("index", id);
    jsonObject.put("name", getName());
    jsonObject.put("type", getType().ordinal());
    jsonObject.put("amount", getAmount());
    jsonObject.put("primaryObjective", primaryToString());
    if (getSecondaryObjective() != null && !secondaryToString().isEmpty())
      jsonObject.put("secondaryObjective", secondaryToString());

    if (isHidden())
      jsonObject.put("isHidden", true);
    if (isOptional())
      jsonObject.put("isOptional", true);
    if (!shouldDisplayProgress())
      jsonObject.put("displayProgress", false);

    if (getRunOnComplete() != null && !getRunOnComplete().isEmpty()) {
      JSONArray runOnCompleteArray = new JSONArray();
      for (String s : getRunOnComplete()) {
        runOnCompleteArray.put(s);
      }
      jsonObject.put("objectiveRunOnComplete", runOnCompleteArray);
    }
    return jsonObject;
  }

  public static QuestObjective fromJson(JSONObject jsonObject) {
    int id = jsonObject.getInt("index");
    String objectiveName = jsonObject.getString("name");
    int type = MathHelper.clamp(jsonObject.getInt("type"), 0, QuestObjective.ObjectiveType.values().length);
    QuestObjective.ObjectiveType objectiveType = QuestObjective.ObjectiveType.values()[type];
    int objectiveAmount = jsonObject.getInt("amount");
    String primaryObjective = jsonObject.getString("primaryObjective");
    String secondaryObjective = "";
    if (jsonObject.has("secondaryObjective"))
      secondaryObjective = jsonObject.getString("secondaryObjective");
    QuestObjective questObjective;
    switch (objectiveType) {
      default:
        questObjective = new QuestObjectiveTypes.GatherObjective(id, objectiveName, stackFromString(primaryObjective), objectiveAmount);
        break;
      case Kill:
        questObjective = new QuestObjectiveTypes.KillObjective(id, objectiveName, primaryObjective, objectiveAmount);
        break;
      case DeliverToEntity:
        questObjective = new QuestObjectiveTypes.DeliverToEntityObjective(id, objectiveName, stackFromString(primaryObjective), secondaryObjective, objectiveAmount);
        break;
      case DeliverToLocation:
        questObjective = new QuestObjectiveTypes.DeliverToLocationObjective(id, objectiveName, stackFromString(primaryObjective), QuestObjectiveTypes.areaFromString(secondaryObjective), objectiveAmount);
        break;
      case Escort:
        // TODO figure out how this should work
        questObjective = new QuestObjectiveTypes.EscortObjective(id, objectiveName, primaryObjective, Path.fromString(secondaryObjective));
        break;
      case Talk:
        questObjective = new QuestObjectiveTypes.TalkObjective(id, objectiveName, primaryObjective, secondaryObjective);
        break;
      case Find:
        questObjective = new QuestObjectiveTypes.FindObjective(id, objectiveName, QuestObjectiveTypes.areaFromString(primaryObjective));
        break;
      case UseOnEntity:
        questObjective = new QuestObjectiveTypes.UseOnEntityObjective(id, objectiveName, stackFromString(primaryObjective), secondaryObjective, objectiveAmount);
        break;
      case UseOnBlock:
        questObjective = new QuestObjectiveTypes.UseOnBlockObjective(id, objectiveName, stackFromString(primaryObjective), QuestObjectiveTypes.blockStateFromString(secondaryObjective), objectiveAmount);
        break;
      case Use:
        questObjective = new QuestObjectiveTypes.UseObjective(id, objectiveName, stackFromString(primaryObjective), objectiveAmount);
        break;
      case Scoreboard:
        questObjective = new QuestObjectiveTypes.ScoreboardObjective(id, objectiveName, primaryObjective, objectiveAmount);
        break;
    }

    if (jsonObject.has("isHidden"))
      questObjective.setHidden(jsonObject.getBoolean("isHidden"));
    if (jsonObject.has("isOptional"))
      questObjective.setOptional(jsonObject.getBoolean("isOptional"));
    if (jsonObject.has("displayProgress"))
      questObjective.setShouldDisplayProgress(jsonObject.getBoolean("displayProgress"));

    if (jsonObject.has("objectiveRunOnComplete")) {
      JSONArray runOnCompleteArray = jsonObject.getJSONArray("objectiveRunOnComplete");
      List<String> runOnCompleteList = new ArrayList<>();
      for (int j = 0; j < runOnCompleteArray.length(); j++) {
        String s = runOnCompleteArray.getString(j);
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
