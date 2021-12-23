package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SResetFunctionNames {

  public SResetFunctionNames() {}

  public static void encode(SResetFunctionNames msg, FriendlyByteBuf buf) {}

  public static SResetFunctionNames decode(FriendlyByteBuf buf) {
    return new SResetFunctionNames();
  }

  public static void handle(SResetFunctionNames msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.resetFunctionNames();
    });
    ctx.get().setPacketHandled(true);
  }
}
