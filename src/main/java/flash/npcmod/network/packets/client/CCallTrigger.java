package flash.npcmod.network.packets.client;

import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CCallTrigger {

  String triggerName;
  int entityid;

  public CCallTrigger(String name, int entityid) {
    this.triggerName = name;
    this.entityid = entityid;
  }

  public static void encode(CCallTrigger msg, FriendlyByteBuf buf) {
    buf.writeUtf(msg.triggerName);
    buf.writeInt(msg.entityid);
  }

  public static CCallTrigger decode(FriendlyByteBuf buf) {
    return new CCallTrigger(buf.readUtf(250), buf.readInt());
  }

  public static void handle(CCallTrigger msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      Entity entity  = sender.level.getEntity(msg.entityid);
      if (entity instanceof NpcEntity){
        Main.LOGGER.info("Triggering");
        NpcEntity npcEntity = (NpcEntity) entity;
        npcEntity.trigger(msg.triggerName);
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
