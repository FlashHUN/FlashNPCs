package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlaySoundPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;

public class PlaySoundFunction extends AbstractFunction {

  public PlaySoundFunction() {
    super("playSoundAt", new String[]{"sound","x","y","z"}, empty);
  }

  @Override
  public void call(String[] params, ServerPlayerEntity sender, NpcEntity npcEntity) {
    try {
      if (params.length == 4) {
        ResourceLocation sound = new ResourceLocation(params[0]);
        double x = Double.parseDouble(params[1]);
        double y = Double.parseDouble(params[2]);
        double z = Double.parseDouble(params[3]);
        sender.connection.sendPacket(new SPlaySoundPacket(sound, SoundCategory.MASTER, new Vector3d(x, y, z), 1f, 1f));
        debugUsage(sender, npcEntity);
      } else {
        warnParameterAmount(npcEntity);
      }
    } catch (NumberFormatException e) {
      return;
    }
  }
}
