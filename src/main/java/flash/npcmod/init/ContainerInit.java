package flash.npcmod.init;

import flash.npcmod.Main;
import flash.npcmod.inventory.container.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.network.IContainerFactory;

public class ContainerInit {

  public static MenuType<QuestStackSelectorContainer> QUEST_STACK_SELECTOR_CONTAINER;
  public static MenuType<ObjectiveStackSelectorContainer> OBJECTIVE_STACK_SELECTOR_CONTAINER;
  public static MenuType<NpcInventoryContainer> NPC_INVENTORY_CONTAINER;
  public static MenuType<NpcTradeContainer> NPC_TRADE_CONTAINER;
  public static MenuType<NpcTradeEditorContainer> NPC_TRADE_EDITOR_CONTAINER;

  public static void register(RegistryEvent.Register<MenuType<?>> event) {
    QUEST_STACK_SELECTOR_CONTAINER = createContainerType((windowId, inventory, buffer)
        -> new QuestStackSelectorContainer(windowId, inventory, buffer.readUtf(100000)));
    OBJECTIVE_STACK_SELECTOR_CONTAINER = createContainerType((windowId, inventory, buffer)
        -> new ObjectiveStackSelectorContainer(windowId, inventory, buffer.readUtf(100000), buffer.readUtf(100000), buffer.readUtf(400)));
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

  private static <T extends AbstractContainerMenu> MenuType<T> createContainerType(IContainerFactory<T> factory) {
    return new MenuType<>(factory);
  }

}
