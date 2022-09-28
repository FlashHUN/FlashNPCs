package flash.npcmod.network.packets.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.inventory.container.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

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

  public static void encode(CRequestContainer msg, FriendlyByteBuf buf) {
    buf.writeBoolean(msg.entityid == -1000);
    if (msg.entityid != -1000)
      buf.writeInt(msg.entityid);
    else
      buf.writeUtf(msg.name, 100000);
    buf.writeInt(msg.containerType.ordinal());
  }

  public static CRequestContainer decode(FriendlyByteBuf buf) {
    boolean isNamePacket = buf.readBoolean();
    if (isNamePacket) {
      String data = buf.readUtf(100000);
      return new CRequestContainer(data, ContainerType.values()[buf.readInt()]);
    } else
      return new CRequestContainer(buf.readInt(), ContainerType.values()[buf.readInt()]);
  }

  public static void handle(CRequestContainer msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
      ServerPlayer sender = ctx.get().getSender();
      if (sender.hasPermissions(msg.containerType.getMinPermissionLevel())) {
        if (msg.entityid != -1000) {
          Entity entity = sender.level.getEntity(msg.entityid);
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
            packetBuffer.writeUtf(split[0], 100000);
            packetBuffer.writeUtf(split[1], 100000);
            packetBuffer.writeUtf(split.length == 2 ? "" : split[2], 400);
          });
        } else if (msg.containerType.equals(ContainerType.QUEST_STACK_SELECTOR)) {
          ContainerType.setName(msg.name);

          NetworkHooks.openGui(sender, ContainerType.getContainerProvider(msg.containerType), packetBuffer -> packetBuffer.writeUtf(msg.name, 100000));
        }
      }
    });
    ctx.get().setPacketHandled(true);
  }

  public enum ContainerType {
    NPCINVENTORY(4) {
      @Override
      public AbstractContainerMenu createMenu(int index, Inventory playerInventory, Player player) {
        return new NpcInventoryContainer(index, playerInventory, entityid);
      }
    },
    TRADES(0) {
      @Override
      public AbstractContainerMenu createMenu(int index, Inventory playerInventory, Player player) {
        return new NpcTradeContainer(index, playerInventory, entityid);
      }
    },
    TRADE_EDITOR(4) {
      @Override
      public AbstractContainerMenu createMenu(int index, Inventory playerInventory, Player player) {
        return new NpcTradeEditorContainer(index, playerInventory, entityid);
      }
    },
    OBJECTIVE_STACK_SELECTOR(4) {
      @Override
      public AbstractContainerMenu createMenu(int index, Inventory playerInventory, Player player) {
        return new ObjectiveStackSelectorContainer(index, playerInventory, objectiveFromString(name), questFromString(name), name.split("::::::::::").length == 2 ? "" : name.split("::::::::::")[2]);
      }

      private QuestObjective objectiveFromString(String name) {
        JsonObject jsonObject = new Gson().fromJson(name.split("::::::::::")[0], JsonObject.class);
        return QuestObjective.fromJson(jsonObject);
      }

      private Quest questFromString(String name) {
        JsonObject jsonObject = new Gson().fromJson(name.split("::::::::::")[1], JsonObject.class);
        return Quest.fromJson(jsonObject);
      }
    },
    QUEST_STACK_SELECTOR(4) {
      @Override
      public AbstractContainerMenu createMenu(int index, Inventory playerInventory, Player player) {
        return new QuestStackSelectorContainer(index, playerInventory, questFromString(name));
      }

      private Quest questFromString(String name) {
        JsonObject jsonObject = new Gson().fromJson(name, JsonObject.class);
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
    public AbstractContainerMenu createMenu(int index, Inventory playerInventory, Player player) {
      return null;
    }

    public static void setName(String newName) {
      name = newName;
    }

    public static void setEntityId(int newId) {
      entityid = newId;
    }

    public static MenuProvider getContainerProvider(ContainerType type) {
      MenuProvider containerProvider = new MenuProvider() {
        @Override
        public Component getDisplayName() {
          return new TextComponent(name);
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int index, Inventory playerInventory, Player player) {
          return type.createMenu(index, playerInventory, player);
        }
      };
      return containerProvider;
    }
  }
}
