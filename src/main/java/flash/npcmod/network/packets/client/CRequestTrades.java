package flash.npcmod.network.packets.client;

import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CRequestTrades {

  int entityid;

  public CRequestTrades(int entityid) {
    this.entityid = entityid;
  }

  public static void encode(CRequestTrades msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.entityid);
  }

  public static CRequestTrades decode(FriendlyByteBuf buf) {
    return new CRequestTrades(buf.readInt());
  }

  public static void handle(CRequestTrades msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();

      Entity entity = sender.level.getEntity(msg.entityid);

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
