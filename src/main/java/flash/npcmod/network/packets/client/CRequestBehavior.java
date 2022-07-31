package flash.npcmod.network.packets.client;

import com.google.gson.JsonObject;
import flash.npcmod.core.behaviors.CommonBehaviorUtil;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSendBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CRequestBehavior {

    String name;

    public CRequestBehavior(String name) {
        this.name = name;
    }

    public static void encode(CRequestBehavior msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name);
    }

    public static CRequestBehavior decode(FriendlyByteBuf buf) {
        return new CRequestBehavior(buf.readUtf(CommonBehaviorUtil.MAX_BEHAVIOR_LENGTH));
    }

    public static void handle(CRequestBehavior msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();

            JsonObject behaviorObject = CommonBehaviorUtil.loadBehaviorFile(msg.name);

            if (behaviorObject == null) {
                String defaultBehaviorJson = CommonBehaviorUtil.DEFAULT_BEHAVIOR_JSON;
                CommonBehaviorUtil.buildBehavior(msg.name, defaultBehaviorJson);
                // Send to server player
                PacketDispatcher.sendTo(new SSendBehavior(msg.name, defaultBehaviorJson), sender);
            }
        });
        ctx.get().setPacketHandled(true);
    }

}
