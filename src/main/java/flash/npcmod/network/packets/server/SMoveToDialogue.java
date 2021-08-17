package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SMoveToDialogue {

  String name;

  public SMoveToDialogue(String name) {
    this.name = name;
  }

  public static void encode(SMoveToDialogue msg, PacketBuffer buf) {
    buf.writeString(msg.name);
  }

  public static SMoveToDialogue decode(PacketBuffer buf) {
    return new SMoveToDialogue(buf.readString(51));
  }

  public static void handle(SMoveToDialogue msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.moveToDialogue(msg.name);
    });
    ctx.get().setPacketHandled(true);
  }
}
