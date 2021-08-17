package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SSendFunctionName {

  String name;

  public SSendFunctionName(String name) {
    this.name = name;
  }

  public static void encode(SSendFunctionName msg, PacketBuffer buf) {
    buf.writeString(msg.name);
  }

  public static SSendFunctionName decode(PacketBuffer buf) {
    return new SSendFunctionName(buf.readString(51));
  }

  public static void handle(SSendFunctionName msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.addFunctionName(msg.name);
    });
    ctx.get().setPacketHandled(true);
  }
}
