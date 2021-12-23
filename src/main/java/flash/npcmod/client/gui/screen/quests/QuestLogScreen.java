package flash.npcmod.client.gui.screen.quests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.capability.quests.QuestCapabilityAttacher;
import flash.npcmod.client.gui.overlay.HudOverlay;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.events.ClientEvents;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CAbandonQuest;
import flash.npcmod.network.packets.client.CTrackQuest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class QuestLogScreen extends Screen {

  private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MODID, "textures/gui/quest_log.png");

  private ImageButton[] questButtons;
  private ImageButton abandonQuestButton, trackQuestButton;

  private List<QuestInstance> acceptedQuests;
  private QuestInstance currentViewingQuest;

  private int guiX, guiY;
  private int scrollOffset;
  private double scrollY;
  private boolean isScrolling;
  private int objectiveScrollY;

  private static final int LINEHEIGHT = Minecraft.getInstance().font.lineHeight+2;
  private static final TranslatableComponent TITLE = new TranslatableComponent("screen.quest_log.title"),
      OBJECTIVES = new TranslatableComponent("screen.quest_log.objectives"),
      REWARD = new TranslatableComponent("screen.quest_log.reward"),
      ITEMREWARD = new TranslatableComponent("screen.quest_log.reward.items"),
      TRACK = new TranslatableComponent("screen.quest_log.track"),
      ABANDON = new TranslatableComponent("screen.quest_log.abandon");

  public QuestLogScreen() {
    super(TextComponent.EMPTY);

    acceptedQuests = new ArrayList<>();
  }

  @Override
  protected void init() {
    guiX = width/2-230/2;
    guiY = height/2-182/2;

    questButtons = new ImageButton[10];
    for (int i = 0; i < questButtons.length; i++) {
      int j = i;
      questButtons[i] = this.addRenderableWidget(new ImageButton(guiX+8, guiY+14+i*16, 102, 16, 0, 198, 16, TEXTURE, btn -> {
        selectQuest(j);
      }));
      questButtons[i].visible = acceptedQuests.size() > i+scrollOffset;
    }

    trackQuestButton = this.addRenderableWidget(new ImageButton(guiX+120, guiY+142, 102, 16, 0, 198, 16, TEXTURE, btn -> {
      if (currentViewingQuest != null && acceptedQuests.contains(currentViewingQuest)) {
        PacketDispatcher.sendToServer(new CTrackQuest(currentViewingQuest.getQuest().getName()));
      }
    }));

    abandonQuestButton = this.addRenderableWidget(new ImageButton(guiX+120, guiY+158, 102, 16, 0, 198, 16, TEXTURE, btn -> {
      if (currentViewingQuest != null && acceptedQuests.contains(currentViewingQuest)) {
        PacketDispatcher.sendToServer(new CAbandonQuest(currentViewingQuest));
        currentViewingQuest = null;
      }
    }));
  }

  private void selectQuest(int i) {
    if (i+ scrollOffset < acceptedQuests.size()) {
      objectiveScrollY = 0;
      this.currentViewingQuest = acceptedQuests.get(i+scrollOffset);
    }
  }

  @Override
  public void tick() {
    acceptedQuests = QuestCapabilityAttacher.getCapability(minecraft.player).getAcceptedQuests();
    if (acceptedQuests.size() > 10 && scrollOffset > acceptedQuests.size()-10)
      updateScrollY(0, 0, 0);

    if (!acceptedQuests.contains(currentViewingQuest))
      currentViewingQuest = null;

    for (int i = 0; i < questButtons.length; i++) {
      questButtons[i].visible = acceptedQuests.size() > i+scrollOffset;
    }

    boolean isViewingQuest = currentViewingQuest != null;
    trackQuestButton.visible = isViewingQuest;
    abandonQuestButton.visible = isViewingQuest;
  }

  @Override
  public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, TEXTURE);
    blit(matrixStack, guiX, guiY, 0, 0, 230, 182);

    font.draw(matrixStack, TITLE.getString(), guiX+7, guiY+4, 4210752);

    if (acceptedQuests.size() > 10) {
      matrixStack.pushPose();
      matrixStack.translate(0, scrollY, 0);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, TEXTURE);
      blit(matrixStack, guiX+113, guiY+14, 102, 182+(isMouseOnScrollBar(mouseX, mouseY) ? 7 : 0), 4, 7);
      matrixStack.popPose();
    }

    super.render(matrixStack, mouseX, mouseY, partialTicks);

    if (currentViewingQuest != null && acceptedQuests.contains(currentViewingQuest)) {
      String givenBy = currentViewingQuest.getPickedUpFromName();

      font.draw(matrixStack, givenBy, guiX+171-font.width(givenBy)/2, guiY+4, 4210752);

      Quest quest = currentViewingQuest.getQuest();

      ResourceLocation questIcon = quest.canComplete() ? ClientEvents.QUEST_COMPLETE_ICON : ClientEvents.QUEST_ICON;
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, questIcon);
      blit(matrixStack, guiX + 165, guiY + 16, 12, 12, 0, 0, 32, 32, 32, 32);

      String questName = quest.getDisplayName();
      List<QuestObjective> objectives = quest.getObjectives();
      List<QuestObjective> visibleObjectives = new ArrayList<>();
      objectives.forEach(objective -> {
        if (!objective.isHidden()) visibleObjectives.add(objective);
      });
      List<ItemStack> itemRewards = quest.getItemRewards();
      int xpReward = quest.getXpReward();

      int yOffset = LINEHEIGHT;

      if (font.width(questName) > 100) {
        List<FormattedCharSequence> trimmedQuestName = font.split(new TextComponent(questName), 100);
        yOffset = trimmedQuestName.size() * LINEHEIGHT;
        for (int i = 0; i < trimmedQuestName.size(); i++) {
          FormattedCharSequence processor = trimmedQuestName.get(i);
          int nameWidth = (int) font.getSplitter().stringWidth(processor);
          font.draw(matrixStack, processor, guiX + 171 - nameWidth / 2, guiY + 29 + LINEHEIGHT * i, 0xFFFF00);
        }
      } else
        drawCenteredString(matrixStack, font, questName, guiX + 171, guiY + 29, 0xFFFF00);

      drawString(matrixStack, font, OBJECTIVES, guiX + 123, guiY + 29 + yOffset, 0xFFFFFF);
      yOffset += LINEHEIGHT;

      int maxVisibleObjectives = Math.min(6 - (yOffset / LINEHEIGHT - 1), visibleObjectives.size());
      for (int i = 0; i < maxVisibleObjectives; i++) {
        if (i+objectiveScrollY < visibleObjectives.size()) {
          QuestObjective objective = visibleObjectives.get(i + objectiveScrollY);
          int x = guiX + 218 - LINEHEIGHT;
          int y = guiY + 29 + yOffset + LINEHEIGHT * i;
          if (visibleObjectives.size() > maxVisibleObjectives && i+objectiveScrollY <= visibleObjectives.size()-maxVisibleObjectives && i == maxVisibleObjectives - 1)
            drawCenteredString(matrixStack, font, "...", guiX + 171, y, 0xFFFFFF);
          else {
            HudOverlay.drawObjective(matrixStack, x, y, objective, 80);
            if (mouseX >= guiX + 121 && mouseX <= guiX + 221 && mouseY >= y && mouseY < y + LINEHEIGHT)
              this.renderTooltip(matrixStack, new TextComponent(objective.getName()), mouseX, mouseY);
          }
        } else {
          objectiveScrollY = 0;
        }
      }

      drawString(matrixStack, font, REWARD, guiX + 123, guiY + 107, 0xFFFFFF);
      if (xpReward > 0)
        drawString(matrixStack, font, new TranslatableComponent("screen.quest_log.reward.xp", xpReward), guiX + 126, guiY + 107 + LINEHEIGHT, 0xFFFFFF);

      if (itemRewards.size() > 0) {
        int itemRewardY = guiY + 107 + LINEHEIGHT * 2;
        drawString(matrixStack, font, ITEMREWARD, guiX + 126, itemRewardY, 0xFFFFFF);

        int itemRewardStringWidth = font.width(ITEMREWARD);
        int maxDisplayedRewards = Math.min((93 - itemRewardStringWidth) / 16, itemRewards.size());
        int itemRewardX = guiX + 123 + itemRewardStringWidth + 2;
        itemRewardY -= 4;
        for (int i = 0; i < maxDisplayedRewards; i++) {
          if (itemRewards.size() > maxDisplayedRewards && i == maxDisplayedRewards - 1)
            drawString(matrixStack, font, "...", itemRewardX, itemRewardY, 0xFFFFFF);
          else {
            ItemStack itemStack = itemRewards.get(i);
            minecraft.getItemRenderer().renderAndDecorateItem(itemStack, itemRewardX, itemRewardY);
            minecraft.getItemRenderer().renderGuiItemDecorations(font, itemStack, itemRewardX, itemRewardY);
            if (mouseX >= itemRewardX && mouseX <= itemRewardX + 16 && mouseY >= itemRewardY && mouseY <= itemRewardY + 16)
              this.renderTooltip(matrixStack, itemStack, mouseX, mouseY);
          }
          itemRewardX += 16;
        }
      }
      drawCenteredString(matrixStack, font, TRACK, trackQuestButton.x + trackQuestButton.getWidth() / 2, trackQuestButton.y + 4, 0xFFFFFF);
      drawCenteredString(matrixStack, font, ABANDON, abandonQuestButton.x + abandonQuestButton.getWidth() / 2, abandonQuestButton.y + 4, 0xFFFFFF);
    }

    for (int i = 0; i < questButtons.length; i++) {
      Button questButton = questButtons[i];
      if (questButton.visible && acceptedQuests.size() > i+ scrollOffset) {
        Quest quest = acceptedQuests.get(i+ scrollOffset).getQuest();
        String questName = quest.getDisplayName();

        if (font.width(questName) > 85)
          questName = font.plainSubstrByWidth(questName, 85-font.width("...")) + "...";

        drawString(matrixStack, font, questName, questButton.x+15, questButton.y+4, questButton.isHoveredOrFocused() ? 0xFFFF00 : 0xFFFFFF);

        ResourceLocation questIcon = quest.canComplete() ? ClientEvents.QUEST_COMPLETE_ICON : ClientEvents.QUEST_ICON;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, questIcon);
        blit(matrixStack, questButton.x+2, questButton.y+2, 12, 12, 0, 0, 32, 32, 32, 32);
      }
    }
  }

  private boolean isMouseOnScrollBar(double mouseX, double mouseY) {
    return mouseX >= guiX+113 && mouseX <= guiX+117 && mouseY >= 14 && mouseY <= 174;
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (button == 0 && isMouseOnScrollBar(mouseX, mouseY)) this.isScrolling = true;
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    if (button != 0) this.isScrolling = false;
    else if (isScrolling) updateScrollY(mouseX, mouseY, dragY);
    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (button == 0) isScrolling = false;
    return super.mouseReleased(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    updateScrollY(mouseX, mouseY, -delta*6);
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  private void updateScrollY(double mouseX, double mouseY, double delta) {
    if (currentViewingQuest != null && mouseX >= guiX+120 && mouseX <= guiX+222 && mouseY >= guiY+14 && mouseY <= guiY+174) {
      String questName = currentViewingQuest.getQuest().getDisplayName();
      List<FormattedCharSequence> trimmedQuestName = font.split(new TextComponent(questName), 100);
      int yOffset = trimmedQuestName.size() * LINEHEIGHT + LINEHEIGHT;

      List<QuestObjective> objectives = currentViewingQuest.getQuest().getObjectives();
      List<QuestObjective> visibleObjectives = new ArrayList<>();
      objectives.forEach(objective -> {
        if (!objective.isHidden()) visibleObjectives.add(objective);
      });

      int maxVisibleObjectives = Math.min(6 - (yOffset / LINEHEIGHT - 1), visibleObjectives.size());
      if (maxVisibleObjectives < visibleObjectives.size()) {
        int d = delta > 0 ? 1 : -1;
        objectiveScrollY = Mth.clamp(objectiveScrollY + d, 0, visibleObjectives.size()-maxVisibleObjectives);
      } else {
        objectiveScrollY = 0;
      }
    }
    else {
      if (acceptedQuests.size() > 10) {
        this.scrollY = Mth.clamp(scrollY + delta, 0, 153);
        this.scrollOffset = (int) (scrollY * (acceptedQuests.size() - 10) / 153);
      } else {
        this.scrollY = 0.0;
        this.scrollOffset = 0;
      }
    }
  }

  public boolean isPauseScreen() {
    return false;
  }
}
