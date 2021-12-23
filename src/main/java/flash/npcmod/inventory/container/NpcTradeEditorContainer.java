package flash.npcmod.inventory.container;

import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.ContainerInit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NpcTradeEditorContainer extends AbstractContainerMenu {
  private NpcEntity npcEntity;

  private SimpleContainer buyingItems, sellingItems;

  public NpcTradeEditorContainer(int id, Inventory inventory, int entityId) {
    super(ContainerInit.NPC_TRADE_EDITOR_CONTAINER, id);

    Entity entity = inventory.player.level.getEntity(entityId);
    if (!(entity instanceof NpcEntity)) return;
    NpcEntity npcEntity = (NpcEntity) entity;
    this.npcEntity = npcEntity;

    buyingItems = new SimpleContainer(36);
    sellingItems = new SimpleContainer(36);

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
  public boolean stillValid(Player playerIn) {
    return playerIn.hasPermissions(4);
  }

  public ItemStack quickMoveStack(Player playerIn, int index) {
    ItemStack itemstack = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot != null && slot.hasItem()) {
      ItemStack stackInSlot = slot.getItem();
      itemstack = stackInSlot.copy();
      if (index < 72) {
        if (!this.moveItemStackTo(stackInSlot, 72, this.slots.size(), true))
          return ItemStack.EMPTY;
      } else if (!this.moveItemStackTo(stackInSlot, 0, 72, false)) {
        return ItemStack.EMPTY;
      }

      if (stackInSlot.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
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
      public void set(ItemStack stack) {
        setBuyingItem(rowIndex, itemIndex, stack);
        container.setChanged();
      }

      @Override
      public void onTake(Player thePlayer, ItemStack stack) {
        thePlayer.inventoryMenu.setCarried(getBuyingItem(rowIndex, itemIndex));
        setBuyingItem(rowIndex, itemIndex, ItemStack.EMPTY);
      }

      @Override
      public ItemStack getItem() {
        return getBuyingItem(rowIndex, itemIndex);
      }
    };
  }

  private Slot getSellSlot(int slotIndex, int rowIndex, int itemIndex) {
    int x = slotIndex < 18 ? 32 : 188;
    int rowOffset = slotIndex < 18 ? rowIndex*18 : (rowIndex-6)*18;
    return new Slot(sellingItems, slotIndex, x+itemIndex*18, -28+rowOffset) {
      @Override
      public void set(ItemStack stack) {
        setSellingItem(rowIndex, itemIndex, stack);
        container.setChanged();
      }

      @Override
      public void onTake(Player thePlayer, ItemStack stack) {
        thePlayer.inventoryMenu.setCarried(getSellingItem(rowIndex, itemIndex));
        setSellingItem(rowIndex, itemIndex, ItemStack.EMPTY);
      }

      @Override
      public ItemStack getItem() {
        return getSellingItem(rowIndex, itemIndex);
      }
    };
  }

  public NpcEntity getNpcEntity() {
    return npcEntity;
  }
}
