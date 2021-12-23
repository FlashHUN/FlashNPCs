package flash.npcmod.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import flash.npcmod.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.TextComponent;

public class DropdownWidget<T extends Enum<T>> extends AbstractWidget {

  private static final Minecraft minecraft = Minecraft.getInstance();

  private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID, "textures/gui/quest_objective_builder.png");

  T myEnum;
  Enum selectedOption;
  Enum[] enumConstants;

  private boolean showOptions;
  private int scrollY;
  private final int maxDisplayedOptions;

  public DropdownWidget(T defaultOption, int x, int y, int width) {
    this(defaultOption, x, y, width, 0);
  }

  public DropdownWidget(T defaultOption, int x, int y, int width, int maxDisplayedOptions) {
    super(x, y, Mth.clamp(width, 0, 200), 13, new TextComponent(defaultOption.name()));
    myEnum = defaultOption;
    selectedOption = defaultOption;
    enumConstants = myEnum.getClass().getEnumConstants();
    this.maxDisplayedOptions = maxDisplayedOptions == 0 ? (minecraft.getWindow().getGuiScaledHeight() - (y + 13 + enumConstants.length*13)) / 13 : Math.abs(maxDisplayedOptions);
  }

  @Override
  public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    Font fontrenderer = minecraft.font;
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.enableDepthTest();
    int maxTextWidth = width-8;
    // draw main widget
    {
      int i = this.getYImage(this.isHoveredOrFocused());
      String name;
      if (fontrenderer.width(this.getMessage().getString()) > maxTextWidth)
        name = fontrenderer.plainSubstrByWidth(this.getMessage().getString(), maxTextWidth-fontrenderer.width("...")) + "...";
      else
        name = this.getMessage().getString();
      drawOption(matrixStack, x, y, i, mouseX, mouseY, name);
      RenderSystem.setShaderTexture(0, TEXTURE);
      blit(matrixStack, x + width, y, 200 + (this.showOptions ? 15 : 0), i * 13, 15, 13);
    }
    // draw options
    if (showOptions) {
      for (int i = 0; i < maxDisplayedOptions(); i++) {
        int j = Mth.clamp(i+scrollY, 0, enumConstants.length);
        String name;
        if (fontrenderer.width(enumConstants[j].name()) > maxTextWidth)
          name = fontrenderer.plainSubstrByWidth(enumConstants[j].name(), maxTextWidth-fontrenderer.width("...")) + "...";
        else
          name = enumConstants[j].name();
        drawOption(matrixStack, x, y+13+i*13, this.getYImage(isMouseOverOption(i, mouseX, mouseY)), mouseX, mouseY, name);
      }
    }
  }

  private int maxDisplayedOptions() {
    return Math.min(enumConstants.length, maxDisplayedOptions);
  }

  private void drawOption(PoseStack matrixStack, int x, int y, int yImage, int mouseX, int mouseY, String text) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
    RenderSystem.setShaderTexture(0, TEXTURE);
    this.blit(matrixStack, x, y, 0, yImage * 13, this.width / 2, 13);
    this.blit(matrixStack, x + this.width / 2, y, 200 - this.width / 2, yImage * 13, this.width / 2, 13);
    this.renderBg(matrixStack, minecraft, mouseX, mouseY);
    int j = getFGColor();
    drawCenteredString(matrixStack, minecraft.font, text, x + this.width / 2, y + 6 / 2, j | Mth.ceil(this.alpha * 255.0F) << 24);
  }

  private boolean isMouseOverOption(int index, double mouseX, double mouseY) {
    double minY = this.y+13+index*13;
    return mouseX >= x && mouseX <= x+width && mouseY >= minY && mouseY <= minY+13;
  }

  private boolean isMouseOverAnyOption(double mouseX, double mouseY) {
    return mouseX >= x && mouseX <= x+width && mouseY >= this.y+13 && mouseY <= this.y+height;
  }

  private void selectOption(int i) {
    i = Mth.clamp(i+scrollY, 0, enumConstants.length);
    this.selectedOption = enumConstants[i];
    this.setMessage(new TextComponent(enumConstants[i].name()));
    this.setShowOptions(false);
  }

  public T getSelectedOption() {
    return (T) Enum.valueOf(myEnum.getClass(), selectedOption.name());
  }

  private void setShowOptions(boolean b) {
    this.showOptions = b;
    if (!showOptions) {
      this.height = 13;
      this.setFocused(false);
    } else {
      this.height = 13+maxDisplayedOptions()*13;
      this.setFocused(true);
    }
  }

  public boolean isShowingOptions() {
    return showOptions;
  }

  @Override
  public void onClick(double mouseX, double mouseY) {
    if (!this.showOptions) setShowOptions(true);
    else if (!isMouseOverAnyOption(mouseX, mouseY)) setShowOptions(false);
    else {
      for (int i = 0; i < maxDisplayedOptions(); i++) {
        double minY = this.y+13+i*13;
        if (mouseY >= minY && mouseY <= minY+13) {
          selectOption(i);
          break;
        }
      }
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (delta > 0) {
      this.scrollY = clampScroll(scrollY - 1);
    } else {
      this.scrollY = clampScroll(scrollY + 1);
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  public int clampScroll(int newScroll) {
    int max = enumConstants.length-maxDisplayedOptions;
    if (max > 0)
      return Mth.clamp(newScroll, 0, max);
    else
      return scrollY;
  }

  @Override
  public void updateNarration(NarrationElementOutput p_169152_) {
    // TODO?
  }
}
