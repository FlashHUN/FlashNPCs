package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import flash.npcmod.core.EntityUtil;
import flash.npcmod.core.behaviors.CommonBehaviorUtil;
import flash.npcmod.core.dialogues.CommonDialogueUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SSyncEntityList {

    String[] types;

    public SSyncEntityList(String[] entityTypes) {
        types = entityTypes;
    }

    public static void encode(SSyncEntityList msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.types.length);
        for (String type : msg.types) {
            buf.writeUtf(type);
        }
    }

    public static SSyncEntityList decode(FriendlyByteBuf buf) {
        int j = buf.readInt();
        List<String> entityTypes = new ArrayList<>();
        for (int i = 0; i < j; i++) {
            entityTypes.add(buf.readUtf());
        }
        return new SSyncEntityList(entityTypes.toArray(new String[0]));
    }

    public static void handle(SSyncEntityList msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Main.PROXY.loadEntities(msg.types);
        });
        ctx.get().setPacketHandled(true);
    }

}
