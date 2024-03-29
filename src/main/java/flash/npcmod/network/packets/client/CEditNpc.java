package flash.npcmod.network.packets.client;

import flash.npcmod.core.PermissionHelper;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CEditNpc {

  int entityid;
  String name, title, texture, dialogue;
  boolean isSlim, isNameVisible, isTextureResourceLocation;
  int textColor;
  ItemStack[] items;
  NPCPose pose;
  String renderer;
  CompoundTag rendererTag;
  float scaleX, scaleY, scaleZ;
  boolean collision;

  public CEditNpc(int entityid, boolean isNameVisible, String name, String title, String texture, boolean isTextureResourceLocation, boolean isSlim,
                  String dialogue, int textColor, ItemStack[] items, NPCPose pose,
                  EntityType<?> renderer, CompoundTag rendererTag, float scaleX, float scaleY, float scaleZ, boolean collision) {
    this(entityid, isNameVisible, name, title, texture, isTextureResourceLocation, isSlim,
            dialogue, textColor, items, pose, EntityType.getKey(renderer).toString(),
            rendererTag, scaleX, scaleY, scaleZ, collision);
  }

  public CEditNpc(int entityid, boolean isNameVisible, String name, String title, String texture, boolean isTextureResourceLocation, boolean isSlim,
                  String dialogue, int textColor, ItemStack[] items, NPCPose pose, String renderer,
                  CompoundTag rendererTag, float scaleX, float scaleY, float scaleZ, boolean collision) {
    this.entityid = entityid;
    this.isNameVisible = isNameVisible;
    this.name = name;
    this.title = title;
    this.texture = texture;
    this.isTextureResourceLocation = isTextureResourceLocation;
    this.isSlim = isSlim;
    this.dialogue = dialogue;
    this.textColor = textColor;
    if (items.length == 6) {
      this.items = items;
    }
    this.pose = pose;
    this.renderer = renderer;
    this.rendererTag = rendererTag;
    this.rendererTag.putString("id", renderer);
    this.scaleX = scaleX;
    this.scaleY = scaleY;
    this.scaleZ = scaleZ;
    this.collision = collision;
  }

  public static void encode(CEditNpc msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.entityid);
    buf.writeBoolean(msg.isNameVisible);
    buf.writeUtf(msg.name);
    buf.writeUtf(msg.title);
    buf.writeUtf(msg.texture);
    buf.writeBoolean(msg.isTextureResourceLocation);
    buf.writeBoolean(msg.isSlim);
    buf.writeUtf(msg.dialogue);
    buf.writeInt(msg.textColor);
    buf.writeInt(msg.pose.ordinal());
    buf.writeUtf(msg.renderer);
    buf.writeNbt(msg.rendererTag);
    buf.writeFloat(msg.scaleX);
    buf.writeFloat(msg.scaleY);
    buf.writeFloat(msg.scaleZ);
    buf.writeBoolean(msg.collision);
    if (msg.items != null) {
      for (int i = 0; i < 6; i++) {
        buf.writeItem(msg.items[i]);
      }
    } else {
      for (int i = 0; i < 6; i++) {
        buf.writeItem(ItemStack.EMPTY);
      }
    }
  }

  public static CEditNpc decode(FriendlyByteBuf buf) {
    int entityid = buf.readInt();
    boolean isNameVisible = buf.readBoolean();
    String name = buf.readUtf(201);
    String title = buf.readUtf(201);
    String texture = buf.readUtf(201);
    boolean isTextureResourceLocation = buf.readBoolean();
    boolean isSlim = buf.readBoolean();
    String dialogue = buf.readUtf(201);
    int textColor = buf.readInt();
    NPCPose pose = NPCPose.values()[buf.readInt()];
    String renderer = buf.readUtf();
    CompoundTag rendererTag = buf.readAnySizeNbt();
    float scaleX = buf.readFloat();
    float scaleY = buf.readFloat();
    float scaleZ = buf.readFloat();
    boolean collision = buf.readBoolean();
    List<ItemStack> items = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      items.add(buf.readItem());
    }
    return new CEditNpc(
            entityid, isNameVisible, name, title, texture, isTextureResourceLocation,
            isSlim, dialogue, textColor, items.toArray(new ItemStack[0]),
            pose, renderer, rendererTag, scaleX, scaleY, scaleZ, collision
    );
  }

  public static void handle(CEditNpc msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      if (PermissionHelper.hasPermission(sender, PermissionHelper.EDIT_NPC)) {
        Entity entity = sender.level.getEntity(msg.entityid);
        if (entity instanceof NpcEntity npcEntity) {
          npcEntity.setCustomNameVisible(msg.isNameVisible);
          npcEntity.setCustomName(new TextComponent(msg.name));
          npcEntity.setTitle(msg.title);
          npcEntity.setTexture(msg.texture);
          npcEntity.setIsTextureResourceLocation(msg.isTextureResourceLocation);
          npcEntity.setSlim(msg.isSlim);
          npcEntity.setDialogue(msg.dialogue);
          npcEntity.setTextColor(msg.textColor);
          npcEntity.setRenderedEntityFromTag(msg.rendererTag);
          npcEntity.setItemSlot(EquipmentSlot.MAINHAND, msg.items[0]);
          npcEntity.setItemSlot(EquipmentSlot.OFFHAND, msg.items[1]);
          npcEntity.setItemSlot(EquipmentSlot.HEAD, msg.items[2]);
          npcEntity.setItemSlot(EquipmentSlot.CHEST, msg.items[3]);
          npcEntity.setItemSlot(EquipmentSlot.LEGS, msg.items[4]);
          npcEntity.setItemSlot(EquipmentSlot.FEET, msg.items[5]);
          npcEntity.setRenderedEntityItems();
          if (msg.scaleX >= 0.1f && msg.scaleY >= 0.1f && msg.scaleZ >= 0.1f)
            npcEntity.setScale(msg.scaleX, msg.scaleY, msg.scaleZ);
          npcEntity.setCollision(msg.collision);

          switch (msg.pose) {
            case CROUCHING -> { npcEntity.setCrouching(true); npcEntity.setSitting(false); }
            case SITTING -> { npcEntity.setCrouching(false); npcEntity.setSitting(true); }
            case STANDING -> { npcEntity.setCrouching(false); npcEntity.setSitting(false); }
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
