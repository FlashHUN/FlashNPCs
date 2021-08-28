package flash.npcmod.commands;

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
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

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
  public void build(LiteralArgumentBuilder<CommandSource> builder) {
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

  private int list(CommandSource source) {
    List<IFormattableTextComponent> quests = new ArrayList<>();
    CommonQuestUtil.QUESTS.forEach(quest -> {
      quests.add(new StringTextComponent(quest.getName()).setStyle(Style.EMPTY.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent(quest.getDisplayName()))).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/flashnpcs quests edit " + quest.getName()))));
    });

    StringTextComponent questsComponent = new StringTextComponent("List of Quests: ");
    for (int i = 0; i < quests.size(); i++) {
      questsComponent.appendSibling(TextComponentUtils.wrapWithSquareBrackets(quests.get(i)).mergeStyle(TextFormatting.GREEN));
      if (i < quests.size()-1)
        questsComponent.appendString(", ");
    }
    source.sendFeedback(questsComponent, false);
    return quests.size();
  }

  private int edit(CommandSource source, String quest) throws CommandSyntaxException {
    PlayerEntity player = source.asPlayer();
    PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.QUESTEDITOR, quest, 0), player);
    return 0;
  }

  private int completeObjective(CommandSource source, PlayerEntity player, String questName, String objectiveName) {
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
          source.sendFeedback(new StringTextComponent("Completed quest objective " + objectiveName + " in " + questName + " for " + player.getName().getString()).mergeStyle(TextFormatting.GREEN), true);
        }
        else
          source.sendFeedback(new StringTextComponent("The Quest " + questName + " doesn't have an Objective named " + objectiveName).mergeStyle(TextFormatting.RED), true);
      } else
        source.sendFeedback(new StringTextComponent(player.getName().getString() + " doesn't have the Quest " + questName).mergeStyle(TextFormatting.RED), true);
    }
    return 0;
  }

  private int reloadAllQuests(CommandSource source) {
    CommonQuestUtil.loadAllQuests();
    List<ServerPlayerEntity> playerList = source.getServer().getPlayerList().getPlayers();
    for (ServerPlayerEntity player : playerList) {
      CommonQuestUtil.syncPlayerQuests(player);
    }
    source.sendFeedback(new StringTextComponent("Reloaded all quests!").mergeStyle(TextFormatting.GREEN), true);
    return 0;
  }

  private int reset(CommandSource source, PlayerEntity player, String quest) {
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
          source.sendFeedback(new StringTextComponent("Reset " + player.getName().getString() + "'s quest progress for quest " + quest).mergeStyle(TextFormatting.GREEN), true);
        }
        else
          source.sendFeedback(new StringTextComponent(player.getName().getString() + " doesn't have any progress in " + quest).mergeStyle(TextFormatting.RED), true);
      }
      else
        source.sendFeedback(new StringTextComponent(quest + " does not exist").mergeStyle(TextFormatting.RED), true);
    }
    return 0;
  }
}
