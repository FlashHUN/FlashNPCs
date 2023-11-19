package flash.npcmod.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DebugCommand extends Command {

  @Override
  public String getName() {
    return "debug";
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 4;
  }

  @Override
  public void build(LiteralArgumentBuilder<CommandSourceStack> builder) {
//    builder.then(literal("player")
//            .then(argument("player", EntityArgument.player())
//            .then(literal("capability")
//                    .then(literal("quests")
//                            .then(literal("tracked").executes(context -> tracked(context.getSource(), EntityArgument.getPlayer(context, "player"))))
//                            .then(literal("accepted").executes(context -> accepted(context.getSource(), EntityArgument.getPlayer(context, "player"))))
//                            .then(literal("completed").executes(context -> completed(context.getSource(), EntityArgument.getPlayer(context, "player"))))
//                    )
//            ))
//    );

    builder/*.then(literal("functions")*/.executes(context -> toggleFunctionDebugMode(context.getSource()))/*)*/;
  }

  @Override
  public boolean isDedicatedServerOnly() {
    return false;
  }

  private int tracked(CommandSourceStack source, ServerPlayer player) {
    IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
    source.sendSuccess(new TextComponent(player.getName().getString() + "'s tracked quest: " + (capability.getTrackedQuest() == null ? "" : capability.getTrackedQuest())), false);
    return 1;
  }

  private int accepted(CommandSourceStack source, ServerPlayer player) {
    IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
    source.sendSuccess(new TextComponent(player.getName().getString() + "'s accepted quests:"), false);

    ArrayList<QuestInstance> accepted = capability.getAcceptedQuests();
    if (accepted.size() > 0) {
      for (QuestInstance questInstance : accepted) {
        Quest quest = questInstance.getQuest();
        source.sendSuccess(new TextComponent("- " + quest.getDisplayName() + " [" +quest.getName() + "]"), false);
      }
      return 1;
    }
    return 0;
  }

  private int completed(CommandSourceStack source, ServerPlayer player) {
    IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
    source.sendSuccess(new TextComponent(player.getName().getString() + "'s completed quests:"), false);

    ArrayList<String> completed = capability.getCompletedQuests();
    if (completed.size() > 0) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < completed.size(); i++) {
        sb.append(completed.get(i));
        if (i < completed.size()-1) {
          sb.append(", ");
        }
      }
      source.sendSuccess(new TextComponent(sb.toString()), false);
      return 1;
    }
    return 0;
  }

  private int toggleFunctionDebugMode(CommandSourceStack source) {
    FunctionUtil.toggleDebugMode();
    boolean debugMode = FunctionUtil.isDebugMode();
    source.sendSuccess(new TextComponent("Toggled Debug Mode to " + debugMode).withStyle(debugMode ? ChatFormatting.GREEN : ChatFormatting.RED), true);
    return 0;
  }
}
