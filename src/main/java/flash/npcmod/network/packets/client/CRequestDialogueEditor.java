package flash.npcmod.network.packets.client;

import flash.npcmod.core.dialogues.CommonDialogueUtil;
import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SOpenScreen;
import flash.npcmod.network.packets.server.SResetFunctionNames;
import flash.npcmod.network.packets.server.SSendDialogueEditor;
import flash.npcmod.network.packets.server.SSendFunctionName;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CRequestDialogueEditor {

  String name;
  int entityid;

  public CRequestDialogueEditor(String name) {
    this(name, -1000);
  }

  public CRequestDialogueEditor(String name, int entityid) {
    this.name = name;
    this.entityid = entityid;
  }

  public static void encode(CRequestDialogueEditor msg, PacketBuffer buf) {
    buf.writeString(msg.name);
    buf.writeInt(msg.entityid);
  }

  public static CRequestDialogueEditor decode(PacketBuffer buf) {
    return new CRequestDialogueEditor(buf.readString(CommonDialogueUtil.MAX_DIALOGUE_LENGTH), buf.readInt());
  }

  public static void handle(CRequestDialogueEditor msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();

      if (sender.hasPermissionLevel(4)) {
        // Send function names to player
        List<String> functionNames = new ArrayList<>();
        for (AbstractFunction function : FunctionUtil.FUNCTIONS) {
          String name = function.getName();
          String[] paramNames = function.getParamNames();
          if (paramNames.length > 0 && !paramNames[0].isEmpty()) {
            name += "::";
            for (int i = 0; i < paramNames.length; i++) {
              name += paramNames[i]+",";
            }
            name = name.substring(0, name.length()-1);
          }
          functionNames.add(name);
        }
        PacketDispatcher.sendTo(new SResetFunctionNames(), sender);
        for (String name : functionNames) {
          PacketDispatcher.sendTo(new SSendFunctionName(name), sender);
        }

        JSONObject dialogue = CommonDialogueUtil.loadDialogueEditorFile(msg.name);

        if (dialogue != null) {
          PacketDispatcher.sendTo(new SSendDialogueEditor(msg.name, dialogue.toString()), sender);
        } else {
          String dialogueEditorJson = CommonDialogueUtil.DEFAULT_DIALOGUE_EDITOR_JSON;
          if (msg.entityid != -1000) {
            Entity entity = sender.world.getEntityByID(msg.entityid);
            if (entity instanceof NpcEntity) {
              NpcEntity npcEntity = (NpcEntity) entity;
              for (String name : CommonDialogueUtil.HELLO_THERE_NAMES) {
                if (npcEntity.getName().getString().equalsIgnoreCase(name)) {
                  dialogueEditorJson = CommonDialogueUtil.DEFAULT_DIALOGUE_EDITOR_JSON_HELLO_THERE;
                  break;
                }
              }
              if (npcEntity.getName().getString().equalsIgnoreCase(CommonDialogueUtil.KICK_GUM_NAME)) {
                dialogueEditorJson = CommonDialogueUtil.DEFAULT_DIALOGUE_EDITOR_JSON_KICK_GUM;
              }
            }
          }
          CommonDialogueUtil.buildDialogueEditor(msg.name, dialogueEditorJson);
          PacketDispatcher.sendTo(new SSendDialogueEditor(msg.name, dialogueEditorJson), sender);
        }

        PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.EDITDIALOGUE, msg.name, msg.entityid), sender);
      }
    });
    ctx.get().setPacketHandled(true);
  }

}
