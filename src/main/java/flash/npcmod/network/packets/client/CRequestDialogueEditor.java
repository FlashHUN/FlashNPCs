package flash.npcmod.network.packets.client;

import com.google.gson.JsonObject;
import flash.npcmod.core.PermissionHelper;
import flash.npcmod.core.dialogues.CommonDialogueUtil;
import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SOpenScreen;
import flash.npcmod.network.packets.server.SResetFunctionNames;
import flash.npcmod.network.packets.server.SSendDialogueEditor;
import flash.npcmod.network.packets.server.SSendFunctionName;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

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

  public static void encode(CRequestDialogueEditor msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeInt(msg.entityid);
  }

  public static CRequestDialogueEditor decode(FriendlyByteBuf buf) {
    return new CRequestDialogueEditor(buf.readUtf(CommonDialogueUtil.MAX_DIALOGUE_LENGTH), buf.readInt());
  }

  public static void handle(CRequestDialogueEditor msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();

      if (PermissionHelper.hasPermission(sender, PermissionHelper.EDIT_DIALOGUE)) {
        // Send function names to player
        List<String> functionNames = new ArrayList<>();
        for (AbstractFunction function : FunctionUtil.FUNCTIONS) {
          StringBuilder name = new StringBuilder(function.getName());
          String[] paramNames = function.getParamNames();
          if (paramNames.length > 0 && !paramNames[0].isEmpty()) {
            name.append("::");
            for (String paramName : paramNames) {
              name.append(paramName).append(",");
            }
            name = new StringBuilder(name.substring(0, name.length() - 1));
          }
          functionNames.add(name.toString());
        }
        PacketDispatcher.sendTo(new SResetFunctionNames(), sender);
        for (String name : functionNames) {
          PacketDispatcher.sendTo(new SSendFunctionName(name), sender);
        }

        JsonObject dialogue = CommonDialogueUtil.loadDialogueEditorFile(msg.name);

        if (dialogue != null) {
          PacketDispatcher.sendTo(new SSendDialogueEditor(msg.name, dialogue.toString()), sender);
        } else {
          String dialogueEditorJson = CommonDialogueUtil.DEFAULT_DIALOGUE_EDITOR_JSON;
          if (msg.entityid != -1000) {
            Entity entity = sender.level.getEntity(msg.entityid);
            if (entity instanceof NpcEntity npcEntity) {
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

        if (msg.entityid != -1000) {
          PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.EDITDIALOGUE, msg.name, msg.entityid), sender);
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }

}
