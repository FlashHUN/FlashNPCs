package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SMoveToDialogue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

public class MoveOnTagFunction extends AbstractFunction {

  public MoveOnTagFunction() {
    super("moveOnTag", new String[]{"tag", "trueOption", "falseOption"}, empty);
  }

  @Override
  public void call(String[] params, ServerPlayer sender, NpcEntity npcEntity) {
    if (params.length == 3) {
      String tag = params[0];
      if (sender.getTags().contains(tag))
        PacketDispatcher.sendTo(new SMoveToDialogue(params[1], npcEntity.getId()), sender);
      else
        PacketDispatcher.sendTo(new SMoveToDialogue(params[2], npcEntity.getId()), sender);

      debugUsage(sender, npcEntity);
    } else {
      warnParameterAmount(sender, npcEntity);
    }
  }
}
