package flash.npcmod.network.packets.client;

import flash.npcmod.core.PermissionHelper;
import flash.npcmod.core.functions.FunctionUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CBuildFunction {

  String name;
  String function;

  public CBuildFunction(String name, String function) {
    this.name = name;
    this.function = function;
  }

  public static void encode(CBuildFunction msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeUtf(msg.function);
  }

  public static CBuildFunction decode(FriendlyByteBuf buf) {
    return new CBuildFunction(buf.readUtf(250),
        buf.readUtf(100000));
  }

  public static void handle(CBuildFunction msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      if (PermissionHelper.hasPermission(ctx.get().getSender(), PermissionHelper.EDIT_FUNCTION)) {
        FunctionUtil.build(msg.name, msg.function);

        FunctionUtil.loadFunctionFile(msg.name);
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
