package flash.npcmod.entity.goals;

import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class NPCMoveToBlockGoal  extends Goal {
    public final double speedModifier;
    protected int nextStartTick;
    protected int tryTicks;
    protected boolean reachedTarget;

    protected final NpcEntity npc;
    public NPCMoveToBlockGoal(NpcEntity npc, double speedModifier) {
        this.npc = npc;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    public boolean canUse() {
        if (reachedTarget) {
            stop();
            return false;
        }
        if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        } else {
            this.nextStartTick = this.nextStartTick(this.npc);
            return true;
        }
    }

    protected int nextStartTick(PathfinderMob p_25618_) {
        return reducedTickDelay(50 + p_25618_.getRandom().nextInt(20));
    }

    public boolean canContinueToUse() {
        return !this.reachedTarget;
    }

    public void start() {
        BlockPos blockpos = this.getMoveToTarget();
        this.moveMobToBlock(blockpos);
        this.tryTicks = 0;
    }

    protected void moveMobToBlock(BlockPos blockPos) {
        this.npc.getNavigation().moveTo((double)((float)blockPos.getX()) + 0.5D, blockPos.getY() + 1, (double)((float)blockPos.getZ()) + 0.5D, this.speedModifier);
    }

    public double acceptedDistance() {
        return 0.5D;
    }

    protected BlockPos getMoveToTarget() {
        return this.npc.getTargetBlock().above();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        BlockPos blockpos = this.getMoveToTarget();
        if (!blockpos.closerToCenterThan(this.npc.position(), this.acceptedDistance())) {
            this.reachedTarget = false;
            ++this.tryTicks;
            if (this.tryTicks % 40 == 0) { // Limit recalculating.
                moveMobToBlock(blockpos);
            }
        } else {
            stop();
            --this.tryTicks;
            this.npc.setOrigin(this.npc.blockPosition());
            this.npc.setGoalReached(true);
            this.reachedTarget = true;
        }

    }
}
