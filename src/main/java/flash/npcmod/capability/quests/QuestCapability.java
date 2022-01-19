package flash.npcmod.capability.quests;

import flash.npcmod.Main;
import flash.npcmod.core.quests.CommonQuestUtil;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncQuestCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestCapability implements IQuestCapability {

  private String trackedQuest;
  private ArrayList<QuestInstance> acceptedQuests;
  private ArrayList<String> completedQuests;
  private Map<QuestObjective, Integer> questProgressMap;

  public QuestCapability() {
    trackedQuest = "";
    acceptedQuests = new ArrayList<>();
    completedQuests = new ArrayList<>();
    questProgressMap = new HashMap<>();
  }

  @Override
  public String getTrackedQuest() {
    return trackedQuest;
  }

  @Override
  public QuestInstance getTrackedQuestInstance() {
    for (QuestInstance questInstance : acceptedQuests) {
      if (questInstance.getQuest().getName().equals(trackedQuest))
        return questInstance;
    }

    return null;
  }

  @Override
  public ArrayList<QuestInstance> getAcceptedQuests() {
    return acceptedQuests;
  }

  @Override
  public ArrayList<String> getCompletedQuests() {
    return completedQuests;
  }

  @Override
  public Map<QuestObjective, Integer> getQuestProgressMap() {
    return questProgressMap;
  }

  @Override
  public void setTrackedQuest(String quest) {
    trackedQuest = quest;
  }

  @Override
  public void setAcceptedQuests(ArrayList<QuestInstance> quests) {
    acceptedQuests = quests;
  }

  @Override
  public void setCompletedQuests(ArrayList<String> questNames) {
    completedQuests = questNames;
  }

  @Override
  public void setQuestProgressMap(Map<QuestObjective, Integer> map) {
    questProgressMap = map;
  }

  @Override
  public void acceptQuest(QuestInstance quest) {
    if (quest != null && quest.getQuest() != null && !acceptedQuests.contains(quest) && (quest.getQuest().isRepeatable() || !completedQuests.contains(quest.getQuest().getName()))) {
      acceptedQuests.add(quest);
      CommonQuestUtil.QUEST_INSTANCE_LIST.add(quest);
      quest.getQuest().getObjectives().forEach(objective -> {
        questProgressMap.put(objective, 0);
      });

      if (trackedQuest == null || trackedQuest.isEmpty())
        setTrackedQuest(quest.getQuest().getName());
    }
  }

  @Override
  public void completeQuest(QuestInstance quest) {
    if (acceptedQuests.contains(quest)) {
      if (quest.getQuest().canComplete()) {
        quest.getQuest().complete(quest.getPlayer(), quest.getPickedUpFrom(), quest.getPickedUpFromName());

        abandonQuest(quest);

        if (!completedQuests.contains(quest.getQuest().getName())) {
          completedQuests.add(quest.getQuest().getName());
          if (!quest.getPlayer().level.isClientSide) {
            PacketDispatcher.sendTo(new SSyncQuestCapability(completedQuests.toArray(new String[0])), quest.getPlayer());
          }
        }
      }
    }
  }

  @Override
  public void abandonQuest(QuestInstance quest) {
    if (acceptedQuests.contains(quest)) {
      acceptedQuests.remove(quest);

      List<QuestObjective> markedForRemoval = new ArrayList<>();
      quest.getQuest().getObjectives().forEach(objective -> {
        questProgressMap.keySet().forEach(questObjective -> {
          if (objectiveToString(objective).equals(objectiveToString(questObjective))) {
            markedForRemoval.add(questObjective);
          }
        });
      });

      markedForRemoval.forEach(objective -> {
        questProgressMap.remove(objective);
      });

      if (trackedQuest.equals(quest.getQuest().getName()))
        trackedQuest = "";

      if (CommonQuestUtil.QUEST_INSTANCE_LIST.contains(quest))
        CommonQuestUtil.QUEST_INSTANCE_LIST.remove(quest);
    }
  }

  private String objectiveToString(QuestObjective objective) {
    return objective.getQuest().getName()+":::"+objective.getName();
  }

  @Override
  public CompoundTag serializeNBT() {
    CompoundTag tag = new CompoundTag();

    if (getTrackedQuest() != null)
      tag.putString("trackedQuest", getTrackedQuest());
    else
      tag.putString("trackedQuest", "");

    ListTag acceptedQuests = new ListTag();
    getAcceptedQuests().forEach((quest) -> {
      CompoundTag tag2 = new CompoundTag();
      tag2.putString("quest", quest.getQuest().getName());
      tag2.putUUID("uuid", quest.getPickedUpFrom());
      tag2.putString("npcname", quest.getPickedUpFromName());
      acceptedQuests.add(tag2);
    });
    tag.put("acceptedQuests", acceptedQuests);

    ListTag completedQuests = new ListTag();
    for (String questName : getCompletedQuests()) {
      CompoundTag tag2 = new CompoundTag();
      tag2.putString("quest", questName);
      completedQuests.add(tag2);
    }
    tag.put("completedQuests", completedQuests);

    ListTag objectiveProgressMap = new ListTag();
    getQuestProgressMap().forEach((objective, progress) -> {
      CompoundTag tag2 = new CompoundTag();
      if (objective.getQuest() != null) {
        tag2.putInt(objective.getQuest().getName() + ":::" + objective.getName(), objective.getProgress());
        objectiveProgressMap.add(tag2);
      }
    });
    tag.put("objectiveProgressMap", objectiveProgressMap);

    return tag;
  }

  @Override
  public void deserializeNBT(CompoundTag tag) {
    ArrayList<QuestInstance> acceptedQuests = new ArrayList<>();
    ListTag acceptedQuestsTag = (ListTag) tag.get("acceptedQuests");
    for (int i = 0; i < acceptedQuestsTag.size(); i++) {
      CompoundTag tag2 = acceptedQuestsTag.getCompound(i);
      Quest quest = CommonQuestUtil.fromName(tag2.getString("quest"));
      if (quest != null)
        acceptedQuests.add(new QuestInstance(quest, tag2.getUUID("uuid"), tag2.getString("npcname")));
    }
    setAcceptedQuests(acceptedQuests);

    setTrackedQuest(tag.getString("trackedQuest"));

    ArrayList<String> completedQuests = new ArrayList<>();
    ListTag completedQuestsTag = (ListTag) tag.get("completedQuests");
    for (int i = 0; i < completedQuestsTag.size(); i++) {
      CompoundTag tag2 = completedQuestsTag.getCompound(i);
      completedQuests.add(tag2.getString("quest"));
    }
    setCompletedQuests(completedQuests);

    Map<QuestObjective, Integer> objectiveProgressMap = new HashMap<>();
    ListTag objectiveProgressMapTag = (ListTag) tag.get("objectiveProgressMap");
    for (int i = 0; i < objectiveProgressMapTag.size(); i++) {
      CompoundTag tag2 = objectiveProgressMapTag.getCompound(i);
      String key = tag2.getAllKeys().toArray(new String[0])[0];
      int progress = tag2.getInt(key);
      String[] splitKey = key.split(":::");
      Quest quest = CommonQuestUtil.fromName(splitKey[0]);
      if (quest != null) {
        QuestObjective objective = Quest.getObjectiveFromName(quest, splitKey[1]);
        if (objective != null)
          objectiveProgressMap.put(objective, progress);
      }
    }
    setQuestProgressMap(objectiveProgressMap);

    getQuestProgressMap().forEach((objective, progress) -> {
      for (QuestInstance quest : acceptedQuests) {
        quest.getQuest().getObjectives().forEach(questObjective -> {
          if (questObjective.equals(objective)) {
            if (progress >= questObjective.getAmount())
              questObjective.setOnCompleteRan(true);

            questObjective.setProgress(progress);
          }
        });
      }
    });
  }
}
