package flash.npcmod.network.packets.client;

import flash.npcmod.Main;
import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.core.trades.TradeOffers;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncTrades;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CRequestTrades {

  int entityid;

  public CRequestTrades(int entityid) {
    this.entityid = entityid;
  }

  public static void encode(CRequestTrades msg, PacketBuffer buf) {
    buf.writeInt(msg.entityid);
  }

  public static CRequestTrades decode(PacketBuffer buf) {
    return new CRequestTrades(buf.readInt());
  }

  public static void handle(CRequestTrades msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();

      Entity entity = sender.world.getEntityByID(msg.entityid);

      if (entity instanceof NpcEntity) {
        NpcEntity npcEntity = (NpcEntity) entity;
        if (!npcEntity.getOffers().isEmpty()) {
          FunctionUtil.callFromName("openTrades", sender, npcEntity);
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }

}
