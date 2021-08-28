package flash.npcmod.network.packets.server;

import flash.npcmod.core.dialogues.CommonDialogueUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SSendDialogueEditor {

  String name;
  String data;

  public SSendDialogueEditor(String name, String data) {
    this.name = name;
    this.data = data;
  }

  public static void encode(SSendDialogueEditor msg, PacketBuffer buf) {
    buf.writeString(msg.name);
    buf.writeString(msg.data);
  }

  public static SSendDialogueEditor decode(PacketBuffer buf) {
    return new SSendDialogueEditor(buf.readString(51), buf.readString(CommonDialogueUtil.MAX_DIALOGUE_LENGTH));
  }

  public static void handle(SSendDialogueEditor msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      CommonDialogueUtil.buildDialogueEditor(msg.name, msg.data);
    });
    ctx.get().setPacketHandled(true);
  }
}
