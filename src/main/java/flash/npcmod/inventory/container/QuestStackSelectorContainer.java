package flash.npcmod.inventory.container;

import flash.npcmod.core.quests.Quest;
import flash.npcmod.init.ContainerInit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class QuestStackSelectorContainer extends AbstractContainerMenu {

  @Nullable
  private List<Slot> selectedSlots;

  private Quest quest;

  public QuestStackSelectorContainer(int id, Inventory inventory, String quest) {
    this(id, inventory, Quest.fromJson(new JSONObject(quest)));
  }

  public QuestStackSelectorContainer(int id, Inventory inventory, Quest quest) {
    super(ContainerInit.QUEST_STACK_SELECTOR_CONTAINER, id);

    this.quest = quest;

    selectedSlots = new ArrayList<>();

    for(int l = 0; l < 3; ++l) {
      for(int j1 = 0; j1 < 9; ++j1) {
        this.addSlot(new Slot(inventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18) {
          @Override
          public void set(ItemStack stack) {
            super.set(stack);
          }

          @Override
          public void onTake(Player thePlayer, ItemStack stack) {
            if (selectedSlots.contains(this))
              selectedSlots.remove(this);
            else
              selectedSlots.add(this);
            this.set(stack);
            thePlayer.inventoryMenu.setCarried(ItemStack.EMPTY);
          }
        });
      }
    }

    for(int i1 = 0; i1 < 9; ++i1) {
      this.addSlot(new Slot(inventory, i1, 8 + i1 * 18, 142) {
        @Override
        public void onTake(Player thePlayer, ItemStack stack) {
          if (selectedSlots.contains(this))
            selectedSlots.remove(this);
          else
            selectedSlots.add(this);
          this.set(stack);
          thePlayer.inventoryMenu.setCarried(ItemStack.EMPTY);
        }
      });
    }
  }

  @Override
  public boolean stillValid(Player playerIn) {
    return playerIn.hasPermissions(4);
  }

  @Nullable
  public List<Slot> getSelectedSlots() {
    return selectedSlots;
  }

  public Quest getQuest() {
    return quest;
  }
}
