package flash.npcmod.events;

import flash.npcmod.entity.NpcEntity;
import flash.npcmod.item.NpcEditorItem;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CHandleNpcEditorRightClick;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientEvents {

  private static final Minecraft minecraft = Minecraft.getInstance();

  @SubscribeEvent
  public void onClick(InputEvent.ClickInputEvent event) {
    if (minecraft.player != null && minecraft.player.isAlive()) {
      if (event.isUseItem()) {
        ItemStack stack = minecraft.player.getHeldItem(event.getHand());
        if (stack.getItem() instanceof NpcEditorItem) {
          RayTraceResult rayTraceResult = minecraft.objectMouseOver;
          if (rayTraceResult.getType().equals(RayTraceResult.Type.MISS) && minecraft.player.isDiscrete()) {
            // If we right click on the air while sneaking, open the function builder
            PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick());
          } else if (rayTraceResult.getType().equals(RayTraceResult.Type.ENTITY)) {
            // If we right click on an entity and it is an NPC, edit it
            EntityRayTraceResult result = (EntityRayTraceResult)rayTraceResult;
            Entity entity = result.getEntity();
            if (entity instanceof NpcEntity) {
              PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick(entity.getEntityId()));
            }
          } else if (rayTraceResult.getType().equals(RayTraceResult.Type.BLOCK)) {
            // If we right click on a block, create a new npc and edit it
            BlockRayTraceResult result = (BlockRayTraceResult)rayTraceResult;
            BlockPos pos = result.getPos();
            PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick(pos));
          }
        }
      }
    }
  }

}
