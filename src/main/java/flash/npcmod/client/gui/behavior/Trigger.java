package flash.npcmod.client.gui.behavior;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Trigger {
    private String name;
    private String nextBehaviorName;
    private final int timer;

    public enum TriggerType {
        DIALOGUE_TRIGGER,
        ACTION_FINISH_TRIGGER,
        TIMER_TRIGGER,
        //DAYTIME_TRIGGER
    }

    private final TriggerType type;

    public Trigger(String name, TriggerType type, int timer, String nextBehaviorName) {
        this.name = name;
        this.type = type;
        this.nextBehaviorName = nextBehaviorName;
        this.timer = timer;
    }

    /**
     * Compare the equality of this object.
     *
     * @param o The comparison object.
     * @return boolean.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trigger trigger = (Trigger) o;
        return name.equals(trigger.name) && this.type.equals(trigger.type) && this.timer == trigger.timer &&
                this.nextBehaviorName.equals(trigger.nextBehaviorName);
    }

    /**
     * Create a new Trigger from a compound tag.
     *
     * @param object The compound tag.
     * @return The new trigger object.
     */
    public static Trigger fromCompoundTag(CompoundTag object) {
        String name = object.getString("name");
        String type = object.getString("type");
        String nextBehaviorName = object.getString("nextBehaviorName");
        int timer = object.getInt("timer");

        return new Trigger(name, TriggerType.valueOf(type), timer, nextBehaviorName);
    }

    /**
     * Create a new Trigger from a Json Object.
     *
     * @param object The Json object.
     * @return The new Trigger.
     */
    public static Trigger fromJSONObject(JsonObject object) {
        String name = object.get("name").getAsString();
        String type = object.get("type").getAsString();
        int timer = object.get("timer").getAsInt();
        String nextBehaviorName = object.get("nextBehaviorName").getAsString();

        return new Trigger(name, TriggerType.valueOf(type), timer, nextBehaviorName);
    }

    /**
     * Get the name of the trigger.
     *
     * @return The trigger name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * The timer for the type TIMER_TRIGGER.
     * @return The timer.
     */
    public int getTimer() {
        return this.timer;
    }

    /**
     * Get the trigger type.
     *
     * @return The trigger type.
     */
    public TriggerType getType() {
        return this.type;
    }

    /**
     * Get the behavior that this trigger points to.
     *
     * @return The behavior.
     */
    public String getNextBehaviorName() {
        return this.nextBehaviorName;
    }

    /**
     * Set the name of this trigger.
     *
     * @param name The trigger name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the behavior this trigger points to.
     *
     * @param nextBehaviorName The next behavior.
     */
    public void setNextBehaviorName(String nextBehaviorName) {
        this.nextBehaviorName = nextBehaviorName;
    }

    /**
     * Convert this to a json object.
     *
     * @return The new json object.
     */
    public JsonObject toJSONObject() {
        JsonObject json = new JsonObject();
        json.addProperty("name", this.name);
        json.addProperty("timer", this.timer);
        json.addProperty("type", this.type.toString());
        json.addProperty("nextBehaviorName", this.nextBehaviorName);
        return json;
    }

    /**
     * Convert this to a compound tag.
     *
     * @return The new compound tag.
     */
    public CompoundTag toCompoundTag() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("name", this.name);
        compoundTag.putInt("timer", timer);
        compoundTag.putString("type", this.type.toString());
        compoundTag.putString("nextBehaviorName", this.nextBehaviorName);
        return compoundTag;
    }
}
