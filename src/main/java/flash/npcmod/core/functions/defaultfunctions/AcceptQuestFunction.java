package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.core.quests.CommonQuestUtil;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SAcceptQuest;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.UUID;

public class AcceptQuestFunction extends AbstractFunction {

  public AcceptQuestFunction() {
    super("acceptQuest", new String[]{"questName","[turnInType]","[turnInUuid]"}, empty);
  }

  @Override
  public void call(String[] params, ServerPlayer sender, NpcEntity npcEntity) {
    if (params.length >= 1 && params.length <= 3) {
      Quest quest = CommonQuestUtil.fromName(params[0]);
      if (quest != null) {
        IQuestCapability capability = QuestCapabilityProvider.getCapability(sender);
        UUID uuid = npcEntity.getUUID();
        QuestInstance.TurnInType turnInType = QuestInstance.TurnInType.QuestGiver;
        if (params.length == 2 && params[1].equalsIgnoreCase("auto")) {
          turnInType = QuestInstance.TurnInType.AutoTurnIn;
        }
        else if (params.length == 3 && params[1].equalsIgnoreCase("npc")) {
          try {
            uuid = UUID.fromString(params[2]);
            turnInType = QuestInstance.TurnInType.NpcByUuid;
          } catch (Exception e) {
            uuid = npcEntity.getUUID();
          }
        }
        QuestInstance questInstance = new QuestInstance(quest, uuid, npcEntity.getName().getString(), turnInType, sender);
        if (!capability.getAcceptedQuests().contains(questInstance)) {
          capability.acceptQuest(questInstance);
          PacketDispatcher.sendTo(new SAcceptQuest(params[0], npcEntity.getId(), turnInType, uuid), sender);
        }
      }
      debugUsage(sender, npcEntity);
    } else {
      warnParameterAmount(sender, npcEntity);
    }
  }

}
