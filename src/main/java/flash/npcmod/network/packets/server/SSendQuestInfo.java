package flash.npcmod.network.packets.server;

import flash.npcmod.core.quests.CommonQuestUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SSendQuestInfo {

  String name;
  String questInfo;

  public SSendQuestInfo(String name, String questInfo) {
    this.name = name;
    this.questInfo = questInfo;
  }

  public static void encode(SSendQuestInfo msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name, 51);
    buf.writeUtf(msg.questInfo, 100000);
  }

  public static SSendQuestInfo decode(FriendlyByteBuf buf) {
    return new SSendQuestInfo(buf.readUtf(51), buf.readUtf(100000));
  }

  public static void handle(SSendQuestInfo msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      CommonQuestUtil.buildQuest(msg.name, msg.questInfo);
    });
    ctx.get().setPacketHandled(true);
  }
}
