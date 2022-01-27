package flash.npcmod.network.packets.client;

import flash.npcmod.core.saves.NpcSaveUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.EntityInit;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SOpenScreen;
import flash.npcmod.network.packets.server.SSyncSavedNpcs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CHandleNpcSaveToolRightClick {

  int handleType;
  int entityid;
  BlockPos pos;

  public CHandleNpcSaveToolRightClick() {
    this.handleType = HandleType.AIR.ordinal();
  }

  public CHandleNpcSaveToolRightClick(int entityid) {
    this.handleType = HandleType.ENTITY.ordinal();
    this.entityid = entityid;
  }

  public CHandleNpcSaveToolRightClick(BlockPos pos) {
    this.handleType = HandleType.BLOCK.ordinal();
    this.pos = pos;
  }

  public static void encode(CHandleNpcSaveToolRightClick msg, PacketBuffer buf) {
    buf.writeInt(msg.handleType);
    if (msg.handleType == HandleType.ENTITY.ordinal())
      buf.writeInt(msg.entityid);
    else if (msg.handleType == HandleType.BLOCK.ordinal())
      buf.writeBlockPos(msg.pos);
  }

  public static CHandleNpcSaveToolRightClick decode(PacketBuffer buf) {
    int handleType = buf.readInt();
    if (handleType == HandleType.ENTITY.ordinal())
      return new CHandleNpcSaveToolRightClick(buf.readInt());
    else if (handleType == HandleType.BLOCK.ordinal())
      return new CHandleNpcSaveToolRightClick(buf.readBlockPos());
    else
      return new CHandleNpcSaveToolRightClick();
  }

  public static void handle(CHandleNpcSaveToolRightClick msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();
      if (sender.hasPermissionLevel(4)) {
        if (sender.isDiscrete()) {
          if (msg.handleType == CHandleNpcEditorRightClick.HandleType.ENTITY.ordinal()) {
            // If we right click on an entity and it is an NPC, save it
            Entity entity = sender.world.getEntityByID(msg.entityid);
            if (entity instanceof NpcEntity) {
              NpcEntity npcEntity = (NpcEntity) entity;
              NpcSaveUtil.BuildResult result = NpcSaveUtil.build(sender.getCachedUniqueIdString(), npcEntity.toJson().toString());
              String s = "";
              TextFormatting color = TextFormatting.WHITE;
              switch (result) {
                case SUCCESS: s = "Successfully Saved " + npcEntity.getName().getString(); color = TextFormatting.GREEN; break;
                case TOOMANY: s = "You can only have " + NpcSaveUtil.MAX_SAVED_NPCS + " NPCs saved!"; color = TextFormatting.RED; break;
                case FAILED: s = "Failed to save " + npcEntity.getName().getString(); color = TextFormatting.RED; break;
                case EXISTS: s = "You already have an NPC with this name saved. Please rename it!"; color = TextFormatting.RED; break;
              }
              sender.sendStatusMessage(new StringTextComponent(s).setStyle(Style.EMPTY.applyFormatting(color)), true);
            }
          }
          else if (msg.handleType == CHandleNpcEditorRightClick.HandleType.BLOCK.ordinal()) {
            // If we right click on a block, open the saved npcs gui
            PacketDispatcher.sendTo(new SSyncSavedNpcs(NpcSaveUtil.load(sender.getCachedUniqueIdString())), sender);
            PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.SAVEDNPCS, msg.pos.getX()+";"+msg.pos.getY()+";"+msg.pos.getZ(), msg.entityid), sender);
          }
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
