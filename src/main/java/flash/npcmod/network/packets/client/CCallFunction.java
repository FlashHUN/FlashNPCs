package flash.npcmod.network.packets.client;

import flash.npcmod.core.functions.FunctionUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CCallFunction {

  String functionName;

  public CCallFunction(String name) {
    this.functionName = name;
  }

  public static void encode(CCallFunction msg, PacketBuffer buf) {
    buf.writeString(msg.functionName);
  }

  public static CCallFunction decode(PacketBuffer buf) {
    return new CCallFunction(buf.readString(51));
  }

  public static void handle(CCallFunction msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      FunctionUtil.callFromName(msg.functionName, ctx.get().getSender());
    });
    ctx.get().setPacketHandled(true);
  }
}
