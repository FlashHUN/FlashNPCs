package flash.npcmod.events;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncQuestCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static flash.npcmod.core.ItemUtil.*;

public class QuestEvents {
  @SubscribeEvent
  public void attach(AttachCapabilitiesEvent<Entity> event) {
    if (event.getObject() instanceof Player) {
      event.addCapability(QuestCapabilityProvider.IDENTIFIER, new QuestCapabilityProvider());
    }
  }

  @SubscribeEvent
  public void playerClone(PlayerEvent.Clone event) {
    event.getOriginal().reviveCaps();
    IQuestCapability oldCap = QuestCapabilityProvider.getCapability(event.getOriginal());
    IQuestCapability newCap = QuestCapabilityProvider.getCapability(event.getPlayer());

    newCap.setTrackedQuest(oldCap.getTrackedQuest());
    newCap.setAcceptedQuests(oldCap.getAcceptedQuests());
    newCap.setCompletedQuests(oldCap.getCompletedQuests());
    newCap.setQuestProgressMap(oldCap.getQuestProgressMap());
    event.getOriginal().invalidateCaps();
  }

  @SubscribeEvent
  public void serverLoginEvent(PlayerEvent.PlayerLoggedInEvent event) {
    Player player = event.getPlayer();
    if (player != null && player.isAlive()) {
      IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
      capability.getAcceptedQuests().forEach(instance -> instance.setPlayer(player));

      syncCapability(player);
    }
  }

  @SubscribeEvent
  public void changeDimesionEvent(PlayerEvent.PlayerChangedDimensionEvent event) {
    Player player = event.getPlayer();
    if (player != null && player.isAlive()) {
      syncCapability(player);
    }
  }

  @SubscribeEvent
  public void respawnEvent(PlayerEvent.PlayerRespawnEvent event) {
    Player player = event.getPlayer();
    if (player != null && player.isAlive()) {
      syncCapability(player);
    }
  }

  @SubscribeEvent
  public void playerTick(TickEvent.PlayerTickEvent event) {
    Player player = event.player;
    if (player != null && player.isAlive()) {
      if (event.side.isServer()) {
        IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
        ArrayList<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
        Map<QuestObjective, Integer> progressMap = capability.getQuestProgressMap();
        for (QuestInstance questInstance : acceptedQuests) {
          List<QuestObjective> objectives = questInstance.getQuest().getObjectives();
          for (QuestObjective objective : objectives) {
            if (!objective.isHidden()) {
              QuestObjective.ObjectiveType type = objective.getType();
              switch (type) {
                case Gather -> {
                  // This should get taken on objective.complete()
                  ItemStack gatherStack = objective.getObjective();
                  int progress = getAmount(player, gatherStack);
                  objective.setProgress(progress);
                }
                case Find -> {
                  BlockPos[] area = objective.getObjective();
                  if (isPlayerInArea(player, area))
                    objective.setProgress(objective.getAmount());
                }
                case DeliverToLocation -> {
                  ItemStack toDeliver = objective.getObjective();
                  BlockPos[] deliveryArea = objective.getSecondaryObjective();
                  if (isPlayerInArea(player, deliveryArea) && hasItem(player, toDeliver)) {
                    int prevProgress = objective.getProgress();
                    objective.setProgress(objective.getProgress() + getAmount(player, toDeliver));
                    takeStack(player, toDeliver, objective.getAmount() - prevProgress);
                  }
                }
                case Scoreboard -> {
                  Scoreboard scoreboard = player.getScoreboard();
                  Objective scoreObjective = scoreboard.getOrCreateObjective(objective.primaryToString());
                  if (scoreObjective != null)
                    objective.setProgress(scoreboard.getOrCreatePlayerScore(player.getName().getString(), scoreObjective).getScore());
                }
              }
              if (objective.isComplete())
                objective.onComplete(player);
            }
            progressMap.put(objective, objective.getProgress());
          }
        }
        PacketDispatcher.sendTo(new SSyncQuestCapability(capability.getAcceptedQuests().toArray(new QuestInstance[0])), player);
      }
    }
  }

  @SubscribeEvent
  public void livingDeathEvent(LivingDeathEvent event) {
    if (event.getSource().getEntity() instanceof Player player) {
      if (player.isAlive() && !player.level.isClientSide) {
        IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
        Map<QuestObjective, Integer> progressMap = capability.getQuestProgressMap();
        for (QuestObjective objective : progressMap.keySet()) {
          if (!objective.isHidden()) {
            if (objective.getType().equals(QuestObjective.ObjectiveType.Kill)) {
              if (EntityType.getKey(event.getEntityLiving().getType()).toString().equals(objective.getObjective()))
                objective.setProgress(objective.getProgress() + 1);
            }
          }
        }
      }
    }
  }

