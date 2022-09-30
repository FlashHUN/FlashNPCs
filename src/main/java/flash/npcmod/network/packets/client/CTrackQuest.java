package flash.npcmod.network.packets.client;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncQuestCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class CTrackQuest {

  String name;

  public CTrackQuest(QuestInstance questInstance) {
    this(questInstance.getQuest().getName());
  }

  public CTrackQuest(String name) {
    this.name = name;
  }

  public static void encode(CTrackQuest msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
  }

  public static CTrackQuest decode(FriendlyByteBuf buf) {
    return new CTrackQuest(buf.readUtf(51));
  }

  public static void handle(CTrackQuest msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      IQuestCapability capability = QuestCapabilityProvider.getCapability(sender);
      if (!msg.name.isEmpty() && !capability.getTrackedQuest().equals(msg.name)) {
        List<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
        for (QuestInstance questInstance : acceptedQuests) {
          if (questInstance.getQuest().getName().equals(msg.name)) {
            capability.setTrackedQuest(msg.name);
            break;
          }
        }
      }
      else capability.setTrackedQuest("");
      PacketDispatcher.sendTo(new SSyncQuestCapability(capability.getTrackedQuest()), sender);
    });
    ctx.get().setPacketHandled(true);
  }
}
