package flash.npcmod.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.quests.CommonQuestUtil;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SOpenScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class QuestsCommand extends Command {
  @Override
  public String getName() {
    return "quests";
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 4;
  }

  @Override
  public void build(LiteralArgumentBuilder<CommandSourceStack> builder) {
    builder.then(literal("list")
        .executes(context -> list(context.getSource())));

    builder.then(literal("edit")
        .then(argument("quest", StringArgumentType.string())
            .executes(context -> edit(context.getSource(), StringArgumentType.getString(context, "quest")))));

    builder.then(literal("completeObjective")
        .then(argument("player", EntityArgument.player())
        .then(argument("quest", StringArgumentType.string())
        .then(argument("objective", StringArgumentType.greedyString())
            .executes(context -> completeObjective(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "quest"), StringArgumentType.getString(context, "objective")))))));
  
    builder.then(literal("completeQuest")
            .then(argument("player", EntityArgument.player())
                    .then(argument("quest", StringArgumentType.string())
                            .then(argument("completeAllObjetives", BoolArgumentType.bool())
                                    .executes(context -> completeQuest(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "quest"), BoolArgumentType.getBool(context, "completeAllObjetives")))))));

    builder.then(literal("reload").executes(context -> reloadAllQuests(context.getSource())));

    builder.then(literal("reset")
        .then(argument("player", EntityArgument.player())
        .then(argument("quest", StringArgumentType.string())
            .executes(context -> reset(context.getSource(), EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "quest"))))));
  }

  @Override
  public boolean isDedicatedServerOnly() {
    return false;
  }

  private int list(CommandSourceStack source) {
    List<MutableComponent> quests = new ArrayList<>();
    CommonQuestUtil.QUESTS.forEach(quest -> {
      quests.add(new TextComponent(quest.getName()).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(quest.getDisplayName()))).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/flashnpcs quests edit " + quest.getName()))));
    });

    TextComponent questsComponent = new TextComponent("List of Quests: ");
    for (int i = 0; i < quests.size(); i++) {
      questsComponent.append(ComponentUtils.wrapInSquareBrackets(quests.get(i)).withStyle(ChatFormatting.GREEN));
      if (i < quests.size()-1)
        questsComponent.append(", ");
    }
    source.sendSuccess(questsComponent, false);
    return quests.size();
  }

  private int edit(CommandSourceStack source, String quest) throws CommandSyntaxException {
    Player player = source.getPlayerOrException();
    PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.QUESTEDITOR, quest, 0), player);
    return 0;
  }

  private int completeObjective(CommandSourceStack source, Player player, String questName, String objectiveName) {
    if (player.isAlive()) {
      IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
      QuestInstance instance = null;
      for (QuestInstance questInstance : capability.getAcceptedQuests()) {
        if (questInstance.getQuest().getName().equals(questName)) {
          instance = questInstance;
          break;
        }
      }
      if (instance != null) {
        Quest quest = instance.getQuest();
        QuestObjective objective = null;
        for (QuestObjective questObjective : quest.getObjectives()) {
          if (questObjective.getName().equals(objectiveName)) {
            objective = questObjective;
            break;
          }
        }
        if (objective != null) {
          objective.forceComplete();
          source.sendSuccess(new TextComponent("Completed quest objective " + objectiveName + " in " + questName + " for " + player.getName().getString()).withStyle(ChatFormatting.GREEN), true);
        }
        else
          source.sendSuccess(new TextComponent("The Quest " + questName + " doesn't have an Objective named " + objectiveName).withStyle(ChatFormatting.RED), true);
      } else
        source.sendSuccess(new TextComponent(player.getName().getString() + " doesn't have the Quest " + questName).withStyle(ChatFormatting.RED), true);
    }
    return 0;
  }
  
  private int completeQuest(CommandSourceStack source, Player player, String questName, boolean completeAllObjetives) {
    if (player.isAlive()) {
      IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
      QuestInstance instance = null;
      for (QuestInstance questInstance : capability.getAcceptedQuests()) {
        if (questInstance.getQuest().getName().equals(questName)) {
          instance = questInstance;
          break;
        }
      }
      if (instance != null) {
        Quest quest = instance.getQuest();
        if(completeAllObjetives){
          for (QuestObjective questObjective : quest.getObjectives()) {
            if (!questObjective.isComplete()) {
              questObjective.forceComplete();
            }
          }
        }
        //completamos la mision
        capability.completeQuest(instance);
        
      } else
        source.sendSuccess(new TextComponent(player.getName().getString() + " doesn't have the Quest " + questName).withStyle(ChatFormatting.RED), true);
    }
    return 0;
  }

  private int reloadAllQuests(CommandSourceStack source) {
    CommonQuestUtil.loadAllQuests();
    List<ServerPlayer> playerList = source.getServer().getPlayerList().getPlayers();
    for (ServerPlayer player : playerList) {
      CommonQuestUtil.syncPlayerQuests(player);
    }
    source.sendSuccess(new TextComponent("Reloaded all quests!").withStyle(ChatFormatting.GREEN), true);
    return 0;
  }

  private int reset(CommandSourceStack source, Player player, String quest) {
    if (player != null && player.isAlive()) {
      Quest quest1 = CommonQuestUtil.fromName(quest);
      if (quest1 != null) {
        IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
        QuestInstance toRemoveInstance = null;
        for (QuestInstance questInstance : capability.getAcceptedQuests()) {
          if (questInstance.getQuest().equals(quest1)) {
            toRemoveInstance = questInstance;
            break;
          }
        }
        String toRemoveName = null;
        for (String questName : capability.getCompletedQuests()) {
          if (questName.equals(quest)) {
            toRemoveName = quest;
            break;
          }
        }
        if (toRemoveInstance != null || toRemoveName != null) {
          if (toRemoveInstance != null)
            capability.abandonQuest(toRemoveInstance);
          if (toRemoveName != null)
            capability.getCompletedQuests().remove(toRemoveName);
          source.sendSuccess(new TextComponent("Reset " + player.getName().getString() + "'s quest progress for quest " + quest).withStyle(ChatFormatting.GREEN), true);
        }
        else
          source.sendSuccess(new TextComponent(player.getName().getString() + " doesn't have any progress in " + quest).withStyle(ChatFormatting.RED), true);
      }
      else
        source.sendSuccess(new TextComponent(quest + " does not exist").withStyle(ChatFormatting.RED), true);
    }
    return 0;
  }
}
