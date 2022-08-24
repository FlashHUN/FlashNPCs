package flash.npcmod.network.packets.client;

import flash.npcmod.entity.NpcEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CEditNpc {

  int entityid;
  String name, texture, dialogue, behavior;
  boolean isSlim, isNameVisible, resetAI;
  int textColor;
  ItemStack[] items;
  NPCPose pose;
  String renderer;

  public CEditNpc(int entityid, boolean isNameVisible, String name, String texture, boolean isSlim, String dialogue,
                  String behavior, int textColor, ItemStack[] items, NPCPose pose, String renderer) {
    this(entityid, isNameVisible, name, texture, isSlim, dialogue, behavior, textColor, items, pose, false, renderer);
  }

  public CEditNpc(int entityid, boolean isNameVisible, String name, String texture, boolean isSlim, String dialogue,
                  String behavior, int textColor, ItemStack[] items, NPCPose pose, EntityType<?> renderer) {
    this(entityid, isNameVisible, name, texture, isSlim, dialogue, behavior, textColor, items, pose, false, renderer);
  }

  public CEditNpc(int entityid, boolean isNameVisible, String name, String texture, boolean isSlim, String dialogue,
                  String behavior, int textColor, ItemStack[] items, NPCPose pose, boolean resetAI, EntityType<?> renderer) {
    this(entityid, isNameVisible, name, texture, isSlim, dialogue, behavior, textColor, items, pose, resetAI, EntityType.getKey(renderer).toString());
  }

  public CEditNpc(int entityid, boolean isNameVisible, String name, String texture, boolean isSlim, String dialogue,
                  String behavior, int textColor, ItemStack[] items, NPCPose pose, boolean resetAI, String renderer) {
    this.entityid = entityid;
    this.isNameVisible = isNameVisible;
    this.name = name;
    this.texture = texture;
    this.isSlim = isSlim;
    this.dialogue = dialogue;
    this.behavior = behavior;
    this.textColor = textColor;
    if (items.length == 6) {
      this.items = items;
    }
    this.pose = pose;
    this.resetAI = resetAI;
    this.renderer = renderer;
  }

  public static void encode(CEditNpc msg, FriendlyByteBuf buf) {
    buf.writeInt(msg.entityid);
    buf.writeBoolean(msg.isNameVisible);
    buf.writeUtf(msg.name);
    buf.writeUtf(msg.texture);
    buf.writeBoolean(msg.isSlim);
    buf.writeBoolean(msg.resetAI);
    buf.writeUtf(msg.dialogue);
    buf.writeUtf(msg.behavior);
    buf.writeInt(msg.textColor);
    buf.writeInt(msg.pose.ordinal());
    buf.writeUtf(msg.renderer);
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
    String texture = buf.readUtf(201);
    boolean isSlim = buf.readBoolean();
    boolean resetAI = buf.readBoolean();
    String dialogue = buf.readUtf(201);
    String behavior = buf.readUtf(201);
    int textColor = buf.readInt();
    NPCPose pose = NPCPose.values()[buf.readInt()];
    String renderer = buf.readUtf();
    List<ItemStack> items = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      items.add(buf.readItem());
    }
    return new CEditNpc(
            entityid, isNameVisible, name, texture, isSlim, dialogue, behavior, textColor, items.toArray(new ItemStack[0]),
            pose, resetAI, renderer
    );
  }

  public static void handle(CEditNpc msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      if (sender.hasPermissions(4)) {
        Entity entity = sender.level.getEntity(msg.entityid);
        if (entity instanceof NpcEntity npcEntity) {
          npcEntity.setCustomNameVisible(msg.isNameVisible);
          npcEntity.setCustomName(new TextComponent(msg.name));
          npcEntity.setTexture(msg.texture);
          npcEntity.setSlim(msg.isSlim);
          npcEntity.setDialogue(msg.dialogue);
          npcEntity.setBehaviorFile(msg.behavior);
          npcEntity.setTextColor(msg.textColor);
          npcEntity.setRenderer(msg.renderer);
          npcEntity.setItemSlot(EquipmentSlot.MAINHAND, msg.items[0]);
          npcEntity.setItemSlot(EquipmentSlot.OFFHAND, msg.items[1]);
          npcEntity.setItemSlot(EquipmentSlot.HEAD, msg.items[2]);
          npcEntity.setItemSlot(EquipmentSlot.CHEST, msg.items[3]);
          npcEntity.setItemSlot(EquipmentSlot.LEGS, msg.items[4]);
          npcEntity.setItemSlot(EquipmentSlot.FEET, msg.items[5]);
          npcEntity.addExtraPropertiesToRenderedEntity();

          switch (msg.pose) {
            case CROUCHING -> { npcEntity.setCrouching(true); npcEntity.setSitting(false); }
            case SITTING -> { npcEntity.setCrouching(false); npcEntity.setSitting(true); }
            case STANDING -> { npcEntity.setCrouching(false); npcEntity.setSitting(false); }
          }

          if(msg.resetAI) npcEntity.resetBehavior();
          npcEntity.refreshGoals();
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
