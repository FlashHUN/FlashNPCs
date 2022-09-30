package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SMoveToDialogue;
import net.minecraft.server.level.ServerPlayer;

public class MoveOnCompleteQuestFunction extends AbstractFunction {

  public MoveOnCompleteQuestFunction() {
    super("moveOnCompleteQuest", new String[]{"questName","trueOption","falseOption"}, empty);
  }

  @Override
  public void call(String[] params, ServerPlayer sender, NpcEntity npcEntity) {
    if (params.length == 3) {
      IQuestCapability capability = QuestCapabilityProvider.getCapability(sender);
      if (capability.getCompletedQuests().contains(params[0]))
        PacketDispatcher.sendTo(new SMoveToDialogue(params[1], npcEntity.getId()), sender);
      else if (!params[2].isEmpty())
        PacketDispatcher.sendTo(new SMoveToDialogue(params[2], npcEntity.getId()), sender);

      debugUsage(sender, npcEntity);
    } else {
      warnParameterAmount(sender, npcEntity);
    }
  }
}
