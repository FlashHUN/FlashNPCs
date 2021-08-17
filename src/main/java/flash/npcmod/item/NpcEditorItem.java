package flash.npcmod.item;

import flash.npcmod.Main;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;

public class NpcEditorItem extends Item {
  public NpcEditorItem() {
    super(new Item.Properties().maxStackSize(1).group(Main.NPC_ITEMGROUP).rarity(Rarity.EPIC));
  }
}
