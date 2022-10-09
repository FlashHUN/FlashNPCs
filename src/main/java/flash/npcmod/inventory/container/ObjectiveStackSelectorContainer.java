package flash.npcmod.inventory.container;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.core.PermissionHelper;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.init.ContainerInit;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class ObjectiveStackSelectorContainer extends AbstractContainerMenu {

  @Nullable
  private Slot selectedSlot;

  private QuestObjective questObjective;
  private String originalName;
  private Quest quest;

  public ObjectiveStackSelectorContainer(int id, Inventory inventory, String objective, String quest, String originalName) {
    this(id, inventory, QuestObjective.fromJson(new Gson().fromJson(objective, JsonObject.class)), Quest.fromJson(new Gson().fromJson(quest, JsonObject.class)), originalName);
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
          public void onTake(Player thePlayer, ItemStack stack) {
            selectedSlot = this;
            set(stack);
            setCarried(ItemStack.EMPTY);
          }
        });
      }
    }

    for(int i1 = 0; i1 < 9; ++i1) {
      this.addSlot(new Slot(inventory, i1, 8 + i1 * 18, 142) {
        @Override
        public void onTake(Player thePlayer, ItemStack stack) {
          selectedSlot = this;
          set(stack);
          setCarried(ItemStack.EMPTY);
        }
      });
    }
  }

  @Override
  public boolean stillValid(Player playerIn) {
    return PermissionHelper.hasPermission(playerIn, PermissionHelper.EDIT_QUEST);
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
