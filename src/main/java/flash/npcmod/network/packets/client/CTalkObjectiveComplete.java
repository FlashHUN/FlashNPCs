package flash.npcmod.network.packets.client;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.quests.QuestObjective;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CTalkObjectiveComplete {

  String objective;

  public CTalkObjectiveComplete(String objectiveName) {
    this.objective = objectiveName;
  }

  public static void encode(CTalkObjectiveComplete msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.objective, 1000);
  }

  public static CTalkObjectiveComplete decode(FriendlyByteBuf buf) {
    return new CTalkObjectiveComplete(buf.readUtf(1000));
  }

  public static void handle(CTalkObjectiveComplete msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      IQuestCapability capability = QuestCapabilityProvider.getCapability(sender);

      String[] split = msg.objective.split(":::");
      String questName = split[0];
      String objectiveName = split[1];

      capability.getAcceptedQuests().forEach(questInstance -> {
        if (questInstance.getQuest().getName().equals(questName)) {
          questInstance.getQuest().getObjectives().forEach(objective -> {
            if (objective.getName().equals(objectiveName)) {
              if (objective.getType().equals(QuestObjective.ObjectiveType.Talk)) {
                if (!objective.isHidden()) {
                  objective.setProgress(objective.getAmount());
                }
              }
            }
          });
        }
      });
    });
    ctx.get().setPacketHandled(true);
  }

}
