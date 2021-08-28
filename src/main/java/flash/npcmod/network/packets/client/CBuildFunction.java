package flash.npcmod.network.packets.client;

import flash.npcmod.core.functions.FunctionUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CBuildFunction {

  String name;
  String function;

  public CBuildFunction(String name, String function) {
    this.name = name;
    this.function = function;
  }

  public static void encode(CBuildFunction msg, PacketBuffer buf) {
    buf.writeString(msg.name);
    buf.writeString(msg.function);
  }

  public static CBuildFunction decode(PacketBuffer buf) {
    return new CBuildFunction(buf.readString(250),
        buf.readString(100000));
  }

  public static void handle(CBuildFunction msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      if (ctx.get().getSender().hasPermissionLevel(4)) {
        FunctionUtil.build(msg.name, msg.function);

        FunctionUtil.loadFunctionFile(msg.name);
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
