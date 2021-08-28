package flash.npcmod.inventory.container;

import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.ContainerInit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class NpcTradeContainer extends Container {
  private NpcEntity npcEntity;

  public NpcTradeContainer(int id, PlayerInventory inventory, int entityId) {
    super(ContainerInit.NPC_TRADE_CONTAINER, id);

    Entity entity = inventory.player.world.getEntityByID(entityId);
    if (!(entity instanceof NpcEntity)) return;
    NpcEntity npcEntity = (NpcEntity) entity;
    this.npcEntity = npcEntity;

    for(int l = 0; l < 3; ++l) {
      for(int j1 = 0; j1 < 9; ++j1) {
        this.addSlot(new Slot(inventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
      }
    }

    for(int i1 = 0; i1 < 9; ++i1) {
      this.addSlot(new Slot(inventory, i1, 8 + i1 * 18, 142));
    }
  }

  @Override
  public boolean canInteractWith(PlayerEntity playerIn) {
    return true;
  }

  public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
    ItemStack itemstack = ItemStack.EMPTY;
    Slot slot = this.inventorySlots.get(index);
    if (slot != null && slot.getHasStack()) {
      ItemStack stackInSlot = slot.getStack();
      return stackInSlot;
    }
    return itemstack;
  }

  public NpcEntity getNpcEntity() {
    return npcEntity;
  }
}
