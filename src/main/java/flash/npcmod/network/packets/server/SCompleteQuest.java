package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SCompleteQuest {

  String name;
  UUID uuid;

  public SCompleteQuest(String name, UUID uuid) {
    this.name = name;
    this.uuid = uuid;
  }

  public static void encode(SCompleteQuest msg, PacketBuffer buf) {
    buf.writeString(msg.name);
    buf.writeUniqueId(msg.uuid);
  }

  public static SCompleteQuest decode(PacketBuffer buf) {
    return new SCompleteQuest(buf.readString(51), buf.readUniqueId());
  }

  public static void handle(SCompleteQuest msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.completeQuest(msg.name, msg.uuid);
    });
    ctx.get().setPacketHandled(true);
  }
}
