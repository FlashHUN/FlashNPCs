package flash.npcmod;

import flash.npcmod.client.gui.screen.inventory.NpcInventoryScreen;
import flash.npcmod.client.render.entity.NpcEntityRenderer;
import flash.npcmod.events.ClientEvents;
import flash.npcmod.init.ContainerInit;
import flash.npcmod.init.EntityInit;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientRegistryEvents {

  @SubscribeEvent
  public static void onClientSetup(final FMLClientSetupEvent event) {
    MinecraftForge.EVENT_BUS.register(new ClientEvents());

    RenderingRegistry.registerEntityRenderingHandler(EntityInit.NPC_ENTITY.get(), NpcEntityRenderer::new);

    ScreenManager.registerFactory(ContainerInit.NPC_INVENTORY_CONTAINER, NpcInventoryScreen::new);
  }

}
