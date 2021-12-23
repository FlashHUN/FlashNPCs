package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityAttacher;
import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.core.quests.CommonQuestUtil;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SAcceptQuest;
import net.minecraft.server.level.ServerPlayer;

public class AcceptQuestFunction extends AbstractFunction {

  public AcceptQuestFunction() {
    super("acceptQuest", new String[]{"questName"}, empty);
  }

  @Override
  public void call(String[] params, ServerPlayer sender, NpcEntity npcEntity) {
    if (params.length == 1) {
      Quest quest = CommonQuestUtil.fromName(params[0]);
      if (quest != null) {
        IQuestCapability capability = QuestCapabilityAttacher.getCapability(sender);
        QuestInstance questInstance = new QuestInstance(quest, npcEntity.getUUID(), npcEntity.getName().getString(), sender);
        if (!capability.getAcceptedQuests().contains(questInstance)) {
          capability.acceptQuest(questInstance);
          PacketDispatcher.sendTo(new SAcceptQuest(params[0], npcEntity.getId()), sender);
        }
      }
      debugUsage(sender, npcEntity);
    } else {
      warnParameterAmount(npcEntity);
    }
  }

}
