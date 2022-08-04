package flash.npcmod.item;

import flash.npcmod.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class BehaviorEditorItem extends Item {
    public BehaviorEditorItem() {
        super(new Item.Properties().stacksTo(1).tab(Main.NPC_ITEMGROUP).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            BlockPos pos = context.getClickedPos();
            Player player = context.getPlayer();
            ItemStack itemStack = context.getItemInHand();

            CompoundTag nbt;
            Long[] path;
            if (itemStack.hasTag()) {
                nbt = itemStack.getTag();
                List<Long> pathList= Arrays.stream(nbt.getLongArray("Path")).boxed().collect(Collectors.toList());
                pathList.add(pos.asLong());
                path = pathList.toArray(new Long[0]);
            } else {
                nbt = new CompoundTag();
                path = new Long[]{pos.asLong()};
            }
            nbt.putLongArray("Path", List.of(path));

            itemStack.setTag(nbt);
            Main.LOGGER.info(nbt);
        }
        return super.useOn(context);
    }
}