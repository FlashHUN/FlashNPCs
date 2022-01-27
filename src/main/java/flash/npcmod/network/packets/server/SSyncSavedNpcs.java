package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SSyncSavedNpcs {
  List<String> savedNpcs;

  public SSyncSavedNpcs(List<String> savedNpcs) {
    this.savedNpcs = savedNpcs;
  }

  public static void encode(SSyncSavedNpcs msg, PacketBuffer buf) {
    buf.writeInt(msg.savedNpcs.size());
    for (String s : msg.savedNpcs) {
      buf.writeString(s, 32767);
    }
  }

  public static SSyncSavedNpcs decode(PacketBuffer buf) {
    List<String> savedNpcs = new ArrayList<>();
    int size = buf.readInt();
    for (int i = 0; i < size; i++) {
      savedNpcs.add(buf.readString());
    }
    return new SSyncSavedNpcs(savedNpcs);
  }

  public static void handle(SSyncSavedNpcs msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.loadSavedNpcs(msg.savedNpcs);
    });
    ctx.get().setPacketHandled(true);
  }
}
