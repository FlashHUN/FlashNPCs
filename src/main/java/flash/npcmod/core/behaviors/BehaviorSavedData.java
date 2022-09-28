package flash.npcmod.core.behaviors;

import flash.npcmod.client.gui.behavior.Behavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class BehaviorSavedData extends SavedData {
    private Behavior[] behaviors;

    public static BehaviorSavedData create() {
        BehaviorSavedData savedData = new BehaviorSavedData();
        savedData.setBehaviors(new Behavior[0]);
        return savedData;
    }

    public Behavior[] getBehaviorSavedData() { return behaviors; }

    public static BehaviorSavedData getBehaviorSavedData(MinecraftServer server, String behaviorFile) {
        return server.overworld().getDataStorage().computeIfAbsent(
                BehaviorSavedData::load, BehaviorSavedData::create, "behaviors_" + behaviorFile);
    }

    public static BehaviorSavedData load(CompoundTag tag) {
        BehaviorSavedData data = create();
        // Load saved data
        data.setBehaviors(loadBehaviorTag(tag));
        return data;
    }

    /**
     * Save the loaded behaviors to the tag.
     * @param tag The tag to save to.
     * @return The saved tag.
     */
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        for (Behavior behavior: behaviors) {
            tag.put(behavior.getName(), behavior.toCompoundTag());
        }
        return tag;
    }

    /**
     * Set the Behaviors. MUST call setDirty() after calling this.
     * @param behaviors The behaviors to save.
     */
    public void setBehaviors(Behavior[] behaviors) { this.behaviors = behaviors; }

    private static Behavior[] loadBehaviorTag(CompoundTag tag) {
        Behavior[] behaviors = new Behavior[tag.size()];
        int count = 0;
        for (String name : tag.getAllKeys()) {
            behaviors[count++] = Behavior.fromCompoundTag(tag.getCompound(name));
        }
        return behaviors;
    }
}
