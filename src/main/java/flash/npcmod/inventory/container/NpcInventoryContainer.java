package flash.npcmod.inventory.container;

import com.mojang.datafixers.util.Pair;
import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.ContainerInit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class NpcInventoryContainer extends AbstractContainerMenu {
  private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[]{InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET};
  private static final EquipmentSlot[] VALID_EQUIPMENT_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

  private NpcEntity npcEntity;

  public NpcInventoryContainer(int id, Inventory inventory, int entityId) {
    super(ContainerInit.NPC_INVENTORY_CONTAINER, id);

    Entity entity = inventory.player.level.getEntity(entityId);
    if (!(entity instanceof NpcEntity)) return;
    NpcEntity npcEntity = (NpcEntity) entity;
    this.npcEntity = npcEntity;

    SimpleContainer npcInventory = new SimpleContainer(getNpcItems(npcEntity));

    // Main Hand Slot
    this.addSlot(new Slot(npcInventory, 0, 115, 44) {
      @Override
      public void set(ItemStack stack) {
        npcEntity.setItemSlot(EquipmentSlot.MAINHAND, stack);
        npcInventory.setChanged();
        super.set(stack);
      }

      @Override
      public ItemStack getItem() {
        return npcEntity.getItemBySlot(EquipmentSlot.MAINHAND);
      }
    });

    // Offhand Slot
    this.addSlot(new Slot(npcInventory, 1, 115, 62) {
      @Override
      public void set(ItemStack stack) {
        npcEntity.setItemSlot(EquipmentSlot.OFFHAND, stack);
        npcInventory.setChanged();
        super.set(stack);
      }

      @Override
      public ItemStack getItem() {
        return npcEntity.getItemBySlot(EquipmentSlot.OFFHAND);
      }

      @OnlyIn(Dist.CLIENT)
      public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
      }
    });

    // Armor Slots
    for(int k = 0; k < 4; ++k) {
      final EquipmentSlot equipmentslottype = VALID_EQUIPMENT_SLOTS[k];
      this.addSlot(new Slot(npcInventory, 2+k, 46, 8 + k * 18) {
        /**
         * Returns the maximum stack size for a given slot (usually the same as getInventoryStackLimit(), but 1 in
         * the case of armor slots)
         */
        public int getMaxStackSize() {
          return 1;
        }

        /**
         * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
         */
        public boolean mayPlace(ItemStack stack) {
          return stack.canEquip(equipmentslottype, npcEntity);
        }

        @Override
        public void set(ItemStack stack) {
          npcEntity.setItemSlot(equipmentslottype, stack);
          npcInventory.setChanged();
          super.set(stack);
        }

        @Override
        public ItemStack getItem() {
          return npcEntity.getItemBySlot(equipmentslottype);
        }

        @OnlyIn(Dist.CLIENT)
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
          return Pair.of(InventoryMenu.BLOCK_ATLAS, ARMOR_SLOT_TEXTURES[equipmentslottype.getIndex()]);
        }
      });
    }

    // Player inventory

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
      if (index < 6) {
        if (!this.moveItemStackTo(stackInSlot, 6, this.slots.size(), true))
          return ItemStack.EMPTY;
      } else if (!this.moveItemStackTo(stackInSlot, 0, 6, false)) {
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

  private ItemStack[] getNpcItems(NpcEntity npcEntity) {
    return new ItemStack[] {
        npcEntity.getMainHandItem(), npcEntity.getOffhandItem(),
        npcEntity.getItemBySlot(EquipmentSlot.HEAD), npcEntity.getItemBySlot(EquipmentSlot.CHEST),
        npcEntity.getItemBySlot(EquipmentSlot.LEGS), npcEntity.getItemBySlot(EquipmentSlot.FEET)
    };
  }

  public NpcEntity getNpcEntity() {
    return npcEntity;
  }
}
