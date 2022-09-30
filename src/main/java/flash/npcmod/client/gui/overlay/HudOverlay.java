package flash.npcmod.client.gui.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import flash.npcmod.Main;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.client.ScreenHelper;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.events.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

// TODO check out overlay registry
@OnlyIn(Dist.CLIENT)
public class HudOverlay extends GuiComponent {

  private static int width, height;
  private static final Minecraft minecraft = Minecraft.getInstance();

  private static final ResourceLocation TEXTURES = new ResourceLocation(Main.MODID, "textures/gui/hud.png");

  private static String previousTrackedQuest;
  private static int trackedQuestUpdateTick;

  public HudOverlay() {
    previousTrackedQuest = "";
    trackedQuestUpdateTick = 0;
  }

  @SubscribeEvent
  public void renderGameOverlay(RenderGameOverlayEvent.Pre event) {
    if (minecraft.player != null && minecraft.player.isAlive()) {
      if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
        width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        height = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        PoseStack matrixStack = event.getMatrixStack();
        IQuestCapability questCapability = QuestCapabilityProvider.getCapability(minecraft.player);

        renderTrackedQuest(matrixStack, questCapability);

        // TODO party system and hud
      }
    }
  }

  private void renderTrackedQuest(PoseStack matrixStack, IQuestCapability capability) {
    QuestInstance trackedQuestInstance = capability.getTrackedQuestInstance();
    if (trackedQuestInstance != null) {
      Quest trackedQuest = trackedQuestInstance.getQuest();
      String name = trackedQuest.getDisplayName();
      List<QuestObjective> objectives = trackedQuest.getObjectives();

      List<QuestObjective> visibleObjectives = new ArrayList<>();
      objectives.forEach(objective -> { if (!objective.isHidden() ) visibleObjectives.add(objective); });

      int oneFourthWidth = width/4;

      int gradientOffset = 20;
      int nameWidth = minecraft.font.width(name);
      int actualWidth = Math.min(oneFourthWidth, nameWidth);

      List<FormattedCharSequence> multiLineName = minecraft.font.split(new TextComponent(name), actualWidth);

      int nameHeight = 5 + multiLineName.size()*(minecraft.font.lineHeight+2);
      int objectivesHeight = 6 + visibleObjectives.size()*(minecraft.font.lineHeight+2);

      int hudWidth = actualWidth + gradientOffset + 2;
      int hudX = width-hudWidth;
      int hudY = height/2-(nameHeight + objectivesHeight)/2;
      {
        int nameBackgroundColor = 0xAA000000;

        ScreenHelper.sidewaysFillGradient(matrixStack, hudX, hudY - 1, hudX + gradientOffset, hudY + nameHeight, 0x00000000, nameBackgroundColor);
        fill(matrixStack, hudX + gradientOffset, hudY - 1, width, hudY + nameHeight, nameBackgroundColor);

        int lineColor = 0x99000000;
        ScreenHelper.sidewaysFillGradient(matrixStack, hudX, hudY, hudX+gradientOffset, hudY+1, 0x22000000, lineColor);
        fill(matrixStack, hudX+gradientOffset, hudY, width, hudY+1, lineColor);
        ScreenHelper.sidewaysFillGradient(matrixStack, hudX, hudY+nameHeight-2, hudX+gradientOffset, hudY+nameHeight-1, 0x22000000, lineColor);
        fill(matrixStack, hudX+gradientOffset, hudY+nameHeight-2, width, hudY+nameHeight-1, lineColor);

        ResourceLocation icon = trackedQuest.canComplete() ? ClientEvents.QUEST_COMPLETE_ICON : ClientEvents.QUEST_ICON;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, icon);
        int iconWidth = minecraft.font.lineHeight+2;
        blit(matrixStack, hudX+3, hudY+(nameHeight-iconWidth)/2, iconWidth, iconWidth, 0, 0, 32, 32, 32, 32);

        for (int i = 0; i < multiLineName.size(); i++) {
          FormattedCharSequence processor = multiLineName.get(i);
          int nameW = (int)minecraft.font.getSplitter().stringWidth(processor);
          int offset = (actualWidth-nameW)/2;
          minecraft.font.draw(matrixStack, processor, width - 5 - actualWidth + offset, hudY + 4 + (minecraft.font.lineHeight+2)*i, 0xFFFF00);
        }
      }
      ScreenHelper.sidewaysFillGradient(matrixStack, hudX, hudY+nameHeight, width, hudY+nameHeight+objectivesHeight, 0x00000000, 0x87000000);

      for (int i = 0; i < visibleObjectives.size(); i++) {
        QuestObjective objective = visibleObjectives.get(i);
        int y = hudY + nameHeight + 3 + i * (minecraft.font.lineHeight + 2);
        int iconSize = minecraft.font.lineHeight + 2;
        int x = width - iconSize - 2;
        drawObjective(matrixStack, x, y, objective, oneFourthWidth);
      }
    }
  }

  public static void drawObjective(PoseStack matrixStack, int x, int y, QuestObjective objective, int maxObjectiveNameWidth) {
    String progress = objective.getProgress() + "/" + objective.getAmount();
    String addon = ": " + (objective.shouldDisplayProgress() ? progress + " " : "");
    String objectiveName = objective.getName() + addon;
    int objectiveNameWidth = minecraft.font.width(objectiveName);
    if (maxObjectiveNameWidth > 0 && objectiveNameWidth > maxObjectiveNameWidth) {
      objectiveName = minecraft.font.plainSubstrByWidth(objective.getName(), maxObjectiveNameWidth - minecraft.font.width(addon) - minecraft.font.width("...")) + "..." + addon;
      objectiveNameWidth = maxObjectiveNameWidth;
    }
    boolean isOptional = objective.isOptional();
    int iconSize = minecraft.font.lineHeight + 2;
    drawString(matrixStack, minecraft.font, objectiveName, x - 2 - objectiveNameWidth, y + 2, isOptional ? 0x96B7FF : 0xFFFFFF);
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, TEXTURES);
    blit(matrixStack, x, y, iconSize, iconSize, objective.isComplete() ? 24 : (isOptional ? 48 : 0), 0, 24, 24, 256, 256);
  }

}
