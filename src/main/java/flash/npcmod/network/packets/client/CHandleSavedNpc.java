package flash.npcmod.network.packets.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.core.PermissionHelper;
import flash.npcmod.core.saves.NpcSaveUtil;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CHandleSavedNpc {

  PacketType type;
  String newName;
  String prevName;
  JsonObject toPlace;
  BlockPos pos;

  public CHandleSavedNpc(String prevName, String newName) {
    type = PacketType.RENAME;
    this.prevName = prevName;
    this.newName = newName;
  }

  public CHandleSavedNpc(JsonObject toPlace, BlockPos pos) {
    type = PacketType.PLACE;
    this.toPlace = toPlace;
    this.pos = pos;
  }

  public CHandleSavedNpc(String toDelete) {
    type = PacketType.DELETE;
    this.prevName = toDelete;
  }

  public static void encode(CHandleSavedNpc msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.type.ordinal());
    switch (msg.type) {
      case PLACE -> { buf.writeUtf(msg.toPlace.toString()); buf.writeBlockPos(msg.pos); }
      case RENAME -> { buf.writeUtf(msg.prevName); buf.writeUtf(msg.newName); }
      case DELETE -> buf.writeUtf(msg.prevName);
    }
  }

  public static CHandleSavedNpc decode(FriendlyByteBuf buf) {
    PacketType type = PacketType.values()[buf.readInt()];
    if (type == PacketType.PLACE) {
      return new CHandleSavedNpc(new Gson().fromJson(buf.readUtf(), JsonObject.class), buf.readBlockPos());
    }
    else if (type == PacketType.DELETE) {
      return new CHandleSavedNpc(buf.readUtf());
    }
    else {
      return new CHandleSavedNpc(buf.readUtf(), buf.readUtf());
    }
  }

  public static void handle(CHandleSavedNpc msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      if (PermissionHelper.hasPermission(sender, PermissionHelper.SAVE_NPC) && sender.isCreative()) {
        switch (msg.type) {
          case PLACE -> {
            NpcEntity npcEntity = NpcEntity.fromJson(sender.level, msg.toPlace);
            BlockPos pos = msg.pos;
            VoxelShape collisionShape = sender.level.getBlockState(pos).getBlockSupportShape(sender.level, pos);
            double blockHeight = collisionShape.isEmpty() ? 0 : collisionShape.bounds().maxY;
            npcEntity.setPos(pos.getX()+0.5, pos.getY()+blockHeight, pos.getZ()+0.5);
            sender.level.addFreshEntity(npcEntity);
          }
          case RENAME -> NpcSaveUtil.rename(sender.getStringUUID(), msg.prevName, msg.newName);
          case DELETE -> NpcSaveUtil.delete(sender, msg.prevName);
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
