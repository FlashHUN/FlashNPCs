package flash.npcmod.network.packets.client;

import flash.npcmod.core.dialogues.CommonDialogueUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CEditDialogue {

  String name;
  String dialogue;
  String dialogueEditor;

  public CEditDialogue(String name, String dialogue, String dialogueEditor) {
    this.name = name;
    this.dialogue = dialogue;
    this.dialogueEditor = dialogueEditor;
  }

  public static void encode(CEditDialogue msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeUtf(msg.dialogue);
    buf.writeUtf(msg.dialogueEditor);
  }

  public static CEditDialogue decode(FriendlyByteBuf buf) {
    return new CEditDialogue(buf.readUtf(51), buf.readUtf(CommonDialogueUtil.MAX_DIALOGUE_LENGTH), buf.readUtf(CommonDialogueUtil.MAX_DIALOGUE_LENGTH));
  }

  public static void handle(CEditDialogue msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      if (ctx.get().getSender().hasPermissions(4)) {
        CommonDialogueUtil.buildDialogue(msg.name, msg.dialogue);
        CommonDialogueUtil.buildDialogueEditor(msg.name, msg.dialogueEditor);
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
