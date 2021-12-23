package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SRandomOptionFunction;
import net.minecraft.server.level.ServerPlayer;

public class RandomOptionFunction extends AbstractFunction {

  public RandomOptionFunction() {
    super("randomOption", empty, empty);
  }

  @Override
  public void call(String[] params, ServerPlayer sender, NpcEntity npcEntity) {
    PacketDispatcher.sendTo(new SRandomOptionFunction(), sender);
    debugUsage(sender, npcEntity);
  }
}
