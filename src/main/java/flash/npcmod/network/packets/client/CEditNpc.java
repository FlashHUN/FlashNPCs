package flash.npcmod.network.packets.client;

import flash.npcmod.entity.NpcEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CEditNpc {

  int entityid;
  String name, texture, dialogue;
  boolean isSlim, isNameVisible;
  int textColor;
  ItemStack[] items;
  NPCPose pose;

  public CEditNpc(int entityid, boolean isNameVisible, String name, String texture, boolean isSlim, String dialogue, int textColor, ItemStack[] items, NPCPose pose) {
    this.entityid = entityid;
    this.isNameVisible = isNameVisible;
    this.name = name;
    this.texture = texture;
    this.isSlim = isSlim;
    this.dialogue = dialogue;
    this.textColor = textColor;
    if (items.length == 6) {
      this.items = items;
    }
    this.pose = pose;
  }

  public static void encode(CEditNpc msg, PacketBuffer buf) {
    buf.writeInt(msg.entityid);
    buf.writeBoolean(msg.isNameVisible);
    buf.writeString(msg.name);
    buf.writeString(msg.texture);
    buf.writeBoolean(msg.isSlim);
    buf.writeString(msg.dialogue);
    buf.writeInt(msg.textColor);
    buf.writeInt(msg.pose.ordinal());
    if (msg.items != null) {
      for (int i = 0; i < 6; i++) {
        buf.writeItemStack(msg.items[i]);
      }
    } else {
      for (int i = 0; i < 6; i++) {
        buf.writeItemStack(ItemStack.EMPTY);
      }
    }
  }

  public static CEditNpc decode(PacketBuffer buf) {
    int entityid = buf.readInt();
    boolean isNameVisible = buf.readBoolean();
    String name = buf.readString(201);
    String texture = buf.readString(201);
    boolean isSlim = buf.readBoolean();
    String dialogue = buf.readString(201);
    int textColor = buf.readInt();
    NPCPose pose = NPCPose.values()[buf.readInt()];
    List<ItemStack> items = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      items.add(buf.readItemStack());
    }
    return new CEditNpc(entityid, isNameVisible, name, texture, isSlim, dialogue, textColor, items.toArray(new ItemStack[0]), pose);
  }

  public static void handle(CEditNpc msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();
      if (sender.hasPermissionLevel(4)) {
        Entity entity = sender.world.getEntityByID(msg.entityid);
        if (entity instanceof NpcEntity) {
          NpcEntity npcEntity = (NpcEntity) entity;
          npcEntity.setCustomNameVisible(msg.isNameVisible);
          npcEntity.setCustomName(new StringTextComponent(msg.name));
          npcEntity.setTexture(msg.texture);
          npcEntity.setSlim(msg.isSlim);
          npcEntity.setDialogue(msg.dialogue);
          npcEntity.setTextColor(msg.textColor);
          npcEntity.setItemStackToSlot(EquipmentSlotType.MAINHAND, msg.items[0]);
          npcEntity.setItemStackToSlot(EquipmentSlotType.OFFHAND, msg.items[1]);
          npcEntity.setItemStackToSlot(EquipmentSlotType.HEAD, msg.items[2]);
          npcEntity.setItemStackToSlot(EquipmentSlotType.CHEST, msg.items[3]);
          npcEntity.setItemStackToSlot(EquipmentSlotType.LEGS, msg.items[4]);
          npcEntity.setItemStackToSlot(EquipmentSlotType.FEET, msg.items[5]);

          switch (msg.pose) {
            case CROUCHING: npcEntity.setCrouching(true); npcEntity.setSitting(false); break;
            case SITTING: npcEntity.setCrouching(false); npcEntity.setSitting(true); break;
            case STANDING: npcEntity.setCrouching(false); npcEntity.setSitting(false); break;
          }
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }

  public enum NPCPose {
    STANDING,
    CROUCHING,
    SITTING
  }

}
