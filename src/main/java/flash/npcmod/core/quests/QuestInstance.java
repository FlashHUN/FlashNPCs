package flash.npcmod.core.quests;

import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;

public class QuestInstance {

  private Quest quest;
  private UUID pickedUpFrom;
  private String pickedUpFromName;
  @Nullable
  private Player player;

  public QuestInstance(Quest quest, UUID pickedUpFrom, String pickedUpFromName) {
    this(quest, pickedUpFrom, pickedUpFromName, null);
  }

  public QuestInstance(Quest quest, UUID pickedUpFrom, String pickedUpFromName, @Nullable Player player) {
    this.quest = quest.copy();
    this.pickedUpFrom = pickedUpFrom;
    this.pickedUpFromName = pickedUpFromName;
    this.player = player;
  }

  public Quest getQuest() {
    return quest;
  }

  public Player getPlayer() {
    return player;
  }

  public void setPlayer(Player player) {
    this.player = player;
  }

  public UUID getPickedUpFrom() {
    return pickedUpFrom;
  }

  public String getPickedUpFromName() {
    return pickedUpFromName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QuestInstance that = (QuestInstance) o;
    if ((player != null && that.player == null) || (player == null && that.player != null)) return false;
    boolean playerNamesMatch = true;
    if (player != null && that.player != null) playerNamesMatch = player.getName().getString().equals(that.player.getName().getString());
    return playerNamesMatch && quest.getName().equals(that.quest.getName()) && pickedUpFrom.equals(that.pickedUpFrom);
  }
}
