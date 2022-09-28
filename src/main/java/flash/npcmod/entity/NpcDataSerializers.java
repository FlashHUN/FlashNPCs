package flash.npcmod.entity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.client.gui.behavior.Behavior;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import org.jetbrains.annotations.NotNull;

public class NpcDataSerializers {
    public static final EntityDataSerializer<Behavior> BEHAVIOR = new EntityDataSerializer<Behavior>() {
        public void write(FriendlyByteBuf buffer, Behavior behavior) {
            buffer.writeUtf(behavior.toJSON().toString());
        }

        public @NotNull Behavior read(FriendlyByteBuf buffer) {
            return Behavior.fromJSONObject(new Gson().fromJson(buffer.readUtf(), JsonObject.class));
        }

        public @NotNull Behavior copy(@NotNull Behavior behavior) {
            return behavior;
        }
    };

    static {
        EntityDataSerializers.registerSerializer(BEHAVIOR);
    }
}
