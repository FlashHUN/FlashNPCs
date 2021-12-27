package flash.npcmod.network.packets.client;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncQuestCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CRequestQuestCapabilitySync {

  public CRequestQuestCapabilitySync() {}

  public static void encode(CRequestQuestCapabilitySync msg, FriendlyByteBuf buf) {}

  public static CRequestQuestCapabilitySync decode(FriendlyByteBuf buf) {
    return new CRequestQuestCapabilitySync();
  }

  public static void handle(CRequestQuestCapabilitySync msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      if (sender != null && sender.isAlive()) {
        IQuestCapability capability = QuestCapabilityProvider.getCapability(sender);

        PacketDispatcher.sendTo(new SSyncQuestCapability(capability.getTrackedQuest()), sender);
        PacketDispatcher.sendTo(new SSyncQuestCapability(capability.getAcceptedQuests().toArray(new QuestInstance[0])), sender);
        PacketDispatcher.sendTo(new SSyncQuestCapability(capability.getCompletedQuests().toArray(new String[0])), sender);
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
