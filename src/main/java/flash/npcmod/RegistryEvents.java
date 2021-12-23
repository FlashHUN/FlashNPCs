package flash.npcmod;

import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.ContainerInit;
import flash.npcmod.init.EntityInit;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegistryEvents {

  @SubscribeEvent
  public void entityAttributes(EntityAttributeCreationEvent event) {
    event.put(EntityInit.NPC_ENTITY.get(), NpcEntity.setCustomAttributes().build());
  }

  @SubscribeEvent
  public static void onContainerRegister(RegistryEvent.Register<MenuType<?>> event) {
    ContainerInit.register(event);
  }

}
