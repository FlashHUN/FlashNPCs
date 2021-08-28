package flash.npcmod.init;

import flash.npcmod.Main;
import flash.npcmod.inventory.container.*;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.network.IContainerFactory;

public class ContainerInit {

  public static ContainerType<QuestStackSelectorContainer> QUEST_STACK_SELECTOR_CONTAINER;
  public static ContainerType<ObjectiveStackSelectorContainer> OBJECTIVE_STACK_SELECTOR_CONTAINER;
  public static ContainerType<NpcInventoryContainer> NPC_INVENTORY_CONTAINER;
  public static ContainerType<NpcTradeContainer> NPC_TRADE_CONTAINER;
  public static ContainerType<NpcTradeEditorContainer> NPC_TRADE_EDITOR_CONTAINER;

  public static void register(RegistryEvent.Register<ContainerType<?>> event) {
    QUEST_STACK_SELECTOR_CONTAINER = createContainerType((windowId, inventory, buffer)
        -> new QuestStackSelectorContainer(windowId, inventory, buffer.readString(100000)));
    OBJECTIVE_STACK_SELECTOR_CONTAINER = createContainerType((windowId, inventory, buffer)
        -> new ObjectiveStackSelectorContainer(windowId, inventory, buffer.readString(100000), buffer.readString(100000), buffer.readString(400)));
    NPC_INVENTORY_CONTAINER = createContainerType((windowId, inventory, buffer)
        -> new NpcInventoryContainer(windowId, inventory, buffer.readInt()));
    NPC_TRADE_CONTAINER = createContainerType((windowId, inventory, buffer)
        -> new NpcTradeContainer(windowId, inventory, buffer.readInt()));
    NPC_TRADE_EDITOR_CONTAINER = createContainerType((windowId, inventory, buffer)
        -> new NpcTradeEditorContainer(windowId, inventory, buffer.readInt()));

    event.getRegistry().registerAll(
        QUEST_STACK_SELECTOR_CONTAINER.setRegistryName(Main.MODID, "quest_stack_selector"),
        OBJECTIVE_STACK_SELECTOR_CONTAINER.setRegistryName(Main.MODID, "objective_stack_selector"),
        NPC_INVENTORY_CONTAINER.setRegistryName(Main.MODID, "npc_inventory"),
        NPC_TRADE_CONTAINER.setRegistryName(Main.MODID, "npc_trade"),
        NPC_TRADE_EDITOR_CONTAINER.setRegistryName(Main.MODID, "npc_trade_editor")
    );
  }

  private static <T extends Container> ContainerType<T> createContainerType(IContainerFactory<T> factory) {
    return new ContainerType<T>(factory);
  }

}
