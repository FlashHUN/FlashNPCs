package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/**
 * A special widget that is used in the HorizontalFrame to create space between widgets.
 */
public class TextWidget extends AbstractWidget {
    public TextWidget(String s) {
        super(0, 0, Minecraft.getInstance().font.width(s), 20, new TextComponent(s));
    }
    public TextWidget(int x, int y, String s) {
        super(x, y, Minecraft.getInstance().font.width(s), 20, new TextComponent(s));
    }

    public static void drawCenteredString(PoseStack poseStack, Font font, Component component, int x, int y, int alpha) {
        FormattedCharSequence formattedcharsequence = component.getVisualOrderText();
        font.drawShadow(poseStack, formattedcharsequence, (float)(x - font.width(formattedcharsequence) / 2), (float)y, alpha);
    }

    @Override
    public void render(PoseStack poseStack, int x, int y, float alpha) {
        if (this.visible) {
            Minecraft minecraft = Minecraft.getInstance();
            int j = getFGColor();
            drawCenteredString(poseStack, minecraft.font, this.getMessage(), this.x + this.width / 2, (this.y + (this.height - 8) / 2), j | Mth.ceil(this.alpha * 255.0F) << 24);
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput narration) {

    }
}
