package flash.npcmod.client.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CBuildFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public class FunctionBuilderScreen extends Screen {

  private String name;
  private String parameterNames;
  private String callable;
  private final List<String> callables;

  private TextFieldWidget nameField;
  private TextFieldWidget parametersField;
  private TextFieldWidget callableField;
  private Button addCallableButton;
  private Button confirmButton;
  private Button[] removeCallableButtons;

  private static int maxCallables;
  private static final int lineHeight = Minecraft.getInstance().fontRenderer.FONT_HEIGHT+2;

  private static final String NAME = "Name: ", PARAMNAMES = "Parameter Names: ", RUNNABLES = "Runnables: ";

  private int scrollY;

  private final Predicate<String> paramFilter = (text) -> {
    Pattern pattern = Pattern.compile("\\s");
    Matcher matcher = pattern.matcher(text);
    return !matcher.find();
  };

  public FunctionBuilderScreen() {
    super(StringTextComponent.EMPTY);
    this.callables = new ArrayList<>();
    this.name = "";
    this.callable = "";
    this.parameterNames = "";
  }

  @Override
  protected void init() {
    this.nameField = this.addButton(new TextFieldWidget(font, 5+font.getStringWidth(NAME), 5, 100, 20, StringTextComponent.EMPTY));
    this.nameField.setResponder(this::setName);
    this.nameField.setValidator(paramFilter);
    this.nameField.setMaxStringLength(50);
    this.nameField.setText(this.name);

    this.parametersField = this.addButton(new TextFieldWidget(font, 5+font.getStringWidth(PARAMNAMES), 30, 100, 20, StringTextComponent.EMPTY));
    this.parametersField.setResponder(this::setParameterNames);
    this.parametersField.setValidator(paramFilter);
    this.parametersField.setMaxStringLength(100);
    this.parametersField.setText(this.parameterNames);

    this.callableField = this.addButton(new TextFieldWidget(font, 5+font.getStringWidth(RUNNABLES), 55, 100, 20, StringTextComponent.EMPTY));
    this.callableField.setResponder(this::setCallable);
    this.callableField.setMaxStringLength(250);
    this.callableField.setText("");

    this.addCallableButton = this.addButton(new Button(115+font.getStringWidth(RUNNABLES), 55, 20, 20, new StringTextComponent("+"), btn -> {
      if (canAddCallable())
        this.callables.add(callable);
      this.callableField.setText("");
    }));
    this.addCallableButton.active = this.canAddCallable();

    this.confirmButton = this.addButton(new Button(width-50, height-20, 50, 20, new StringTextComponent("Confirm"), btn -> {
      PacketDispatcher.sendToServer(new CBuildFunction(this.name, build()));
      closeScreen();
    }));
    this.confirmButton.active = this.name.length() > 0 && this.callables.size() > 0;

    maxCallables = (height-110)/lineHeight;
    removeCallableButtons = new Button[maxCallables];
    for (int i = 0; i < maxCallables; i++) {
      int j = i;
      removeCallableButtons[i] = this.addButton(new Button(5, 78+i*lineHeight, 11, 11, new StringTextComponent("-"), btn -> {
        this.removeCallable(j);
      }));
      removeCallableButtons[i].visible = false;
    }
  }

  private boolean canAddCallable() {
    return this.callable.startsWith("function:") || this.callable.startsWith("/");
  }

  private void removeCallable(int i) {
    if (i+scrollY < this.callables.size()) {
      this.callables.remove(i+scrollY);
      scrollY = clampScroll(scrollY-1);
    }
  }

  private void setName(String s) {
    this.name = s;
  }

  private void setParameterNames(String s) {
    this.parameterNames = s;
  }

  private void setCallable(String s) {
    this.callable = s;
  }

  @Override
  public void tick() {
    this.nameField.tick();
    this.parametersField.tick();
    this.callableField.tick();

    this.addCallableButton.active = canAddCallable();

    this.confirmButton.active = this.name.length() > 0 && this.callables.size() > 0;

    for (int i = 0; i < removeCallableButtons.length; i++) {
      removeCallableButtons[i].visible = callables.size() > i+scrollY;
    }
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);

    int center = (20-font.FONT_HEIGHT)/2;
    drawString(matrixStack, font, NAME, 5, 5+center, 0xFFFFFF);
    drawString(matrixStack, font, PARAMNAMES, 5, 30+center, 0xFFFFFF);
    drawString(matrixStack, font, RUNNABLES, 5, 55+center, 0xFFFFFF);

    int size = this.callables.size();
    for (int i = 0; i < Math.min(size, maxCallables); i++) {
      if (callables.size() > i+scrollY)
        drawString(matrixStack, font, callables.get(i+scrollY), 20, 80 + i * lineHeight, 0xFFFFFF);
    }

    super.render(matrixStack, mouseX, mouseY, partialTicks);
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
    int max = this.callables.size()-maxCallables;
    if (max > 0)
      return MathHelper.clamp(newScroll, 0, max);
    else
      return 0;
  }

  private String build() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.parameterNames).append("\n");
    for (int i = 0; i < this.callables.size(); i++) {
      sb.append(this.callables.get(i));
      if (i < this.callables.size()-1) {
        sb.append("\n");
      }
    }
    return sb.toString();
  }
}
