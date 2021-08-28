package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SMoveToDialogue;
import net.minecraft.entity.player.ServerPlayerEntity;

public class MoveToDialogueFunction extends AbstractFunction {

  public MoveToDialogueFunction() {
    super("moveToDialogue", new String[]{"dialogueName"}, empty);
  }

  @Override
  public void call(String[] params, ServerPlayerEntity sender, NpcEntity npcEntity) {
    if (params.length == 1) {
      PacketDispatcher.sendTo(new SMoveToDialogue(params[0], npcEntity.getEntityId()), sender);
      debugUsage(sender, npcEntity);
    } else {
      warnParameterAmount(npcEntity);
    }
  }
}
