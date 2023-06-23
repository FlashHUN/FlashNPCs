package flash.npcmod.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.StringReader;
import flash.npcmod.Main;
import flash.npcmod.client.gui.widget.ColorSliderWidget;
import flash.npcmod.client.gui.widget.CustomCheckbox;
import flash.npcmod.client.gui.widget.EntityDropdownWidget;
import flash.npcmod.client.gui.widget.EnumDropdownWidget;
import flash.npcmod.core.ColorUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.init.EntityInit;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CEditNpc;
import flash.npcmod.network.packets.client.CRequestContainer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
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
    private String title;
    private String dialogue;
    private String behavior;
    private int textColor;
    private String texture;
    private boolean isSlim, isNameVisible, isTextureResourceLocation;
    private ItemStack[] items;
    private CEditNpc.NPCPose pose;
    private EntityType<?> renderedType;
    private CompoundTag rendererTag;
    private float scaleX, scaleY, scaleZ;
    private boolean collision;

    private static NpcData fromNpc(NpcEntity npcEntity) {
      NpcData data = new NpcData();

      data.pose = npcEntity.isCrouching() ? CEditNpc.NPCPose.CROUCHING : npcEntity.isSitting() ? CEditNpc.NPCPose.SITTING : CEditNpc.NPCPose.STANDING;
      data.name = npcEntity.getName().getString();
      data.title = npcEntity.getTitle();
      data.isNameVisible = npcEntity.isCustomNameVisible();
      data.texture = npcEntity.getTexture();
      data.isTextureResourceLocation = npcEntity.isTextureResourceLocation();
      data.isSlim = npcEntity.isSlim();
      data.dialogue = npcEntity.getDialogue();
      data.behavior = npcEntity.getBehaviorFile();
      data.textColor = npcEntity.getTextColor();
      data.items = new ItemStack[]{npcEntity.getMainHandItem(), npcEntity.getOffhandItem(),
              npcEntity.getItemBySlot(EquipmentSlot.HEAD), npcEntity.getItemBySlot(EquipmentSlot.CHEST),
              npcEntity.getItemBySlot(EquipmentSlot.LEGS), npcEntity.getItemBySlot(EquipmentSlot.FEET)};
      data.renderedType = npcEntity.getRenderedEntityType();
      data.rendererTag = npcEntity.getRenderedEntityTagWithoutId();
      data.scaleX = npcEntity.getScaleX();
      data.scaleY = npcEntity.getScaleY();
      data.scaleZ = npcEntity.getScaleZ();
      data.collision = npcEntity.hasCollision();

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
      npcEntity.setTitle(title);
      npcEntity.setTexture(texture);
      npcEntity.setIsTextureResourceLocation(isTextureResourceLocation);
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
      npcEntity.setRenderedEntityFromTag(rendererTag);
      npcEntity.setScale(scaleX, scaleY, scaleZ);
      npcEntity.setCollision(collision);
    }
  }

  private final NpcEntity npcEntity;
  private final NpcData originalData;
  private final NpcData currentData;
  private final CompoundTag originalRendererTagCopy;

  public EditBox redField, greenField, blueField, scaleXField, scaleYField, scaleZField, rendererTagField;
  private Checkbox slimCheckBox, nameVisibleCheckbox, isResourceLocationCheckbox, collisionCheckbox;
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
    originalRendererTagCopy = originalData.rendererTag.copy();

    this.r = ColorUtil.hexToR(currentData.textColor);
    this.g = ColorUtil.hexToG(currentData.textColor);
    this.b = ColorUtil.hexToB(currentData.textColor);
  }

  private static int max(int... numbers) {
    int max = numbers[0];
    for (int i = 1; i < numbers.length; i++)
      if (numbers[i] > max)
        max = numbers[i];
    return max;
  }

  @Override
  protected void init() {
    minX = 5 + max(
            font.width("Name: "), font.width("Texture: "),
            font.width("Dialogue: "), font.width("Text Color: "),
            font.width("Behavior: "), font.width("Title: ")
    );
    EditBox nameField = this.addRenderableWidget(new EditBox(font, minX, 5, 120, 20, TextComponent.EMPTY));
    nameField.setResponder(this::setName);
    nameField.setMaxLength(200);
    nameField.setValue(currentData.name);

    this.nameVisibleCheckbox = this.addRenderableWidget(new Checkbox(minX + 130 + font.width("Visible? "), 5, 20, 20, TextComponent.EMPTY, currentData.isNameVisible));

    EditBox titleField = this.addRenderableWidget(new EditBox(font, minX, 30, 120, 20, TextComponent.EMPTY));
    titleField.setResponder(this::setTitle);
    titleField.setMaxLength(200);
    titleField.setValue(currentData.title);

    EditBox textureField = this.addRenderableWidget(new EditBox(font, minX, 55, 120, 20, TextComponent.EMPTY));
    textureField.setResponder(this::setTexture);
    textureField.setFilter(textFilter);
    textureField.setMaxLength(200);
    textureField.setValue(currentData.texture);

    EditBox dialogueField = this.addRenderableWidget(new EditBox(font, minX, 80, 120, 20, TextComponent.EMPTY));
    dialogueField.setResponder(this::setDialogue);
    dialogueField.setFilter(textFilter);
    dialogueField.setMaxLength(200);
    dialogueField.setValue(currentData.dialogue);

    EditBox behaviorField = this.addRenderableWidget(new EditBox(font, minX, 105, 120, 20, TextComponent.EMPTY));
    behaviorField.setResponder(this::setBehavior);
    behaviorField.setFilter(textFilter);
    behaviorField.setMaxLength(200);
    behaviorField.setValue(currentData.behavior);

    this.slimCheckBox = this.addRenderableWidget(new Checkbox(minX + 130 + font.width("Slim? "), 30, 20, 20, TextComponent.EMPTY, currentData.isSlim));
    this.slimCheckBox.active = currentData.renderedType == EntityInit.NPC_ENTITY.get();

    this.isResourceLocationCheckbox = this.addRenderableWidget(new Checkbox(minX + 155 + font.width("Slim? ") + font.width("ResourceLocation? "), 30, 20, 20, TextComponent.EMPTY, currentData.isTextureResourceLocation));

    this.redSlider = this.addRenderableWidget(new ColorSliderWidget(this, minX, 130, 20, 75, ColorSliderWidget.Color.RED));
    this.greenSlider = this.addRenderableWidget(new ColorSliderWidget(this, minX + 30, 130, 20, 75, ColorSliderWidget.Color.GREEN));
    this.blueSlider = this.addRenderableWidget(new ColorSliderWidget(this, minX + 60, 130, 20, 75, ColorSliderWidget.Color.BLUE));

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
              new CEditNpc(this.npcEntity.getId(), currentData.isNameVisible, currentData.name, currentData.title, currentData.texture, currentData.isTextureResourceLocation, currentData.isSlim, currentData.dialogue,
                      currentData.behavior, currentData.textColor, currentData.items, currentData.pose, currentData.renderedType, currentData.rendererTag, currentData.scaleX, currentData.scaleY, currentData.scaleZ, currentData.collision));
      isConfirmClose = true;
      minecraft.setScreen(null);
    }));

    // Inventory Button
    this.addRenderableWidget(new Button(width - 60, height - 40, 60, 20, new TextComponent("Inventory"), btn -> {
      PacketDispatcher.sendToServer(
              new CEditNpc(this.npcEntity.getId(), currentData.isNameVisible, currentData.name, currentData.title, currentData.texture, currentData.isTextureResourceLocation, currentData.isSlim, currentData.dialogue,
                      currentData.behavior, currentData.textColor, currentData.items, currentData.pose, currentData.renderedType, currentData.rendererTag, currentData.scaleX, currentData.scaleY, currentData.scaleZ, currentData.collision));
      PacketDispatcher.sendToServer(new CRequestContainer(this.npcEntity.getId(), CRequestContainer.ContainerType.NPCINVENTORY));
    }));

    // Trades Button
    this.addRenderableWidget(new Button(width - 60, height - 60, 60, 20, new TextComponent("Trades"), btn -> {
      PacketDispatcher.sendToServer(
              new CEditNpc(this.npcEntity.getId(), currentData.isNameVisible, currentData.name, currentData.title, currentData.texture, currentData.isTextureResourceLocation, currentData.isSlim, currentData.dialogue,
                      currentData.behavior, currentData.textColor, currentData.items, currentData.pose, currentData.renderedType, currentData.rendererTag, currentData.scaleX, currentData.scaleY, currentData.scaleZ, currentData.collision));
      PacketDispatcher.sendToServer(new CRequestContainer(this.npcEntity.getId(), CRequestContainer.ContainerType.TRADE_EDITOR));
    }));

    // Reset Behavior Button
    this.addRenderableWidget(new Button(width - 60, height - 80, 60, 20, new TextComponent("Reset AI"), btn -> {
      PacketDispatcher.sendToServer(
              new CEditNpc(this.npcEntity.getId(), currentData.isNameVisible, currentData.name, currentData.title, currentData.texture, currentData.isTextureResourceLocation, currentData.isSlim, currentData.dialogue,
                      currentData.behavior, currentData.textColor, currentData.items, currentData.pose, true, currentData.renderedType, currentData.rendererTag, currentData.scaleX, currentData.scaleY, currentData.scaleZ, currentData.collision));
    }));

    this.poseDropdown = this.addRenderableWidget(new EnumDropdownWidget<>(currentData.pose, minX + 210, 5, 80));
    this.poseDropdown.active = currentData.renderedType == EntityInit.NPC_ENTITY.get();

    this.rendererDropdown = this.addRenderableWidget(new EntityDropdownWidget(currentData.renderedType, minX + 125, 105, 165, 8, true));

    this.collisionCheckbox = this.addRenderableWidget(new Checkbox(minX + 130 + font.width("Collision: "), 120, 20, 20, TextComponent.EMPTY, currentData.collision, false));
    this.collisionCheckbox.visible = this.collisionCheckbox.active = !this.rendererDropdown.isShowingOptions();

    this.rendererTagField = this.addRenderableWidget(new EditBox(font, minX + 125, 80, 165, 20, TextComponent.EMPTY));
    this.rendererTagField.setResponder(this::setRendererTag);
    this.rendererTagField.setMaxLength(1000);
    if (currentData.rendererTag.contains("id"))
      currentData.rendererTag.remove("id");
    this.rendererTagField.setValue(currentData.rendererTag.getAsString());
  }

  private void setName(String s) {
    currentData.name = s;
    npcEntity.setCustomName(new TextComponent(s));
  }

  private void setTitle(String s) {
    currentData.title = s;
    npcEntity.setTitle(s);
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
      npcEntity.setCustomName(new TextComponent(currentData.name));
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

  private void setRendererTag(String s)
  {
    if (s.isEmpty()) {
      currentData.rendererTag = originalRendererTagCopy;
    } else {
      try {
        currentData.rendererTag = new TagParser(new StringReader(s)).readStruct();
      } catch (Exception ignored) {
        currentData.rendererTag = originalRendererTagCopy;
      }
    }
    npcEntity.setRenderedEntityFromTag(currentData.rendererTag);
  }

  @Override
  public void tick() {
    currentData.isSlim = slimCheckBox.selected();
    npcEntity.setSlim(currentData.isSlim);
    currentData.isNameVisible = nameVisibleCheckbox.selected();
    npcEntity.setCustomNameVisible(currentData.isNameVisible);
    currentData.isTextureResourceLocation = isResourceLocationCheckbox.selected();
    npcEntity.setIsTextureResourceLocation(currentData.isTextureResourceLocation);

    if (poseDropdown.getSelectedOption() != currentData.pose) {
      currentData.pose = poseDropdown.getSelectedOption();
      switch (currentData.pose) {
        case CROUCHING -> { npcEntity.setCrouching(true); npcEntity.setSitting(false); }
        case SITTING -> { npcEntity.setCrouching(false); npcEntity.setSitting(true); }
        case STANDING -> { npcEntity.setCrouching(false); npcEntity.setSitting(false); }
      }
    }
    currentData.collision = collisionCheckbox.selected();
    this.collisionCheckbox.visible = this.collisionCheckbox.active = !this.rendererDropdown.isShowingOptions();

    currentData.renderedType = this.rendererDropdown.getSelectedType();
    if (!npcEntity.getRenderedEntityType().equals(currentData.renderedType)) {
      var tag = currentData.rendererTag.copy();
      tag.putString("id", EntityType.getKey(currentData.renderedType).toString());
      npcEntity.setRenderedEntityFromTag(tag);
    }

    if (currentData.renderedType != EntityInit.NPC_ENTITY.get()) {
      this.poseDropdown.active = false;
      this.poseDropdown.selectOption(CEditNpc.NPCPose.STANDING);
      this.slimCheckBox.active = false;
      if (this.slimCheckBox.selected())
        this.slimCheckBox.onPress();
      this.rendererTagField.active = this.rendererTagField.visible = true;
    }
    else {
      this.poseDropdown.active = true;
      this.slimCheckBox.active = true;
      this.rendererTagField.active = this.rendererTagField.visible = false;
    }
  }

  @Override
  public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);

    // Render the NPC on screen
    {
      int x = width - 60;
      int y = (int) (height / 4 * 2.5f);
      float bbHeight = Math.max(npcEntity.getBbHeight(), 1f);
      int scale = (int) (height / 3f / bbHeight);
      InventoryScreen.renderEntityInInventory(x, y, scale, 40, -20, npcEntity);
    }

    int center = (20 - font.lineHeight) / 2;
    drawString(matrixStack, font, "Name: ", 5, 5 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Visible? ", minX + 130, 5 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Title: ", 5, 30 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Texture: ", 5, 55 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Slim? ", minX + 130, 30 + center, this.slimCheckBox.active ? 0xFFFFFF : 0x7D7D7D);
    drawString(matrixStack, font, "ResourceLocation? ", minX + 155 + font.width("Slim? "), 30 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Scale: ", minX + 130, 55 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Collision: ", minX + 130, 120 + center, 0xFFFFFF);

    drawString(matrixStack, font, "Dialogue: ", 5, 80 + center, 0xFFFFFF);
    drawString(matrixStack, font, "Behavior: ", 5, 105 + center, 0xFFFFFF);

    drawString(matrixStack, font, "Text Color: ", 5, 130 + (100 - font.lineHeight) / 2, 0xFFFFFF);

    fill(matrixStack, minX + 89, 146, minX + 116, 172, 0xFF000000);
    fill(matrixStack, minX + 90, 147, minX + 115, 171, ColorUtil.hexToHexA(currentData.textColor));

    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }

  @Override
  public void onClose() {
    if (!isConfirmClose)
      originalData.setNpcData(npcEntity);
    super.onClose();
  }
}