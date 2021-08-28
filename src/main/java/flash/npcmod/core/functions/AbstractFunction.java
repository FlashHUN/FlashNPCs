package flash.npcmod.core.functions;

import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.entity.player.ServerPlayerEntity;

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

  public abstract void call(String[] params, ServerPlayerEntity sender, NpcEntity npcEntity);

  public String getName() {
    return name;
  }

  public String[] getParamNames() {
    return paramNames;
  }

  protected void warnParameterAmount(NpcEntity npcEntity) {
    Main.LOGGER.warn("Function " + name + " in " + npcEntity.getName().getString() + " does not have the right amount of parameters");
  }

  protected void debugUsage(ServerPlayerEntity sender, NpcEntity npcEntity) {
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
