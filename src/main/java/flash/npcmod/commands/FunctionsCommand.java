package flash.npcmod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import flash.npcmod.core.functions.Function;
import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.EntityInit;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

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
  public void build(LiteralArgumentBuilder<CommandSource> builder) {
    builder.then(literal("list").executes(context -> list(context.getSource())));

    builder.then(literal("run")
        .then(argument("function", StringArgumentType.string())
        .executes(context -> runAs(context.getSource(), context.getSource().asPlayer(), StringArgumentType.getString(context, "function")))));

    builder.then(literal("runAs")
        .then(argument("player", EntityArgument.player()).then(argument("function", StringArgumentType.string())
        .executes(context -> runAs(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "function"))))));
  }

  @Override
  public boolean isDedicatedServerOnly() {
    return false;
  }

  private int list(CommandSource source) {
    List<IFormattableTextComponent> functions = new ArrayList<>();
    FunctionUtil.FUNCTIONS.forEach(abstractFunction -> {
      if (abstractFunction instanceof Function) {
        String functionName = abstractFunction.getName();
        String[] parameterNames = abstractFunction.getParamNames();
        StringTextComponent functionTextComponent = new StringTextComponent(functionName);
        if (parameterNames.length > 0 && !(parameterNames.length == 1 && parameterNames[0].isEmpty())) {
          functionTextComponent.appendString("::");
          for (int i = 0; i < parameterNames.length; i++) {
            functionTextComponent.appendSibling(new StringTextComponent(parameterNames[i]).mergeStyle(TextFormatting.AQUA));
            if (i < parameterNames.length - 1)
              functionTextComponent.appendString(",");
          }
        }
        StringBuilder callables = new StringBuilder();
        for (int i = 0; i < abstractFunction.getCallables().length; i++) {
          String callable = abstractFunction.getCallables()[i];
          callables.append(callable);
          if (i < abstractFunction.getCallables().length - 1)
            callables.append("\n");
        }
        functions.add(functionTextComponent.setStyle(Style.EMPTY.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(callables.toString())))));
      }
    });

    StringTextComponent functionsComponent = new StringTextComponent("List of Functions: ");
    for (int i = 0; i < functions.size(); i++) {
      functionsComponent.appendSibling(TextComponentUtils.wrapWithSquareBrackets(functions.get(i)).mergeStyle(TextFormatting.GREEN));
      if (i < functions.size()-1)
        functionsComponent.appendString(", ");
    }

    source.sendFeedback(functionsComponent, false);
    return functions.size();
  }

  private int runAs(CommandSource source, ServerPlayerEntity player, String name) {
    NpcEntity npcEntity = EntityInit.NPC_ENTITY.get().create(player.world);
    npcEntity.setCustomName(new StringTextComponent("FAKE NPC"));
    FunctionUtil.callFromName(name, player, npcEntity);
    source.sendFeedback(new StringTextComponent("Called Function " + name + " as " + player.getName().getString()).mergeStyle(TextFormatting.GREEN), true);
    return 0;
  }
}
