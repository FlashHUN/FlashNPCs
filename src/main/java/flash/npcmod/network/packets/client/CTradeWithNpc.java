package flash.npcmod.network.packets.client;

import flash.npcmod.core.trades.TradeOffer;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CTradeWithNpc {

  int entityid;
  int tradeid;

  public CTradeWithNpc(int entityid, int tradeid) {
    this.entityid = entityid;
    this.tradeid = tradeid;
  }

  public static void encode(CTradeWithNpc msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.entityid);
    buf.writeInt(msg.tradeid);
  }

  public static CTradeWithNpc decode(FriendlyByteBuf buf) {
    return new CTradeWithNpc(buf.readInt(), buf.readInt());
  }

  public static void handle(CTradeWithNpc msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      Entity entity = sender.level.getEntity(msg.entityid);
      if (entity instanceof NpcEntity) {
        NpcEntity npcEntity = (NpcEntity) entity;
        if (msg.tradeid >= 0 && msg.tradeid < npcEntity.getOffers().size()) {
          TradeOffer tradeOffer = npcEntity.getOffers().get(msg.tradeid);
          tradeOffer.doTransaction(sender);
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }

}
