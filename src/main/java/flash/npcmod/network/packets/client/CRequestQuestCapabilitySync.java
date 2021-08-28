package flash.npcmod.network.packets.client;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncQuestCapability;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CRequestQuestCapabilitySync {

  public CRequestQuestCapabilitySync() {}

  public static void encode(CRequestQuestCapabilitySync msg, PacketBuffer buf) {}

  public static CRequestQuestCapabilitySync decode(PacketBuffer buf) {
    return new CRequestQuestCapabilitySync();
  }

  public static void handle(CRequestQuestCapabilitySync msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();
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
