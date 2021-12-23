package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SMoveToDialogue {

  String name;
  int entityid;

  public SMoveToDialogue(String name, int entityid) {
    this.name = name;
    this.entityid = entityid;
  }

  public static void encode(SMoveToDialogue msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeInt(msg.entityid);
  }

  public static SMoveToDialogue decode(FriendlyByteBuf buf) {
    return new SMoveToDialogue(buf.readUtf(51), buf.readInt());
  }

  public static void handle(SMoveToDialogue msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.moveToDialogue(msg.name, msg.entityid);
    });
    ctx.get().setPacketHandled(true);
  }
}
