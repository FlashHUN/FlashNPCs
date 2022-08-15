package flash.npcmod.client.gui.behavior;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import flash.npcmod.Main;
import flash.npcmod.network.packets.client.CEditNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Actions are properties of NPC behaviors. An action describes fully how the npc should move/ interact/ pose in the world.
 */
@OnlyIn(Dist.CLIENT)
public class Action {
  private final ActionType actionType;
  private final BlockPos targetBlockPos;
  private final String name, pathName;
  private final CEditNpc.NPCPose pose;
  private final int radius;
  private long[] path;

  public enum ActionType {
      //FOLLOW_PLAYER,
      FOLLOW_PATH,
      STANDSTILL,
      WANDER,
      INTERACT_WITH,
      //LOOK_AT
  }

  public Action() {
    this(BlockPos.ZERO);
  }

  public Action(BlockPos blockPos) {
    this.name = "";
    this.pose = CEditNpc.NPCPose.STANDING;
    this.actionType = ActionType.STANDSTILL;
    this.radius = 0;
    this.targetBlockPos = blockPos;
    this.pathName = null;
    this.path = new long[0];
  }

  public Action(String name, CEditNpc.NPCPose pose, ActionType actionType, BlockPos targetBlockPos, int radius, long[] path) {
    this.name = name;
    this.pose = pose;
    this.actionType = actionType;
    this.targetBlockPos = targetBlockPos;
    this.radius = radius;
    this.pathName = null;
    this.path = path;
  }

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
            actionTag.getInt("radius"),
            actionTag.getLongArray("path")
    );
  }

  public static Action fromJSONObject(JsonObject object) {
    String name = object.get("name").getAsString();
    CEditNpc.NPCPose pose = CEditNpc.NPCPose.valueOf(object.get("pose").getAsString());
    ActionType actionType = ActionType.valueOf(object.get("actionType").getAsString());
    BlockPos targetBlockPos = BlockPos.of(object.get("targetBlockPos").getAsLong());
    int radius = object.get("radius").getAsInt();
    JsonArray path = object.getAsJsonArray("path");
    long[] waitingPath = new long[path.size()];
    for (int i = 0; i < path.size(); i++) waitingPath[i] = path.get(i).getAsLong();
    return new Action(name, pose, actionType, targetBlockPos, radius, waitingPath);
  }

  public ActionType getActionType() { return this.actionType; }

  public BlockPos getTargetBlockPos() { return this.targetBlockPos; }

  public String getName() { return this.name; }

  public long[] getPath() { return this.path; }
  public CEditNpc.NPCPose getPose() { return this.pose; }

  public int getRadius() { return this.radius; }

  public void setPath(long[] path) {
    this.path = path;
    Main.LOGGER.info("Loaded path");
  }

  public CompoundTag toCompoundTag() {
    CompoundTag actionTag = new CompoundTag();
    actionTag.putString("actionType", String.valueOf(this.actionType));
    actionTag.putString("name", this.name);
    actionTag.putString("pose", this.pose.name());
    actionTag.put("targetBlockPos", NbtUtils.writeBlockPos(this.targetBlockPos));
    actionTag.putInt("radius", this.radius);
    actionTag.putLongArray("path", this.path);
    return actionTag;
  }

  public JsonObject toJSONObject() {
    JsonObject json = new JsonObject();
    json.addProperty("name", this.name);
    json.addProperty("pose", String.valueOf(this.pose));
    json.addProperty("actionType", String.valueOf(this.actionType));
    json.addProperty("targetBlockPos", this.targetBlockPos.asLong());
    json.addProperty("radius", this.radius);
    JsonArray pathArray = new JsonArray();
    for (long l : this.path) {
      pathArray.add(l);
    }
    json.add("path", pathArray);
    return json;
  }
}
