package flash.npcmod.events;

import flash.npcmod.core.EntityUtil;
import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.core.quests.CommonQuestUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncEntityList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class Events {

  // Load all functions and entity types on world load
  @SubscribeEvent
  public static void onWorldLoad(WorldEvent.Load event) {
    if (!event.getWorld().isClientSide()) {
      FunctionUtil.loadAllFunctions();
      CommonQuestUtil.loadAllQuests();
      if (!EntityUtil.hasLoadedEntities()) {
        EntityUtil.loadAllValidEntities((Level) event.getWorld());
      }
    }
  }

  @SubscribeEvent
  public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getPlayer().hasPermissions(4)) {
      PacketDispatcher.sendTo(new SSyncEntityList(EntityUtil.getEntityTypes()), event.getPlayer());
    }
  }

  @SubscribeEvent
  public static void onNpcDamage(LivingDamageEvent event) {
    if (event.getEntity() instanceof NpcEntity && event.getSource() != DamageSource.OUT_OF_WORLD) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onNpcKnockback(LivingKnockBackEvent event) {
    if (event.getEntity() instanceof NpcEntity) {
      event.setCanceled(true);
    }
  }

}
