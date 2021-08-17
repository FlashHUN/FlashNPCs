package flash.npcmod.network.packets.client;

import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.EntityInit;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SOpenScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CHandleNpcEditorRightClick {

  int handleType;
  int entityid;
  BlockPos pos;

  public CHandleNpcEditorRightClick() {
    this.handleType = HandleType.AIR.ordinal();
  }

  public CHandleNpcEditorRightClick(int entityid) {
    this.handleType = HandleType.ENTITY.ordinal();
    this.entityid = entityid;
  }

  public CHandleNpcEditorRightClick(BlockPos pos) {
    this.handleType = HandleType.BLOCK.ordinal();
    this.pos = pos;
  }

  public static void encode(CHandleNpcEditorRightClick msg, PacketBuffer buf) {
    buf.writeInt(msg.handleType);
    if (msg.handleType == HandleType.ENTITY.ordinal())
      buf.writeInt(msg.entityid);
    else if (msg.handleType == HandleType.BLOCK.ordinal())
      buf.writeBlockPos(msg.pos);
  }

  public static CHandleNpcEditorRightClick decode(PacketBuffer buf) {
    int handleType = buf.readInt();
    if (handleType == HandleType.ENTITY.ordinal())
      return new CHandleNpcEditorRightClick(buf.readInt());
    else if (handleType == HandleType.BLOCK.ordinal())
      return new CHandleNpcEditorRightClick(buf.readBlockPos());
    else
      return new CHandleNpcEditorRightClick();
  }

  public static void handle(CHandleNpcEditorRightClick msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();
      if (sender.hasPermissionLevel(4)) {
        if (msg.handleType == HandleType.AIR.ordinal() && sender.isDiscrete()) {
          // If we right click in the air while sneaking, open the function builder
          PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.FUNCTIONBUILDER, "", 0), sender);
        }
        else if (msg.handleType == HandleType.ENTITY.ordinal()) {
          // If we right click on an entity and it is an NPC, edit it
          Entity entity = sender.world.getEntityByID(msg.entityid);
          if (entity instanceof NpcEntity) {
            PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.EDITNPC, "", msg.entityid), sender);
          }
        }
        else if (msg.handleType == HandleType.BLOCK.ordinal()) {
          // If we right click on a block, create a new NPC and start editing it
          NpcEntity newNpc = EntityInit.NPC_ENTITY.get().create(sender.world);
          BlockPos pos = msg.pos;
          newNpc.setPosition(pos.getX()+0.5, pos.getY()+1.5, pos.getZ()+0.5);
          // TODO entity property for default position, so the npc can teleport back every X ticks, in case it was moved
          sender.world.addEntity(newNpc);
          PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.EDITNPC, "", newNpc.getEntityId()), sender);
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }

  public enum HandleType {
    ENTITY,
    AIR,
    BLOCK
  }
}
