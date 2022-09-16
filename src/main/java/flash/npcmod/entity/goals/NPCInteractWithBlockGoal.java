package flash.npcmod.entity.goals;

import flash.npcmod.entity.NpcEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class NPCInteractWithBlockGoal extends NPCMoveToBlockGoal {

    private boolean interactOnce;
    public NPCInteractWithBlockGoal(NpcEntity npc, double speed_modifier) {
        super(npc, speed_modifier);
        this.interactOnce = true;
    }

    /**
     * Check if this goal can be used.
     * @return Boolean.
     */
    public boolean canUse() {
        if(super.canUse()){
            if (this.npc.isNotMovingToBlock())
                stop();
            else return true;
        }
        return false;
    }

    public double acceptedDistance() {
        return 2.5D;
    }

    /**
     * Called every tick.
     */
    public void tick() {
        super.tick();
        if (this.reachedTarget && interactOnce) {

            BlockState blockState = this.npc.level.getBlockState(this.npc.getTargetBlock());
            if (blockState.is(BlockTags.BUTTONS) || blockState.is(BlockTags.WOODEN_BUTTONS)) {
                ((ButtonBlock)blockState.getBlock()).press(blockState, this.npc.level, this.npc.getTargetBlock());
                this.npc.gameEvent(GameEvent.BLOCK_PRESS);
            } else if (blockState.is(Blocks.LEVER)) {
                ((LeverBlock)blockState.getBlock()).pull(blockState, this.npc.level, this.npc.getTargetBlock());
                this.npc.gameEvent(GameEvent.BLOCK_SWITCH, this.npc.getTargetBlock());
            }
            interactOnce = false;
            this.npc.setOrigin(this.npc.blockPosition());
            this.npc.setGoalReached(true);
        }
    }
}
