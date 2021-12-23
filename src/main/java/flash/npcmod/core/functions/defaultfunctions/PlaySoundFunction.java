package flash.npcmod.core.functions.defaultfunctions;

import flash.npcmod.core.functions.AbstractFunction;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class PlaySoundFunction extends AbstractFunction {

  public PlaySoundFunction() {
    super("playSoundAt", new String[]{"sound","x","y","z"}, empty);
  }

  @Override
  public void call(String[] params, ServerPlayer sender, NpcEntity npcEntity) {
    try {
      if (params.length == 4) {
        ResourceLocation sound = new ResourceLocation(params[0]);
        double x = Double.parseDouble(params[1]);
        double y = Double.parseDouble(params[2]);
        double z = Double.parseDouble(params[3]);
        sender.connection.send(new ClientboundCustomSoundPacket(sound, SoundSource.MASTER, new Vec3(x, y, z), 1f, 1f));
        debugUsage(sender, npcEntity);
      } else {
        warnParameterAmount(npcEntity);
      }
    } catch (NumberFormatException e) {
      return;
    }
  }
}
