package flash.npcmod.network.packets.client;

import flash.npcmod.entity.NpcEntity;
import flash.npcmod.inventory.container.NpcInventoryContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class CRequestContainer {

  int entityid;

  public CRequestContainer(int entityid) {
    this.entityid = entityid;
  }

  public static void encode(CRequestContainer msg, PacketBuffer buf) {
    buf.writeInt(msg.entityid);
  }

  public static CRequestContainer decode(PacketBuffer buf) {
    return new CRequestContainer(buf.readInt());
  }

  public static void handle(CRequestContainer msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();
      if (sender.hasPermissionLevel(4)) {
        Entity entity = sender.world.getEntityByID(msg.entityid);
        if (entity instanceof NpcEntity) {
          NpcEntity npcEntity = (NpcEntity)entity;
          INamedContainerProvider containerProvider = new INamedContainerProvider() {
            @Override
            public ITextComponent getDisplayName() {
              return new StringTextComponent(npcEntity.getName().getString());
            }

            @Nullable
            @Override
            public Container createMenu(int index, PlayerInventory playerInventory, PlayerEntity player) {
              return new NpcInventoryContainer(index, playerInventory, msg.entityid);
            }
          };

          NetworkHooks.openGui(sender, containerProvider, packetBuffer -> packetBuffer.writeInt(msg.entityid));
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }
}
