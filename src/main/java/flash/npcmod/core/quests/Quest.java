package flash.npcmod.core.quests;

import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.config.ConfigHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static flash.npcmod.core.ItemUtil.*;

public class Quest {

  private String name;
  private String displayName;
  private List<QuestObjective> objectives;
  private int xpReward;
  private List<ItemStack> itemRewards;
  private boolean repeatable;
  private List<String> runOnComplete;

  public Quest(String name, String displayName, List<QuestObjective> objectives) {
    this(name, displayName, objectives, 0);
  }

  public Quest(String name, String displayName, List<QuestObjective> objectives, int xpReward) {
    this(name, displayName, objectives, xpReward, new ArrayList<>(), true, new ArrayList<>());
  }

  public Quest(String name, String displayName, List<QuestObjective> objectives, List<ItemStack> itemRewards) {
    this(name, displayName, objectives, 0, itemRewards, true, new ArrayList<>());
  }

  public Quest(String name, String displayName, List<QuestObjective> objectives, int xpReward, List<ItemStack> itemRewards, boolean repeatable, List<String> runOnComplete) {
    this.name = name;
    this.displayName = displayName;
    this.objectives = objectives;
    this.xpReward = xpReward;
    this.itemRewards = itemRewards;
    this.repeatable = repeatable;
    this.runOnComplete = runOnComplete;

    objectives.forEach(objective -> objective.setQuest(this));
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public List<QuestObjective> getObjectives() {
    return objectives;
  }

  public int getXpReward() {
    return xpReward;
  }

  public List<ItemStack> getItemRewards() {
    return itemRewards;
  }

  public void setItemRewards(List<ItemStack> itemRewards) {
    this.itemRewards = itemRewards;
  }

  public boolean isRepeatable() {
    return repeatable;
  }

  public void setRepeatable(boolean b) {
    this.repeatable = repeatable;
  }

  public List<String> getRunOnComplete() {
    return runOnComplete;
  }

  public boolean canComplete() {
    for (QuestObjective objective : objectives) {
      if (!objective.isOptional() && !objective.isComplete()) return false;
    }
    return true;
  }

  public void complete(PlayerEntity player, UUID pickedUpFrom, String pickedUpFromName) {
    if (!canComplete()) return;

    for (QuestObjective objective : objectives) {
      if (objective instanceof QuestObjectiveTypes.GatherObjective) {
        takeStack(player, objective.getObjective(), objective.getAmount());
      }
    }

    player.giveExperiencePoints(xpReward);

    if (!itemRewards.isEmpty()) {
      for (ItemStack stack : itemRewards) {
        giveStack(player, stack);
      }
    }

    for (String command : runOnComplete) {
      if (command.startsWith("/")) {
        if (!player.world.isRemote) {
          if (ConfigHolder.COMMON.isInvalidCommand(command)) continue;

          MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
          server.getCommandManager().handleCommand(server.getCommandSource().withFeedbackDisabled(), command.replaceAll("@p", player.getName().getString()));
        }
      } else if (command.startsWith("acceptQuest:")) {
        Quest quest = CommonQuestUtil.fromName(command.substring(12));
        if (quest != null)
          QuestCapabilityProvider.getCapability(player).acceptQuest(new QuestInstance(quest, pickedUpFrom, pickedUpFromName, player));
      }
    }
  }

  @Nullable
  public static QuestObjective getObjectiveFromName(Quest quest, String name) {
    for (QuestObjective questObjective : quest.objectives) {
      if (questObjective.getName().equals(name))
        return questObjective;
    }

    return null;
  }

  public static Quest fromJson(JSONObject object) {
    String name = object.getString("name");
    String displayName = object.getString("displayName");
    List<QuestObjective> objectives = new ArrayList<>();
    if (object.has("objectives")) {
      JSONArray objectivesArray = object.getJSONArray("objectives");
      for (int i = 0; i < objectivesArray.length(); i++) {
        JSONObject objectiveObject = objectivesArray.getJSONObject(i);
        objectives.add(QuestObjective.fromJson(objectiveObject));
      }
    }

    int xpReward = 0;
    if (object.has("xpReward"))
      xpReward = object.getInt("xpReward");

    List<ItemStack> itemRewards = new ArrayList<>();
    if (object.has("itemRewards")) {
      JSONArray itemsArray = object.getJSONArray("itemRewards");
      for (int i = 0; i < itemsArray.length(); i++) {
        JSONObject itemObject = itemsArray.getJSONObject(i);
        ItemStack item = stackFromString(itemObject.getString("stack"));
        item.setCount(itemObject.getInt("count"));
        itemRewards.add(item);
      }
    }

    boolean repeatable = true;
    if (object.has("repeatable")) repeatable = object.getBoolean("repeatable");

    List<String> runOnComplete = new ArrayList<>();
    if (object.has("runOnComplete")) {
      JSONArray runArray = object.getJSONArray("runOnComplete");
      for (int i = 0; i < runArray.length(); i++) {
        runOnComplete.add(runArray.getString(i));
      }
    }

    return new Quest(name, displayName, objectives, xpReward, itemRewards, repeatable, runOnComplete);
  }

  public JSONObject toJson() {
    JSONObject object = new JSONObject();
    object.put("name", getName());
    object.put("displayName", getDisplayName());

    JSONArray objectives = new JSONArray();
    for (QuestObjective objective : getObjectives()) {
      objectives.put(objective.toJson());
    }
    if (!objectives.isEmpty())
      object.put("objectives", objectives);

    object.put("xpReward", getXpReward());

    JSONArray itemRewards = new JSONArray();
    for (ItemStack stack : getItemRewards()) {
      JSONObject stackObject = new JSONObject();
      stackObject.put("stack", stackToString(stack));
      stackObject.put("count", stack.getCount());
      itemRewards.put(stackObject);
    }
    if (!itemRewards.isEmpty())
      object.put("itemRewards", itemRewards);

    if (!isRepeatable()) object.put("repeatable", false);

    JSONArray runOnComplete = new JSONArray();
    for (String command : getRunOnComplete()) {
      runOnComplete.put(command);
    }
    if (!runOnComplete.isEmpty())
      object.put("runOnComplete", runOnComplete);

    return object;
  }

  // We need to copy the quest and objectives, cause instantiated classes are passed through as a reference
  // which would mean that we'd edit everybody's quest progress when editing someone's... I think? If not, remove this.
  public Quest copy() {
    List<QuestObjective> objectives = new ArrayList<>();
    for (QuestObjective objective : this.objectives) {
      objectives.add(objective.copy());
    }
    return new Quest(name, displayName, objectives, xpReward, itemRewards, repeatable, runOnComplete);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Quest quest = (Quest) o;
    return name.equals(quest.name);
  }
}
