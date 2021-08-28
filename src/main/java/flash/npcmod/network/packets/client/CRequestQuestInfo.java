package flash.npcmod.network.packets.client;

import flash.npcmod.Main;
import flash.npcmod.core.quests.CommonQuestUtil;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSendQuestInfo;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.json.JSONObject;

import java.util.function.Supplier;

public class CRequestQuestInfo {

  String name;

  public CRequestQuestInfo(String name) {
    this.name = name;
  }

  public static void encode(CRequestQuestInfo msg, PacketBuffer buf) {
    buf.writeString(msg.name);
  }

  public static CRequestQuestInfo decode(PacketBuffer buf) {
    return new CRequestQuestInfo(buf.readString(51));
  }

  public static void handle(CRequestQuestInfo msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();

      JSONObject quest = CommonQuestUtil.loadQuest(msg.name);

      if (quest != null) {
        PacketDispatcher.sendTo(new SSendQuestInfo(msg.name, quest.toString()), sender);
      } else {
        Main.LOGGER.warn(sender.getName().getString() + " requested invalid quest info: " + msg.name);
      }

    });
    ctx.get().setPacketHandled(true);
  }

}
