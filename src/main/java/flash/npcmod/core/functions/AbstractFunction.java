package flash.npcmod.core.functions;

import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public abstract class AbstractFunction {
  protected static final String[] empty = new String[0];

  protected String name;
  protected String[] paramNames;
  protected String[] callables;

  public AbstractFunction(String name, String[] paramNames, String[] callables) {
    this.name = name;
    this.paramNames = paramNames;
    this.callables = callables;
  }

  public String[] getCallables() {
    return callables;
  }

  public abstract void call(String[] params, ServerPlayer sender, NpcEntity npcEntity);

  public String getName() {
    return name;
  }

  public String[] getParamNames() {
    return paramNames;
  }

  protected void warnParameterAmount(ServerPlayer sender, NpcEntity npcEntity) {
    String warningMessage = "Function " + name + " in " + npcEntity.getName().getString() + " does not have the right amount of parameters";
    if (FunctionUtil.isDebugMode()) {
      sender.sendMessage(new TextComponent("[FlashNPCs] ".concat(warningMessage)), ChatType.SYSTEM, Util.NIL_UUID);
    }
    Main.LOGGER.warn(warningMessage);
  }

  protected void debugUsage(ServerPlayer sender, NpcEntity npcEntity) {
    Main.LOGGER.debug(sender.getName().getString() + " used function " + name + " from npc " + npcEntity.getName().getString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AbstractFunction function = (AbstractFunction) o;
    return name.equals(function.name);
  }
}
