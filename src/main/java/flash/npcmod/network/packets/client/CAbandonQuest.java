package flash.npcmod.network.packets.client;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.quests.QuestInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

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

  public static void encode(CAbandonQuest msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeUUID(msg.pickedUpFrom);
  }

  public static CAbandonQuest decode(FriendlyByteBuf buf) {
    return new CAbandonQuest(buf.readUtf(51),
        buf.readUUID());
  }

  public static void handle(CAbandonQuest msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
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
