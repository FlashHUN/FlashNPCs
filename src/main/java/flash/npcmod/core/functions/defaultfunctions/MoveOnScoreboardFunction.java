package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SMoveToDialogue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;

public class MoveOnScoreboardFunction extends AbstractFunction {

  public MoveOnScoreboardFunction() {
    super("moveOnScoreboard", new String[]{"objective", "value", "min|max|exact", "trueOption", "falseOption"}, empty);
  }

  @Override
  public void call(String[] params, ServerPlayer sender, NpcEntity npcEntity) {
    if (params.length == 5) {
      int value;
      try {
        value = Integer.parseInt(params[1]);
      } catch (NumberFormatException e) {
        return;
      }

      if (!params[2].equals("min") && !params[2].equals("max"))
        params[2] = "exact";

      Objective scoreObjective = sender.getScoreboard().getOrCreateObjective(params[0]);
      if (scoreObjective != null) {
        int points = sender.getScoreboard().getOrCreatePlayerScore(sender.getName().getString(), scoreObjective).getScore();

        boolean canMoveTo;

        if (params[2].equals("min")) canMoveTo = value <= points;
        else if (params[2].equals("max")) canMoveTo = value >= points;
        else canMoveTo = value == points;

        if (canMoveTo)
          PacketDispatcher.sendTo(new SMoveToDialogue(params[3], npcEntity.getId()), sender);
        else if (!params[4].isEmpty())
          PacketDispatcher.sendTo(new SMoveToDialogue(params[4], npcEntity.getId()), sender);

        debugUsage(sender, npcEntity);
      }
    } else {
      warnParameterAmount(npcEntity);
    }
  }
}
