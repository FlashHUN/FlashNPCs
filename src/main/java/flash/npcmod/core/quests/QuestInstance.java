package flash.npcmod.core.quests;

import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;

public class QuestInstance {

  private final Quest quest;
  private final UUID pickedUpFrom;
  private final String pickedUpFromName;
  private final TurnInType turnInType;
  @Nullable
  private Player player;


  public QuestInstance(Quest quest, UUID pickedUpFrom, String pickedUpFromName, TurnInType turnInType) {
    this(quest, pickedUpFrom, pickedUpFromName, turnInType, null);
  }

  public QuestInstance(Quest quest, UUID pickedUpFrom, String pickedUpFromName, TurnInType turnInType, @Nullable Player player) {
    this.quest = quest.copy();
    this.pickedUpFrom = pickedUpFrom;
    this.pickedUpFromName = pickedUpFromName;
    this.turnInType = turnInType;
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

  public TurnInType getTurnInType() {
    return turnInType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QuestInstance that = (QuestInstance) o;
    if ((player != null && that.player == null) || (player == null && that.player != null)) return false;
    boolean playerNamesMatch = true;
    if (player != null && that.player != null) playerNamesMatch = player.getName().getString().equals(that.player.getName().getString());
    return playerNamesMatch && quest.getName().equals(that.quest.getName()) && pickedUpFrom.equals(that.pickedUpFrom) && turnInType.equals(that.turnInType);
  }

  public enum TurnInType {
    QuestGiver,
    NpcByUuid,
    AutoTurnIn
  }
}
