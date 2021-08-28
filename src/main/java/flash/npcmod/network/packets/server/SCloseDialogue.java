package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SCloseDialogue {

  public SCloseDialogue() {}

  public static void encode(SCloseDialogue msg, PacketBuffer buf) {}

  public static SCloseDialogue decode(PacketBuffer buf) {
    return new SCloseDialogue();
  }

  public static void handle(SCloseDialogue msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.closeDialogue();
    });
    ctx.get().setPacketHandled(true);
  }
}
