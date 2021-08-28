package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CRequestContainer;
import flash.npcmod.network.packets.server.SSyncTrades;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.NetworkHooks;

public class OpenTradesFunction extends AbstractFunction {

  public OpenTradesFunction() {
    super("openTrades", empty, empty);
  }

  @Override
  public void call(String[] params, ServerPlayerEntity sender, NpcEntity npcEntity) {
    PacketDispatcher.sendTo(new SSyncTrades(npcEntity), sender);

    CRequestContainer.ContainerType.setEntityId(npcEntity.getEntityId());
    CRequestContainer.ContainerType.setName(npcEntity.getName().getString());

    NetworkHooks.openGui(sender, CRequestContainer.ContainerType.getContainerProvider(CRequestContainer.ContainerType.TRADES), packetBuffer -> packetBuffer.writeInt(npcEntity.getEntityId()));
    debugUsage(sender, npcEntity);
  }
}
