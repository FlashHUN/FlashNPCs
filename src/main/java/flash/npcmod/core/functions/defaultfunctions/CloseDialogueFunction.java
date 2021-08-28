package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SCloseDialogue;
import net.minecraft.entity.player.ServerPlayerEntity;

public class CloseDialogueFunction extends AbstractFunction {

  public CloseDialogueFunction() {
    super("close", empty, empty);
  }

  @Override
  public void call(String[] params, ServerPlayerEntity sender, NpcEntity npcEntity) {
    PacketDispatcher.sendTo(new SCloseDialogue(), sender);
    debugUsage(sender, npcEntity);
  }

}
