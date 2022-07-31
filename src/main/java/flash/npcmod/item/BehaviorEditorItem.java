package flash.npcmod.item;

import flash.npcmod.Main;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class BehaviorEditorItem extends Item {
    public BehaviorEditorItem() {
        super(new Item.Properties().stacksTo(1).tab(Main.NPC_ITEMGROUP).rarity(Rarity.EPIC));
    }
}