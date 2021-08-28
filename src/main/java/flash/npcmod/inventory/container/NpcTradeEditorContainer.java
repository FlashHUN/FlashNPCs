package flash.npcmod.inventory.container;

import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.ContainerInit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class NpcTradeEditorContainer extends Container {
  private NpcEntity npcEntity;

  private Inventory buyingItems, sellingItems;

  public NpcTradeEditorContainer(int id, PlayerInventory inventory, int entityId) {
    super(ContainerInit.NPC_TRADE_EDITOR_CONTAINER, id);

    Entity entity = inventory.player.world.getEntityByID(entityId);
    if (!(entity instanceof NpcEntity)) return;
    NpcEntity npcEntity = (NpcEntity) entity;
    this.npcEntity = npcEntity;

    buyingItems = new Inventory(36);
    sellingItems = new Inventory(36);

    for (int i = 0; i < 18; i++) {
      int index = i / 3;
      int itemIndex = i % 3;
      this.addSlot(getBuySlot(i, index, itemIndex));
      this.addSlot(getBuySlot(i+18, index+6, itemIndex));
    }

    for (int i = 0; i < 18; i++) {
      int index = i / 3;
      int itemIndex = i % 3;
      this.addSlot(getSellSlot(i, index, itemIndex));
      this.addSlot(getSellSlot(i+18, index+6, itemIndex));
    }

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
    return playerIn.hasPermissionLevel(4);
  }

  public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
    ItemStack itemstack = ItemStack.EMPTY;
    Slot slot = this.inventorySlots.get(index);
    if (slot != null && slot.getHasStack()) {
      ItemStack stackInSlot = slot.getStack();
      itemstack = stackInSlot.copy();
      if (index < 72) {
        if (!this.mergeItemStack(stackInSlot, 72, this.inventorySlots.size(), true))
          return ItemStack.EMPTY;
      } else if (!this.mergeItemStack(stackInSlot, 0, 72, false)) {
        return ItemStack.EMPTY;
      }

      if (stackInSlot.isEmpty()) {
        slot.putStack(ItemStack.EMPTY);
      } else {
        slot.onSlotChanged();
      }
    }

    return itemstack;
  }

  private ItemStack getBuyingItem(int index, int itemIndex) {
    return npcEntity.getOffers().get(index).getBuyingStacks()[itemIndex];
  }

  private void setBuyingItem(int index, int itemIndex, ItemStack stack) {
    npcEntity.getOffers().get(index).setBuyingStack(itemIndex, stack);
  }

  private ItemStack getSellingItem(int index, int itemIndex) {
    return npcEntity.getOffers().get(index).getSellingStacks()[itemIndex];
  }

  private void setSellingItem(int index, int itemIndex, ItemStack stack) {
    npcEntity.getOffers().get(index).setSellingStack(itemIndex, stack);
  }

  private Slot getBuySlot(int slotIndex, int rowIndex, int itemIndex) {
    int x = slotIndex < 18 ? -64 : 92;
    int rowOffset = slotIndex < 18 ? rowIndex*18 : (rowIndex-6)*18;
    return new Slot(buyingItems, slotIndex, x+itemIndex*18, -28+rowOffset) {
      @Override
      public void putStack(ItemStack stack) {
        setBuyingItem(rowIndex, itemIndex, stack);
        inventory.markDirty();
      }

      @Override
      public ItemStack onTake(PlayerEntity thePlayer, ItemStack stack) {
        thePlayer.inventory.setItemStack(getBuyingItem(rowIndex, itemIndex));
        setBuyingItem(rowIndex, itemIndex, ItemStack.EMPTY);
        return stack;
      }

      @Override
      public ItemStack getStack() {
        return getBuyingItem(rowIndex, itemIndex);
      }
    };
  }

  private Slot getSellSlot(int slotIndex, int rowIndex, int itemIndex) {
    int x = slotIndex < 18 ? 32 : 188;
    int rowOffset = slotIndex < 18 ? rowIndex*18 : (rowIndex-6)*18;
    return new Slot(sellingItems, slotIndex, x+itemIndex*18, -28+rowOffset) {
      @Override
      public void putStack(ItemStack stack) {
        setSellingItem(rowIndex, itemIndex, stack);
        inventory.markDirty();
      }

      @Override
      public ItemStack onTake(PlayerEntity thePlayer, ItemStack stack) {
        thePlayer.inventory.setItemStack(getSellingItem(rowIndex, itemIndex));
        setSellingItem(rowIndex, itemIndex, ItemStack.EMPTY);
        return stack;
      }

      @Override
      public ItemStack getStack() {
        return getSellingItem(rowIndex, itemIndex);
      }
    };
  }

  public NpcEntity getNpcEntity() {
    return npcEntity;
  }
}
