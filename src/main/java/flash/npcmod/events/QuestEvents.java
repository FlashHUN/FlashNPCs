package flash.npcmod.events;

import flash.npcmod.Main;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncQuestCapability;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Map;

import static flash.npcmod.core.ItemUtil.*;

public class QuestEvents {

  @SubscribeEvent
  public void playerTick(TickEvent.PlayerTickEvent event) {
    PlayerEntity player = event.player;
    if (player != null && player.isAlive()) {
      IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
      ArrayList<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
      if (event.side.isServer()) {
        Map<QuestObjective, Integer> progressMap = capability.getQuestProgressMap();
        acceptedQuests.forEach(questInstance -> {
          questInstance.getQuest().getObjectives().forEach(objective -> {
            if (!objective.isHidden()) {
              QuestObjective.ObjectiveType type = objective.getType();
              switch (type) {
                case Gather:
                  // This should get taken on objective.complete()
                  ItemStack gatherStack = objective.getObjective();
                  int progress = getAmount(player, gatherStack);
                  objective.setProgress(progress);
                  break;
                case Find:
                  BlockPos[] area = objective.getObjective();
                  if (isPlayerInArea(player, area))
                    objective.setProgress(objective.getAmount());
                  break;
                case DeliverToLocation:
                  ItemStack toDeliver = objective.getObjective();
                  BlockPos[] deliveryArea = objective.getSecondaryObjective();
                  if (isPlayerInArea(player, deliveryArea) && hasItem(player, toDeliver)) {
                    int prevProgress = objective.getProgress();
                    objective.setProgress(objective.getProgress() + getAmount(player, toDeliver));
                    takeStack(player, toDeliver, objective.getAmount() - prevProgress);
                  }
                  break;
                case Scoreboard:
                  Scoreboard scoreboard = player.getWorldScoreboard();
                  ScoreObjective scoreObjective = scoreboard.getObjective(objective.primaryToString());
                  if (scoreObjective != null)
                    objective.setProgress(scoreboard.getOrCreateScore(player.getName().getString(), scoreObjective).getScorePoints());
                  break;
              }
              if (objective.isComplete())
                objective.onComplete(player);
            }
            progressMap.put(objective, objective.getProgress());
          });
        });
        PacketDispatcher.sendTo(new SSyncQuestCapability(capability.getAcceptedQuests().toArray(new QuestInstance[0])), player);
      }
    }
  }

  @SubscribeEvent
  public void livingDeathEvent(LivingDeathEvent event) {
    if (event.getSource().getTrueSource() instanceof PlayerEntity) {
      PlayerEntity player = (PlayerEntity) event.getSource().getTrueSource();
      if (player != null && player.isAlive() && !player.world.isRemote) {
        IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
        Map<QuestObjective, Integer> progressMap = capability.getQuestProgressMap();
        progressMap.forEach((objective, integer) -> {
          if (!objective.isHidden()) {
            if (objective.getType().equals(QuestObjective.ObjectiveType.Kill)) {
              if (EntityType.getKey(event.getEntityLiving().getType()).toString().equals(objective.getObjective()))
                objective.setProgress(objective.getProgress() + 1);
            }
          }
        });
      }
    }
  }

  @SubscribeEvent
  public void interact(PlayerInteractEvent.EntityInteractSpecific event) {
    PlayerEntity player = event.getPlayer();
    if (player != null && player.isAlive()) {
      IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
      ArrayList<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
      if (event.getSide().isServer()) {
        acceptedQuests.forEach(questInstance -> {
          questInstance.getQuest().getObjectives().forEach(objective -> {
            if (!objective.isHidden()) {
              QuestObjective.ObjectiveType type = objective.getType();
              switch (type) {
                case DeliverToEntity:
                  if (matches(objective.getObjective(), event.getItemStack())
                      && EntityType.getKey(event.getTarget().getType()).toString().equals(objective.getSecondaryObjective())) {
                    int prevProgress = objective.getProgress();
                    objective.setProgress(objective.getProgress() + getAmount(player, objective.getObjective()));
                    takeStack(player, objective.getObjective(), objective.getAmount() - prevProgress);
                  }
                  break;
                case UseOnEntity:
                  if (matches(objective.getObjective(), event.getItemStack())
                      && EntityType.getKey(event.getTarget().getType()).toString().equals(objective.getSecondaryObjective()))
                    objective.setProgress(objective.getProgress() + 1);
                  break;
                case Use:
                  if (event.getItemStack().getUseDuration() == 0 && matches(objective.getObjective(), event.getItemStack())) {
                    objective.setProgress(objective.getProgress() + 1);
                  }
              }
            }
          });
        });
      }
    }
  }

