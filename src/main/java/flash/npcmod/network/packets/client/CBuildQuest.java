package flash.npcmod.network.packets.client;

import flash.npcmod.core.PermissionHelper;
import flash.npcmod.core.quests.CommonQuestUtil;
import flash.npcmod.core.quests.Quest;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.network.NetworkEvent;

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

  public static void encode(CBuildQuest msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeUtf(msg.jsonText);
  }

  public static CBuildQuest decode(FriendlyByteBuf buf) {
    return new CBuildQuest(buf.readUtf(51),
        buf.readUtf(100000));
  }

  public static void handle(CBuildQuest msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      if (PermissionHelper.hasPermission(ctx.get().getSender(), PermissionHelper.EDIT_QUEST)) {
        CommonQuestUtil.buildQuest(msg.name, msg.jsonText);

        Quest quest = CommonQuestUtil.loadQuestFile(msg.name);
        if (quest != null)
          ctx.get().getSender().displayClientMessage(new TextComponent("Successfully built quest \'" + msg.name + "\'").withStyle(ChatFormatting.GREEN), false);
        else
          ctx.get().getSender().displayClientMessage(new TextComponent("Couldn't build quest \'" + msg.name + "\'").withStyle(ChatFormatting.RED), false);
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
