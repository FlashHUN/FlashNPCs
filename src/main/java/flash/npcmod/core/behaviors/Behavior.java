package flash.npcmod.core.behaviors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import flash.npcmod.core.node.NodeData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static flash.npcmod.core.behaviors.CommonBehaviorUtil.INVALID_BEHAVIOR;

@OnlyIn(Dist.CLIENT)
public class Behavior extends NodeData {
    public String dialogueName;
    private Action action;
    private final List<Trigger> triggers;

    public Behavior() {
        super(INVALID_BEHAVIOR, "", new Behavior[0]);
        this.dialogueName = "";
        this.action = new Action();
        this.triggers = new ArrayList<>();
    }

    public Behavior(String name, String dialogueName, Action action, String function, Trigger[] triggers, Behavior[] children) {
        super(name, function, children);
        this.dialogueName = dialogueName;
        this.action = action;
        this.triggers = new ArrayList<>();
        Collections.addAll(this.triggers, triggers);
    }

    @Override
    public void addChild(NodeData nodeData) {
        if (!nodeData.equals(this)) {
            if (!this.children.contains(nodeData)) {
                this.children.add(nodeData);

                this.triggers.add(new Trigger(getNextTriggerName(), Trigger.TriggerType.DIALOGUE_TRIGGER, 0, nodeData.getName()));
            }
        }
    }

