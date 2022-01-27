package flash.npcmod.core.trades;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;

public class TradeOffers extends ArrayList<TradeOffer> {

  public TradeOffers() {}

  public TradeOffers(CompoundNBT nbt) {
    ListNBT listnbt = nbt.getList("Recipes", 10);

    for(int i = 0; i < listnbt.size(); ++i) {
      this.add(new TradeOffer(listnbt.getCompound(i)));
    }
  }

  public void write(PacketBuffer buffer) {
    buffer.writeByte((byte)(this.size() & 255));

    for(int i = 0; i < this.size(); ++i) {
      TradeOffer merchantoffer = this.get(i);
      for (int j = 0; j < merchantoffer.getBuyingStacks().length; j++) {
        buffer.writeItemStack(merchantoffer.getBuyingStacks()[i]);
      }
      for (int j = 0; j < merchantoffer.getSellingStacks().length; j++) {
        buffer.writeItemStack(merchantoffer.getSellingStacks()[i]);
      }
    }

  }

  public static TradeOffers read(PacketBuffer buffer) {
    TradeOffers merchantoffers = new TradeOffers();
    int i = buffer.readByte() & 255;

    for(int j = 0; j < i; ++j) {
      ItemStack buyingStack1 = buffer.readItemStack();
      ItemStack buyingStack2 = buffer.readItemStack();
      ItemStack buyingStack3 = buffer.readItemStack();

      ItemStack sellingStack1 = buffer.readItemStack();
      ItemStack sellingStack2 = buffer.readItemStack();
      ItemStack sellingStack3 = buffer.readItemStack();

      TradeOffer merchantoffer = new TradeOffer(buyingStack1, buyingStack2, buyingStack3, sellingStack1, sellingStack2, sellingStack3);

      merchantoffers.add(merchantoffer);
    }

    return merchantoffers;
  }

  public CompoundNBT write() {
    CompoundNBT compoundnbt = new CompoundNBT();
    ListNBT listnbt = new ListNBT();

    for(int i = 0; i < this.size(); ++i) {
      TradeOffer merchantoffer = this.get(i);
      listnbt.add(merchantoffer.write());
    }

    compoundnbt.put("Recipes", listnbt);
    return compoundnbt;
  }

  public static TradeOffers read(String s) {
    try {
      CompoundNBT tag = new JsonToNBT(new StringReader(s)).readStruct();
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
