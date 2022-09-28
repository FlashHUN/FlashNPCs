package flash.npcmod.mixin;

import com.mojang.authlib.GameProfile;
import flash.npcmod.core.EntityUtil;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncEntityList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(OpCommand.class)
public class OpCommandMixin {

    @Inject(method = "opPlayers", at = @At("HEAD"))
    private static void opPlayers(CommandSourceStack source, Collection<GameProfile> gameProfiles, CallbackInfoReturnable<Integer> cir) {
        PlayerList playerlist = source.getServer().getPlayerList();
        for (GameProfile gameProfile : gameProfiles) {
            if (!playerlist.isOp(gameProfile)) {
                Player player = playerlist.getPlayer(gameProfile.getId());
                if (player != null) {
                    PacketDispatcher.sendTo(new SSyncEntityList(EntityUtil.getEntityTypes()), player);
                }
            }
        }
    }

}
