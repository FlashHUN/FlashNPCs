package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SSendFunctionName {

  String name;

  public SSendFunctionName(String name) {
    this.name = name;
  }

  public static void encode(SSendFunctionName msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
  }

  public static SSendFunctionName decode(FriendlyByteBuf buf) {
    return new SSendFunctionName(buf.readUtf(250));
  }

  public static void handle(SSendFunctionName msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.addFunctionName(msg.name);
    });
    ctx.get().setPacketHandled(true);
  }
}
