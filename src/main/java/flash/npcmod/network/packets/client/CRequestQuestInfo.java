package flash.npcmod.network.packets.client;

import com.google.gson.JsonObject;
import flash.npcmod.Main;
import flash.npcmod.core.quests.CommonQuestUtil;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSendQuestInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CRequestQuestInfo {

  String name;

  public CRequestQuestInfo(String name) {
    this.name = name;
  }

  public static void encode(CRequestQuestInfo msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
  }

  public static CRequestQuestInfo decode(FriendlyByteBuf buf) {
    return new CRequestQuestInfo(buf.readUtf(51));
  }

  public static void handle(CRequestQuestInfo msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();

      JsonObject quest = CommonQuestUtil.loadQuestAsJson(msg.name);

      if (quest != null) {
        PacketDispatcher.sendTo(new SSendQuestInfo(msg.name, quest.toString()), sender);
      } else {
        Main.LOGGER.warn(sender.getName().getString() + " requested invalid quest info: " + msg.name);
      }

    });
    ctx.get().setPacketHandled(true);
  }

}
