package flash.npcmod.network.packets.client;

import flash.npcmod.core.PermissionHelper;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.EntityInit;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SOpenScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CHandleNpcEditorRightClick {

  int handleType;
  int entityid;
  BlockPos pos;
  String fileName;

  public CHandleNpcEditorRightClick() {
    this.handleType = HandleType.AIR_SNEAK.ordinal();
  }

  public CHandleNpcEditorRightClick(String s) {
    this.handleType = HandleType.AIR.ordinal();
    this.fileName = s;
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
    else if (msg.handleType == HandleType.AIR.ordinal()) {
      buf.writeUtf(msg.fileName);
    }
  }

  public static CHandleNpcEditorRightClick decode(FriendlyByteBuf buf) {
    int handleType = buf.readInt();
    if (handleType == HandleType.ENTITY.ordinal())
      return new CHandleNpcEditorRightClick(buf.readInt());
    else if (handleType == HandleType.BLOCK.ordinal())
      return new CHandleNpcEditorRightClick(buf.readBlockPos());
    else if (handleType == HandleType.AIR.ordinal())
      return new CHandleNpcEditorRightClick(buf.readUtf());
    else
      return new CHandleNpcEditorRightClick();
  }

  public static void handle(CHandleNpcEditorRightClick msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      if (msg.handleType == HandleType.AIR_SNEAK.ordinal() && PermissionHelper.hasPermission(sender, PermissionHelper.EDIT_FUNCTION)) {
        // If we right-click in the air while sneaking, open the function builder
        PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.FUNCTIONBUILDER, "", 0), sender);
      }
      else if (msg.handleType == HandleType.ENTITY.ordinal() && PermissionHelper.hasPermission(sender, PermissionHelper.EDIT_NPC)) {
        // If we right-click on an entity and if it is an NPC, edit it
        Entity entity = sender.level.getEntity(msg.entityid);
        if (entity instanceof NpcEntity) {
          PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.EDITNPC, "", msg.entityid), sender);
        }
      }
      else if (msg.handleType == HandleType.BLOCK.ordinal() && PermissionHelper.hasPermission(sender, PermissionHelper.EDIT_NPC)) {
        // If we right-click on a block, create a new NPC and start editing it
        NpcEntity newNpc = EntityInit.NPC_ENTITY.get().create(sender.level);
        BlockPos pos = msg.pos;
        VoxelShape collisionShape = sender.level.getBlockState(pos).getBlockSupportShape(sender.level, pos);
        double blockHeight = collisionShape.isEmpty() ? 0 : collisionShape.bounds().maxY;
        newNpc.setPos(pos.getX()+0.5, pos.getY()+blockHeight, pos.getZ()+0.5);
        sender.level.addFreshEntity(newNpc);
        PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.EDITNPC, "", newNpc.getId()), sender);
      }
      else if (msg.handleType == HandleType.AIR.ordinal() && PermissionHelper.hasPermission(sender, PermissionHelper.EDIT_DIALOGUE)) {
        PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.EDITDIALOGUE, "", 0), sender);
      }
    });
    ctx.get().setPacketHandled(true);
  }

  public enum HandleType {
    ENTITY,
    AIR_SNEAK,
    AIR,
    BLOCK
  }
}
