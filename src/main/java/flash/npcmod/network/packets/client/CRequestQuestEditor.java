package flash.npcmod.network.packets.client;

import flash.npcmod.core.PermissionHelper;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SOpenScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CRequestQuestEditor {

  public CRequestQuestEditor() {}

  public static void encode(CRequestQuestEditor msg, FriendlyByteBuf buf) {}

  public static CRequestQuestEditor decode(FriendlyByteBuf buf) {
    return new CRequestQuestEditor();
  }

  public static void handle(CRequestQuestEditor msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();

      if (PermissionHelper.hasPermission(sender, PermissionHelper.EDIT_QUEST) && sender.isCreative()) {
        PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.QUESTEDITOR, "", 0), sender);
      }
    });
    ctx.get().setPacketHandled(true);
  }

}
