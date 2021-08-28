package flash.npcmod;

import flash.npcmod.client.gui.overlay.HudOverlay;
import flash.npcmod.client.gui.screen.inventory.*;
import flash.npcmod.client.render.entity.NpcEntityRenderer;
import flash.npcmod.events.ClientEvents;
import flash.npcmod.init.ContainerInit;
import flash.npcmod.init.EntityInit;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientRegistryEvents {

  @SubscribeEvent
  public static void onClientSetup(final FMLClientSetupEvent event) {
    for (ClientEvents.KeyBindings keyBind : ClientEvents.KeyBindings.values()) {
      ClientRegistry.registerKeyBinding(keyBind.get());
    }

    MinecraftForge.EVENT_BUS.register(new ClientEvents());
    MinecraftForge.EVENT_BUS.register(new HudOverlay());

    RenderingRegistry.registerEntityRenderingHandler(EntityInit.NPC_ENTITY.get(), NpcEntityRenderer::new);

    ScreenManager.registerFactory(ContainerInit.QUEST_STACK_SELECTOR_CONTAINER, QuestStackSelectorScreen::new);
    ScreenManager.registerFactory(ContainerInit.OBJECTIVE_STACK_SELECTOR_CONTAINER, ObjectiveStackSelectorScreen::new);
    ScreenManager.registerFactory(ContainerInit.NPC_INVENTORY_CONTAINER, NpcInventoryScreen::new);
    ScreenManager.registerFactory(ContainerInit.NPC_TRADE_CONTAINER, NpcTradeScreen::new);
    ScreenManager.registerFactory(ContainerInit.NPC_TRADE_EDITOR_CONTAINER, NpcTradeEditorScreen::new);
  }

}
