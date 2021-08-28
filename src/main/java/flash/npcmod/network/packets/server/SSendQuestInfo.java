package flash.npcmod.network.packets.server;

import flash.npcmod.core.quests.CommonQuestUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SSendQuestInfo {

  String name;
  String questInfo;

  public SSendQuestInfo(String name, String questInfo) {
    this.name = name;
    this.questInfo = questInfo;
  }

  public static void encode(SSendQuestInfo msg, PacketBuffer buf) {
    buf.writeString(msg.name, 51);
    buf.writeString(msg.questInfo, 100000);
  }

  public static SSendQuestInfo decode(PacketBuffer buf) {
    return new SSendQuestInfo(buf.readString(51), buf.readString(100000));
  }

  public static void handle(SSendQuestInfo msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      CommonQuestUtil.buildQuest(msg.name, msg.questInfo);
    });
    ctx.get().setPacketHandled(true);
  }
}
