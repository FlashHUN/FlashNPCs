package flash.npcmod.network.packets.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.core.behaviors.Behavior;
import flash.npcmod.core.behaviors.BehaviorSavedData;
import flash.npcmod.core.behaviors.CommonBehaviorUtil;
import flash.npcmod.core.dialogues.CommonDialogueUtil;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CEditBehavior {

  String behaviorJson;
  String behaviorEditor;
  int entityId;
  String name;

  public CEditBehavior(String name, int entityId, String behaviorJson, String behaviorEditorJson) {
    this.name = name;
    this.behaviorJson = behaviorJson;
    this.behaviorEditor = behaviorEditorJson;
    this.entityId = entityId;
  }

  public static void encode(CEditBehavior msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeInt(msg.entityId);
    buf.writeUtf(msg.behaviorJson);
    buf.writeUtf(msg.behaviorEditor);
  }

  public static CEditBehavior decode(FriendlyByteBuf buf) {
    return new CEditBehavior(
            buf.readUtf(51), buf.readInt(), buf.readUtf(CommonDialogueUtil.MAX_DIALOGUE_LENGTH),
            buf.readUtf(CommonDialogueUtil.MAX_DIALOGUE_LENGTH)
    );
  }

  public static void handle(CEditBehavior msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      if (sender.hasPermissions(4)) {
        BehaviorSavedData savedData = BehaviorSavedData.getBehaviorSavedData(ctx.get().getSender().getServer(), msg.name);
        savedData.setBehaviors(Behavior.multipleFromJSONObject(new Gson().fromJson(msg.behaviorJson, JsonObject.class)));
        savedData.setDirty();
        // refresh the current npc ai.
        if (msg.entityId != -1000) {
          NpcEntity npc = ((NpcEntity) sender.level.getEntity(msg.entityId));
          if (npc != null) {
            npc.resetBehavior();
          }
        }
        CommonBehaviorUtil.buildBehavior(msg.name, msg.behaviorJson);
        CommonBehaviorUtil.buildBehaviorEditor(msg.name, msg.behaviorEditor);
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
