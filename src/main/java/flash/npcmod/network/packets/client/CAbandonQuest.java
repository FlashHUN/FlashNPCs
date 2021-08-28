package flash.npcmod.network.packets.client;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.quests.QuestInstance;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class CAbandonQuest {

  String name;
  UUID pickedUpFrom;

  public CAbandonQuest(QuestInstance questInstance) {
    this(questInstance.getQuest().getName(), questInstance.getPickedUpFrom());
  }

  public CAbandonQuest(String name, UUID pickedUpFrom) {
    this.name = name;
    this.pickedUpFrom = pickedUpFrom;
  }

  public static void encode(CAbandonQuest msg, PacketBuffer buf) {
    buf.writeString(msg.name);
    buf.writeUniqueId(msg.pickedUpFrom);
  }

  public static CAbandonQuest decode(PacketBuffer buf) {
    return new CAbandonQuest(buf.readString(51),
        buf.readUniqueId());
  }

  public static void handle(CAbandonQuest msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();
      IQuestCapability capability = QuestCapabilityProvider.getCapability(sender);
      List<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
      for (QuestInstance questInstance : acceptedQuests) {
        if (questInstance.getQuest().getName().equals(msg.name) && questInstance.getPickedUpFrom().equals(msg.pickedUpFrom)) {
          capability.abandonQuest(questInstance);
          break;
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
