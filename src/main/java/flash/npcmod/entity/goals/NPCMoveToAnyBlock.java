package flash.npcmod.entity.goals;

import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * WIP Class to move NPCs to blocks of a type that can be specified.
 */
public class NPCMoveToAnyBlock extends Goal{
    public final double speedModifier;
    protected int nextStartTick;
    protected int tryTicks;
    private BlockPos blockPos = BlockPos.ZERO;
    private boolean reachedTarget;

    private final NpcEntity npc;
    private static final int DEFAULT_SEARCH_RANGE = 8;
    private final int searchRange;
    private final int verticalSearchRange;
    protected int verticalSearchStart;

    public NPCMoveToAnyBlock(NpcEntity npc, double speed_modifier) {
        this(npc, speed_modifier, DEFAULT_SEARCH_RANGE);
    }

    public NPCMoveToAnyBlock(NpcEntity npc, double speedModifier, int horizSearchRange) {
        this(npc, speedModifier, horizSearchRange, 5);
    }

    public NPCMoveToAnyBlock(NpcEntity npc, double speedModifier, int horizSearchRange, int verticalSearchRange) {
        this.npc = npc;
        this.speedModifier = speedModifier;
        this.searchRange = horizSearchRange;
        this.verticalSearchStart = 0;
        this.verticalSearchRange = verticalSearchRange;
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
            return this.findNearestBlock();
        }
    }

    protected int nextStartTick(PathfinderMob p_25618_) {
        return reducedTickDelay(50 + p_25618_.getRandom().nextInt(20));
    }

    public boolean canContinueToUse() {
        return !this.reachedTarget;
    }

    public void start() {
        this.moveMobToBlock();
        this.tryTicks = 0;
    }

    protected void moveMobToBlock() {
        this.npc.getNavigation().moveTo((double)((float)this.blockPos.getX()) + 0.5D, this.blockPos.getY() + 1, (double)((float)this.blockPos.getZ()) + 0.5D, this.speedModifier);
    }

    public double acceptedDistance() {
        return 0.5D;
    }

    protected BlockPos getMoveToTarget() {
        return this.blockPos.above();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        BlockPos blockpos = this.getMoveToTarget();
        BlockPos targetBlock = this.npc.getTargetBlock();
        Vec3 targetPos = new Vec3(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());
        if (!blockpos.closerToCenterThan(this.npc.position(), this.acceptedDistance())) {
            this.reachedTarget = false;
            ++this.tryTicks;
            if (this.tryTicks % 40 == 0) { // Limit recalculating.
                this.npc.getNavigation().moveTo((double) ((float) blockpos.getX()) + 0.5D, blockpos.getY(), (double) ((float) blockpos.getZ()) + 0.5D, this.speedModifier);
            }

        } else if (blockpos.closerToCenterThan(targetPos, this.acceptedDistance())) {
            this.npc.setOrigin(this.npc.blockPosition());
            this.npc.setGoalReached(true);
            this.reachedTarget = true;
        } else { // reached intermediary position.
            stop();
            --this.tryTicks;

        }

    }

    protected boolean findNearestBlock() {
        BlockPos blockpos = this.npc.blockPosition();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        boolean exactSearch, blockFound = false;
        BlockPos targetPos = this.npc.getTargetBlock();
        double currentDist = Mth.sqrt((float) this.npc.blockPosition().distToCenterSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ()));
        double xDist = Mth.abs(this.npc.blockPosition().getX() - targetPos.getX());
        double yDist = Mth.abs(this.npc.blockPosition().getY() - targetPos.getY());
        double zDist = Mth.abs(this.npc.blockPosition().getZ() - targetPos.getZ());
        exactSearch = (xDist < this.searchRange && yDist < this.searchRange && zDist < this.searchRange);

        if (exactSearch) {
            Main.LOGGER.info("Close enough to find" + targetPos);
            this.blockPos = targetPos;
            return true;
        } else {
            Main.LOGGER.info("Too far from " + targetPos + " finding in between position");
        }

        // search starting at the vertical search start then alternate up and down as the search continues.
        for(int k = this.verticalSearchStart; k <= this.verticalSearchRange; k = k > 0 ? -k : 1 - k) {
            for(int l = 0; l < this.searchRange; ++l) {
                for(int i1 = 0; i1 <= l; i1 = i1 > 0 ? -i1 : 1 - i1) {
                    for(int j1 = i1 < l && i1 > -l ? l : 0; j1 <= l; j1 = j1 > 0 ? -j1 : 1 - j1) {
                        mutableBlockPos.setWithOffset(blockpos, i1, k - 1, j1);
                        if (this.npc.isWithinRestriction(mutableBlockPos) && this.isValidTarget(mutableBlockPos, currentDist)) {
                            currentDist = Mth.sqrt((float) mutableBlockPos.distToCenterSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ()));
                            Main.LOGGER.info("Found " + mutableBlockPos + " dist: " + currentDist);
                            this.blockPos = mutableBlockPos.immutable();
                            Main.LOGGER.info("Set to " + this.blockPos);
                            blockFound = true;
                        }
                    }
                }
            }
        }

        return blockFound;
    }

    /**
     * Check if the block being compared is closer than our current position.
     * @param blockPos The block to check is closer.
     * @param currentDist The current dist.
     * @return True if closer.
     */
    protected boolean isValidTarget(BlockPos blockPos, double currentDist) {
        return blockPos.closerThan(this.npc.getTargetBlock(), currentDist);
    }
}