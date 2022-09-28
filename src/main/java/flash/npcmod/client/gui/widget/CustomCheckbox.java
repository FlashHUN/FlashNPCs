package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomCheckbox extends AbstractButton {
    private final ResourceLocation texture;
    private boolean selected;
    private final boolean showLabel;

    public CustomCheckbox(int p_93826_, int p_93827_, int p_93828_, int p_93829_, Component p_93830_, boolean p_93831_, ResourceLocation texture) {
        this(p_93826_, p_93827_, p_93828_, p_93829_, p_93830_, p_93831_, true, texture);
    }

    public CustomCheckbox(int p_93833_, int p_93834_, int p_93835_, int p_93836_, Component p_93837_, boolean p_93838_, boolean p_93839_, ResourceLocation texture) {
        super(p_93833_, p_93834_, p_93835_, p_93836_, p_93837_);
        this.texture = texture;
        this.selected = p_93838_;
        this.showLabel = p_93839_;
    }

    public void onPress() {
        this.selected = !this.selected;
    }

    public boolean selected() {
        return this.selected;
    }

    public void updateNarration(NarrationElementOutput p_168846_) {
        p_168846_.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                p_168846_.add(NarratedElementType.USAGE, new TranslatableComponent("narration.checkbox.usage.focused"));
            } else {
                p_168846_.add(NarratedElementType.USAGE, new TranslatableComponent("narration.checkbox.usage.hovered"));
            }
        }

    }

    public void renderButton(PoseStack p_93843_, int p_93844_, int p_93845_, float p_93846_) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableDepthTest();
        Font font = minecraft.font;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        blit(p_93843_, this.x, this.y, this.isHoveredOrFocused() ? width : 0.0F, this.selected ? height : 0.0F, width, height, 64, 64);
        this.renderBg(p_93843_, minecraft, p_93844_, p_93845_);
        if (this.showLabel) {
            drawString(p_93843_, font, this.getMessage(), this.x + 24, this.y + (this.height - 8) / 2, 14737632 | Mth.ceil(this.alpha * 255.0F) << 24);
        }

    }
}
