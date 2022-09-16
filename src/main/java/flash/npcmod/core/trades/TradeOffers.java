package flash.npcmod.core.trades;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public class TradeOffers extends ArrayList<TradeOffer> {

  public TradeOffers() {}

  public TradeOffers(CompoundTag nbt) {
    ListTag listnbt = nbt.getList("Recipes", 10);

    for(int i = 0; i < listnbt.size(); ++i) {
      this.add(new TradeOffer(listnbt.getCompound(i)));
    }
  }

  public void write(FriendlyByteBuf buffer) {
    buffer.writeByte((byte)(this.size() & 255));

    for(int i = 0; i < this.size(); ++i) {
      TradeOffer merchantoffer = this.get(i);
      for (int j = 0; j < merchantoffer.getBuyingStacks().length; j++) {
        buffer.writeItem(merchantoffer.getBuyingStacks()[i]);
      }
      for (int j = 0; j < merchantoffer.getSellingStacks().length; j++) {
        buffer.writeItem(merchantoffer.getSellingStacks()[i]);
      }
    }

  }

  public static TradeOffers read(FriendlyByteBuf buffer) {
    TradeOffers merchantoffers = new TradeOffers();
    int i = buffer.readByte() & 255;

    for(int j = 0; j < i; ++j) {
      ItemStack buyingStack1 = buffer.readItem();
      ItemStack buyingStack2 = buffer.readItem();
      ItemStack buyingStack3 = buffer.readItem();

      ItemStack sellingStack1 = buffer.readItem();
      ItemStack sellingStack2 = buffer.readItem();
      ItemStack sellingStack3 = buffer.readItem();

      TradeOffer merchantoffer = new TradeOffer(buyingStack1, buyingStack2, buyingStack3, sellingStack1, sellingStack2, sellingStack3);

      merchantoffers.add(merchantoffer);
    }

    return merchantoffers;
  }

  public CompoundTag write() {
    CompoundTag compoundnbt = new CompoundTag();
    ListTag listnbt = new ListTag();

    for(int i = 0; i < this.size(); ++i) {
      TradeOffer merchantoffer = this.get(i);
      listnbt.add(merchantoffer.write());
    }

    compoundnbt.put("Recipes", listnbt);
    return compoundnbt;
  }

  public static TradeOffers read(String s) {
    try {
      CompoundTag tag = new TagParser(new StringReader(s)).readStruct();
      return new TradeOffers(tag);
    } catch (CommandSyntaxException e) {
      return new TradeOffers();
    }
  }

  @Override
  public boolean isEmpty() {
    if (size() == 0) return true;

    for (TradeOffer offer : this) {
      if (!offer.isEmpty()) return false;
    }

    return true;
  }
}
