package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;

/**
 * A special widget that is used in the HorizontalFrame to create space between widgets.
 */
public class SpacerWidget extends AbstractWidget {
    public SpacerWidget(int x, int y) {
        super(x, y, 0, 0, new TextComponent(""));
    }

    @Override
    public void render(PoseStack poseStack, int x, int y, float alpha) {
    }

    @Override
    public void updateNarration(NarrationElementOutput narration) {

    }
}
