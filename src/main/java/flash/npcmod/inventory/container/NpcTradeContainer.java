package flash.npcmod.inventory.container;

import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.ContainerInit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NpcTradeContainer extends AbstractContainerMenu {
  private NpcEntity npcEntity;

  public NpcTradeContainer(int id, Inventory inventory, int entityId) {
    super(ContainerInit.NPC_TRADE_CONTAINER, id);

    Entity entity = inventory.player.level.getEntity(entityId);
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
  public boolean stillValid(Player playerIn) {
    return true;
  }

  public ItemStack quickMoveStack(Player playerIn, int index) {
    ItemStack itemstack = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot != null && slot.hasItem()) {
      ItemStack stackInSlot = slot.getItem();
      return stackInSlot;
    }
    return itemstack;
  }

  public NpcEntity getNpcEntity() {
    return npcEntity;
  }
}
