package flash.npcmod.client.gui.screen;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.ClientProxy;
import flash.npcmod.Main;
import flash.npcmod.core.trades.TradeOffer;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CHandleSavedNpc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SavedNpcsScreen extends Screen {

  private int selectedIndex;
  private int renameResultTick;
  private int renameResultColor;
  private String renameResult;
  private final String pos;
  private String newInternalName;
  private NpcEntity selected;

  private Button placeButton, renameButton, deleteButton;
  private EditBox renameTextBox;

  private List<JsonObject> savedNpcs;
  private SavedNpcsScreen.NpcList list;

  public SavedNpcsScreen(String pos) {
    super(TextComponent.EMPTY);
    this.pos = pos;
    this.newInternalName = "";
    savedNpcs = new ArrayList<>();
    ClientProxy.SAVED_NPCS.forEach(s -> savedNpcs.add(new Gson().fromJson(s, JsonObject.class)));
  }

  @Override
  protected void init() {
    this.list = new SavedNpcsScreen.NpcList();
    this.addWidget(this.list);
    if (this.selected != null) {
      this.list.setSelected(list.children().get(selectedIndex));
    }
    int buttonY = height - 25;
    this.placeButton = this.addRenderableWidget(new Button(5, buttonY, 100, 20, new TextComponent("Place"), onPress -> {
      if (list.getSelected() != null) {
        JsonObject npcJson = list.getSelected().asJson;
        try {
          int[] posArray = Arrays.stream(this.pos.split(";")).mapToInt(Integer::valueOf).toArray();
          BlockPos pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
          PacketDispatcher.sendToServer(new CHandleSavedNpc(npcJson, pos));
        } catch (Exception ignored) {
          Main.LOGGER.warn("Couldn't place Saved NPC: invalid BlockPos ("+this.pos+")");
        }
        onClose();
      }
    }));
    this.deleteButton = this.addRenderableWidget(new Button(5+100+5, buttonY, 100, 20, new TextComponent("Delete"), onPress -> {
      if (list.getSelected() != null) {
        PacketDispatcher.sendToServer(new CHandleSavedNpc(list.getSelected().name.getString()));
        savedNpcs.remove(list.getSelectedIndex());
        list.update();
        selected = null;
      }
    }));
    this.renameButton = this.addRenderableWidget(new Button(width - 5 - 100, buttonY, 100, 20, new TextComponent("Rename"), onPress -> {
      if (list.getSelected() != null) {
        String prevname = list.getSelected().name.getString();
        boolean exists = savedNpcs.stream().filter(json -> json.get("internalName").getAsString().equals(newInternalName)).count() > 0;
        if (!exists) {
          list.getSelected().asJson.addProperty("internalName", newInternalName);
          list.getSelected().name = new TextComponent(newInternalName);
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
    this.renameTextBox = this.addRenderableWidget(new EditBox(font, 5+105+105, buttonY, width - 110 - 110 - 105, 20, TextComponent.EMPTY));
    this.renameTextBox.setResponder(this::setNewInternalName);
    this.renameTextBox.setMaxLength(50);
    this.renameTextBox.setValue(this.newInternalName);
    this.updateButtonValidity(this.list.getSelected() != null);
  }

  public void setNewInternalName(String newInternalName) {
    this.newInternalName = newInternalName;
  }

  public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(stack);
    if (this.list != null) {
      this.list.render(stack, mouseX, mouseY, partialTicks);
    }

    if (this.selected != null) {
      float bbHeight = Math.max(selected.getBbHeight(), 1f);
      int scale = (int) (54 / bbHeight);
      InventoryScreen.renderEntityInInventory(30, 70, scale, -30, -15, selected);
      drawCenteredString(stack, this.font, this.selected.getName(), this.width / 2, 8, 0xFFFFFF);
      if (this.selected.getDialogue() != null && !this.selected.getDialogue().isEmpty()) {
        drawString(stack, this.font, "Dialogue: " + this.selected.getDialogue(), 60, 20, this.selected.getTextColor());
      }
      if (!this.selected.getOffers().isEmpty()) {
        drawString(stack, this.font, "Trades:", 60, 32, 0xFFFFFF);
        List<TradeOffer> tradeOffers = this.selected.getOffers().stream().filter(offer -> !offer.isEmpty()).toList();
        int w = 16*6+4+font.width("-->")+2;
        for (int i = 0; i < Math.min(6, tradeOffers.size()); i++) {
          int x = 62 + (i / 2 * w);
          int y = 42 + (i % 2 * 18);
          renderTradeOffer(tradeOffers.get(i), stack, x, y, mouseX, mouseY);
        }

        if (tradeOffers.size() > 6) {
          drawString(stack, this.font, ". . .", 62+3*w, 53, 0xFFFFFF);
        }
      }
    }

    super.render(stack, mouseX, mouseY, partialTicks);

    if (renameResultTick > 0) {
      drawCenteredString(stack, this.font, renameResult, this.width / 2, this.height / 2 - font.lineHeight / 2, renameResultColor);
    }
  }

  private void renderTradeOffer(TradeOffer offer, PoseStack poseStack, int x, int y, int mouseX, int mouseY) {
    for (int i = 0; i < offer.getBuyingStacks().length; i++) {
      ItemStack stack = offer.getBuyingStacks()[i];
      if (!stack.isEmpty()) {
        minecraft.getItemRenderer().renderAndDecorateItem(stack, x+i*16, y);
        minecraft.getItemRenderer().renderGuiItemDecorations(font, stack, x+i*16, y);
        if (mouseX >= x+i*16 && mouseX < x + 16 + i*16 && mouseY >= y && mouseY <= y + 16)
          this.renderTooltip(poseStack, stack, mouseX, mouseY);
      }
    }

    int x2 = x+50;
    drawString(poseStack, this.font, "-->", x2, y+4, 0xFFFFFF);

    int x3 = x2 + font.width("-->") + 2;
    for (int i = 0; i < offer.getSellingStacks().length; i++) {
      ItemStack stack = offer.getSellingStacks()[i];
      if (!stack.isEmpty()) {
        minecraft.getItemRenderer().renderAndDecorateItem(stack, x3+i*16, y);
        minecraft.getItemRenderer().renderGuiItemDecorations(font, stack, x3+i*16, y);
        if (mouseX >= x3+i*16 && mouseX < x3 + 16 + i*16 && mouseY >= y && mouseY <= y + 16)
          this.renderTooltip(poseStack, stack, mouseX, mouseY);
      }
    }

    drawString(poseStack, this.font, "|", x3 + 48, y+4, 0xFFFFFF);
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
  record NpcInfo(Component internalName, NpcEntity npcEntity, JsonObject asJson) {
    public static NpcInfo fromJson(JsonObject jsonObject) {
      return new NpcInfo(new TextComponent(jsonObject.get("internalName").getAsString()), NpcEntity.fromJson(Minecraft.getInstance().level, jsonObject), jsonObject);
    }
  }

  @OnlyIn(Dist.CLIENT)
  class NpcList extends ObjectSelectionList<SavedNpcsScreen.NpcList.Entry> {

    public NpcList() {
      super(SavedNpcsScreen.this.minecraft, SavedNpcsScreen.this.width, SavedNpcsScreen.this.height, 80, SavedNpcsScreen.this.height - 37, 24);

      List<SavedNpcsScreen.NpcInfo> savedNpcs = SavedNpcsScreen.this.savedNpcs
              .stream()
              .map(NpcInfo::fromJson)
              .toList();
      for(SavedNpcsScreen.NpcInfo savednpcsscreen$npcinfo : savedNpcs) {
        this.addEntry(new SavedNpcsScreen.NpcList.Entry(savednpcsscreen$npcinfo));
      }
    }

    public int getSelectedIndex() {
      return this.children().indexOf(getSelected());
    }

    public void setSelected(@Nullable SavedNpcsScreen.NpcList.Entry p_96472_) {
      super.setSelected(p_96472_);
      SavedNpcsScreen.this.updateButtonValidity(p_96472_ != null);
    }

    protected void update() {
      List<SavedNpcsScreen.NpcInfo> savedNpcs = SavedNpcsScreen.this.savedNpcs
              .stream()
              .map(NpcInfo::fromJson)
              .toList();
      clearEntries();
      for(SavedNpcsScreen.NpcInfo savednpcsscreen$npcinfo : savedNpcs) {
        this.addEntry(new SavedNpcsScreen.NpcList.Entry(savednpcsscreen$npcinfo));
      }
    }

    protected boolean isFocused() {
      return SavedNpcsScreen.this.getFocused() == this;
    }

    public boolean keyPressed(int p_96466_, int p_96467_, int p_96468_) {
      if (super.keyPressed(p_96466_, p_96467_, p_96468_)) {
        return true;
      } else {
        if ((p_96466_ == 257 || p_96466_ == 335) && this.getSelected() != null) {
          this.getSelected().select();
        }

        return false;
      }
    }

    @OnlyIn(Dist.CLIENT)
    public class Entry extends ObjectSelectionList.Entry<SavedNpcsScreen.NpcList.Entry> {
      private Component name;
      private final NpcEntity npcEntity;
      private JsonObject asJson;

      public Entry(NpcInfo npcInfo) {
        this.name = npcInfo.internalName();
        this.npcEntity = npcInfo.npcEntity();
        this.asJson = npcInfo.asJson();
      }

      public void render(PoseStack stack, int p_96490_, int y, int x, int p_96493_, int p_96494_, int p_96495_, int p_96496_, boolean p_96497_, float p_96498_) {
        float bbHeight = Math.max(npcEntity.getBbHeight(), 1f);
        int scale = (int) (18 / bbHeight);
        InventoryScreen.renderEntityInInventory(x+10, y+18, scale, -40, -20, npcEntity);
        SavedNpcsScreen.this.font.draw(stack, this.name, (float)(x + 18 + 5), (float)(y + 6), 16777215);
      }

      public boolean mouseClicked(double p_96481_, double p_96482_, int p_96483_) {
        if (p_96483_ == 0) {
          this.select();
        }

        return false;
      }

      void select() {
        SavedNpcsScreen.NpcList.this.setSelected(this);
        SavedNpcsScreen.this.selected = this.npcEntity;
        SavedNpcsScreen.this.selectedIndex = getSelectedIndex();
        SavedNpcsScreen.this.renameTextBox.setValue(this.name.getString());
      }

      public Component getNarration() {
        return new TranslatableComponent("narrator.select", this.name);
      }
    }
  }

}
