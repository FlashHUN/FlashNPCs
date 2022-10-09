package flash.npcmod.network.packets.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.core.PermissionHelper;
import flash.npcmod.core.saves.NpcSaveUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
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
  boolean isGlobal;
  JsonObject npcJson;
  BlockPos pos;

  public CHandleSavedNpc(String prevName, String newName, boolean isGlobal) {
    type = PacketType.RENAME;
    this.prevName = prevName;
    this.newName = newName;
    this.isGlobal = isGlobal;
  }

  public CHandleSavedNpc(JsonObject npcJson, BlockPos pos) {
    type = PacketType.PLACE;
    this.npcJson = npcJson;
    this.pos = pos;
  }

  public CHandleSavedNpc(JsonObject npcJson) {
    type = PacketType.GLOBAL_SAVE;
    this.npcJson = npcJson;
  }

  public CHandleSavedNpc(String toDelete, boolean isGlobal) {
    type = PacketType.DELETE;
    this.prevName = toDelete;
    this.isGlobal = isGlobal;
  }

  public static void encode(CHandleSavedNpc msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.type.ordinal());
    switch (msg.type) {
      case PLACE -> { buf.writeUtf(msg.npcJson.toString()); buf.writeBlockPos(msg.pos); }
      case RENAME -> { buf.writeUtf(msg.prevName); buf.writeUtf(msg.newName); buf.writeBoolean(msg.isGlobal); }
      case DELETE -> { buf.writeUtf(msg.prevName); buf.writeBoolean(msg.isGlobal); }
      case GLOBAL_SAVE -> buf.writeUtf(msg.npcJson.toString());
    }
  }

  public static CHandleSavedNpc decode(FriendlyByteBuf buf) {
    PacketType type = PacketType.values()[buf.readInt()];
    return switch (type) {
      case PLACE -> new CHandleSavedNpc(new Gson().fromJson(buf.readUtf(), JsonObject.class), buf.readBlockPos());
      case RENAME -> new CHandleSavedNpc(buf.readUtf(), buf.readUtf(), buf.readBoolean());
      case DELETE -> new CHandleSavedNpc(buf.readUtf(), buf.readBoolean());
      case GLOBAL_SAVE -> new CHandleSavedNpc(new Gson().fromJson(buf.readUtf(), JsonObject.class));
    };
  }

  public static void handle(CHandleSavedNpc msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      if (PermissionHelper.hasPermission(sender, PermissionHelper.SAVE_NPC) && sender.isCreative()) {
        switch (msg.type) {
          case PLACE -> {
            NpcEntity npcEntity = NpcEntity.fromJson(sender.level, msg.npcJson);
            BlockPos pos = msg.pos;
            VoxelShape collisionShape = sender.level.getBlockState(pos).getBlockSupportShape(sender.level, pos);
            double blockHeight = collisionShape.isEmpty() ? 0 : collisionShape.bounds().maxY;
            npcEntity.setPos(pos.getX()+0.5, pos.getY()+blockHeight, pos.getZ()+0.5);
            sender.level.addFreshEntity(npcEntity);
          }
          case RENAME -> NpcSaveUtil.rename(sender.getStringUUID(), msg.prevName, msg.newName, msg.isGlobal);
          case DELETE -> NpcSaveUtil.delete(sender, msg.prevName, msg.isGlobal);
          case GLOBAL_SAVE -> NpcSaveUtil.buildGlobal(msg.npcJson);
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }

  enum PacketType {
    RENAME,
    PLACE,
    DELETE,
    GLOBAL_SAVE
  }
}
