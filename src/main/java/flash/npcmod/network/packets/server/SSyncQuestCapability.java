package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

public class SSyncQuestCapability {

  CapabilityType type;
  String trackedQuest;
  QuestInstance[] acceptedQuests;
  String[] completedQuests;
  Map<QuestObjective, Integer> objectiveProgressMap;

  public SSyncQuestCapability() {
    this.type = CapabilityType.TRACKED_QUEST;
    this.trackedQuest = "";
  }

  public SSyncQuestCapability(String trackedQuest) {
    this.type = CapabilityType.TRACKED_QUEST;
    this.trackedQuest = trackedQuest;
  }

  public SSyncQuestCapability(QuestInstance[] acceptedQuests) {
    this.type = CapabilityType.ACCEPTED_QUESTS;
    this.acceptedQuests = acceptedQuests;
  }

  public SSyncQuestCapability(String[] completedQuests) {
    this.type = CapabilityType.COMPLETED_QUESTS;
    this.completedQuests = completedQuests;
  }

  public SSyncQuestCapability(Map<QuestObjective, Integer> objectiveProgressMap) {
    this.type = CapabilityType.PROGRESS_MAP;
    this.objectiveProgressMap = objectiveProgressMap;
  }

  public static void encode(SSyncQuestCapability msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.type.ordinal());
    switch (msg.type) {
      case TRACKED_QUEST:
        if (msg.trackedQuest != null) {
          buf.writeUtf(msg.trackedQuest, 51);
        } else
          buf.writeUtf("");
        break;
      case ACCEPTED_QUESTS:
        buf.writeInt(msg.acceptedQuests.length);
        for (QuestInstance questInstance : msg.acceptedQuests) {
          buf.writeUtf(questInstance.getQuest().getName(), 51);
          buf.writeUUID(questInstance.getPickedUpFrom());
          buf.writeUtf(questInstance.getPickedUpFromName(), 200);
          for (int i = 0; i < questInstance.getQuest().getObjectives().size(); i++) {
            QuestObjective objective = questInstance.getQuest().getObjectives().get(i);
            buf.writeInt(objective.getId());
            buf.writeInt(objective.getProgress());
            buf.writeBoolean(objective.isHidden());
          }
        }
        break;
      case COMPLETED_QUESTS:
        buf.writeInt(msg.completedQuests.length);
        for (String name : msg.completedQuests) {
          buf.writeUtf(name, 51);
        }
        break;
      case PROGRESS_MAP:
        buf.writeInt(msg.objectiveProgressMap.keySet().size());
        msg.objectiveProgressMap.forEach((questObjective, progress) -> {
          buf.writeUtf(questObjective.getQuest().getName()+":::"+questObjective.getName(), 300);
          buf.writeInt(questObjective.getProgress());
        });
        break;
    }
  }

  public static SSyncQuestCapability decode(FriendlyByteBuf buf) {
    return Main.PROXY.decodeQuestCapabilitySync(buf);
  }

  public static void handle(SSyncQuestCapability msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      switch (msg.type) {
        case TRACKED_QUEST:
          Main.PROXY.syncTrackedQuest(msg.trackedQuest);
          break;
        case ACCEPTED_QUESTS:
          Main.PROXY.syncAcceptedQuests(new ArrayList<>(Arrays.asList(msg.acceptedQuests)));
          break;
        case COMPLETED_QUESTS:
          Main.PROXY.syncCompletedQuests(new ArrayList<>(Arrays.asList(msg.completedQuests)));
          break;
        case PROGRESS_MAP:
          Main.PROXY.syncQuestProgressMap(msg.objectiveProgressMap);
          break;
      }
    });
    ctx.get().setPacketHandled(true);
  }

  public enum CapabilityType {
    TRACKED_QUEST,
    ACCEPTED_QUESTS,
    COMPLETED_QUESTS,
    PROGRESS_MAP
  }
}
