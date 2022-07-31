package flash.npcmod.network.packets.server;

import flash.npcmod.core.behaviors.CommonBehaviorUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SSendBehaviorEditor {

  String name;
  String data;

  public SSendBehaviorEditor(String name, String data) {
    this.name = name;
    this.data = data;
  }

  public static void encode(SSendBehaviorEditor msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeUtf(msg.data);
  }

  public static SSendBehaviorEditor decode(FriendlyByteBuf buf) {
    return new SSendBehaviorEditor(buf.readUtf(51), buf.readUtf(CommonBehaviorUtil.MAX_BEHAVIOR_LENGTH));
  }

  public static void handle(SSendBehaviorEditor msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      CommonBehaviorUtil.buildBehaviorEditor(msg.name, msg.data);
    });
    ctx.get().setPacketHandled(true);
  }
}
