package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

/**
 * A special widget that is used in the DirectionalFrame to create space between widgets. This will override any
 * Alignment of the Frame and will instead expand to add all available empty space to itself, effectively pushing
 * all widgets away from itself. If multiple Spacers are used within the same DirectionalFrame, then the available
 * size will be split evenly among the Spacers.
 * @see DirectionalFrame
 */
public class SpacerWidget extends AbstractWidget {
    public SpacerWidget(int x, int y) {
        super(x, y, 0, 0, new TextComponent(""));
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int x, int y, float alpha) {
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput narration) {

    }
}
