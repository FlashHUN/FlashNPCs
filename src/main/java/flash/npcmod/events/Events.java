package flash.npcmod.events;

import flash.npcmod.core.EntityUtil;
import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.core.quests.CommonQuestUtil;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class Events {

  static boolean hasLoadedEntities = false;

  // Load all functions and entity types on world load
  @SubscribeEvent
  public static void onWorldLoad(WorldEvent.Load event) {
    if (!event.getWorld().isRemote()) {
      FunctionUtil.loadAllFunctions();
      CommonQuestUtil.loadAllQuests();
    }
    if (!hasLoadedEntities) {
      EntityUtil.loadAllEntitiesIntoEnum((World) event.getWorld());
      hasLoadedEntities = true;
    }
  }

}
