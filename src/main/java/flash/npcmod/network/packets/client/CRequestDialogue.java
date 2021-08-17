package flash.npcmod.network.packets.client;

import flash.npcmod.core.dialogues.CommonDialogueUtil;
import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SOpenScreen;
import flash.npcmod.network.packets.server.SSendDialogue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.json.JSONObject;

import java.util.function.Supplier;

public class CRequestDialogue {

  String name;
  int entityid;

  public CRequestDialogue(String name) {
    this(name, -1000);
  }

  public CRequestDialogue(String name, int entityid) {
    this.name = name;
    this.entityid = entityid;
  }

  public static void encode(CRequestDialogue msg, PacketBuffer buf) {
    buf.writeString(msg.name);
    buf.writeInt(msg.entityid);
  }

  public static CRequestDialogue decode(PacketBuffer buf) {
    return new CRequestDialogue(buf.readString(CommonDialogueUtil.MAX_DIALOGUE_LENGTH), buf.readInt());
  }

  public static void handle(CRequestDialogue msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();

      FunctionUtil.loadAllFunctions();

      JSONObject dialogue = CommonDialogueUtil.loadDialogueFile(msg.name);

      if (dialogue != null) {
        PacketDispatcher.sendTo(new SSendDialogue(msg.name, dialogue.toString()), sender);
      } else {
        String dialogueJson = CommonDialogueUtil.DEFAULT_DIALOGUE_JSON;
        if (msg.entityid != -1000) {
          Entity entity = sender.world.getEntityByID(msg.entityid);
          if (entity instanceof NpcEntity) {
            NpcEntity npcEntity = (NpcEntity) entity;
            for (String name : CommonDialogueUtil.HELLO_THERE_NAMES) {
              if (npcEntity.getName().getString().equalsIgnoreCase(name)) {
                dialogueJson = CommonDialogueUtil.DEFAULT_DIALOGUE_JSON_HELLO_THERE;
                break;
              }
            }
          }
        }
        CommonDialogueUtil.buildDialogue(msg.name, dialogueJson);
        PacketDispatcher.sendTo(new SSendDialogue(msg.name, dialogueJson), sender);
      }

      PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.DIALOGUE, msg.name, msg.entityid), sender);

    });
    ctx.get().setPacketHandled(true);
  }

}
