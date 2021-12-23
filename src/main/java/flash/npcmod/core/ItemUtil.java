package flash.npcmod.core;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemUtil {

  public static boolean matches(ItemStack stack1, ItemStack stack2) {
    return ItemStack.isSame(stack1, stack2) && ItemStack.tagMatches(stack1, stack2);
  }

  public static void takeStack(Player player, ItemStack itemStack) {
    takeStack(player, itemStack, itemStack.getCount());
  }

  public static void takeStack(Player player, ItemStack itemStack, int neededCount) {
    if (itemStack.isEmpty()) return;

    for (ItemStack stack : player.getInventory().items) {
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

  public static void giveStack(Player player, ItemStack itemstack) {
    if (itemstack.isEmpty()) return;

    ItemEntity itemEntity = player.drop(itemstack, true, true);
    if (itemEntity != null) {
      itemEntity.setNoPickUpDelay();
      itemEntity.setOwner(player.getUUID());
    }
  }

  public static boolean hasAmount(Player sender, ItemStack itemStackIn) {
    if (itemStackIn.isEmpty()) return true;

    int neededCount = itemStackIn.getCount();
    int currentCount = 0;
    for(ItemStack itemStack : sender.getInventory().items) {
      if (matches(itemStackIn, itemStack))
        currentCount += itemStack.getCount();

      if (currentCount >= neededCount) return true;
    }

    return false;
  }

  public static boolean hasItem(Player sender, ItemStack itemStackIn) {
    if (itemStackIn.isEmpty()) return true;

    for(ItemStack itemStack : sender.getInventory().items) {
      if (matches(itemStackIn, itemStack))
        return true;
    }

    return false;
  }

  public static int getAmount(Player sender, ItemStack itemStackIn) {
    if (itemStackIn.isEmpty()) return 0;

    int currentCount = 0;
    for(ItemStack itemStack : sender.getInventory().items) {
      if (matches(itemStackIn, itemStack))
        currentCount += itemStack.getCount();
    }

    return currentCount;
  }

  public static ItemStack stackFromString(String s) {
    ItemArgument itemArgument = new ItemArgument();
    try {
      ItemInput itemInput = itemArgument.parse(new StringReader(s));
      return itemInput.createItemStack(1, false);
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
