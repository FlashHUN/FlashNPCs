package flash.npcmod.entity.goals;

import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Random;

public class NPCWanderGoal extends Goal {

    private final NpcEntity mob;
    private final int interval;
    private BlockPos wantedPos;
    private final double minimumWalk;

    public NPCWanderGoal(PathfinderMob pathfinderMob, int tickDelay) {
        mob = (NpcEntity) pathfinderMob;
        interval = reducedTickDelay(tickDelay);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        this.minimumWalk = 2.0D;
    }

    public boolean canUse() {
        if (this.mob.isVehicle()) {
            return false;
        } else if (this.mob.level.isNight()) {
            return false;
        } else if (this.mob.getRandom().nextInt(this.interval) != 0) {
            return false;
        } else {
            // LandRandomPos generates a random position. 15 horizontal 7 vertical The function at the end takes in a block position
            // and returns a weight that this should be taken. After 10 attempts, it will always take the one that
            // was most suitable.
            Vec3 vec3 = LandRandomPos.getPos(this.mob, 15, 7, (blockPos) ->
                    blockPos.distManhattan(mob.getOrigin()) > this.mob.getRadius() ? -1 : 0);
            if (vec3 == null) {
                this.wantedPos = null;
            } else {
                // Final pass to reject all positions that were too high.
                BlockPos blockPos = new BlockPos(vec3);
                this.wantedPos = this.mob.getOrigin().distManhattan(blockPos) > this.mob.getRadius() ? null : blockPos;
            }
            if (wantedPos != null) {
                Main.LOGGER.info("Wandering pos found!" + wantedPos);
            }
            return this.wantedPos != null;
        }
    }

    public boolean canContinueToUse() {
        return this.wantedPos != null && !this.mob.getNavigation().isDone() && this.mob.getNavigation().getTargetPos().equals(this.wantedPos);
    }

    public void tick() {
        if (this.wantedPos != null) {
            PathNavigation pathnavigation = this.mob.getNavigation();
            if (pathnavigation.isDone() && !this.wantedPos.closerThan(new BlockPos(this.mob.position()), minimumWalk)) {
                Vec3 vec3 = Vec3.atBottomCenterOf(this.wantedPos);
                Vec3 vec31 = this.mob.position();
                Vec3 vec32 = vec31.subtract(vec3);
                vec3 = vec32.scale(0.4D).add(vec3);
                Vec3 vec33 = vec3.subtract(vec31).normalize().scale(10.0D).add(vec31);
                BlockPos blockpos = new BlockPos(vec33);
                blockpos = this.mob.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockpos);
                if (!pathnavigation.moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), 1.0D)) {
                    this.moveRandomly();
                }
            }

        }
    }

    private void moveRandomly() {
        Random random = this.mob.getRandom();
        BlockPos blockpos = this.mob.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.mob.blockPosition().offset(-8 + random.nextInt(16), 0, -8 + random.nextInt(16)));
        this.mob.getNavigation().moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), 1.0D);
    }
}
