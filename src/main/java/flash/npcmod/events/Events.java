package flash.npcmod.events;

import flash.npcmod.core.functions.FunctionUtil;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class Events {

  // Load all functions on world load
  @SubscribeEvent
  public static void onWorldLoad(WorldEvent.Load event) {
    if (!event.getWorld().isRemote()) {
      FunctionUtil.loadAllFunctions();
    }
  }

}
