package flash.npcmod.network.packets.server;

import flash.npcmod.core.behaviors.CommonBehaviorUtil;
import flash.npcmod.core.dialogues.CommonDialogueUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SSendBehavior {

  String name;
  String behavior;

  public SSendBehavior(String name, String behavior) {
    this.name = name;
    this.behavior = behavior;
  }

  public static void encode(SSendBehavior msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeUtf(msg.behavior);
  }

  public static SSendBehavior decode(FriendlyByteBuf buf) {
    return new SSendBehavior(buf.readUtf(51), buf.readUtf(CommonDialogueUtil.MAX_DIALOGUE_LENGTH));
  }

  public static void handle(SSendBehavior msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      CommonBehaviorUtil.buildBehavior(msg.name, msg.behavior);
    });
    ctx.get().setPacketHandled(true);
  }
}
