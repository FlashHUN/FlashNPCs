package flash.npcmod.network.packets.client;

import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CCallFunction {

  String functionName;
  int entityid;

  public CCallFunction(String name, int entityid) {
    this.functionName = name;
    this.entityid = entityid;
  }

  public static void encode(CCallFunction msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.functionName);
    buf.writeInt(msg.entityid);
  }

  public static CCallFunction decode(FriendlyByteBuf buf) {
    return new CCallFunction(buf.readUtf(250), buf.readInt());
  }

  public static void handle(CCallFunction msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      Entity entity  = sender.level.getEntity(msg.entityid);
      if (entity instanceof NpcEntity)
        FunctionUtil.callFromName(msg.functionName, sender, (NpcEntity) entity);
    });
    ctx.get().setPacketHandled(true);
  }
}
