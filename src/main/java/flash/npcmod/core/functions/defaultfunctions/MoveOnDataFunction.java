package flash.npcmod.core.functions.defaultfunctions;

import com.mojang.brigadier.StringReader;
import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SMoveToDialogue;
import net.minecraft.nbt.*;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;

public class MoveOnDataFunction extends AbstractFunction {

  public MoveOnDataFunction() {
    super("moveOnData", new String[]{"tag","value","trueOption","falseOption"}, empty);
  }

  @Override
  public void call(String[] params, ServerPlayer sender, NpcEntity npcEntity) {
    if (params.length == 4) {
      EntityDataAccessor dataAccessor = new EntityDataAccessor(sender);
      CompoundTag data = dataAccessor.getData();

      Tag value;
      try {
        value = new TagParser(new StringReader(params[1])).readValue();
      } catch (Exception e) {
        value = StringTag.valueOf(params[1]);
      }

      if (NbtUtils.compareNbt(data.get(params[0]), value, false))
        PacketDispatcher.sendTo(new SMoveToDialogue(params[2], npcEntity.getId()), sender);
      else if (!params[3].isEmpty())
        PacketDispatcher.sendTo(new SMoveToDialogue(params[3], npcEntity.getId()), sender);

      debugUsage(sender, npcEntity);
    } else {
      warnParameterAmount(sender, npcEntity);
    }
  }

}
