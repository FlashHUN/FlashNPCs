package flash.npcmod.client.gui.screen.dialogue;

import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.client.gui.widget.DialogueDisplayWidget;
import flash.npcmod.client.gui.widget.TextButton;
import flash.npcmod.core.client.dialogues.ClientDialogueUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CCallFunction;
import flash.npcmod.network.packets.client.CCallTrigger;
import flash.npcmod.network.packets.client.CRequestQuestCapabilitySync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class DialogueScreen extends Screen {

  private static final Random RND = new Random();

  public List<String> displayedText;
  private String[] currentOptions, currentOptionNames;

  private final NpcEntity npcEntity;
  private DialogueDisplayWidget dialogueDisplayWidget;

  private int npcTextColor;
  private String dialogueName;
  private String playerName;

  public DialogueScreen(String name, NpcEntity npcEntity) {
    super(TextComponent.EMPTY);
    dialogueName = name;

    ClientDialogueUtil.loadDialogue(name);
    ClientDialogueUtil.initDialogue();

    this.npcEntity = npcEntity;
    this.npcTextColor = npcEntity.getTextColor();

    displayedText = new ArrayList<>();

    playerName = Minecraft.getInstance().player.getName().getString();

    if (!ClientDialogueUtil.getCurrentText().isEmpty())
      addDisplayedNPCText(ClientDialogueUtil.getCurrentText());

    if (!ClientDialogueUtil.getCurrentFunction().isEmpty())
      PacketDispatcher.sendToServer(new CCallFunction(ClientDialogueUtil.getCurrentFunction(), npcEntity.getId()));

    PacketDispatcher.sendToServer(new CRequestQuestCapabilitySync());
  }

  public String getNpcName() {
    return npcEntity.getName().getString();
  }

  public int getNpcTextColor() {
    return npcTextColor;
  }

  public String getDialogueName() {
    return dialogueName;
  }

  @Override
  protected void init() {
    this.dialogueDisplayWidget = new DialogueDisplayWidget(this, 20, 20, width-120, height-60);

    resetOptionButtons();
  }

  public void resetOptionButtons() {
    currentOptions = ClientDialogueUtil.getDialogueOptionsFromChildren();
    currentOptionNames = ClientDialogueUtil.getDialogueOptionNamesFromChildren();
    clearWidgets();
    if (currentOptionNames.length > 0) {
      int x = 20;
      int width = this.width-120-x;
      int optionsHeight = 0;
      int[] optionHeights = new int[currentOptionNames.length];
      for (int i = 0; i < currentOptionNames.length; i++) {
        List<FormattedCharSequence> trimmedText = font.split(new TextComponent(currentOptions[i]), width);
        int height = trimmedText.size()*10;
        optionHeights[i] = height;
        optionsHeight += height;
      }
      int y = height-15-optionsHeight;
      this.dialogueDisplayWidget.setHeight(y-dialogueDisplayWidget.y-30);
      for (int i = 0; i < currentOptionNames.length; i++) {
        String name = currentOptionNames[i];
        String text = currentOptions[i];
        this.addRenderableWidget(new TextButton(x, y, width, new TextComponent(text.replaceAll("@p", playerName).replaceAll("@npc", getNpcName())), btn -> {
          if (!text.isEmpty()) {
            addDisplayedPlayerText(text);
          }
          ClientDialogueUtil.loadDialogueOption(name);
          if (!ClientDialogueUtil.getCurrentResponse().isEmpty()) {
            addDisplayedNPCText(ClientDialogueUtil.getCurrentResponse());
          }
          if (!ClientDialogueUtil.getCurrentTrigger().isEmpty()) {
            Main.LOGGER.info("Sending Call trigger");
            PacketDispatcher.sendToServer(new CCallTrigger(ClientDialogueUtil.getCurrentTrigger(), this.npcEntity.getId()));
          }
          this.dialogueDisplayWidget.clampScroll(0);
          if (!ClientDialogueUtil.getCurrentFunction().isEmpty()) {
            PacketDispatcher.sendToServer(new CCallFunction(ClientDialogueUtil.getCurrentFunction(), this.npcEntity.getId()));
          }

          resetOptionButtons();
        }));
        y += optionHeights[i]+2;
      }
    }
  }

  public void chooseRandomOption() {
    List<TextButton> options = new ArrayList<>();
    this.children().forEach(btn -> {
      if (btn instanceof TextButton) options.add((TextButton) btn);
    });

    options.get(RND.nextInt(options.size())).onPress();
  }

  public void addDisplayedPlayerText(String text) {
    addDisplayedText(playerName, text);
  }

  public void addDisplayedNPCText(String text) {
    addDisplayedText(getNpcName(), text);
  }

  private void addDisplayedText(String name, String text) {
    String newText = name + ": " + text.replaceAll("@p", playerName).replaceAll("@npc", getNpcName());
    displayedText.add(newText);
  }

  @Override
  public void tick() {

  }

  @Override
  public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    renderBackground(matrixStack);

    // Render the NPC on screen
    {
      int x = width-60;
      int y = (int) (height/4*2.5f);
      float bbHeight = Math.max(npcEntity.getBbHeight(), 1f);
      int scale = (int) (height / 3f / bbHeight);
      InventoryScreen.renderEntityInInventory(x, y, scale, 40, -20, npcEntity);
    }

    // Render the actual dialogue text
    this.dialogueDisplayWidget.renderButton(matrixStack, mouseX, mouseY, partialTicks);

    // Render a line between the text and the options
    {
      int x = 20;
      int maxX = this.width-120;
      int y = this.dialogueDisplayWidget.y + this.dialogueDisplayWidget.getHeight();
      hLine(matrixStack, x, maxX, y + 9, 0xFF000000);
      hLine(matrixStack, x, maxX, y + 10, 0xFFFFFFFF);
      hLine(matrixStack, x, maxX, y + 11, 0xFF000000);
    }
    // Render the dialogue options
    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    this.dialogueDisplayWidget.mouseScrolled(mouseX, mouseY, delta);
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
