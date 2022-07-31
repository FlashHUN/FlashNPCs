package flash.npcmod.entity.goals;

import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class NPCInteractWithBlockGoal extends MoveToBlockGoal {
    private final NpcEntity npc;
    private static final int DEFAULT_SEARCH_RANGE = 8;

    private boolean runOnce;

    public NPCInteractWithBlockGoal(NpcEntity npc, double speed_modifier) {
        super(npc, speed_modifier, DEFAULT_SEARCH_RANGE);
        this.npc = npc;
        this.runOnce = false;
    }

    /**
     * Check if this goal can be used.
     * @return Boolean.
     */
    public boolean canUse() {
        if(super.canUse() && runOnce){
            if (this.npc.isNotMovingToBlock())
                stop();
            else return true;
        }
        return false;
    }

    /**
     * Check if the block is a valid target.
     * @param level The level.
     * @param blockPos The block position.
     * @return Boolean.
     */
    protected boolean isValidTarget(@NotNull LevelReader level, BlockPos blockPos) {
        return blockPos.equals(this.npc.getTargetBlock());
    }

    public boolean shouldRecalculatePath() {
        return canUse() && this.tryTicks % 40 == 0;
    }

    public void start() {
        super.start();
        this.runOnce = true;
    }

    public double acceptedDistance() {
        return 2.5D;
    }

    /**
     * Called every tick.
     */
    public void tick() {
        super.tick();
        if (this.runOnce && this.npc.getTargetBlock().closerThan(new BlockPos(this.mob.position()), this.acceptedDistance())) {
            BlockState blockState = this.npc.level.getBlockState(this.npc.getTargetBlock());
            if (blockState.is(BlockTags.BUTTONS) || blockState.is(BlockTags.WOODEN_BUTTONS)) {
                ((ButtonBlock)blockState.getBlock()).press(blockState, this.npc.level, this.npc.getTargetBlock());
            } else if (blockState.is(Blocks.LEVER)) {
                ((LeverBlock)blockState.getBlock()).pull(blockState, this.npc.level, this.npc.getTargetBlock());
            }
            this.npc.setOrigin(this.npc.blockPosition());
            this.npc.setGoalReached(true);
            this.runOnce = false;
            this.stop();
        }
    }
}
