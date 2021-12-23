package flash.npcmod.network.packets.client;

import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.EntityInit;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SOpenScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkEvent;

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

  public static void encode(CHandleNpcEditorRightClick msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.handleType);
    if (msg.handleType == HandleType.ENTITY.ordinal())
      buf.writeInt(msg.entityid);
    else if (msg.handleType == HandleType.BLOCK.ordinal())
      buf.writeBlockPos(msg.pos);
  }

  public static CHandleNpcEditorRightClick decode(FriendlyByteBuf buf) {
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
      ServerPlayer sender = ctx.get().getSender();
      if (sender.hasPermissions(4)) {
        if (msg.handleType == HandleType.AIR.ordinal() && sender.isDiscrete()) {
          // If we right click in the air while sneaking, open the function builder
          PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.FUNCTIONBUILDER, "", 0), sender);
        }
        else if (msg.handleType == HandleType.ENTITY.ordinal()) {
          // If we right click on an entity and it is an NPC, edit it
          Entity entity = sender.level.getEntity(msg.entityid);
          if (entity instanceof NpcEntity) {
            PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.EDITNPC, "", msg.entityid), sender);
          }
        }
        else if (msg.handleType == HandleType.BLOCK.ordinal()) {
          // If we right click on a block, create a new NPC and start editing it
          NpcEntity newNpc = EntityInit.NPC_ENTITY.get().create(sender.level);
          BlockPos pos = msg.pos;
          VoxelShape collisionShape = sender.level.getBlockState(pos).getBlockSupportShape(sender.level, pos);
          double blockHeight = collisionShape.isEmpty() ? 0 : collisionShape.bounds().maxY;
          newNpc.setPos(pos.getX()+0.5, pos.getY()+blockHeight, pos.getZ()+0.5);
          sender.level.addFreshEntity(newNpc);
          PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.EDITNPC, "", newNpc.getId()), sender);
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
