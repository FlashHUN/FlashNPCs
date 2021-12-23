package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SCompleteQuest {

  String name;
  UUID uuid;

  public SCompleteQuest(String name, UUID uuid) {
    this.name = name;
    this.uuid = uuid;
  }

  public static void encode(SCompleteQuest msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeUUID(msg.uuid);
  }

  public static SCompleteQuest decode(FriendlyByteBuf buf) {
    return new SCompleteQuest(buf.readUtf(51), buf.readUUID());
  }

  public static void handle(SCompleteQuest msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.completeQuest(msg.name, msg.uuid);
    });
    ctx.get().setPacketHandled(true);
  }
}
