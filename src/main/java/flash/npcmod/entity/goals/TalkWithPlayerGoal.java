package flash.npcmod.entity.goals;

import flash.npcmod.entity.NpcEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class TalkWithPlayerGoal extends Goal{
    private final NpcEntity mob;

    public TalkWithPlayerGoal(NpcEntity npc) {
        this.mob = npc;
        this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
    }

    public boolean canUse() {
        if (!this.mob.isAlive()) {
            return false;
        } else if (this.mob.isInWater()) {
            return false;
        } else if (!this.mob.isOnGround()) {
            return false;
        } else if (this.mob.hurtMarked) {
            return false;
        } else {
            Player player = this.mob.getTalkingPlayer();
            if (player == null) {
                return false;
            } else if (this.mob.distanceToSqr(player) > 16.0D) {
                return false;
            } else {
                return true;
            }
        }
    }

    public void start() {
        this.mob.getNavigation().stop();
    }

    public void stop() {
        this.mob.setTalkingPlayer(null);
    }
}
