package flash.npcmod.item;

import flash.npcmod.Main;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
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
    public void appendHoverText(ItemStack itemStack, @Nullable Level p_40881_, List<Component> list, TooltipFlag tooltipFlag) {
        if (Screen.hasShiftDown()) {
            list.add(new TranslatableComponent("tooltip.flashnpcs.behavior_editor_shift"));
        } else {
            list.add(new TranslatableComponent("tooltip.flashnpcs.shift"));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            BlockPos pos = context.getClickedPos();
            ItemStack itemStack = context.getItemInHand();

            CompoundTag nbt;
            Long[] path;
            if (itemStack.hasTag()) {
                nbt = itemStack.getTag();
                List<Long> pathList= Arrays.stream(nbt.getLongArray("Path")).boxed().collect(Collectors.toList());
                if (pathList.size() > 0 && pathList.get(pathList.size() - 1) == pos.asLong()){
                    pathList.remove(pathList.size() - 1);
                } else {
                    pathList.add(pos.asLong());
                }
                path = pathList.toArray(new Long[0]);
            } else {
                nbt = new CompoundTag();
                path = new Long[]{pos.asLong()};
            }
            nbt.putLongArray("Path", List.of(path));

            itemStack.setTag(nbt);
        }
        return super.useOn(context);
    }
}