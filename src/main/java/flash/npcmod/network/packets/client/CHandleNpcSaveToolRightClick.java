package flash.npcmod.network.packets.client;

import flash.npcmod.core.saves.NpcSaveUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CHandleNpcEditorRightClick.HandleType;
import flash.npcmod.network.packets.server.SOpenScreen;
import flash.npcmod.network.packets.server.SSyncSavedNpcs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

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

  public static void encode(CHandleNpcSaveToolRightClick msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.handleType);
    if (msg.handleType == HandleType.ENTITY.ordinal())
      buf.writeInt(msg.entityid);
    else if (msg.handleType == HandleType.BLOCK.ordinal())
      buf.writeBlockPos(msg.pos);
  }

  public static CHandleNpcSaveToolRightClick decode(FriendlyByteBuf buf) {
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
      ServerPlayer sender = ctx.get().getSender();
      if (sender.hasPermissions(4)) {
        if (sender.isDiscrete()) {
          if (msg.handleType == HandleType.ENTITY.ordinal()) {
            // If we right click on an entity and it is an NPC, save it
            Entity entity = sender.level.getEntity(msg.entityid);
            if (entity instanceof NpcEntity npcEntity) {
              NpcSaveUtil.BuildResult result = NpcSaveUtil.build(sender.getStringUUID(), npcEntity.toJson().toString());
              String s = "";
              ChatFormatting color = ChatFormatting.WHITE;
              switch (result) {
                case SUCCESS -> { s = "Successfully Saved " + npcEntity.getName().getString(); color = ChatFormatting.GREEN; }
                case TOOMANY -> { s = "You can only have " + NpcSaveUtil.MAX_SAVED_NPCS + " NPCs saved!"; color = ChatFormatting.RED; }
                case FAILED -> { s = "Failed to save " + npcEntity.getName().getString(); color = ChatFormatting.RED; }
                case EXISTS -> { s = "You already have an NPC with this name saved. Please rename it!"; color = ChatFormatting.RED; }
              }
              sender.displayClientMessage(new TextComponent(s).setStyle(Style.EMPTY.applyFormat(color)), true);
            }
          }
          else if (msg.handleType == HandleType.BLOCK.ordinal()) {
            // If we right click on a block, open the saved npcs gui
            PacketDispatcher.sendTo(new SSyncSavedNpcs(NpcSaveUtil.loadGlobal(), true), sender);
            PacketDispatcher.sendTo(new SSyncSavedNpcs(NpcSaveUtil.load(sender.getStringUUID()), false), sender);
            PacketDispatcher.sendTo(new SOpenScreen(SOpenScreen.EScreens.SAVEDNPCS, msg.pos.getX()+";"+msg.pos.getY()+";"+msg.pos.getZ(), msg.entityid), sender);
          }
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
