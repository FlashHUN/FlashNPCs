package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import flash.npcmod.core.trades.TradeOffer;
import flash.npcmod.core.trades.TradeOffers;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SSyncTrades {

  int entityid;
  TradeOffers tradeOffers;

  public SSyncTrades(NpcEntity npcEntity) {
    this(npcEntity.getEntityId(), npcEntity.getOffers());
  }

  public SSyncTrades(int entityid, TradeOffers tradeOffers) {
    this.entityid = entityid;
    this.tradeOffers = tradeOffers;
  }

  public static void encode(SSyncTrades msg, PacketBuffer buf) {
    buf.writeInt(msg.entityid);
    buf.writeInt(msg.tradeOffers.size());
    for (TradeOffer tradeOffer : msg.tradeOffers) {
      buf.writeItemStack(tradeOffer.getBuyingStacks()[0]);
      buf.writeItemStack(tradeOffer.getBuyingStacks()[1]);
      buf.writeItemStack(tradeOffer.getBuyingStacks()[2]);
      buf.writeItemStack(tradeOffer.getSellingStacks()[0]);
      buf.writeItemStack(tradeOffer.getSellingStacks()[1]);
      buf.writeItemStack(tradeOffer.getSellingStacks()[2]);
    }
  }

  public static SSyncTrades decode(PacketBuffer buf) {
    int entityid = buf.readInt();
    int tradeOffersSize = buf.readInt();
    TradeOffers tradeOffers = new TradeOffers();
    for (int i = 0; i < tradeOffersSize; i++) {
      tradeOffers.add(new TradeOffer(buf.readItemStack(), buf.readItemStack(), buf.readItemStack(),
          buf.readItemStack(), buf.readItemStack(), buf.readItemStack()));
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
