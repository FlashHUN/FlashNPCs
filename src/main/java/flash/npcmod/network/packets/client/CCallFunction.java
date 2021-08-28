package flash.npcmod.network.packets.client;

import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CCallFunction {

  String functionName;
  int entityid;

  public CCallFunction(String name, int entityid) {
    this.functionName = name;
    this.entityid = entityid;
  }

  public static void encode(CCallFunction msg, PacketBuffer buf) {
    buf.writeString(msg.functionName);
    buf.writeInt(msg.entityid);
  }

  public static CCallFunction decode(PacketBuffer buf) {
    return new CCallFunction(buf.readString(250), buf.readInt());
  }

  public static void handle(CCallFunction msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();
      Entity entity  = sender.world.getEntityByID(msg.entityid);
      if (entity instanceof NpcEntity)
        FunctionUtil.callFromName(msg.functionName, sender, (NpcEntity) entity);
    });
    ctx.get().setPacketHandled(true);
  }
}
