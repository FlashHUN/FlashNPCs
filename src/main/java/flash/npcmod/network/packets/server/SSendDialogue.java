package flash.npcmod.network.packets.server;

import flash.npcmod.core.dialogues.CommonDialogueUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SSendDialogue {

  String name;
  String dialogue;

  public SSendDialogue(String name, String dialogue) {
    this.name = name;
    this.dialogue = dialogue;
  }

  public static void encode(SSendDialogue msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeUtf(msg.dialogue);
  }

  public static SSendDialogue decode(FriendlyByteBuf buf) {
    return new SSendDialogue(buf.readUtf(51), buf.readUtf(CommonDialogueUtil.MAX_DIALOGUE_LENGTH));
  }

  public static void handle(SSendDialogue msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      CommonDialogueUtil.buildDialogue(msg.name, msg.dialogue);
    });
    ctx.get().setPacketHandled(true);
  }
}
