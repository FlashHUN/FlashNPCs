package flash.npcmod.network.packets.server;

import flash.npcmod.Main;
import flash.npcmod.core.quests.QuestInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SAcceptQuest {

  String name;
  int entityid;
  UUID uuid;
  QuestInstance.TurnInType turnInType;

  public SAcceptQuest(String name, int entityid, QuestInstance.TurnInType turnInType, UUID uuid) {
    this.name = name;
    this.entityid = entityid;
    this.turnInType = turnInType;
    this.uuid = uuid;
  }

  public static void encode(SAcceptQuest msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.name);
    buf.writeInt(msg.entityid);
    buf.writeInt(msg.turnInType.ordinal());
    buf.writeUUID(msg.uuid);
  }

  public static SAcceptQuest decode(FriendlyByteBuf buf) {
    return new SAcceptQuest(buf.readUtf(51), buf.readInt(), QuestInstance.TurnInType.values()[buf.readInt()], buf.readUUID());
  }

  public static void handle(SAcceptQuest msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      Main.PROXY.acceptQuest(msg.name, msg.entityid, msg.turnInType, msg.uuid);
    });
    ctx.get().setPacketHandled(true);
  }
}
