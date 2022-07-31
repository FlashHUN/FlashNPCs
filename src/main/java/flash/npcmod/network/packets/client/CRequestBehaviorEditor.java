package flash.npcmod.network.packets.client;

import com.google.gson.JsonObject;
import flash.npcmod.core.behaviors.CommonBehaviorUtil;
import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.core.functions.FunctionUtil;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CRequestBehaviorEditor {

  String name;
  int entityid;

  public CRequestBehaviorEditor(String name) {
    this(name, -1000);
  }

  public CRequestBehaviorEditor(String name, int entityid) {
    this.name = name;
    this.entityid = entityid;
  }

  public static void encode(CRequestBehaviorEditor msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeInt(msg.entityid);
  }

  public static CRequestBehaviorEditor decode(FriendlyByteBuf buf) {
    return new CRequestBehaviorEditor(buf.readUtf(CommonBehaviorUtil.MAX_BEHAVIOR_LENGTH), buf.readInt());
  }

  public static void handle(CRequestBehaviorEditor msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();

      if (sender.hasPermissions(4)) {
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

        JsonObject behavior = CommonBehaviorUtil.loadBehaviorEditorFile(msg.name);

        if (behavior != null) {
          PacketDispatcher.sendTo(new SSendBehaviorEditor(msg.name, behavior.toString()), sender);
        } else {
          String behaviorEditorJson = CommonBehaviorUtil.DEFAULT_BEHAVIOR_EDITOR_JSON;
          CommonBehaviorUtil.buildBehaviorEditor(msg.name, behaviorEditorJson);
          PacketDispatcher.sendTo(new SSendBehaviorEditor(msg.name, behaviorEditorJson), sender);
        }
        PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.EDITBEHAVIOR, msg.name, msg.entityid), sender);
      }
    });
    ctx.get().setPacketHandled(true);
  }

}
