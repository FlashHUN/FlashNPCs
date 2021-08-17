package flash.npcmod.init;

import flash.npcmod.inventory.container.NpcInventoryContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.network.IContainerFactory;

public class ContainerInit {

  public static ContainerType<NpcInventoryContainer> NPC_INVENTORY_CONTAINER;

  public static void register(RegistryEvent.Register<ContainerType<?>> event) {
    NPC_INVENTORY_CONTAINER = createContainerType((windowId, inventory, buffer)
        -> new NpcInventoryContainer(windowId, inventory, buffer.readInt()));

    event.getRegistry().registerAll(
        NPC_INVENTORY_CONTAINER.setRegistryName("npc_inventory")
    );
  }

  private static <T extends Container> ContainerType<T> createContainerType(IContainerFactory<T> factory) {
    return new ContainerType<T>(factory);
  }

}
