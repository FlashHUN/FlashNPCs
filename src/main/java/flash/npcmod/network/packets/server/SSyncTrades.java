package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import flash.npcmod.core.trades.TradeOffer;
import flash.npcmod.core.trades.TradeOffers;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SSyncTrades {

  int entityid;
  TradeOffers tradeOffers;

  public SSyncTrades(NpcEntity npcEntity) {
    this(npcEntity.getId(), npcEntity.getOffers());
  }

  public SSyncTrades(int entityid, TradeOffers tradeOffers) {
    this.entityid = entityid;
    this.tradeOffers = tradeOffers;
  }

  public static void encode(SSyncTrades msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.entityid);
    buf.writeInt(msg.tradeOffers.size());
    for (TradeOffer tradeOffer : msg.tradeOffers) {
      buf.writeItem(tradeOffer.getBuyingStacks()[0]);
      buf.writeItem(tradeOffer.getBuyingStacks()[1]);
      buf.writeItem(tradeOffer.getBuyingStacks()[2]);
      buf.writeItem(tradeOffer.getSellingStacks()[0]);
      buf.writeItem(tradeOffer.getSellingStacks()[1]);
      buf.writeItem(tradeOffer.getSellingStacks()[2]);
    }
  }

  public static SSyncTrades decode(FriendlyByteBuf buf) {
    int entityid = buf.readInt();
    int tradeOffersSize = buf.readInt();
    TradeOffers tradeOffers = new TradeOffers();
    for (int i = 0; i < tradeOffersSize; i++) {
      tradeOffers.add(new TradeOffer(buf.readItem(), buf.readItem(), buf.readItem(),
          buf.readItem(), buf.readItem(), buf.readItem()));
    }
    return new SSyncTrades(entityid, tradeOffers);
  }

  public static void handle(SSyncTrades msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.syncTrades(msg.entityid, msg.tradeOffers);
    });
    ctx.get().setPacketHandled(true);
  }
}
