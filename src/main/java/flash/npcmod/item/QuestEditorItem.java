package flash.npcmod.item;

import flash.npcmod.Main;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class QuestEditorItem extends Item {

  public QuestEditorItem() {
    super(new Item.Properties().stacksTo(1).tab(Main.NPC_ITEMGROUP).rarity(Rarity.EPIC));
  }
}
