package flash.npcmod.core.trades;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import static flash.npcmod.core.ItemUtil.*;

public class TradeOffer {

  private final ItemStack[] buyingStacks, sellingStacks;

  public TradeOffer() {
    this(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
  }

  public TradeOffer(CompoundNBT dataTag) {
    this(ItemStack.read(dataTag.getCompound("buyA")), ItemStack.read(dataTag.getCompound("buyB")), ItemStack.read(dataTag.getCompound("buyC")),
        ItemStack.read(dataTag.getCompound("sellA")), ItemStack.read(dataTag.getCompound("sellB")), ItemStack.read(dataTag.getCompound("sellC")));
  }

  public TradeOffer(ItemStack buyingFirst, ItemStack buyingSecond, ItemStack buyingThird,
                    ItemStack sellingFirst, ItemStack sellingSecond, ItemStack sellingThird) {
    this(new ItemStack[] { buyingFirst, buyingSecond, buyingThird }, new ItemStack[] { sellingFirst, sellingSecond, sellingThird });
  }

  public TradeOffer(ItemStack[] buyingStacks, ItemStack[] sellingStacks) {
    this.buyingStacks = buyingStacks;
    this.sellingStacks = sellingStacks;
  }

  public ItemStack[] getBuyingStacks() {
    return buyingStacks;
  }

  public ItemStack[] getSellingStacks() {
    return sellingStacks;
  }

  public void setBuyingStack(int index, ItemStack stack) {
    if (index >= 0 && index <= 2) {
      this.buyingStacks[index] = stack;
    }
  }

  public void setSellingStack(int index, ItemStack stack) {
    if (index >= 0 && index <= 2) {
      this.sellingStacks[index] = stack;
    }
  }

  public CompoundNBT write() {
    CompoundNBT compoundnbt = new CompoundNBT();
    compoundnbt.put("buyA", this.buyingStacks[0].write(new CompoundNBT()));
    compoundnbt.put("buyB", this.buyingStacks[1].write(new CompoundNBT()));
    compoundnbt.put("buyC", this.buyingStacks[2].write(new CompoundNBT()));
    compoundnbt.put("sellA", this.sellingStacks[0].write(new CompoundNBT()));
    compoundnbt.put("sellB", this.sellingStacks[1].write(new CompoundNBT()));
    compoundnbt.put("sellC", this.sellingStacks[2].write(new CompoundNBT()));
    return compoundnbt;
  }

  public boolean canDoTransaction(PlayerEntity sender) {
    for (ItemStack stack : buyingStacks) {
      if (!hasAmount(sender, stack)) return false;
    }
    return true;
  }

  public void doTransaction(ServerPlayerEntity sender) {
    if (isEmpty() || !canDoTransaction(sender)) return;

    for (ItemStack stack : buyingStacks) {
      if (!stack.isEmpty())
        takeStack(sender, stack);
    }

    for (ItemStack stack : sellingStacks) {
      if (!stack.isEmpty())
        giveStack(sender, stack);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TradeOffer that = (TradeOffer) o;
    if (buyingStacks.length != that.buyingStacks.length || sellingStacks.length != that.sellingStacks.length) return false;
    for (int i = 0; i < buyingStacks.length; i++) {
      if (!matches(buyingStacks[i], that.buyingStacks[i])) return false;
    }
    for (int i = 0; i < sellingStacks.length; i++) {
      if (!matches(sellingStacks[i], that.sellingStacks[i])) return false;
    }
    return true;
  }

  public boolean isEmpty() {
    if (buyingStacks.length == 0 || sellingStacks.length == 0) return true;

    boolean hasBuyingStack = false;
    boolean hasSellingStack = false;
    for (ItemStack stack : buyingStacks) {
      if (!stack.isEmpty()) { hasBuyingStack = true; break; }
    }
    for (ItemStack stack : sellingStacks) {
      if (!stack.isEmpty()) { hasSellingStack = true; break; }
    }

    return !hasBuyingStack || !hasSellingStack;
  }
}
