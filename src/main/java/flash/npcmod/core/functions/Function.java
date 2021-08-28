package flash.npcmod.core.functions;

import flash.npcmod.config.ConfigHolder;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class Function extends AbstractFunction {

  public Function(String name, String[] paramNames, String[] callables) {
    super(name, paramNames, callables);
  }

  public void call(String[] params, ServerPlayerEntity sender, NpcEntity npcEntity) {
    if (params.length == paramNames.length || (paramNames.length == 1 && paramNames[0].isEmpty())) {
      for (String callable : callables) {
        if (callable.startsWith("function:")) {
          String function = callable;
          if (paramNames.length > 0 && !paramNames[0].isEmpty()) {
            for (int i = 0; i < paramNames.length; i++) {
              String param = params[i];
              function = FunctionUtil.replaceParameters(function, paramNames[i], param);
            }
          }
          FunctionUtil.callFromName(function, sender, npcEntity);
        } else if (callable.startsWith("/")) {
          if (ConfigHolder.COMMON.isInvalidCommand(callable)) continue;

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
      debugUsage(sender, npcEntity);
    } else {
      warnParameterAmount(npcEntity);
    }
  }
}
