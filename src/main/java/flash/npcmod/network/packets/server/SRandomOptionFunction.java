package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SRandomOptionFunction {

  public SRandomOptionFunction() {}

  public static void encode(SRandomOptionFunction msg, PacketBuffer buf) {}

  public static SRandomOptionFunction decode(PacketBuffer buf) {
    return new SRandomOptionFunction();
  }

  public static void handle(SRandomOptionFunction msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.randomDialogueOption();
    });
    ctx.get().setPacketHandled(true);
  }
}
