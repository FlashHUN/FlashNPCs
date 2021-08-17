package flash.npcmod.network.packets.client;

import flash.npcmod.core.dialogues.CommonDialogueUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

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

  public static void encode(CEditDialogue msg, PacketBuffer buf) {
    buf.writeString(msg.name);
    buf.writeString(msg.dialogue);
    buf.writeString(msg.dialogueEditor);
  }

  public static CEditDialogue decode(PacketBuffer buf) {
    return new CEditDialogue(buf.readString(51), buf.readString(CommonDialogueUtil.MAX_DIALOGUE_LENGTH), buf.readString(CommonDialogueUtil.MAX_DIALOGUE_LENGTH));
  }

  public static void handle(CEditDialogue msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      if (ctx.get().getSender().hasPermissionLevel(4)) {
        CommonDialogueUtil.buildDialogue(msg.name, msg.dialogue);
        CommonDialogueUtil.buildDialogueEditor(msg.name, msg.dialogueEditor);
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
