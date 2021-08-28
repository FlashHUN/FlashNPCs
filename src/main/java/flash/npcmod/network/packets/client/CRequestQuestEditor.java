package flash.npcmod.network.packets.client;

import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SOpenScreen;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CRequestQuestEditor {

  public CRequestQuestEditor() {}

  public static void encode(CRequestQuestEditor msg, PacketBuffer buf) {}

  public static CRequestQuestEditor decode(PacketBuffer buf) {
    return new CRequestQuestEditor();
  }

  public static void handle(CRequestQuestEditor msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();

      if (sender.hasPermissionLevel(4) && sender.isCreative()) {
        PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.QUESTEDITOR, "", 0), sender);
      }
    });
    ctx.get().setPacketHandled(true);
  }

}
