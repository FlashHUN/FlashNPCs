package flash.npcmod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import flash.npcmod.core.functions.Function;
import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.EntityInit;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class FunctionsCommand extends Command {
  @Override
  public String getName() {
    return "functions";
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 4;
  }

  @Override
  public void build(LiteralArgumentBuilder<CommandSourceStack> builder) {
    builder.then(literal("list").executes(context -> list(context.getSource())));

    builder.then(literal("run")
        .then(argument("function", StringArgumentType.string())
        .executes(context -> runAs(context.getSource(), context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "function")))));

    builder.then(literal("runAs")
        .then(argument("player", EntityArgument.player()).then(argument("function", StringArgumentType.string())
        .executes(context -> runAs(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "function"))))));
  }

  @Override
  public boolean isDedicatedServerOnly() {
    return false;
  }

  private int list(CommandSourceStack source) {
    List<MutableComponent> functions = new ArrayList<>();
    FunctionUtil.FUNCTIONS.forEach(abstractFunction -> {
      if (abstractFunction instanceof Function) {
        String functionName = abstractFunction.getName();
        String[] parameterNames = abstractFunction.getParamNames();
        TextComponent functionTextComponent = new TextComponent(functionName);
        if (parameterNames.length > 0 && !(parameterNames.length == 1 && parameterNames[0].isEmpty())) {
          functionTextComponent.append("::");
          for (int i = 0; i < parameterNames.length; i++) {
            functionTextComponent.append(new TextComponent(parameterNames[i]).withStyle(ChatFormatting.AQUA));
            if (i < parameterNames.length - 1)
              functionTextComponent.append(",");
          }
        }
        StringBuilder callables = new StringBuilder();
        for (int i = 0; i < abstractFunction.getCallables().length; i++) {
          String callable = abstractFunction.getCallables()[i];
          callables.append(callable);
          if (i < abstractFunction.getCallables().length - 1)
            callables.append("\n");
        }
        functions.add(functionTextComponent.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(callables.toString())))));
      }
    });

    TextComponent functionsComponent = new TextComponent("List of Functions: ");
    for (int i = 0; i < functions.size(); i++) {
      functionsComponent.append(ComponentUtils.wrapInSquareBrackets(functions.get(i)).withStyle(ChatFormatting.GREEN));
      if (i < functions.size()-1)
        functionsComponent.append(", ");
    }

    source.sendSuccess(functionsComponent, false);
    return functions.size();
  }

  private int runAs(CommandSourceStack source, ServerPlayer player, String name) {
    NpcEntity npcEntity = EntityInit.NPC_ENTITY.get().create(player.level);
    npcEntity.setCustomName(new TextComponent("FAKE NPC"));
    FunctionUtil.callFromName(name, player, npcEntity);
    source.sendSuccess(new TextComponent("Called Function " + name + " as " + player.getName().getString()).withStyle(ChatFormatting.GREEN), true);
    return 0;
  }
}
