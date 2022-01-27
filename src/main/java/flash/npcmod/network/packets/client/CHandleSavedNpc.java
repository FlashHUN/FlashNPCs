package flash.npcmod.network.packets.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.core.saves.NpcSaveUtil;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraftforge.fml.network.NetworkEvent;
import org.json.JSONObject;

import java.util.function.Supplier;

public class CHandleSavedNpc {

  PacketType type;
  String newName;
  String prevName;
  JSONObject toPlace;
  BlockPos pos;

  public CHandleSavedNpc(String prevName, String newName) {
    type = PacketType.RENAME;
    this.prevName = prevName;
    this.newName = newName;
  }

  public CHandleSavedNpc(JSONObject toPlace, BlockPos pos) {
    type = PacketType.PLACE;
    this.toPlace = toPlace;
    this.pos = pos;
  }

  public CHandleSavedNpc(String toDelete) {
    type = PacketType.DELETE;
    this.prevName = toDelete;
  }

  public static void encode(CHandleSavedNpc msg, PacketBuffer buf) {
    buf.writeInt(msg.type.ordinal());
    switch (msg.type) {
      case PLACE: buf.writeString(msg.toPlace.toString()); buf.writeBlockPos(msg.pos); break;
      case RENAME: buf.writeString(msg.prevName); buf.writeString(msg.newName); break;
      case DELETE: buf.writeString(msg.prevName); break;
    }
  }

  public static CHandleSavedNpc decode(PacketBuffer buf) {
    PacketType type = PacketType.values()[buf.readInt()];
    if (type == PacketType.PLACE) {
      return new CHandleSavedNpc(new JSONObject(buf.readString()), buf.readBlockPos());
    }
    else if (type == PacketType.DELETE) {
      return new CHandleSavedNpc(buf.readString());
    }
    else {
      return new CHandleSavedNpc(buf.readString(), buf.readString());
    }
  }

  public static void handle(CHandleSavedNpc msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();
      if (sender.hasPermissionLevel(4) && sender.isCreative()) {
        switch (msg.type) {
          case PLACE:
            NpcEntity npcEntity = NpcEntity.fromJson(sender.world, msg.toPlace);
            BlockPos pos = msg.pos;
            VoxelShape collisionShape = sender.world.getBlockState(pos).getCollisionShape(sender.world, pos);
            double blockHeight = collisionShape.isEmpty() ? 0 : collisionShape.getBoundingBox().maxY;
            npcEntity.setPosition(pos.getX()+0.5, pos.getY()+blockHeight, pos.getZ()+0.5);
            sender.world.addEntity(npcEntity);
            break;
          case RENAME: NpcSaveUtil.rename(sender.getCachedUniqueIdString(), msg.prevName, msg.newName); break;
          case DELETE: NpcSaveUtil.delete(sender, msg.prevName); break;
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }

  enum PacketType {
    RENAME,
    PLACE,
    DELETE
  }
}
