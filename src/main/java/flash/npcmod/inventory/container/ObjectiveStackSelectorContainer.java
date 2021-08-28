package flash.npcmod.inventory.container;

import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.init.ContainerInit;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import org.json.JSONObject;

import javax.annotation.Nullable;

public class ObjectiveStackSelectorContainer extends Container {

  @Nullable
  private Slot selectedSlot;

  private QuestObjective questObjective;
  private String originalName;
  private Quest quest;

  public ObjectiveStackSelectorContainer(int id, PlayerInventory inventory, String objective, String quest, String originalName) {
    this(id, inventory, QuestObjective.fromJson(new JSONObject(objective)), Quest.fromJson(new JSONObject(quest)), originalName);
  }

  public ObjectiveStackSelectorContainer(int id, PlayerInventory inventory, QuestObjective objective, Quest quest, String originalName) {
    super(ContainerInit.OBJECTIVE_STACK_SELECTOR_CONTAINER, id);

    questObjective = objective;
    this.quest = quest;
    this.originalName = originalName;

    selectedSlot = null;

    for(int l = 0; l < 3; ++l) {
      for(int j1 = 0; j1 < 9; ++j1) {
        this.addSlot(new Slot(inventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18) {
          @Override
          public void putStack(ItemStack stack) {
            super.putStack(stack);
          }

          @Override
          public ItemStack onTake(PlayerEntity thePlayer, ItemStack stack) {
            selectedSlot = this;
            this.putStack(stack);
            thePlayer.inventory.setItemStack(ItemStack.EMPTY);
            return ItemStack.EMPTY;
          }
        });
      }
    }

    for(int i1 = 0; i1 < 9; ++i1) {
      this.addSlot(new Slot(inventory, i1, 8 + i1 * 18, 142) {
        @Override
        public ItemStack onTake(PlayerEntity thePlayer, ItemStack stack) {
          selectedSlot = this;
          this.putStack(stack);
          thePlayer.inventory.setItemStack(ItemStack.EMPTY);
          return ItemStack.EMPTY;
        }
      });
    }
  }

  @Override
  public boolean canInteractWith(PlayerEntity playerIn) {
    return playerIn.hasPermissionLevel(4);
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
