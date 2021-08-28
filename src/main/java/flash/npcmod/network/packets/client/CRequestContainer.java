package flash.npcmod.network.packets.client;

import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.inventory.container.*;
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
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class CRequestContainer {

  int entityid;
  ContainerType containerType;
  String name;

  public CRequestContainer(int entityid, ContainerType type) {
    this.entityid = entityid;
    this.containerType = type;
  }

  public CRequestContainer(String name, ContainerType type) {
    this.name = name;
    this.entityid = -1000;
    this.containerType = type;
  }

  public static void encode(CRequestContainer msg, PacketBuffer buf) {
    buf.writeBoolean(msg.entityid == -1000);
    if (msg.entityid != -1000)
      buf.writeInt(msg.entityid);
    else
      buf.writeString(msg.name, 100000);
    buf.writeInt(msg.containerType.ordinal());
  }

  public static CRequestContainer decode(PacketBuffer buf) {
    boolean isNamePacket = buf.readBoolean();
    if (isNamePacket) {
      String data = buf.readString(100000);
      return new CRequestContainer(data, ContainerType.values()[buf.readInt()]);
    } else
      return new CRequestContainer(buf.readInt(), ContainerType.values()[buf.readInt()]);
  }

  public static void handle(CRequestContainer msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayerEntity sender = ctx.get().getSender();
      if (sender.hasPermissionLevel(msg.containerType.getMinPermissionLevel())) {
        if (msg.entityid != -1000) {
          Entity entity = sender.world.getEntityByID(msg.entityid);
          if (entity instanceof NpcEntity) {
            NpcEntity npcEntity = (NpcEntity) entity;

            ContainerType.setEntityId(msg.entityid);
            ContainerType.setName(npcEntity.getName().getString());

            NetworkHooks.openGui(sender, ContainerType.getContainerProvider(msg.containerType), packetBuffer -> packetBuffer.writeInt(msg.entityid));
          }
        } else if (msg.containerType.equals(ContainerType.OBJECTIVE_STACK_SELECTOR)) {
          ContainerType.setName(msg.name);

          String[] split = msg.name.split("::::::::::");

          NetworkHooks.openGui(sender, ContainerType.getContainerProvider(msg.containerType), packetBuffer -> {
            packetBuffer.writeString(split[0], 100000);
            packetBuffer.writeString(split[1], 100000);
            packetBuffer.writeString(split.length == 2 ? "" : split[2], 400);
          });
        } else if (msg.containerType.equals(ContainerType.QUEST_STACK_SELECTOR)) {
          ContainerType.setName(msg.name);

          NetworkHooks.openGui(sender, ContainerType.getContainerProvider(msg.containerType), packetBuffer -> packetBuffer.writeString(msg.name, 100000));
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }

  public enum ContainerType {
    NPCINVENTORY(4) {
      @Override
      public Container createMenu(int index, PlayerInventory playerInventory, PlayerEntity player) {
        return new NpcInventoryContainer(index, playerInventory, entityid);
      }
    },
    TRADES(0) {
      @Override
      public Container createMenu(int index, PlayerInventory playerInventory, PlayerEntity player) {
        return new NpcTradeContainer(index, playerInventory, entityid);
      }
    },
    TRADE_EDITOR(4) {
      @Override
      public Container createMenu(int index, PlayerInventory playerInventory, PlayerEntity player) {
        return new NpcTradeEditorContainer(index, playerInventory, entityid);
      }
    },
    OBJECTIVE_STACK_SELECTOR(4) {
      @Override
      public Container createMenu(int index, PlayerInventory playerInventory, PlayerEntity player) {
        return new ObjectiveStackSelectorContainer(index, playerInventory, objectiveFromString(name), questFromString(name), name.split("::::::::::").length == 2 ? "" : name.split("::::::::::")[2]);
      }

      private QuestObjective objectiveFromString(String name) {
        JSONObject jsonObject = new JSONObject(name.split("::::::::::")[0]);
        return QuestObjective.fromJson(jsonObject);
      }

      private Quest questFromString(String name) {
        JSONObject jsonObject = new JSONObject(name.split("::::::::::")[1]);
        return Quest.fromJson(jsonObject);
      }
    },
    QUEST_STACK_SELECTOR(4) {
      @Override
      public Container createMenu(int index, PlayerInventory playerInventory, PlayerEntity player) {
        return new QuestStackSelectorContainer(index, playerInventory, questFromString(name));
      }

      private Quest questFromString(String name) {
        JSONObject jsonObject = new JSONObject(name);
        return Quest.fromJson(jsonObject);
      }
    };

    private static String name;
    private static int entityid;
    private int minPermissionLevel;

    ContainerType(int minPermissionLevel) {
      this.minPermissionLevel = minPermissionLevel;
    }

    public int getMinPermissionLevel() {
      return minPermissionLevel;
    }

    @Nullable
    public Container createMenu(int index, PlayerInventory playerInventory, PlayerEntity player) {
      return null;
    }

    public static void setName(String newName) {
      name = newName;
    }

    public static void setEntityId(int newId) {
      entityid = newId;
    }

    public static INamedContainerProvider getContainerProvider(ContainerType type) {
      INamedContainerProvider containerProvider = new INamedContainerProvider() {
        @Override
        public ITextComponent getDisplayName() {
          return new StringTextComponent(name);
        }

        @Nullable
        @Override
        public Container createMenu(int index, PlayerInventory playerInventory, PlayerEntity player) {
          return type.createMenu(index, playerInventory, player);
        }
      };
      return containerProvider;
    }
  }
}
