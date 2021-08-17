package flash.npcmod.inventory.container;

import com.mojang.datafixers.util.Pair;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.ContainerInit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class NpcInventoryContainer extends Container {
  private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[]{PlayerContainer.EMPTY_ARMOR_SLOT_BOOTS, PlayerContainer.EMPTY_ARMOR_SLOT_LEGGINGS, PlayerContainer.EMPTY_ARMOR_SLOT_CHESTPLATE, PlayerContainer.EMPTY_ARMOR_SLOT_HELMET};
  private static final EquipmentSlotType[] VALID_EQUIPMENT_SLOTS = new EquipmentSlotType[]{EquipmentSlotType.HEAD, EquipmentSlotType.CHEST, EquipmentSlotType.LEGS, EquipmentSlotType.FEET};

  private NpcEntity npcEntity;

  public NpcInventoryContainer(int id, PlayerInventory inventory, int entityId) {
    super(ContainerInit.NPC_INVENTORY_CONTAINER, id);

    Entity entity = inventory.player.world.getEntityByID(entityId);
    if (!(entity instanceof NpcEntity)) return;
    NpcEntity npcEntity = (NpcEntity) entity;
    this.npcEntity = npcEntity;

    Inventory npcInventory = new Inventory(getNpcItems(npcEntity));

    this.addSlot(new Slot(npcInventory, 0, 115, 44) {
      @Override
      public void putStack(ItemStack stack) {
        npcEntity.setItemStackToSlot(EquipmentSlotType.MAINHAND, stack);
        npcInventory.markDirty();
        inventory.markDirty();
      }

      @Override
      public ItemStack onTake(PlayerEntity thePlayer, ItemStack stack) {
        thePlayer.inventory.setItemStack(npcEntity.getItemStackFromSlot(EquipmentSlotType.MAINHAND));
        npcEntity.setItemStackToSlot(EquipmentSlotType.MAINHAND, ItemStack.EMPTY);
        npcInventory.markDirty();
        this.onSlotChanged();
        return stack;
      }

      @Override
      public ItemStack getStack() {
        return npcEntity.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
      }
    });

    this.addSlot(new Slot(npcInventory, 1, 115, 62) {
      @Override
      public void putStack(ItemStack stack) {
        npcEntity.setItemStackToSlot(EquipmentSlotType.OFFHAND, stack);
        npcInventory.markDirty();
        inventory.markDirty();
      }

      @Override
      public ItemStack onTake(PlayerEntity thePlayer, ItemStack stack) {
        thePlayer.inventory.setItemStack(npcEntity.getItemStackFromSlot(EquipmentSlotType.OFFHAND));
        npcEntity.setItemStackToSlot(EquipmentSlotType.OFFHAND, ItemStack.EMPTY);
        npcInventory.markDirty();
        this.onSlotChanged();
        return stack;
      }

      @Override
      public ItemStack getStack() {
        return npcEntity.getItemStackFromSlot(EquipmentSlotType.OFFHAND);
      }

      @OnlyIn(Dist.CLIENT)
      public Pair<ResourceLocation, ResourceLocation> getBackground() {
        return Pair.of(PlayerContainer.LOCATION_BLOCKS_TEXTURE, PlayerContainer.EMPTY_ARMOR_SLOT_SHIELD);
      }
    });

    for(int k = 0; k < 4; ++k) {
      final EquipmentSlotType equipmentslottype = VALID_EQUIPMENT_SLOTS[k];
      this.addSlot(new Slot(npcInventory, 2+k, 46, 8 + k * 18) {
        /**
         * Returns the maximum stack size for a given slot (usually the same as getInventoryStackLimit(), but 1 in
         * the case of armor slots)
         */
        public int getSlotStackLimit() {
          return 1;
        }

        /**
         * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
         */
        public boolean isItemValid(ItemStack stack) {
          return stack.canEquip(equipmentslottype, npcEntity);
        }

        @Override
        public void putStack(ItemStack stack) {
          npcEntity.setItemStackToSlot(equipmentslottype, stack);
          npcInventory.markDirty();
          inventory.markDirty();
        }

        @Override
        public ItemStack onTake(PlayerEntity thePlayer, ItemStack stack) {
          thePlayer.inventory.setItemStack(npcEntity.getItemStackFromSlot(equipmentslottype));
          npcEntity.setItemStackToSlot(equipmentslottype, ItemStack.EMPTY);
          npcInventory.markDirty();
          this.onSlotChanged();
          return stack;
        }

        @Override
        public ItemStack getStack() {
          return npcEntity.getItemStackFromSlot(equipmentslottype);
        }

        @OnlyIn(Dist.CLIENT)
        public Pair<ResourceLocation, ResourceLocation> getBackground() {
          return Pair.of(PlayerContainer.LOCATION_BLOCKS_TEXTURE, ARMOR_SLOT_TEXTURES[equipmentslottype.getIndex()]);
        }
      });
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
      if (index < 6) {
        if (!this.mergeItemStack(stackInSlot, 6, this.inventorySlots.size(), true))
          return ItemStack.EMPTY;
      } else if (!this.mergeItemStack(stackInSlot, 0, 6, false)) {
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

  private ItemStack[] getNpcItems(NpcEntity npcEntity) {
    return new ItemStack[] {
        npcEntity.getHeldItemMainhand(), npcEntity.getHeldItemOffhand(),
        npcEntity.getItemStackFromSlot(EquipmentSlotType.HEAD), npcEntity.getItemStackFromSlot(EquipmentSlotType.CHEST),
        npcEntity.getItemStackFromSlot(EquipmentSlotType.LEGS), npcEntity.getItemStackFromSlot(EquipmentSlotType.FEET)
    };
  }

  public NpcEntity getNpcEntity() {
    return npcEntity;
  }
}
