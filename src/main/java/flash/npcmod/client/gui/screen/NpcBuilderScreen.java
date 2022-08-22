package flash.npcmod.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.client.gui.widget.ColorSliderWidget;
import flash.npcmod.client.gui.widget.EnumDropdownWidget;
import flash.npcmod.core.ColorUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CEditNpc;
import flash.npcmod.network.packets.client.CRequestContainer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public class NpcBuilderScreen extends Screen {

  private final NpcEntity npcEntity;
  private String name;
  private String dialogue;
  private String behavior;
  private int textColor;
  private String texture;
  private boolean isSlim, isNameVisible;
  private final ItemStack[] items;

  public EditBox redField, greenField, blueField;
  private Checkbox slimCheckBox, nameVisibleCheckbox;
  private ColorSliderWidget redSlider, greenSlider, blueSlider;
  private EnumDropdownWidget<CEditNpc.NPCPose> poseDropdown;

  private int r, g, b;
  private CEditNpc.NPCPose pose;

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
        return i >= 0 && i <= 255;
      } catch (NumberFormatException e) {
        return false;
      }
    }
  };

  public NpcBuilderScreen(NpcEntity npcEntity) {
    super(TextComponent.EMPTY);

    this.npcEntity = npcEntity;

    this.pose = npcEntity.isCrouching() ? CEditNpc.NPCPose.CROUCHING : npcEntity.isSitting() ? CEditNpc.NPCPose.SITTING : CEditNpc.NPCPose.STANDING;
    this.name = npcEntity.getName().getString();
    this.isNameVisible = npcEntity.isCustomNameVisible();
    this.texture = npcEntity.getTexture();
    this.isSlim = npcEntity.isSlim();
    this.dialogue = npcEntity.getDialogue();
    this.behavior = npcEntity.getBehaviorFile();
    this.textColor = npcEntity.getTextColor();
    this.items = new ItemStack[]{npcEntity.getMainHandItem(), npcEntity.getOffhandItem(),
        npcEntity.getItemBySlot(EquipmentSlot.HEAD), npcEntity.getItemBySlot(EquipmentSlot.CHEST),
        npcEntity.getItemBySlot(EquipmentSlot.LEGS), npcEntity.getItemBySlot(EquipmentSlot.FEET)};

    this.r = ColorUtil.hexToR(textColor);
    this.g = ColorUtil.hexToG(textColor);
    this.b = ColorUtil.hexToB(textColor);
  }

  @Override
  protected void init() {
    minX = 5 + Math.max(
        Math.max(
            Math.max(font.width("Name: "), font.width("Texture: ")),
            Math.max(font.width("Dialogue: "), font.width("Text Color: "))),
        font.width("Behavior: ")    );
    EditBox nameField = this.addRenderableWidget(new EditBox(font, minX, 5, 120, 20, TextComponent.EMPTY));
    nameField.setResponder(this::setName);
    nameField.setMaxLength(200);
    nameField.setValue(this.name);

    this.nameVisibleCheckbox = this.addRenderableWidget(new Checkbox(minX + 130 + font.width("Visible? "), 5, 20, 20, TextComponent.EMPTY, isNameVisible));

    EditBox textureField = this.addRenderableWidget(new EditBox(font, minX, 30, 120, 20, TextComponent.EMPTY));
    textureField.setResponder(this::setTexture);
    textureField.setFilter(textFilter);
    textureField.setMaxLength(200);
    textureField.setValue(this.texture);

    EditBox dialogueField = this.addRenderableWidget(new EditBox(font, minX, 55, 120, 20, TextComponent.EMPTY));
    dialogueField.setResponder(this::setDialogue);
    dialogueField.setFilter(textFilter);
    dialogueField.setMaxLength(200);
    dialogueField.setValue(this.dialogue);

    EditBox behaviorField = this.addRenderableWidget(new EditBox(font, minX, 80, 120, 20, TextComponent.EMPTY));
    behaviorField.setResponder(this::setBehavior);
    behaviorField.setFilter(textFilter);
    behaviorField.setMaxLength(200);
    behaviorField.setValue(this.behavior);

    this.slimCheckBox = this.addRenderableWidget(new Checkbox(minX + 130 + font.width("Slim? "), 30, 20, 20, TextComponent.EMPTY, isSlim));

    this.redSlider = this.addRenderableWidget(new ColorSliderWidget(this, minX, 105, 20, 100, ColorSliderWidget.Color.RED));
    this.greenSlider = this.addRenderableWidget(new ColorSliderWidget(this, minX + 30, 105, 20, 100, ColorSliderWidget.Color.GREEN));
    this.blueSlider = this.addRenderableWidget(new ColorSliderWidget(this, minX + 60, 105, 20, 100, ColorSliderWidget.Color.BLUE));

    this.redField = this.addRenderableWidget(new EditBox(font, minX - 5, 210, 30, 20, TextComponent.EMPTY));
    this.redField.setResponder(this::setRFromString);
    this.redField.setFilter(colorFilter);
    this.redField.setMaxLength(3);
    this.redField.setValue(String.valueOf(r));

    this.greenField = this.addRenderableWidget(new EditBox(font, minX + 25, 210, 30, 20, TextComponent.EMPTY));
    this.greenField.setResponder(this::setGFromString);
    this.greenField.setFilter(colorFilter);
    this.greenField.setMaxLength(3);
    this.greenField.setValue(String.valueOf(g));

    this.blueField = this.addRenderableWidget(new EditBox(font, minX + 55, 210, 30, 20, TextComponent.EMPTY));
    this.blueField.setResponder(this::setBFromString);
    this.blueField.setFilter(colorFilter);
    this.blueField.setMaxLength(3);
    this.blueField.setValue(String.valueOf(b));

    // Confirm Button
    this.addRenderableWidget(new Button(width - 60, height - 20, 60, 20, new TextComponent("Confirm"), btn -> {
      PacketDispatcher.sendToServer(
              new CEditNpc(this.npcEntity.getId(), this.isNameVisible, this.name, this.texture, this.isSlim, this.dialogue,
                      this.behavior, this.textColor, this.items, this.pose));
      minecraft.setScreen(null);
    }));

    // Inventory Button
    this.addRenderableWidget(new Button(width - 60, height - 40, 60, 20, new TextComponent("Inventory"), btn -> {
      PacketDispatcher.sendToServer(
              new CEditNpc(this.npcEntity.getId(), this.isNameVisible, this.name, this.texture, this.isSlim, this.dialogue,
                      this.behavior, this.textColor, this.items, this.pose));
      PacketDispatcher.sendToServer(new CRequestContainer(this.npcEntity.getId(), CRequestContainer.ContainerType.NPCINVENTORY));
    }));

    // Trades Button
    this.addRenderableWidget(new Button(width - 60, height - 60, 60, 20, new TextComponent("Trades"), btn -> {
      PacketDispatcher.sendToServer(
              new CEditNpc(this.npcEntity.getId(), this.isNameVisible, this.name, this.texture, this.isSlim, this.dialogue,
                      this.behavior, this.textColor, this.items, this.pose));
      PacketDispatcher.sendToServer(new CRequestContainer(this.npcEntity.getId(), CRequestContainer.ContainerType.TRADE_EDITOR));
    }));

    // Reset Behavior Button
    this.addRenderableWidget(new Button(width - 60, height - 80, 60, 20, new TextComponent("Reset AI"), btn -> {
      PacketDispatcher.sendToServer(
              new CEditNpc(this.npcEntity.getId(), this.isNameVisible, this.name, this.texture, this.isSlim, this.dialogue,
                      this.behavior, this.textColor, this.items, this.pose, true));
    }));

    this.poseDropdown = this.addRenderableWidget(new EnumDropdownWidget<>(this.pose, minX + 210, 5, 80));
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

  private void setBehavior(String s) {
    this.behavior = s;
  }

  public int getR() {
    return r;
  }

  public void setR(int i) {
    r = Mth.clamp(i, 0, 255);
    this.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  private void setRFromString(String s) {
    if (s.isEmpty()) {
      r = 0;
    } else {
      try {
        r = Mth.clamp(Integer.parseInt(s), 0, 255);
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
    g = Mth.clamp(i, 0, 255);
    this.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  private void setGFromString(String s) {
    if (s.isEmpty()) {
      g = 0;
    } else {
      try {
        g = Mth.clamp(Integer.parseInt(s), 0, 255);
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
    b = Mth.clamp(i, 0, 255);
    this.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  private void setBFromString(String s) {
    if (s.isEmpty()) {
      b = 0;
    } else {
      try {
        b = Mth.clamp(Integer.parseInt(s), 0, 255);
      } catch (NumberFormatException e) {
      }
    }
    blueSlider.updateColorY();
    this.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  @Override
  public void tick() {
//    this.nameField.tick();
//    this.textureField.tick();
//    this.dialogueField.tick();
//    this.behaviorField.tick();
//    this.redField.tick();
//    this.greenField.tick();
//    this.blueField.tick();
    this.isSlim = slimCheckBox.selected();
    npcEntity.setSlim(isSlim);
    this.isNameVisible = nameVisibleCheckbox.selected();
    npcEntity.setCustomNameVisible(isNameVisible);

    if (poseDropdown.getSelectedOption() != this.pose) {
      this.pose = poseDropdown.getSelectedOption();
      switch (pose) {
        case CROUCHING -> { npcEntity.setCrouching(true); npcEntity.setSitting(false); }
        case SITTING -> { npcEntity.setCrouching(false); npcEntity.setSitting(true); }
        case STANDING -> { npcEntity.setCrouching(false); npcEntity.setSitting(false); }
      }
    }
  }

  @Override
  public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);

    // Render the NPC on screen
    {
      int x = width - 60;
      int y = height / 4 * 3;
      int scale = height / 3;
      InventoryScreen.renderEntityInInventory(x, y, scale, 40, -20, npcEntity);
    }

    int center = (20 - font.lineHeight) / 2;
    drawString(matrixStack, font, "Name: ", 5, 5 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Visible? ", minX + 130, 5 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Texture: ", 5, 30 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Slim? ", minX + 130, 30 + center, 0xFFFFFF);

    drawString(matrixStack, font, "Dialogue: ", 5, 55 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Behavior: ", 5, 80 + center, 0xFFFFFF);

    drawString(matrixStack, font, "Text Color: ", 5, 105 + (100 - font.lineHeight) / 2, 0xFFFFFF);

    fill(matrixStack, minX + 99, 121, minX + 126, 147, 0xFF000000);
    fill(matrixStack, minX + 100, 122, minX + 125, 146, ColorUtil.hexToHexA(textColor));

    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }
}