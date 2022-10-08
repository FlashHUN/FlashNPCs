package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SMoveToDialogue;
import net.minecraft.server.level.ServerPlayer;

public class MoveToDialogueFunction extends AbstractFunction {

  public MoveToDialogueFunction() {
    super("moveToDialogue", new String[]{"dialogueName"}, empty);
  }

  @Override
  public void call(String[] params, ServerPlayer sender, NpcEntity npcEntity) {
    if (params.length == 1) {
      PacketDispatcher.sendTo(new SMoveToDialogue(params[0], npcEntity.getId()), sender);
      debugUsage(sender, npcEntity);
    } else {
      warnParameterAmount(sender, npcEntity);
    }
  }
}
