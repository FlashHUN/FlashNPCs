package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SResetFunctionNames {

  public SResetFunctionNames() {}

  public static void encode(SResetFunctionNames msg, PacketBuffer buf) {}

  public static SResetFunctionNames decode(PacketBuffer buf) {
    return new SResetFunctionNames();
  }

  public static void handle(SResetFunctionNames msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.resetFunctionNames();
    });
    ctx.get().setPacketHandled(true);
  }
}
