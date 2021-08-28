package flash.npcmod.network.packets.client;

import flash.npcmod.core.quests.CommonQuestUtil;
import flash.npcmod.core.quests.Quest;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CBuildQuest {

  String name;
  String jsonText;

  public CBuildQuest(Quest quest) {
    this(quest.getName(), quest.toJson().toString());
  }

  public CBuildQuest(String name, String jsonText) {
    this.name = name;
    this.jsonText = jsonText;
  }

  public static void encode(CBuildQuest msg, PacketBuffer buf) {
    buf.writeString(msg.name);
    buf.writeString(msg.jsonText);
  }

  public static CBuildQuest decode(PacketBuffer buf) {
    return new CBuildQuest(buf.readString(51),
        buf.readString(100000));
  }

  public static void handle(CBuildQuest msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      if (ctx.get().getSender().hasPermissionLevel(4)) {
        CommonQuestUtil.buildQuest(msg.name, msg.jsonText);

        Quest quest = CommonQuestUtil.loadQuestFile(msg.name);
        if (quest != null)
          ctx.get().getSender().sendStatusMessage(new StringTextComponent("Successfully built quest \'" + msg.name + "\'").mergeStyle(TextFormatting.GREEN), false);
        else
          ctx.get().getSender().sendStatusMessage(new StringTextComponent("Couldn't build quest \'" + msg.name + "\'").mergeStyle(TextFormatting.RED), false);
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
