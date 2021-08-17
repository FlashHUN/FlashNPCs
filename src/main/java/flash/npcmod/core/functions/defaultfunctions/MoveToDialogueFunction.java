package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.core.functions.Function;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SMoveToDialogue;
import net.minecraft.entity.player.ServerPlayerEntity;

public class MoveToDialogueFunction extends Function {

  public MoveToDialogueFunction() {
    super("moveToDialogue", new String[]{"dialogueName"}, empty);
  }

  @Override
  public String call(String[] params, ServerPlayerEntity sender) {
    PacketDispatcher.sendTo(new SMoveToDialogue(params[0]), sender);
    return "";
  }
}
