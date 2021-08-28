package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SMoveToDialogue;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.scoreboard.ScoreObjective;

public class MoveOnScoreboardFunction extends AbstractFunction {

  public MoveOnScoreboardFunction() {
    super("moveOnScoreboard", new String[]{"objective", "value", "min|max|exact", "trueOption", "falseOption"}, empty);
  }

  @Override
  public void call(String[] params, ServerPlayerEntity sender, NpcEntity npcEntity) {
    if (params.length == 5) {
      int value;
      try {
        value = Integer.parseInt(params[1]);
      } catch (NumberFormatException e) {
        return;
      }

      if (!params[2].equals("min") && !params[2].equals("max"))
        params[2] = "exact";

      ScoreObjective scoreObjective = sender.getWorldScoreboard().getObjective(params[0]);
      if (scoreObjective != null) {
        int points = sender.getWorldScoreboard().getOrCreateScore(sender.getName().getString(), scoreObjective).getScorePoints();

        boolean canMoveTo;

        if (params[2].equals("min")) canMoveTo = value <= points;
        else if (params[2].equals("max")) canMoveTo = value >= points;
        else canMoveTo = value == points;

        if (canMoveTo)
          PacketDispatcher.sendTo(new SMoveToDialogue(params[3], npcEntity.getEntityId()), sender);
        else if (!params[4].isEmpty())
          PacketDispatcher.sendTo(new SMoveToDialogue(params[4], npcEntity.getEntityId()), sender);

        debugUsage(sender, npcEntity);
      }
    } else {
      warnParameterAmount(npcEntity);
    }
  }
}
