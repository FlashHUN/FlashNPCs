package flash.npcmod.entity.goals;

import flash.npcmod.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class NPCFollowPathGoal extends Goal {
    protected int nextStartTick;
    public final double speedModifier;
    protected int tryTicks;
    protected int index;
    protected int pathSize;
    protected boolean reachedTarget;

    protected final NpcEntity npc;
    public NPCFollowPathGoal(NpcEntity npc, double speedModifier) {
        this.npc = npc;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        this.index = 0;
        this.pathSize = 0;
    }

    /** For some ungodly reason, the path navigator checks if the manhattan distance is less than 1.0f
     * which is not necessarily true for the tick check. So if accepted distance is <3, the npc will get
     * stuck just outside the range accepted check.
     */
    public double acceptedDistance() {
        return 3D;
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

    public boolean canContinueToUse() {
        return !this.reachedTarget;
    }

    public void start() {
        BlockPos blockpos = this.getMoveToTarget();
        this.moveMobToBlock(blockpos);
        this.tryTicks = 0;
        this.index = 0;
    }
    protected void moveMobToBlock(BlockPos blockPos) {
        this.npc.getNavigation().moveTo((float)blockPos.getX() + 0.5D, blockPos.getY()+0.5D, (float)blockPos.getZ() + 0.5D, this.speedModifier);
    }

    protected int nextStartTick(PathfinderMob p_25618_) {
        return reducedTickDelay(50 + p_25618_.getRandom().nextInt(20));
    }

    protected BlockPos getMoveToTarget() {
        long[] path = this.npc.getCurrentBehavior().getAction().getPath();
        this.pathSize = path.length;
        if (path.length == 0 || this.index >= path.length) return this.npc.blockPosition();
        return BlockPos.of(path[index]).above();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        BlockPos blockpos = this.getMoveToTarget();
        if (!blockpos.closerToCenterThan(this.npc.position(), this.acceptedDistance())) {
            this.reachedTarget = false;
            ++this.tryTicks;
            if (this.tryTicks % 40 == 0 || this.npc.getNavigation().isDone()) { // Limit recalculating.
                moveMobToBlock(blockpos);
            }
        } else if (index < pathSize){
            index++;
        } else {
            stop();
            --this.tryTicks;
            this.npc.setOrigin(this.npc.blockPosition());
            this.npc.setGoalReached(true);
            this.reachedTarget = true;
        }

    }
}
