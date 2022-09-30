package flash.npcmod.core.quests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.config.ConfigHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static flash.npcmod.core.ItemUtil.*;

public class Quest {

  private final String name;
  private final String displayName;
  private final List<QuestObjective> objectives;
  private final int xpReward;
  private List<ItemStack> itemRewards;
  private final boolean repeatable;
  private final List<String> runOnComplete;

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

  public List<String> getRunOnComplete() {
    return runOnComplete;
  }

  public boolean canComplete() {
    for (QuestObjective objective : objectives) {
      if (!objective.isOptional() && !objective.isComplete()) return false;
    }
    return true;
  }

  public void complete(Player player, UUID pickedUpFrom, String pickedUpFromName, QuestInstance.TurnInType turnInType) {
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
        if (!player.level.isClientSide) {
          if (ConfigHolder.COMMON.isInvalidCommand(command)) continue;

          MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
          server.getCommands().performCommand(server.createCommandSourceStack().withSuppressedOutput(), command.replaceAll("@p", player.getName().getString()));
        }
      } else if (command.startsWith("acceptQuest:")) {
        Quest quest = CommonQuestUtil.fromName(command.substring(12));
        if (quest != null)
          QuestCapabilityProvider.getCapability(player).acceptQuest(new QuestInstance(quest, pickedUpFrom, pickedUpFromName, turnInType, player));
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

  public static Quest fromJson(JsonObject object) {
    String name = object.get("name").getAsString();
    String displayName = object.get("displayName").getAsString();
    List<QuestObjective> objectives = new ArrayList<>();
    if (object.has("objectives")) {
      JsonArray objectivesArray = object.getAsJsonArray("objectives");
      for (int i = 0; i < objectivesArray.size(); i++) {
        JsonObject objectiveObject = objectivesArray.get(i).getAsJsonObject();
        objectives.add(QuestObjective.fromJson(objectiveObject));
      }
    }

    int xpReward = 0;
    if (object.has("xpReward"))
      xpReward = object.get("xpReward").getAsInt();

    List<ItemStack> itemRewards = new ArrayList<>();
    if (object.has("itemRewards")) {
      JsonArray itemsArray = object.getAsJsonArray("itemRewards");
      for (int i = 0; i < itemsArray.size(); i++) {
        JsonObject itemObject = itemsArray.get(i).getAsJsonObject();
        ItemStack item = stackFromString(itemObject.get("stack").getAsString());
        item.setCount(itemObject.get("count").getAsInt());
        itemRewards.add(item);
      }
    }

    boolean repeatable = true;
    if (object.has("repeatable"))
      repeatable = object.get("repeatable").getAsBoolean();

    List<String> runOnComplete = new ArrayList<>();
    if (object.has("runOnComplete")) {
      JsonArray runArray = object.getAsJsonArray("runOnComplete");
      for (int i = 0; i < runArray.size(); i++) {
        runOnComplete.add(runArray.get(i).getAsString());
      }
    }

    return new Quest(name, displayName, objectives, xpReward, itemRewards, repeatable, runOnComplete);
  }

  public JsonObject toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("name", getName());
    object.addProperty("displayName", getDisplayName());

    JsonArray objectives = new JsonArray();
    for (QuestObjective objective : getObjectives()) {
      objectives.add(objective.toJson());
    }
    if (!objectives.isEmpty())
      object.add("objectives", objectives);

    object.addProperty("xpReward", getXpReward());

    JsonArray itemRewards = new JsonArray();
    for (ItemStack stack : getItemRewards()) {
      JsonObject stackObject = new JsonObject();
      stackObject.addProperty("stack", stackToString(stack));
      stackObject.addProperty("count", stack.getCount());
      itemRewards.add(stackObject);
    }
    if (!itemRewards.isEmpty())
      object.add("itemRewards", itemRewards);

    if (!isRepeatable())
      object.addProperty("repeatable", false);

    JsonArray runOnComplete = new JsonArray();
    for (String command : getRunOnComplete()) {
      runOnComplete.add(command);
    }
    if (!runOnComplete.isEmpty())
      object.add("runOnComplete", runOnComplete);

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
