package flash.npcmod.core.functions;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class Function {
  protected static final String[] empty = new String[0];

  private String name;
  private String[] paramNames;
  private String[] callables;

  public Function(String name, String[] paramNames, String[] callables) {
    this.name = name;
    this.paramNames = paramNames;
    this.callables = callables;
  }

  public String call(String[] params, ServerPlayerEntity sender) {
    for (String callable : callables) {
      if (callable.startsWith("function:")) {
        String function = callable;
        if (paramNames.length > 0 && !paramNames[0].isEmpty()) {
          for (int i = 0; i < paramNames.length; i++) {
            String param = params[i];
            function = FunctionUtil.replaceParameters(function, paramNames[i], param);
          }
        }
        FunctionUtil.callFromName(function, sender);
      } else if (callable.startsWith("/")) {
        String command = callable;
        command = FunctionUtil.replaceSelectors(command, sender);
        if (paramNames.length > 0 && !paramNames[0].isEmpty()) {
          for (int i = 0; i < paramNames.length; i++) {
            String param = params[i];
            command = FunctionUtil.replaceParameters(command, paramNames[i], param);
          }
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        server.getCommandManager().handleCommand(server.getCommandSource().withFeedbackDisabled(), command);
      }
    }
    return "";
  }

  public String getName() {
    return name;
  }

  public String[] getParamNames() {
    return paramNames;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Function function = (Function) o;
    return name.equals(function.name);
  }
}