  @SubscribeEvent
  public void blockInteract(PlayerInteractEvent.RightClickBlock event) {
    PlayerEntity player = event.getPlayer();
    if (player != null && player.isAlive() && event.getSide().isServer()) {
      IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
      ArrayList<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
      acceptedQuests.forEach(questInstance -> {
        questInstance.getQuest().getObjectives().forEach(objective -> {
          if (!objective.isHidden()) {
            if (objective.getType().equals(QuestObjective.ObjectiveType.UseOnBlock)) {
              if (matches(objective.getObjective(), event.getItemStack()) && event.getWorld().getBlockState(event.getHitVec().getPos()).equals(objective.getSecondaryObjective()))
                objective.setProgress(objective.getProgress() + 1);
            } else if (objective.getType().equals(QuestObjective.ObjectiveType.Use)) {
              if (event.getItemStack().getUseDuration() == 0 && matches(objective.getObjective(), event.getItemStack()))
                objective.setProgress(objective.getProgress() + 1);
            }
          }
        });
      });
    }
  }

  @SubscribeEvent
  public void itemUse(PlayerInteractEvent.RightClickItem event) {
    LivingEntity entity = event.getEntityLiving();
    if (entity instanceof PlayerEntity) {
      PlayerEntity player = (PlayerEntity) event.getEntityLiving();
      if (player != null && player.isAlive() && event.getSide().isServer()) {
        ItemStack itemStack = event.getItemStack();

        if (itemStack.getUseDuration() == 0) {
          IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
          ArrayList<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
          acceptedQuests.forEach(questInstance -> {
            questInstance.getQuest().getObjectives().forEach(objective -> {
              if (!objective.isHidden()) {
                if (objective.getType().equals(QuestObjective.ObjectiveType.Use)) {
                  if (matches(objective.getObjective(), itemStack))
                    objective.setProgress(objective.getProgress() + 1);
                }
              }
            });
          });
        }
      }
    }
  }

  @SubscribeEvent
  public void itemUse(LivingEntityUseItemEvent.Finish event) {
    LivingEntity entity = event.getEntityLiving();
    if (entity instanceof PlayerEntity) {
      PlayerEntity player = (PlayerEntity) event.getEntityLiving();
      if (player != null && player.isAlive() && !player.world.isRemote) {
        ItemStack itemStack = event.getItem();
        if (itemStack.getUseDuration() > 0) {
          IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
          ArrayList<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
          acceptedQuests.forEach(questInstance -> {
            questInstance.getQuest().getObjectives().forEach(objective -> {
              if (!objective.isHidden()) {
                if (objective.getType().equals(QuestObjective.ObjectiveType.Use)) {
                  if (matches(objective.getObjective(), itemStack))
                    objective.setProgress(objective.getProgress() + 1);
                }
              }
            });
          });
        }
      }
    }
  }

  private static boolean isPlayerInArea(PlayerEntity player, BlockPos[] area) {
    BlockPos corner1 = area[0];
    BlockPos corner2 = area[1];
    int x1 = Math.min(corner1.getX(), corner2.getX());
    int y1 = Math.min(corner1.getY(), corner2.getY());
    int z1 = Math.min(corner1.getZ(), corner2.getZ());
    int x2 = Math.max(corner1.getX(), corner2.getX());
    int y2 = Math.max(corner1.getY(), corner2.getY());
    int z2 = Math.max(corner1.getZ(), corner2.getZ());
    double playerX = player.getPosX();
    double playerY = player.getPosY();
    double playerZ = player.getPosZ();
    return playerX >= x1 && playerX <= x2 && playerY >= y1 && playerY <= y2 && playerZ >= z1 && playerZ <= z2;
  }

}
