package flash.npcmod.capability.quests;

import flash.npcmod.Main;
import flash.npcmod.core.quests.CommonQuestUtil;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncQuestCapability;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QuestCapabilityStorage implements Capability.IStorage<IQuestCapability> {
  @Nullable
  @Override
  public INBT writeNBT(Capability<IQuestCapability> capability, IQuestCapability instance, Direction side) {
    CompoundNBT tag = new CompoundNBT();

    if (instance.getTrackedQuest() != null)
      tag.putString("trackedQuest", instance.getTrackedQuest());
    else
      tag.putString("trackedQuest", "");

    ListNBT acceptedQuests = new ListNBT();
    instance.getAcceptedQuests().forEach((quest) -> {
      CompoundNBT tag2 = new CompoundNBT();
      tag2.putString("quest", quest.getQuest().getName());
      tag2.putUniqueId("uuid", quest.getPickedUpFrom());
      tag2.putString("npcname", quest.getPickedUpFromName());
      acceptedQuests.add(tag2);
    });
    tag.put("acceptedQuests", acceptedQuests);

    ListNBT completedQuests = new ListNBT();
    for (String questName : instance.getCompletedQuests()) {
      CompoundNBT tag2 = new CompoundNBT();
      tag2.putString("quest", questName);
      completedQuests.add(tag2);
    }
    tag.put("completedQuests", completedQuests);

    ListNBT objectiveProgressMap = new ListNBT();
    instance.getQuestProgressMap().forEach((objective, progress) -> {
      CompoundNBT tag2 = new CompoundNBT();
      if (objective.getQuest() != null) {
        tag2.putInt(objective.getQuest().getName() + ":::" + objective.getName(), objective.getProgress());
        objectiveProgressMap.add(tag2);
      }
    });
    tag.put("objectiveProgressMap", objectiveProgressMap);

    return tag;
  }

  @Override
  public void readNBT(Capability<IQuestCapability> capability, IQuestCapability instance, Direction side, INBT nbt) {
    CompoundNBT tag = (CompoundNBT) nbt;

    ArrayList<QuestInstance> acceptedQuests = new ArrayList<>();
    ListNBT acceptedQuestsTag = (ListNBT) tag.get("acceptedQuests");
    for (int i = 0; i < acceptedQuestsTag.size(); i++) {
      CompoundNBT tag2 = acceptedQuestsTag.getCompound(i);
      Quest quest = CommonQuestUtil.fromName(tag2.getString("quest"));
      if (quest != null)
        acceptedQuests.add(new QuestInstance(quest, tag2.getUniqueId("uuid"), tag2.getString("npcname")));
    }
    instance.setAcceptedQuests(acceptedQuests);

    instance.setTrackedQuest(tag.getString("trackedQuest"));

    ArrayList<String> completedQuests = new ArrayList<>();
    ListNBT completedQuestsTag = (ListNBT) tag.get("completedQuests");
    for (int i = 0; i < completedQuestsTag.size(); i++) {
      CompoundNBT tag2 = completedQuestsTag.getCompound(i);
      completedQuests.add(tag2.getString("quest"));
    }
    instance.setCompletedQuests(completedQuests);

    Map<QuestObjective, Integer> objectiveProgressMap = new HashMap<>();
    ListNBT objectiveProgressMapTag = (ListNBT) tag.get("objectiveProgressMap");
    for (int i = 0; i < objectiveProgressMapTag.size(); i++) {
      CompoundNBT tag2 = objectiveProgressMapTag.getCompound(i);
      String key = tag2.keySet().toArray(new String[0])[0];
      int progress = tag2.getInt(key);
      String[] splitKey = key.split(":::");
      Quest quest = CommonQuestUtil.fromName(splitKey[0]);
      if (quest != null) {
        QuestObjective objective = Quest.getObjectiveFromName(quest, splitKey[1]);
        if (objective != null)
          objectiveProgressMap.put(objective, progress);
      }
    }
    instance.setQuestProgressMap(objectiveProgressMap);

    instance.getQuestProgressMap().forEach((objective, progress) -> {
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

  @Mod.EventBusSubscriber(modid = Main.MODID)
  private static class EventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
      if (event.getObject() instanceof PlayerEntity) {
        event.addCapability(new ResourceLocation(Main.MODID, "quests"), new QuestCapabilityProvider());
      }
    }

    @SubscribeEvent
    public static void playerClone(final PlayerEvent.Clone event) {
      final IQuestCapability oldCap = QuestCapabilityProvider.getCapability(event.getOriginal());
      final IQuestCapability newCap = QuestCapabilityProvider.getCapability(event.getPlayer());

      if (oldCap != null && newCap != null) {
        newCap.setTrackedQuest(oldCap.getTrackedQuest());
        newCap.setAcceptedQuests(oldCap.getAcceptedQuests());
        newCap.setCompletedQuests(oldCap.getCompletedQuests());
        newCap.setQuestProgressMap(oldCap.getQuestProgressMap());
      }
    }

    @SubscribeEvent
    public static void serverLoginEvent(final PlayerEvent.PlayerLoggedInEvent event) {
      PlayerEntity player = event.getPlayer();
      if (player != null && player.isAlive()) {
        IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
        capability.getAcceptedQuests().forEach(instance -> instance.setPlayer(player));

        syncCapability(player);
      }
    }

    @SubscribeEvent
    public static void changeDimesionEvent(final PlayerEvent.PlayerChangedDimensionEvent event) {
      PlayerEntity player = event.getPlayer();
      if (player != null && player.isAlive()) {
        syncCapability(player);
      }
    }

    @SubscribeEvent
    public static void respawnEvent(final PlayerEvent.PlayerRespawnEvent event) {
      PlayerEntity player = event.getPlayer();
      if (player != null && player.isAlive()) {
        syncCapability(player);
      }
    }

    private static void syncCapability(PlayerEntity player) {
      IQuestCapability questCapability = QuestCapabilityProvider.getCapability(player);

      if (questCapability.getTrackedQuest() != null)
        PacketDispatcher.sendTo(new SSyncQuestCapability(questCapability.getTrackedQuest()), player);

      PacketDispatcher.sendTo(new SSyncQuestCapability(questCapability.getAcceptedQuests().toArray(new QuestInstance[0])), player);
      PacketDispatcher.sendTo(new SSyncQuestCapability(questCapability.getCompletedQuests().toArray(new String[0])), player);
    }
  }
}
