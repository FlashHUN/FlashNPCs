package flash.npcmod.mixin;

import flash.npcmod.entity.NpcEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NameTagItem.class)
public class NameTagMixin extends Item {
  public NameTagMixin(Properties properties) {
    super(properties);
  }

  @Inject(method = "interactLivingEntity", at = @At("INVOKE"), cancellable = true)
  public void interactLivingEntity(ItemStack stack, Player playerIn, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
    if (target instanceof NpcEntity) ci.setReturnValue(InteractionResult.FAIL);
  }
}
