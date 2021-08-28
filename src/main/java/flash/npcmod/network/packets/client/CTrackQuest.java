package flash.npcmod.network.packets.client;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncQuestCapability;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

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

  public static void encode(CTrackQuest msg, PacketBuffer buf) {
    buf.writeString(msg.name);
  }

  public static CTrackQuest decode(PacketBuffer buf) {
    return new CTrackQuest(buf.readString(51));
  }

  public static void handle(CTrackQuest msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();
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
