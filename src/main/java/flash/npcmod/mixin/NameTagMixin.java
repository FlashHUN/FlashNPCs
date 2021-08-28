package flash.npcmod.mixin;

import flash.npcmod.entity.NpcEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.NameTagItem;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NameTagItem.class)
public class NameTagMixin extends Item {
  public NameTagMixin(Properties properties) {
    super(properties);
  }

  @Inject(method = "itemInteractionForEntity", at = @At("INVOKE"), cancellable = true)
  public void itemInteractionForEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand, CallbackInfoReturnable<ActionResultType> ci) {
    if (target instanceof NpcEntity) ci.setReturnValue(ActionResultType.FAIL);
  }
}
