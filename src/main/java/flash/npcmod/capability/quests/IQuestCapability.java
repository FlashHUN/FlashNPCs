package flash.npcmod.capability.quests;

import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.Map;

public interface IQuestCapability extends INBTSerializable<CompoundTag> {

  String getTrackedQuest();
  QuestInstance getTrackedQuestInstance();
  ArrayList<QuestInstance> getAcceptedQuests();
  ArrayList<String> getCompletedQuests();
  Map<QuestObjective, Integer> getQuestProgressMap();
  void setTrackedQuest(String trackedQuest);
  void setAcceptedQuests(ArrayList<QuestInstance> acceptedQuests);
  void setCompletedQuests(ArrayList<String> questNames);
  void setQuestProgressMap(Map<QuestObjective, Integer> map);

  void acceptQuest(QuestInstance quest);
  void completeQuest(QuestInstance quest);
  void abandonQuest(QuestInstance quest);

}