    /**
     * Adds the Child to the list and prevents adding itself.
     *
     * @param nodeData The nodeData to add.
     */
    @Override
    public void addChild(NodeData nodeData, int index) {
        if (!nodeData.equals(this)) {
            if (!this.children.contains(nodeData)) {
                this.children.add(nodeData);
                if (index < this.triggers.size()) {
                    this.triggers.get(index).setNextBehaviorName(nodeData.getName());
                } else {
                    this.triggers.add(new Trigger(getNextTriggerName(), Trigger.TriggerType.DIALOGUE_TRIGGER, 0, nodeData.getName()));
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Behavior behavior = (Behavior) o;
        return name.equals(behavior.name) && dialogueName.equals(behavior.dialogueName) &&
                action.equals(behavior.action) && function.equals(behavior.function) &&
                children.equals(behavior.children);
    }

    public static Behavior fromCompoundTag(CompoundTag behaviorTag) {
        Action action = Action.fromCompound(behaviorTag.getCompound("action"));

        CompoundTag triggerTag = behaviorTag.getCompound("triggers");
        Trigger[] triggers = new Trigger[triggerTag.size()];
        int triggerCount = 0;
        for (String key: triggerTag.getAllKeys()) {
            triggers[triggerCount] = Trigger.fromCompoundTag(triggerTag.getCompound(key));
            triggerCount += 1;
        }
        CompoundTag childrenTags = behaviorTag.getCompound("children");
        Behavior[] children = new Behavior[childrenTags.size()];
        int childCount = 0;
        for (String key : childrenTags.getAllKeys()) {
            children[childCount++] = fromCompoundTag(childrenTags.getCompound(key));
        }

        return new Behavior(
            behaviorTag.getString("name"),
            behaviorTag.getString("dialogueName"),
            action,
            behaviorTag.getString("function"),
            triggers,
            children
        );
    }

    public static Behavior fromJSONObject(JsonObject object) {
        Behavior[] children = new Behavior[0];
        if (object.has("children")) {
            JsonArray currentChildren = object.getAsJsonArray("children");
            children = new Behavior[currentChildren.size()];
            for (int i = 0; i < currentChildren.size(); i++) {
                JsonObject currentChild = currentChildren.get(i).getAsJsonObject();
                Behavior childDialogue = fromJSONObject(currentChild);
                children[i] = childDialogue;
            }
        }

        String name = object.get("name").getAsString();
        String dialogueName = "";
        if (object.has("dialogueName")) {
            dialogueName = object.get("dialogueName").getAsString();
        }

        Action action;
        if (object.has("action")) {
            action = Action.fromJSONObject(object.getAsJsonObject("action"));
        } else {
            action = new Action();
        }

        String function = object.has("function") ? object.get("function").getAsString() : "";

        Trigger[] triggers = new Trigger[0];
        if (object.has("triggers")) {
            JsonArray array = object.getAsJsonArray("triggers");
            if (array != null) {
                triggers = new Trigger[array.size()];
                for (int i = 0; i < triggers.length; i++) {
                    triggers[i] = Trigger.fromJSONObject(array.get(i).getAsJsonObject());
                }
            }
        }

        return new Behavior(name, dialogueName, action, function, triggers, children);
    }

    public Action getAction() {
        return action;
    }

    public Behavior[] getChildren() {
        Behavior[] children = new Behavior[this.children.size()];
        int i = 0;
        for (NodeData child: this.children)
            children[i++] = (Behavior) child;
        return children;
    }

    public String getDialogueName() {
        return dialogueName;
    }


    /**
     * Generate a unique trigger name.
     * @return A unique String.
     */
    public String getNextTriggerName() {
        String name = "Trigger";
        List<String> names = this.triggers.stream().map(Trigger::getName).collect(Collectors.toList());
        int count = 2;
        while (names.contains(name)) {
            name = "Trigger " + count++;
        }
        return name;
    }

    public Trigger[] getTriggers() {
        return this.triggers.toArray(new Trigger[0]);
    }

    /**
     * Check if this behavior is invalid by checking the name.
     * @return True if invalid.
     */
    public boolean isInvalid() { return this.name.equals(INVALID_BEHAVIOR); }

    public static Behavior[] multipleFromJSONObject(JsonObject object) {
        if (object.has("entries")) {
            JsonArray entries = object.getAsJsonArray("entries");
            Behavior[] behaviors = new Behavior[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                behaviors[i] = fromJSONObject(entries.get(i).getAsJsonObject());
            }
            return behaviors;
        } else {
            return new Behavior[]{fromJSONObject(object)};
        }

    }

    public static Behavior newBehavior(BlockPos blockPos) {
        return new Behavior("newBehaviorNode", "", new Action(blockPos), "", new Trigger[0], new Behavior[0]);
    }


    @Override
    public void removeChild(NodeData nodeData) {
        if (isChild(nodeData)) {
            this.children.remove(nodeData);
        }
    }

    public void removeTrigger(int index) {
        if (index < 0 || index > this.triggers.size()) return;
        this.triggers.remove(index);
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void setDialogueName(String dialogueName) {
        this.dialogueName = dialogueName;
    }

    public void setTrigger(int i, Trigger trigger) {
        this.triggers.set(i, trigger);
    }

    public CompoundTag toCompoundTag() {
        CompoundTag behaviorTag = new CompoundTag();
        behaviorTag.putString("name", this.name);
        behaviorTag.putString("dialogueName", this.dialogueName);
        behaviorTag.putString("function", this.function);

        // Create children tag.
        CompoundTag childrenTag = new CompoundTag();
        for (NodeData child : this.children) {
            childrenTag.put(child.getName(), ((Behavior) child).toCompoundTag());
        }
        behaviorTag.put("children", childrenTag);
        // Create action tag.
        behaviorTag.put("action", action.toCompoundTag());
        // Create triggers tag.
        CompoundTag triggerTag = new CompoundTag();
        for (Trigger trigger : this.triggers)
            triggerTag.put(trigger.getName(), trigger.toCompoundTag());
        behaviorTag.put("triggers", triggerTag);
        return behaviorTag;
    }

    public JsonObject toJSON() {
        Trigger[] triggers = this.getTriggers();
        JsonArray triggersJson = new JsonArray();
        for (Trigger trigger : triggers) triggersJson.add(trigger.toJSONObject());
        JsonObject dialogueObject = super.toJSON();

        dialogueObject.addProperty("dialogueName", dialogueName);
        dialogueObject.add("triggers", triggersJson);
        dialogueObject.add("action", action.toJSONObject());
        return dialogueObject;
    }
}
