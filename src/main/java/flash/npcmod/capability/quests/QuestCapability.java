package flash.npcmod.capability.quests;

import flash.npcmod.core.quests.CommonQuestUtil;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncQuestCapability;

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
          if (!quest.getPlayer().world.isRemote) {
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
}
