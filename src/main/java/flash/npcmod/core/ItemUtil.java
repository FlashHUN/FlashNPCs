package flash.npcmod.core;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.arguments.ItemArgument;
import net.minecraft.command.arguments.ItemInput;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class ItemUtil {

  public static boolean matches(ItemStack stack1, ItemStack stack2) {
    return ItemStack.areItemsEqual(stack1, stack2) && ItemStack.areItemStackTagsEqual(stack1, stack2);
  }

  public static void takeStack(PlayerEntity player, ItemStack itemStack) {
    takeStack(player, itemStack, itemStack.getCount());
  }

  public static void takeStack(PlayerEntity player, ItemStack itemStack, int neededCount) {
    if (itemStack.isEmpty()) return;

    for (ItemStack stack : player.inventory.mainInventory) {
      if (matches(itemStack, stack)) {
        int count = stack.getCount();
        stack.shrink(neededCount);
        neededCount -= count;
      }

      if (neededCount <= 0) {
        break;
      }
    }
  }

  public static void giveStack(PlayerEntity player, ItemStack itemstack) {
    if (itemstack.isEmpty()) return;

    ItemEntity itemEntity = player.dropItem(itemstack, true, true);
    if (itemEntity != null) {
      itemEntity.setNoPickupDelay();
      itemEntity.setOwnerId(player.getUniqueID());
    }
  }

  public static boolean hasAmount(PlayerEntity sender, ItemStack itemStackIn) {
    if (itemStackIn.isEmpty()) return true;

    int neededCount = itemStackIn.getCount();
    int currentCount = 0;
    for(ItemStack itemStack : sender.inventory.mainInventory) {
      if (matches(itemStackIn, itemStack))
        currentCount += itemStack.getCount();

      if (currentCount >= neededCount) return true;
    }

    return false;
  }

  public static boolean hasItem(PlayerEntity sender, ItemStack itemStackIn) {
    if (itemStackIn.isEmpty()) return true;

    for(ItemStack itemStack : sender.inventory.mainInventory) {
      if (matches(itemStackIn, itemStack))
        return true;
    }

    return false;
  }

  public static int getAmount(PlayerEntity sender, ItemStack itemStackIn) {
    if (itemStackIn.isEmpty()) return 0;

    int currentCount = 0;
    for(ItemStack itemStack : sender.inventory.mainInventory) {
      if (matches(itemStackIn, itemStack))
        currentCount += itemStack.getCount();
    }

    return currentCount;
  }

  public static ItemStack stackFromString(String s) {
    ItemArgument itemArgument = new ItemArgument();
    try {
      ItemInput itemInput = itemArgument.parse(new StringReader(s));
      return itemInput.createStack(1, false);
    } catch (CommandSyntaxException e) {
      return ItemStack.EMPTY;
    }
  }

  public static String stackToString(ItemStack itemStack) {
    return itemStack == null || itemStack.isEmpty() ? "minecraft:empty" :
        itemStack.getItem().getRegistryName()
            +(itemStack.hasTag() ? itemStack.getTag().toString() : "");
  }
}
