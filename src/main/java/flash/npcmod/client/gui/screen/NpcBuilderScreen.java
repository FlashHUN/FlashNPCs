package flash.npcmod.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.client.gui.widget.ColorSliderWidget;
import flash.npcmod.client.gui.widget.CustomCheckbox;
import flash.npcmod.client.gui.widget.EntityDropdownWidget;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
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

  private static final ResourceLocation CHAIN_LINK_TEXTURE = new ResourceLocation(Main.MODID, "textures/gui/chain_icon.png");

  private static class NpcData {

    private String name;
    private String dialogue;
    private String behavior;
    private int textColor;
    private String texture;
    private boolean isSlim, isNameVisible;
    private ItemStack[] items;
    private CEditNpc.NPCPose pose;
    private EntityType<?> renderer;
    private float scaleX, scaleY, scaleZ;

    private static NpcData fromNpc(NpcEntity npcEntity) {
      NpcData data = new NpcData();

      data.pose = npcEntity.isCrouching() ? CEditNpc.NPCPose.CROUCHING : npcEntity.isSitting() ? CEditNpc.NPCPose.SITTING : CEditNpc.NPCPose.STANDING;
      data.name = npcEntity.getName().getString();
      data.isNameVisible = npcEntity.isCustomNameVisible();
      data.texture = npcEntity.getTexture();
      data.isSlim = npcEntity.isSlim();
      data.dialogue = npcEntity.getDialogue();
      data.behavior = npcEntity.getBehaviorFile();
      data.textColor = npcEntity.getTextColor();
      data.items = new ItemStack[]{npcEntity.getMainHandItem(), npcEntity.getOffhandItem(),
              npcEntity.getItemBySlot(EquipmentSlot.HEAD), npcEntity.getItemBySlot(EquipmentSlot.CHEST),
              npcEntity.getItemBySlot(EquipmentSlot.LEGS), npcEntity.getItemBySlot(EquipmentSlot.FEET)};
      data.renderer = npcEntity.getRendererType();
      data.scaleX = npcEntity.getScaleX();
      data.scaleY = npcEntity.getScaleY();
      data.scaleZ = npcEntity.getScaleZ();

      return data;
    }

    private void setNpcData(NpcEntity npcEntity) {
      switch (pose) {
        case CROUCHING -> { npcEntity.setCrouching(true); npcEntity.setSitting(false); }
        case SITTING -> { npcEntity.setCrouching(false); npcEntity.setSitting(true); }
        case STANDING -> { npcEntity.setCrouching(false); npcEntity.setSitting(false); }
      }
      npcEntity.setCustomName(new TextComponent(name));
      npcEntity.setCustomNameVisible(isNameVisible);
      npcEntity.setTexture(texture);
      npcEntity.setSlim(isSlim);
      npcEntity.setDialogue(dialogue);
      npcEntity.setBehaviorFile(behavior);
      npcEntity.setTextColor(textColor);
      npcEntity.setItemSlot(EquipmentSlot.MAINHAND, items[0]);
      npcEntity.setItemSlot(EquipmentSlot.OFFHAND, items[1]);
      npcEntity.setItemSlot(EquipmentSlot.HEAD, items[2]);
      npcEntity.setItemSlot(EquipmentSlot.CHEST, items[3]);
      npcEntity.setItemSlot(EquipmentSlot.LEGS, items[4]);
      npcEntity.setItemSlot(EquipmentSlot.FEET, items[5]);
      npcEntity.setRenderer(renderer);
      npcEntity.setScale(scaleX, scaleY, scaleZ);
    }
  }

  private final NpcEntity npcEntity;
  private final NpcData originalData;
  private final NpcData currentData;

  public EditBox redField, greenField, blueField, scaleXField, scaleYField, scaleZField;
  private Checkbox slimCheckBox, nameVisibleCheckbox;
  private CustomCheckbox scaleLinkCheckbox;
  private ColorSliderWidget redSlider, greenSlider, blueSlider;
  private EnumDropdownWidget<CEditNpc.NPCPose> poseDropdown;
  private EntityDropdownWidget rendererDropdown;

  private int r, g, b;
  private boolean isConfirmClose;

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

  private final Predicate<String> scaleFilter = (text) -> {
    if (text.isEmpty()) {
      return true;
    } else {
      try {
        float f = Float.parseFloat(text);
        return f >= 0f && f <= 15f; // Inputting numbers between 0 & 1 wouldn't be easy if we didn't allow >=0 as input
      } catch (NumberFormatException e) {
        return false;
      }
    }
  };

  public NpcBuilderScreen(NpcEntity npcEntity) {
    super(TextComponent.EMPTY);

    this.npcEntity = npcEntity;
    this.originalData = NpcData.fromNpc(npcEntity);
    this.currentData = NpcData.fromNpc(npcEntity);

    this.r = ColorUtil.hexToR(currentData.textColor);
    this.g = ColorUtil.hexToG(currentData.textColor);
    this.b = ColorUtil.hexToB(currentData.textColor);
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
    nameField.setValue(currentData.name);

    this.nameVisibleCheckbox = this.addRenderableWidget(new Checkbox(minX + 130 + font.width("Visible? "), 5, 20, 20, TextComponent.EMPTY, currentData.isNameVisible));

    EditBox textureField = this.addRenderableWidget(new EditBox(font, minX, 30, 120, 20, TextComponent.EMPTY));
    textureField.setResponder(this::setTexture);
    textureField.setFilter(textFilter);
    textureField.setMaxLength(200);
    textureField.setValue(currentData.texture);

    EditBox dialogueField = this.addRenderableWidget(new EditBox(font, minX, 55, 120, 20, TextComponent.EMPTY));
    dialogueField.setResponder(this::setDialogue);
    dialogueField.setFilter(textFilter);
    dialogueField.setMaxLength(200);
    dialogueField.setValue(currentData.dialogue);

    EditBox behaviorField = this.addRenderableWidget(new EditBox(font, minX, 80, 120, 20, TextComponent.EMPTY));
    behaviorField.setResponder(this::setBehavior);
    behaviorField.setFilter(textFilter);
    behaviorField.setMaxLength(200);
    behaviorField.setValue(currentData.behavior);

    this.slimCheckBox = this.addRenderableWidget(new Checkbox(minX + 130 + font.width("Slim? "), 30, 20, 20, TextComponent.EMPTY, currentData.isSlim));

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

    this.scaleLinkCheckbox = this.addRenderableWidget(new CustomCheckbox(minX + 235 + font.width("Scale: "), 54, 22, 22, TextComponent.EMPTY, false, CHAIN_LINK_TEXTURE));

    this.scaleXField = this.addRenderableWidget(new EditBox(font, minX + 130 + font.width("Scale: "), 55, 30, 20, TextComponent.EMPTY));
    this.scaleXField.setResponder(this::setScaleXFromString);
    this.scaleXField.setFilter(scaleFilter);
    this.scaleXField.setMaxLength(5);
    this.scaleXField.setValue(String.valueOf(currentData.scaleX));

    this.scaleYField = this.addRenderableWidget(new EditBox(font, minX + 165 + font.width("Scale: "), 55, 30, 20, TextComponent.EMPTY));
    this.scaleYField.setResponder(this::setScaleYFromString);
    this.scaleYField.setFilter(scaleFilter);
    this.scaleYField.setMaxLength(5);
    this.scaleYField.setValue(String.valueOf(currentData.scaleY));

    this.scaleZField = this.addRenderableWidget(new EditBox(font, minX + 200 + font.width("Scale: "), 55, 30, 20, TextComponent.EMPTY));
    this.scaleZField.setResponder(this::setScaleZFromString);
    this.scaleZField.setFilter(scaleFilter);
    this.scaleZField.setMaxLength(5);
    this.scaleZField.setValue(String.valueOf(currentData.scaleZ));

    // Confirm Button
    this.addRenderableWidget(new Button(width - 60, height - 20, 60, 20, new TextComponent("Confirm"), btn -> {
      PacketDispatcher.sendToServer(
              new CEditNpc(this.npcEntity.getId(), currentData.isNameVisible, currentData.name, currentData.texture, currentData.isSlim, currentData.dialogue,
                      currentData.behavior, currentData.textColor, currentData.items, currentData.pose, currentData.renderer, currentData.scaleX, currentData.scaleY, currentData.scaleZ));
      isConfirmClose = true;
      minecraft.setScreen(null);
    }));

    // Inventory Button
    this.addRenderableWidget(new Button(width - 60, height - 40, 60, 20, new TextComponent("Inventory"), btn -> {
      PacketDispatcher.sendToServer(
              new CEditNpc(this.npcEntity.getId(), currentData.isNameVisible, currentData.name, currentData.texture, currentData.isSlim, currentData.dialogue,
                      currentData.behavior, currentData.textColor, currentData.items, currentData.pose, currentData.renderer, currentData.scaleX, currentData.scaleY, currentData.scaleZ));
      PacketDispatcher.sendToServer(new CRequestContainer(this.npcEntity.getId(), CRequestContainer.ContainerType.NPCINVENTORY));
    }));

    // Trades Button
    this.addRenderableWidget(new Button(width - 60, height - 60, 60, 20, new TextComponent("Trades"), btn -> {
      PacketDispatcher.sendToServer(
              new CEditNpc(this.npcEntity.getId(), currentData.isNameVisible, currentData.name, currentData.texture, currentData.isSlim, currentData.dialogue,
                      currentData.behavior, currentData.textColor, currentData.items, currentData.pose, currentData.renderer, currentData.scaleX, currentData.scaleY, currentData.scaleZ));
      PacketDispatcher.sendToServer(new CRequestContainer(this.npcEntity.getId(), CRequestContainer.ContainerType.TRADE_EDITOR));
    }));

    // Reset Behavior Button
    this.addRenderableWidget(new Button(width - 60, height - 80, 60, 20, new TextComponent("Reset AI"), btn -> {
      PacketDispatcher.sendToServer(
              new CEditNpc(this.npcEntity.getId(), currentData.isNameVisible, currentData.name, currentData.texture, currentData.isSlim, currentData.dialogue,
                      currentData.behavior, currentData.textColor, currentData.items, currentData.pose, true, currentData.renderer, currentData.scaleX, currentData.scaleY, currentData.scaleZ));
    }));

    this.poseDropdown = this.addRenderableWidget(new EnumDropdownWidget<>(currentData.pose, minX + 210, 5, 80));

    this.rendererDropdown = this.addRenderableWidget(new EntityDropdownWidget(currentData.renderer, minX + 125, 85, 165, 10, true));
  }

  private void setName(String s) {
    currentData.name = s;
  }

  private void setTexture(String s) {
    currentData.texture = s;
    npcEntity.setTexture(s);
  }

  private void setDialogue(String s) {
    currentData.dialogue = s;
  }

  private void setBehavior(String s) {
    currentData.behavior = s;
  }

  public int getR() {
    return r;
  }

  public void setR(int i) {
    r = Mth.clamp(i, 0, 255);
    currentData.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  private void setRFromString(String s) {
    if (s.isEmpty()) {
      r = 0;
    } else {
      try {
        r = Mth.clamp(Integer.parseInt(s), 0, 255);
      } catch (NumberFormatException ignored) {
      }
    }
    redSlider.updateColorY();
    currentData.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  public int getG() {
    return g;
  }

  public void setG(int i) {
    g = Mth.clamp(i, 0, 255);
    currentData.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  private void setGFromString(String s) {
    if (s.isEmpty()) {
      g = 0;
    } else {
      try {
        g = Mth.clamp(Integer.parseInt(s), 0, 255);
      } catch (NumberFormatException ignored) {
      }
    }
    greenSlider.updateColorY();
    currentData.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  public int getB() {
    return b;
  }

  public void setB(int i) {
    b = Mth.clamp(i, 0, 255);
    currentData.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  private void setBFromString(String s) {
    if (s.isEmpty()) {
      b = 0;
    } else {
      try {
        b = Mth.clamp(Integer.parseInt(s), 0, 255);
      } catch (NumberFormatException ignored) {
      }
    }
    blueSlider.updateColorY();
    currentData.textColor = ColorUtil.rgbToHex(r, g, b);
  }

  private void setScaleXFromString(String s) {
    if (s.isEmpty()) {
      currentData.scaleX = 1f;
    }
    else {
      try {
        currentData.scaleX = Mth.clamp(Float.parseFloat(s), 0.1f, 15f);
      } catch (NumberFormatException ignored) {
      }
    }
    if (scaleLinkCheckbox.selected() && this.getFocused() == scaleXField) {
      scaleYField.setValue(String.valueOf(currentData.scaleX));
      scaleZField.setValue(String.valueOf(currentData.scaleX));
    }
    npcEntity.setScale(currentData.scaleX, currentData.scaleY, currentData.scaleZ);
  }

  private void setScaleYFromString(String s) {
    if (s.isEmpty()) {
      currentData.scaleY = 1f;
    }
    else {
      try {
        currentData.scaleY = Mth.clamp(Float.parseFloat(s), 0.1f, 15f);
      } catch (NumberFormatException ignored) {
      }
    }
    if (scaleLinkCheckbox.selected() && this.getFocused() == scaleYField) {
      scaleXField.setValue(String.valueOf(currentData.scaleY));
      scaleZField.setValue(String.valueOf(currentData.scaleY));
    }
    npcEntity.setScale(currentData.scaleX, currentData.scaleY, currentData.scaleZ);
  }

  private void setScaleZFromString(String s) {
    if (s.isEmpty()) {
      currentData.scaleZ = 1f;
    }
    else {
      try {
        currentData.scaleZ = Mth.clamp(Float.parseFloat(s), 0.1f, 15f);
      } catch (NumberFormatException ignored) {
      }
    }
    if (scaleLinkCheckbox.selected() && this.getFocused() == scaleZField) {
      scaleXField.setValue(String.valueOf(currentData.scaleZ));
      scaleYField.setValue(String.valueOf(currentData.scaleZ));
    }
    npcEntity.setScale(currentData.scaleX, currentData.scaleY, currentData.scaleZ);
  }

  @Override
  public void tick() {
    currentData.isSlim = slimCheckBox.selected();
    npcEntity.setSlim(currentData.isSlim);
    currentData.isNameVisible = nameVisibleCheckbox.selected();
    npcEntity.setCustomNameVisible(currentData.isNameVisible);

    if (poseDropdown != null && poseDropdown.getSelectedOption() != currentData.pose) {
      currentData.pose = poseDropdown.getSelectedOption();
      switch (currentData.pose) {
        case CROUCHING -> { npcEntity.setCrouching(true); npcEntity.setSitting(false); }
        case SITTING -> { npcEntity.setCrouching(false); npcEntity.setSitting(true); }
        case STANDING -> { npcEntity.setCrouching(false); npcEntity.setSitting(false); }
      }
    }

    if (this.rendererDropdown != null) {
      currentData.renderer = this.rendererDropdown.getSelectedType();
      if (!npcEntity.getRendererType().equals(currentData.renderer))
        npcEntity.setRenderer(currentData.renderer);
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
    drawString(matrixStack, font, "Scale: ", minX + 130, 55 + center, 0xFFFFFF);

    drawString(matrixStack, font, "Dialogue: ", 5, 55 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Behavior: ", 5, 80 + center, 0xFFFFFF);

    drawString(matrixStack, font, "Text Color: ", 5, 105 + (100 - font.lineHeight) / 2, 0xFFFFFF);

    fill(matrixStack, minX + 89, 121, minX + 116, 147, 0xFF000000);
    fill(matrixStack, minX + 90, 122, minX + 115, 146, ColorUtil.hexToHexA(currentData.textColor));

    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }

  @Override
  public void onClose() {
    if (!isConfirmClose)
      originalData.setNpcData(npcEntity);
    super.onClose();
  }
}