  @SubscribeEvent
  public void interact(PlayerInteractEvent.EntityInteractSpecific event) {
    Player player = event.getPlayer();
    if (player != null && player.isAlive()) {
      if (event.getSide().isServer()) {
        IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
        ArrayList<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
        for (QuestInstance questInstance : acceptedQuests) {
          List<QuestObjective> objectives = questInstance.getQuest().getObjectives();
          for (QuestObjective objective : objectives) {
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
          }
        }
      }
    }
  }

  @SubscribeEvent
  public void blockInteract(PlayerInteractEvent.RightClickBlock event) {
    Player player = event.getPlayer();
    if (player != null && player.isAlive() && event.getSide().isServer()) {
      IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
      ArrayList<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
      for (QuestInstance questInstance : acceptedQuests) {
        List<QuestObjective> objectives = questInstance.getQuest().getObjectives();
        for (QuestObjective objective : objectives) {
          if (!objective.isHidden()) {
            if (objective.getType().equals(QuestObjective.ObjectiveType.UseOnBlock)) {
              if (matches(objective.getObjective(), event.getItemStack()) && event.getWorld().getBlockState(event.getHitVec().getBlockPos()).equals(objective.getSecondaryObjective()))
                objective.setProgress(objective.getProgress() + 1);
            } else if (objective.getType().equals(QuestObjective.ObjectiveType.Use)) {
              if (event.getItemStack().getUseDuration() == 0 && matches(objective.getObjective(), event.getItemStack()))
                objective.setProgress(objective.getProgress() + 1);
            }
          }
        }
      }
    }
  }

  @SubscribeEvent
  public void itemUse(PlayerInteractEvent.RightClickItem event) {
    LivingEntity entity = event.getEntityLiving();
    if (entity instanceof Player player) {
      if (player.isAlive() && event.getSide().isServer()) {
        ItemStack itemStack = event.getItemStack();

        if (itemStack.getUseDuration() == 0) {
          IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
          ArrayList<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
          for (QuestInstance questInstance : acceptedQuests) {
            List<QuestObjective> objectives = questInstance.getQuest().getObjectives();
            for (QuestObjective objective : objectives) {
              if (!objective.isHidden()) {
                if (objective.getType().equals(QuestObjective.ObjectiveType.Use)) {
                  if (matches(objective.getObjective(), itemStack))
                    objective.setProgress(objective.getProgress() + 1);
                }
              }
            }
          }
        }
      }
    }
  }

  @SubscribeEvent
  public void itemUse(LivingEntityUseItemEvent.Finish event) {
    LivingEntity entity = event.getEntityLiving();
    if (entity instanceof Player player) {
      if (player.isAlive() && !player.level.isClientSide) {
        ItemStack itemStack = event.getItem();
        if (itemStack.getUseDuration() > 0) {
          IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
          ArrayList<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
          for (QuestInstance questInstance : acceptedQuests) {
            List<QuestObjective> objectives = questInstance.getQuest().getObjectives();
            for(QuestObjective objective : objectives) {
              if (!objective.isHidden()) {
                if (objective.getType().equals(QuestObjective.ObjectiveType.Use)) {
                  if (matches(objective.getObjective(), itemStack))
                    objective.setProgress(objective.getProgress() + 1);
                }
              }
            }
          }
        }
      }
    }
  }

  private static boolean isPlayerInArea(Player player, BlockPos[] area) {
    BlockPos corner1 = area[0];
    BlockPos corner2 = area[1];
    int x1 = Math.min(corner1.getX(), corner2.getX());
    int y1 = Math.min(corner1.getY(), corner2.getY());
    int z1 = Math.min(corner1.getZ(), corner2.getZ());
    int x2 = Math.max(corner1.getX(), corner2.getX());
    int y2 = Math.max(corner1.getY(), corner2.getY());
    int z2 = Math.max(corner1.getZ(), corner2.getZ());
    double playerX = player.getX();
    double playerY = player.getY();
    double playerZ = player.getZ();
    return playerX >= x1 && playerX <= x2 && playerY >= y1 && playerY <= y2 && playerZ >= z1 && playerZ <= z2;
  }

  private static void syncCapability(Player player) {
    IQuestCapability questCapability = QuestCapabilityProvider.getCapability(player);

    if (questCapability.getTrackedQuest() != null)
      PacketDispatcher.sendTo(new SSyncQuestCapability(questCapability.getTrackedQuest()), player);

    PacketDispatcher.sendTo(new SSyncQuestCapability(questCapability.getAcceptedQuests().toArray(new QuestInstance[0])), player);
    PacketDispatcher.sendTo(new SSyncQuestCapability(questCapability.getCompletedQuests().toArray(new String[0])), player);
  }

}
