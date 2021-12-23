package flash.npcmod.network.packets.server;

import flash.npcmod.core.dialogues.CommonDialogueUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SSendDialogueEditor {

  String name;
  String data;

  public SSendDialogueEditor(String name, String data) {
    this.name = name;
    this.data = data;
  }

  public static void encode(SSendDialogueEditor msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeUtf(msg.data);
  }

  public static SSendDialogueEditor decode(FriendlyByteBuf buf) {
    return new SSendDialogueEditor(buf.readUtf(51), buf.readUtf(CommonDialogueUtil.MAX_DIALOGUE_LENGTH));
  }

  public static void handle(SSendDialogueEditor msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      CommonDialogueUtil.buildDialogueEditor(msg.name, msg.data);
    });
    ctx.get().setPacketHandled(true);
  }
}
