package flash.npcmod.item;

import flash.npcmod.Main;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class NpcSaveToolItem extends Item {
  public NpcSaveToolItem() {
    super(new Properties().stacksTo(1).tab(Main.NPC_ITEMGROUP).rarity(Rarity.EPIC));
  }

  @Override
  public void appendHoverText(ItemStack itemStack, @Nullable Level p_40881_, List<Component> list, TooltipFlag tooltipFlag) {
    if (Screen.hasShiftDown()) {
      list.add(new TranslatableComponent("tooltip.flashnpcs.save_tool_shift"));
    } else {
      list.add(new TranslatableComponent("tooltip.flashnpcs.shift"));
    }
  }
}
