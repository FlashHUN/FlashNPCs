package flash.npcmod;

import flash.npcmod.client.gui.overlay.HudOverlay;
import flash.npcmod.client.gui.screen.inventory.*;
import flash.npcmod.client.render.entity.NpcEntityRenderer;
import flash.npcmod.events.ClientEvents;
import flash.npcmod.init.ContainerInit;
import flash.npcmod.init.EntityInit;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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

    MenuScreens.register(ContainerInit.QUEST_STACK_SELECTOR_CONTAINER, QuestStackSelectorScreen::new);
    MenuScreens.register(ContainerInit.OBJECTIVE_STACK_SELECTOR_CONTAINER, ObjectiveStackSelectorScreen::new);
    MenuScreens.register(ContainerInit.NPC_INVENTORY_CONTAINER, NpcInventoryScreen::new);
    MenuScreens.register(ContainerInit.NPC_TRADE_CONTAINER, NpcTradeScreen::new);
    MenuScreens.register(ContainerInit.NPC_TRADE_EDITOR_CONTAINER, NpcTradeEditorScreen::new);
  }

  @SubscribeEvent
  public static void registerEntityRenders(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(EntityInit.NPC_ENTITY.get(), NpcEntityRenderer::new);
  }

}
