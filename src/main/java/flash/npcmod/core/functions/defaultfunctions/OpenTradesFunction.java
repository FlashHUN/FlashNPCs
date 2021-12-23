package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CRequestContainer;
import flash.npcmod.network.packets.server.SSyncTrades;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;

public class OpenTradesFunction extends AbstractFunction {

  public OpenTradesFunction() {
    super("openTrades", empty, empty);
  }

  @Override
  public void call(String[] params, ServerPlayer sender, NpcEntity npcEntity) {
    PacketDispatcher.sendTo(new SSyncTrades(npcEntity), sender);

    CRequestContainer.ContainerType.setEntityId(npcEntity.getId());
    CRequestContainer.ContainerType.setName(npcEntity.getName().getString());

    NetworkHooks.openGui(sender, CRequestContainer.ContainerType.getContainerProvider(CRequestContainer.ContainerType.TRADES), packetBuffer -> packetBuffer.writeInt(npcEntity.getId()));
    debugUsage(sender, npcEntity);
  }
}
