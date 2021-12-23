package flash.npcmod.core.trades;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import static flash.npcmod.core.ItemUtil.*;

public class TradeOffer {

  private final ItemStack[] buyingStacks, sellingStacks;

  public TradeOffer() {
    this(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
  }

  public TradeOffer(CompoundTag dataTag) {
    this(ItemStack.of(dataTag.getCompound("buyA")), ItemStack.of(dataTag.getCompound("buyB")), ItemStack.of(dataTag.getCompound("buyC")),
        ItemStack.of(dataTag.getCompound("sellA")), ItemStack.of(dataTag.getCompound("sellB")), ItemStack.of(dataTag.getCompound("sellC")));
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

  public CompoundTag write() {
    CompoundTag compoundnbt = new CompoundTag();
    compoundnbt.put("buyA", this.buyingStacks[0].save(new CompoundTag()));
    compoundnbt.put("buyB", this.buyingStacks[1].save(new CompoundTag()));
    compoundnbt.put("buyC", this.buyingStacks[2].save(new CompoundTag()));
    compoundnbt.put("sellA", this.sellingStacks[0].save(new CompoundTag()));
    compoundnbt.put("sellB", this.sellingStacks[1].save(new CompoundTag()));
    compoundnbt.put("sellC", this.sellingStacks[2].save(new CompoundTag()));
    return compoundnbt;
  }

  public boolean canDoTransaction(Player sender) {
    for (ItemStack stack : buyingStacks) {
      if (!hasAmount(sender, stack)) return false;
    }
    return true;
  }

  public void doTransaction(ServerPlayer sender) {
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
