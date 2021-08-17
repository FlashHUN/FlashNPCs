package flash.npcmod.network.packets.server;

import flash.npcmod.core.dialogues.CommonDialogueUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SSendDialogue {

  String name;
  String dialogue;

  public SSendDialogue(String name, String dialogue) {
    this.name = name;
    this.dialogue = dialogue;
  }

  public static void encode(SSendDialogue msg, PacketBuffer buf) {
    buf.writeString(msg.name);
    buf.writeString(msg.dialogue);
  }

  public static SSendDialogue decode(PacketBuffer buf) {
    return new SSendDialogue(buf.readString(51), buf.readString(32767));
  }

  public static void handle(SSendDialogue msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      CommonDialogueUtil.buildDialogue(msg.name, msg.dialogue);
    });
    ctx.get().setPacketHandled(true);
  }
}
