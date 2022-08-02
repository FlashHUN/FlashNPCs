package flash.npcmod.client.gui.behavior;

import com.google.gson.JsonObject;
import flash.npcmod.network.packets.client.CEditNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Action {
  private final ActionType actionType;
  private final BlockPos targetBlockPos;
  private final String name;
  private final CEditNpc.NPCPose pose;
  private final int radius;

  /**
   * The type of action.
   */
  public enum ActionType {
      //FOLLOW_PLAYER,
      MOVE_TO_BLOCK,
      STANDSTILL,
      WANDER,
      //INTERACT_WITH,
      //LOOK_AT
  }

  /**
   * Create a default action.
   */
  public Action() {
    this(BlockPos.ZERO);
  }

  public Action(BlockPos blockPos) {
    this.name = "";
    this.pose = CEditNpc.NPCPose.STANDING;
    this.actionType = ActionType.STANDSTILL;
    this.radius = 0;
    this.targetBlockPos = blockPos;
  }

  /**
   * Create an action with name and pose.
   * @param name The name of the action.
   * @param pose The pose to take.
   */
  public Action(String name, CEditNpc.NPCPose pose, ActionType actionType, BlockPos targetBlockPos, int radius) {
    this.name = name;
    this.pose = pose;
    this.actionType = actionType;
    this.targetBlockPos = targetBlockPos;
    this.radius = radius;
  }

  /**
   * Check the equality.
   * @param o The other object.
   * @return boolean.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Action action = (Action) o;
    return name.equals(action.name) && pose.equals(action.pose);
  }

  public static Action fromCompound(CompoundTag actionTag) {
    return new Action(
            actionTag.getString("name"),
            CEditNpc.NPCPose.valueOf(actionTag.getString("pose")),
            Action.ActionType.valueOf(actionTag.getString("actionType")),
            NbtUtils.readBlockPos(actionTag.getCompound("targetBlockPos")),
            actionTag.getInt("radius")
    );
  }

  /**
   * Create an action from a JSON object.
   * @param object The json.
   * @return The new action.
   */
  public static Action fromJSONObject(JsonObject object) {
    String name = object.get("name").getAsString();
    CEditNpc.NPCPose pose = CEditNpc.NPCPose.valueOf(object.get("pose").getAsString());
    ActionType actionType = ActionType.valueOf(object.get("actionType").getAsString());
    BlockPos targetBlockPos = BlockPos.of(object.get("targetBlockPos").getAsLong());
    int radius = object.get("radius").getAsInt();
    return new Action(name, pose, actionType, targetBlockPos, radius);
  }

  /**
   * Get the action type.
   * @return Action type.
   */
  public ActionType getActionType() { return this.actionType; }

  /**
   * Get the target block position.
   * @return Target block.
   */
  public BlockPos getTargetBlockPos() { return this.targetBlockPos; }

  /**
   * Get the name of the action.
   * @return The name.
   */
  public String getName() { return this.name; }

  /**
   * Get the pose of the action.
   * @return The pose.
   */
  public CEditNpc.NPCPose getPose() { return this.pose; }

  /**
   * Get the radius of the action.
   * @return The radius.
   */
  public int getRadius() { return this.radius; }

  public CompoundTag toCompoundTag() {
    CompoundTag actionTag = new CompoundTag();
    actionTag.putString("actionType", String.valueOf(this.actionType));
    actionTag.putString("name", this.name);
    actionTag.putString("pose", this.pose.name());
    actionTag.put("targetBlockPos", NbtUtils.writeBlockPos(this.targetBlockPos));
    actionTag.putInt("radius", this.radius);
    return actionTag;
  }

  /**
   * Convert this object to json.
   * @return JsonObject.
   */
  public JsonObject toJSONObject() {
    JsonObject json = new JsonObject();
    json.addProperty("name", this.name);
    json.addProperty("pose", String.valueOf(this.pose));
    json.addProperty("actionType", String.valueOf(this.actionType));
    json.addProperty("targetBlockPos", this.targetBlockPos.asLong());
    json.addProperty("radius", this.radius);
    return json;
  }
}
