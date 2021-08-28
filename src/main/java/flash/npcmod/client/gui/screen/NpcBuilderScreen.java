package flash.npcmod.client.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import flash.npcmod.client.gui.widget.ColorSliderWidget;
import flash.npcmod.core.ColorUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CEditNpc;
import flash.npcmod.network.packets.client.CRequestContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public class NpcBuilderScreen extends Screen {

  private NpcEntity npcEntity;
  private String name;
  private String dialogue;
  private int textColor;
  private String texture;
  private boolean isSlim, isNameVisible;
  private ItemStack[] items;

  private TextFieldWidget nameField, dialogueField, textureField;
  public TextFieldWidget redField, greenField, blueField;
  private CheckboxButton slimCheckBox, nameVisibleCheckbox;
  private Button confirmButton, inventoryButton, tradesButton;
  private ColorSliderWidget redSlider, greenSlider, blueSlider;

  private int r, g, b;

  private static int minX;

  private final Predicate<String> textFilter = (text) -> {
    Pattern pattern = Pattern.compile("\\s");
    Matcher matcher = pattern.matcher(text);
    return !matcher.find();
  };

  private final Predicate<String> colorFilter = (text) -> {
    if (text.isEmpty()) {
      return true;
    } else {
      try {
        int i = Integer.parseInt(text);
        if (i >= 0 && i <= 255) {
          return true;
        } else {
          return false;
        }
      } catch (NumberFormatException e) {
        return false;
      }
    }
  };

  public NpcBuilderScreen(NpcEntity npcEntity) {
    super(StringTextComponent.EMPTY);

    this.npcEntity = npcEntity;

    this.name = npcEntity.getName().getString();
    this.isNameVisible = npcEntity.isCustomNameVisible();
    this.texture = npcEntity.getTexture();
    this.isSlim = npcEntity.isSlim();
    this.dialogue = npcEntity.getDialogue();
    this.textColor = npcEntity.getTextColor();
    this.items = new ItemStack[]{npcEntity.getHeldItemMainhand(), npcEntity.getHeldItemOffhand(),
        npcEntity.getItemStackFromSlot(EquipmentSlotType.HEAD), npcEntity.getItemStackFromSlot(EquipmentSlotType.CHEST),
        npcEntity.getItemStackFromSlot(EquipmentSlotType.LEGS), npcEntity.getItemStackFromSlot(EquipmentSlotType.FEET)};

    this.r = ColorUtil.hexToR(textColor);
    this.g = ColorUtil.hexToG(textColor);
    this.b = ColorUtil.hexToB(textColor);
  }

  @Override
  protected void init() {
    minX = 5 + Math.max(Math.max(font.getStringWidth("Name: "), font.getStringWidth("Texture: ")), Math.max(font.getStringWidth("Dialogue: "), font.getStringWidth("Text Color: ")));
    this.nameField = this.addButton(new TextFieldWidget(font, minX, 5, 120, 20, StringTextComponent.EMPTY));
    this.nameField.setResponder(this::setName);
    this.nameField.setMaxStringLength(200);
    this.nameField.setText(this.name);

    this.nameVisibleCheckbox = this.addButton(new CheckboxButton(minX + 130 + font.getStringWidth("Visible? "), 5, 20, 20, StringTextComponent.EMPTY, isNameVisible));

    this.textureField = this.addButton(new TextFieldWidget(font, minX, 30, 120, 20, StringTextComponent.EMPTY));
    this.textureField.setResponder(this::setTexture);
    this.textureField.setValidator(textFilter);
    this.textureField.setMaxStringLength(200);
    this.textureField.setText(this.texture);

    this.dialogueField = this.addButton(new TextFieldWidget(font, minX, 55, 120, 20, StringTextComponent.EMPTY));
    this.dialogueField.setResponder(this::setDialogue);
    this.dialogueField.setValidator(textFilter);
    this.dialogueField.setMaxStringLength(200);
    this.dialogueField.setText(this.dialogue);

    this.slimCheckBox = this.addButton(new CheckboxButton(minX + 130 + font.getStringWidth("Slim? "), 30, 20, 20, StringTextComponent.EMPTY, isSlim));

    this.redSlider = this.addButton(new ColorSliderWidget(this, minX, 85, 20, 100, ColorSliderWidget.Color.RED));
    this.greenSlider = this.addButton(new ColorSliderWidget(this, minX + 30, 85, 20, 100, ColorSliderWidget.Color.GREEN));
    this.blueSlider = this.addButton(new ColorSliderWidget(this, minX + 60, 85, 20, 100, ColorSliderWidget.Color.BLUE));

    this.redField = this.addButton(new TextFieldWidget(font, minX - 5, 190, 30, 20, StringTextComponent.EMPTY));
    this.redField.setResponder(this::setRFromString);
    this.redField.setValidator(colorFilter);
    this.redField.setMaxStringLength(3);
    this.redField.setText(String.valueOf(r));

    this.greenField = this.addButton(new TextFieldWidget(font, minX + 25, 190, 30, 20, StringTextComponent.EMPTY));
    this.greenField.setResponder(this::setGFromString);
    this.greenField.setValidator(colorFilter);
    this.greenField.setMaxStringLength(3);
    this.greenField.setText(String.valueOf(g));

    this.blueField = this.addButton(new TextFieldWidget(font, minX + 55, 190, 30, 20, StringTextComponent.EMPTY));
    this.blueField.setResponder(this::setBFromString);
    this.blueField.setValidator(colorFilter);
    this.blueField.setMaxStringLength(3);
    this.blueField.setText(String.valueOf(b));

    this.confirmButton = this.addButton(new Button(width - 60, height - 20, 60, 20, new StringTextComponent("Confirm"), btn -> {
      PacketDispatcher.sendToServer(new CEditNpc(this.npcEntity.getEntityId(), this.isNameVisible, this.name, this.texture, this.isSlim, this.dialogue, this.textColor, this.items));
      minecraft.displayGuiScreen(null);
    }));

    this.inventoryButton = this.addButton(new Button(width - 60, height - 40, 60, 20, new StringTextComponent("Inventory"), btn -> {
      PacketDispatcher.sendToServer(new CEditNpc(this.npcEntity.getEntityId(), this.isNameVisible, this.name, this.texture, this.isSlim, this.dialogue, this.textColor, this.items));
      PacketDispatcher.sendToServer(new CRequestContainer(this.npcEntity.getEntityId(), CRequestContainer.ContainerType.NPCINVENTORY));
    }));

    this.tradesButton = this.addButton(new Button(width - 60, height - 60, 60, 20, new StringTextComponent("Trades"), btn -> {
      PacketDispatcher.sendToServer(new CEditNpc(this.npcEntity.getEntityId(), this.isNameVisible, this.name, this.texture, this.isSlim, this.dialogue, this.textColor, this.items));
      PacketDispatcher.sendToServer(new CRequestContainer(this.npcEntity.getEntityId(), CRequestContainer.ContainerType.TRADE_EDITOR));
    }));
  }

  private void setName(String s) {
    this.name = s;
  }

  private void setTexture(String s) {
    this.texture = s;
    npcEntity.setTexture(s);
  }

  private void setDialogue(String s) {
    this.dialogue = s;
  }

  public int getR() {
    return r;
  }

  public void setR(int i) {
    r = MathHelper.clamp(i, 0, 255);
    this.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  private void setRFromString(String s) {
    if (s.isEmpty()) {
      r = 0;
    } else {
      try {
        r = MathHelper.clamp(Integer.parseInt(s), 0, 255);
      } catch (NumberFormatException e) {
      }
    }
    redSlider.updateColorY();
    this.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  public int getG() {
    return g;
  }

  public void setG(int i) {
    g = MathHelper.clamp(i, 0, 255);
    this.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  private void setGFromString(String s) {
    if (s.isEmpty()) {
      g = 0;
    } else {
      try {
        g = MathHelper.clamp(Integer.parseInt(s), 0, 255);
      } catch (NumberFormatException e) {
      }
    }
    greenSlider.updateColorY();
    this.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  public int getB() {
    return b;
  }

  public void setB(int i) {
    b = MathHelper.clamp(i, 0, 255);
    this.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  private void setBFromString(String s) {
    if (s.isEmpty()) {
      b = 0;
    } else {
      try {
        b = MathHelper.clamp(Integer.parseInt(s), 0, 255);
      } catch (NumberFormatException e) {
      }
    }
    blueSlider.updateColorY();
    this.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  @Override
  public void tick() {
    this.isSlim = slimCheckBox.isChecked();
    npcEntity.setSlim(isSlim);
    this.isNameVisible = nameVisibleCheckbox.isChecked();
    npcEntity.setCustomNameVisible(isNameVisible);
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);

    // Render the NPC on screen
    {
      int x = width - 60;
      int y = height / 4 * 3;
      int scale = height / 3;
      InventoryScreen.drawEntityOnScreen(x, y, scale, 40, -20, npcEntity);
    }

    int center = (20 - font.FONT_HEIGHT) / 2;
    drawString(matrixStack, font, "Name: ", 5, 5 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Visible? ", minX + 130, 5 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Texture: ", 5, 30 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Slim? ", minX + 130, 30 + center, 0xFFFFFF);

    drawString(matrixStack, font, "Dialogue: ", 5, 55 + center, 0xFFFFFF);

    drawString(matrixStack, font, "Text Color: ", 5, 85 + (100 - font.FONT_HEIGHT) / 2, 0xFFFFFF);

    fill(matrixStack, minX + 99, 121, minX + 126, 147, 0xFF000000);
    fill(matrixStack, minX + 100, 122, minX + 125, 146, ColorUtil.hexToHexA(textColor));

    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }
}