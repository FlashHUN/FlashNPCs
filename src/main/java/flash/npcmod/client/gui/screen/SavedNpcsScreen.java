package flash.npcmod.client.gui.screen;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import flash.npcmod.ClientProxy;
import flash.npcmod.Main;
import flash.npcmod.core.trades.TradeOffer;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CHandleSavedNpc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.FlatPresetsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SavedNpcsScreen extends Screen {

  private int selectedIndex;
  private int renameResultTick;
  private int renameResultColor;
  private String renameResult;
  private final String pos;
  private String newInternalName;
  private NpcEntity selected;

  private Button placeButton, renameButton, deleteButton;
  private TextFieldWidget renameTextBox;

  private List<JSONObject> savedNpcs;
  private SavedNpcsScreen.NpcList list;

  public SavedNpcsScreen(String pos) {
    super(StringTextComponent.EMPTY);
    this.pos = pos;
    this.newInternalName = "";
    savedNpcs = new ArrayList<>();
    ClientProxy.SAVED_NPCS.forEach(s -> savedNpcs.add(new JSONObject(s)));
  }

  @Override
  protected void init() {
    this.list = new SavedNpcsScreen.NpcList();
    this.addListener(this.list);
    if (this.selected != null) {
      this.list.setSelected(list.getEventListeners().get(selectedIndex));
    }
    int buttonY = height - 25;
    this.placeButton = this.addButton(new Button(5, buttonY, 100, 20, new StringTextComponent("Place"), onPress -> {
      if (list.getSelected() != null) {
        JSONObject npcJson = list.getSelected().asJson;
        try {
          int[] posArray = Arrays.stream(this.pos.split(";")).mapToInt(Integer::valueOf).toArray();
          BlockPos pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
          PacketDispatcher.sendToServer(new CHandleSavedNpc(npcJson, pos));
        } catch (Exception ignored) {
          Main.LOGGER.warn("Couldn't place Saved NPC: invalid BlockPos ("+this.pos+")");
        }
        closeScreen();
      }
    }));
    this.deleteButton = this.addButton(new Button(5+100+5, buttonY, 100, 20, new StringTextComponent("Delete"), onPress -> {
      if (list.getSelected() != null) {
        PacketDispatcher.sendToServer(new CHandleSavedNpc(list.getSelected().name.getString()));
        savedNpcs.remove(list.getSelectedIndex());
        list.update();
        selected = null;
      }
    }));
    this.renameButton = this.addButton(new Button(width - 5 - 100, buttonY, 100, 20, new StringTextComponent("Rename"), onPress -> {
      if (list.getSelected() != null) {
        String prevname = list.getSelected().name.getString();
        boolean exists = savedNpcs.stream().filter(json -> json.getString("internalName").equals(newInternalName)).count() > 0;
        if (!exists) {
          list.getSelected().asJson.put("internalName", newInternalName);
          list.getSelected().name = new StringTextComponent(newInternalName);
          PacketDispatcher.sendToServer(new CHandleSavedNpc(prevname, newInternalName));
          renameResult = "Success!";
          renameResultColor = 0x00FF00;
        }
        else {
          Main.LOGGER.warn("Tried to rename saved npc to an already existing name");
          renameResult = "A Saved NPC with this name already exists!";
          renameResultColor = 0xFF0000;
        }
        renameResultTick = 40;
      }
    }));
    this.renameTextBox = this.addButton(new TextFieldWidget(font, 5+105+105, buttonY, width - 110 - 110 - 105, 20, StringTextComponent.EMPTY));
    this.renameTextBox.setResponder(this::setNewInternalName);
    this.renameTextBox.setMaxStringLength(50);
    this.renameTextBox.setText(this.newInternalName);
    this.updateButtonValidity(this.list.getSelected() != null);
  }

  public void setNewInternalName(String newInternalName) {
    this.newInternalName = newInternalName;
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);
    if (this.list != null) {
      this.list.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    if (this.selected != null) {
      InventoryScreen.drawEntityOnScreen(30, 70, 30, -30, -15, selected);
      drawCenteredString(matrixStack, this.font, this.selected.getName(), this.width / 2, 8, 0xFFFFFF);
      if (this.selected.getDialogue() != null && !this.selected.getDialogue().isEmpty()) {
        drawString(matrixStack, this.font, "Dialogue: " + this.selected.getDialogue(), 60, 20, this.selected.getTextColor());
      }
      if (!this.selected.getOffers().isEmpty()) {
        drawString(matrixStack, this.font, "Trades:", 60, 32, 0xFFFFFF);
        List<TradeOffer> tradeOffers = this.selected.getOffers().stream().filter(offer -> !offer.isEmpty()).collect(Collectors.toList());
        int w = 16*6+4+font.getStringWidth("-->")+2;
        for (int i = 0; i < Math.min(6, tradeOffers.size()); i++) {
          int x = 62 + (i / 2 * w);
          int y = 42 + (i % 2 * 18);
          renderTradeOffer(tradeOffers.get(i), matrixStack, x, y, mouseX, mouseY);
        }

        if (tradeOffers.size() > 6) {
          drawString(matrixStack, this.font, ". . .", 62+3*w, 53, 0xFFFFFF);
        }
      }
    }

    super.render(matrixStack, mouseX, mouseY, partialTicks);

    if (renameResultTick > 0) {
      drawCenteredString(matrixStack, this.font, renameResult, this.width / 2, this.height / 2 - font.FONT_HEIGHT / 2, renameResultColor);
    }
  }

  private void renderTradeOffer(TradeOffer offer, MatrixStack matrixStack, int x, int y, int mouseX, int mouseY) {
    for (int i = 0; i < offer.getBuyingStacks().length; i++) {
      ItemStack stack = offer.getBuyingStacks()[i];
      if (!stack.isEmpty()) {
        minecraft.getItemRenderer().renderItemAndEffectIntoGUI(stack, x+i*16, y);
        minecraft.getItemRenderer().renderItemOverlays(font, stack, x+i*16, y);
        if (mouseX >= x+i*16 && mouseX < x + 16 + i*16 && mouseY >= y && mouseY <= y + 16)
          this.renderTooltip(matrixStack, stack, mouseX, mouseY);
      }
    }

    int x2 = x+50;
    drawString(matrixStack, this.font, "-->", x2, y+4, 0xFFFFFF);

    int x3 = x2 + font.getStringWidth("-->") + 2;
    for (int i = 0; i < offer.getSellingStacks().length; i++) {
      ItemStack stack = offer.getSellingStacks()[i];
      if (!stack.isEmpty()) {
        minecraft.getItemRenderer().renderItemAndEffectIntoGUI(stack, x3+i*16, y);
        minecraft.getItemRenderer().renderItemOverlays(font, stack, x3+i*16, y);
        if (mouseX >= x3+i*16 && mouseX < x3 + 16 + i*16 && mouseY >= y && mouseY <= y + 16)
          this.renderTooltip(matrixStack, stack, mouseX, mouseY);
      }
    }

    drawString(matrixStack, this.font, "|", x3 + 48, y+4, 0xFFFFFF);
  }

  @Override
  public void tick() {
    if (renameResultTick > 0) {
      renameResultTick--;
    }
  }

  public boolean mouseScrolled(double p_96381_, double p_96382_, double p_96383_) {
    return this.list.mouseScrolled(p_96381_, p_96382_, p_96383_);
  }

  public void updateButtonValidity(boolean p_96450_) {
    this.placeButton.active = p_96450_;
    this.deleteButton.active = p_96450_;
    this.renameButton.active = p_96450_;
    this.renameTextBox.active = p_96450_;
  }

  @OnlyIn(Dist.CLIENT)
  static class NpcInfo {
    public final ITextComponent name;
    public final NpcEntity npcEntity;
    public final JSONObject asJson;

    public NpcInfo(ITextComponent name, NpcEntity npcEntity, JSONObject asJson) {
      this.name = name;
      this.npcEntity = npcEntity;
      this.asJson = asJson;
    }

    public static NpcInfo fromJson(JSONObject jsonObject) {
      return new NpcInfo(new StringTextComponent(jsonObject.getString("internalName")), NpcEntity.fromJson(Minecraft.getInstance().world, jsonObject), jsonObject);
    }
  }

  @OnlyIn(Dist.CLIENT)
  class NpcList extends ExtendedList<SavedNpcsScreen.NpcList.Entry> {
    public NpcList() {
      super(SavedNpcsScreen.this.minecraft, SavedNpcsScreen.this.width, SavedNpcsScreen.this.height, 80, SavedNpcsScreen.this.height - 37, 24);

      List<SavedNpcsScreen.NpcInfo> savedNpcs = SavedNpcsScreen.this.savedNpcs
              .stream()
              .map(NpcInfo::fromJson)
              .collect(Collectors.toList());
      for(SavedNpcsScreen.NpcInfo savednpcsscreen$npcinfo : savedNpcs) {
        this.addEntry(new SavedNpcsScreen.NpcList.Entry(savednpcsscreen$npcinfo));
      }
    }

    public int getSelectedIndex() {
      return this.getEventListeners().indexOf(getSelected());
    }

    public void setSelected(@Nullable SavedNpcsScreen.NpcList.Entry entry) {
      super.setSelected(entry);
      SavedNpcsScreen.this.updateButtonValidity(entry != null);
    }

    protected void update() {
      List<SavedNpcsScreen.NpcInfo> savedNpcs = SavedNpcsScreen.this.savedNpcs
              .stream()
              .map(NpcInfo::fromJson)
              .collect(Collectors.toList());
      clearEntries();
      for(SavedNpcsScreen.NpcInfo savednpcsscreen$npcinfo : savedNpcs) {
        this.addEntry(new SavedNpcsScreen.NpcList.Entry(savednpcsscreen$npcinfo));
      }
    }

    protected boolean isFocused() {
      return SavedNpcsScreen.this.getListener() == this;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (super.keyPressed(keyCode, scanCode, modifiers)) {
        return true;
      } else {
        if ((keyCode == 257 || keyCode == 335) && this.getSelected() != null) {
          this.getSelected().select();
        }

        return false;
      }
    }

    @OnlyIn(Dist.CLIENT)
    public class Entry extends ExtendedList.AbstractListEntry<SavedNpcsScreen.NpcList.Entry> {
      private ITextComponent name;
      private final NpcEntity npcEntity;
      private JSONObject asJson;

      public Entry(NpcInfo npcInfo) {
        this.name = npcInfo.name;
        this.npcEntity = npcInfo.npcEntity;
        this.asJson = npcInfo.asJson;
      }

      public void render(MatrixStack matrixStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
        InventoryScreen.drawEntityOnScreen(left+10, top+18, 10, -40, -20, npcEntity);
        SavedNpcsScreen.this.font.drawText(matrixStack, this.name, (float)(left + 18 + 5), (float)(top + 6), 16777215);
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
          this.select();
        }

        return false;
      }

      private void select() {
        SavedNpcsScreen.NpcList.this.setSelected(this);
        SavedNpcsScreen.this.selected = this.npcEntity;
        SavedNpcsScreen.this.selectedIndex = getSelectedIndex();
        SavedNpcsScreen.this.renameTextBox.setText(this.name.getString());
      }
    }
  }
}
