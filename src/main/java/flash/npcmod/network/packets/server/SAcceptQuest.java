package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SAcceptQuest {

  String name;
  int entityid;

  public SAcceptQuest(String name, int entityid) {
    this.name = name;
    this.entityid = entityid;
  }

  public static void encode(SAcceptQuest msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeInt(msg.entityid);
  }

  public static SAcceptQuest decode(FriendlyByteBuf buf) {
    return new SAcceptQuest(buf.readUtf(51), buf.readInt());
  }

  public static void handle(SAcceptQuest msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.acceptQuest(msg.name, msg.entityid);
    });
    ctx.get().setPacketHandled(true);
  }
}
