package flash.npcmod.item;

import flash.npcmod.Main;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;

public class NpcSaveToolItem extends Item {
  public NpcSaveToolItem() {
    super(new Properties().maxStackSize(1).group(Main.NPC_ITEMGROUP).rarity(Rarity.EPIC));
  }
}
