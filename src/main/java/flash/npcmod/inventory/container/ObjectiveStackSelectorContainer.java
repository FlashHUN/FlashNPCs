package flash.npcmod.inventory.container;

import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.init.ContainerInit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class ObjectiveStackSelectorContainer extends AbstractContainerMenu {

  @Nullable
  private Slot selectedSlot;

  private QuestObjective questObjective;
  private String originalName;
  private Quest quest;

  public ObjectiveStackSelectorContainer(int id, Inventory inventory, String objective, String quest, String originalName) {
    this(id, inventory, QuestObjective.fromJson(new JSONObject(objective)), Quest.fromJson(new JSONObject(quest)), originalName);
  }

  public ObjectiveStackSelectorContainer(int id, Inventory inventory, QuestObjective objective, Quest quest, String originalName) {
    super(ContainerInit.OBJECTIVE_STACK_SELECTOR_CONTAINER, id);

    questObjective = objective;
    this.quest = quest;
    this.originalName = originalName;

    selectedSlot = null;

    for(int l = 0; l < 3; ++l) {
      for(int j1 = 0; j1 < 9; ++j1) {
        this.addSlot(new Slot(inventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18) {
          @Override
          public void set(ItemStack stack) {
            super.set(stack);
          }

          @Override
          public void onTake(Player thePlayer, ItemStack stack) {
            selectedSlot = this;
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
          selectedSlot = this;
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
  public Slot getSelectedSlot() {
    return selectedSlot;
  }

  public QuestObjective getQuestObjective() {
    return questObjective;
  }

  public String getOriginalName() {
    return originalName;
  }

  public Quest getQuest() {
    return quest;
  }
}
