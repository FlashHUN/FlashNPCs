package flash.npcmod.events;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import flash.npcmod.Main;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.client.gui.screen.quests.QuestLogScreen;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.item.NpcEditorItem;
import flash.npcmod.item.NpcSaveToolItem;
import flash.npcmod.item.QuestEditorItem;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CHandleNpcEditorRightClick;
import flash.npcmod.network.packets.client.CHandleNpcSaveToolRightClick;
import flash.npcmod.network.packets.client.CRequestQuestEditor;
import flash.npcmod.network.packets.client.CTrackQuest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientEvents {

  private static final Minecraft minecraft = Minecraft.getInstance();

  public static final ResourceLocation QUEST_COMPLETE_ICON = new ResourceLocation(Main.MODID, "textures/render/icons/complete_quest.png");
  public static final ResourceLocation QUEST_IN_PROGRESS_ICON = new ResourceLocation(Main.MODID, "textures/render/icons/in_progress_quest.png");
  public static final ResourceLocation QUEST_ICON = new ResourceLocation(Main.MODID, "textures/render/icons/new_quest.png");

  // Key Input
  public enum KeyBindings {
    OPEN_QUEST_LOG("quest_log", GLFW.GLFW_KEY_K) {
      @Override
      public void onPress() {
        minecraft.displayGuiScreen(new QuestLogScreen());
      }
    },
    UNTRACK("untrack", GLFW.GLFW_KEY_PERIOD) {
      @Override
      public void onPress() {
        PacketDispatcher.sendToServer(new CTrackQuest(""));
      }
    };

    KeyBinding keyBinding;

    KeyBindings(String name, int keyCode) {
      keyBinding = new KeyBinding("key."+name, keyCode, "key.categories.flashnpcs");
    }

    public KeyBinding get() {
      return keyBinding;
    }

    public void onPress() {}
  }

  @SubscribeEvent
  public void onKeyInput(InputEvent.KeyInputEvent event) {
    PlayerEntity player = minecraft.player;
    if (minecraft.isGameFocused() && player != null && player.isAlive() && event.getAction() == 1) {
      for (KeyBindings keyBind : KeyBindings.values()) {
        if (keyBind.get().isPressed())
          keyBind.onPress();
      }
    }
  }

  // Item Usage
  @SubscribeEvent
  public void onClick(InputEvent.ClickInputEvent event) {
    if (minecraft.player != null && minecraft.player.isAlive()) {
      if (event.isUseItem()) {
        ItemStack stack = minecraft.player.getHeldItem(event.getHand());
        if (minecraft.player.hasPermissionLevel(4) && minecraft.player.isCreative()) {
          if (stack.getItem() instanceof NpcEditorItem) {
            RayTraceResult rayTraceResult = minecraft.objectMouseOver;
            if (rayTraceResult.getType().equals(RayTraceResult.Type.MISS) && minecraft.player.isDiscrete()) {
              // If we right click on the air while sneaking, open the function builder
              PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick());
            } else if (rayTraceResult.getType().equals(RayTraceResult.Type.ENTITY)) {
              // If we right click on an entity and it is an NPC, edit it
              EntityRayTraceResult result = (EntityRayTraceResult) rayTraceResult;
              Entity entity = result.getEntity();
              if (entity instanceof NpcEntity) {
                PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick(entity.getEntityId()));
              }
            } else if (rayTraceResult.getType().equals(RayTraceResult.Type.BLOCK)) {
              // If we right click on a block, create a new npc and edit it
              BlockRayTraceResult result = (BlockRayTraceResult) rayTraceResult;
              BlockPos pos = result.getPos();
              PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick(pos));
            }
          } else if (stack.getItem() instanceof QuestEditorItem) {
            PacketDispatcher.sendToServer(new CRequestQuestEditor());
          } else if (stack.getItem() instanceof NpcSaveToolItem) {
            if (minecraft.player.isDiscrete()) {
              RayTraceResult rayTraceResult = minecraft.objectMouseOver;
              if (rayTraceResult.getType().equals(RayTraceResult.Type.ENTITY)) {
                // If we right click on an NPC, save it
                EntityRayTraceResult result = (EntityRayTraceResult) rayTraceResult;
                Entity entity = result.getEntity();
                if (entity instanceof NpcEntity) {
                  PacketDispatcher.sendToServer(new CHandleNpcSaveToolRightClick(entity.getEntityId()));
                }
              } else if (rayTraceResult.getType().equals(RayTraceResult.Type.BLOCK)) {
                // If we right click on a block, open our saved npcs gui
                BlockRayTraceResult result = (BlockRayTraceResult) rayTraceResult;
                BlockPos pos = result.getPos();
                PacketDispatcher.sendToServer(new CHandleNpcSaveToolRightClick(pos));
              }
            }
          }
        }
      }
    }
  }

  @SubscribeEvent
  public void renderQuestIconAboveNpc(RenderNameplateEvent event) {
    if (minecraft.player == null || !minecraft.player.isAlive()) return;
    if (!(event.getEntity() instanceof NpcEntity)) return;

    NpcEntity npcEntity = (NpcEntity) event.getEntity();

    IQuestCapability capability = QuestCapabilityProvider.getCapability(minecraft.player);

    boolean shouldDrawIcon = false;
    boolean canComplete = false;
    List<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
    for (QuestInstance instance : acceptedQuests) {
      if (instance.getPickedUpFrom().equals(npcEntity.getUniqueID())) shouldDrawIcon = true; canComplete = instance.getQuest().canComplete(); break;
    }

    if (!shouldDrawIcon) return;

    MatrixStack matrixStack = event.getMatrixStack();
    IRenderTypeBuffer bufferIn = event.getRenderTypeBuffer();
    int lightIn = event.getPackedLight();
    float partialTicks = event.getPartialTicks();

    renderIcon(npcEntity, matrixStack, bufferIn, lightIn, partialTicks, canComplete);
  }

  protected void renderIcon(NpcEntity npcEntity, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, float partialTicks, boolean canComplete) {
    ResourceLocation icon = canComplete ? QUEST_COMPLETE_ICON : QUEST_IN_PROGRESS_ICON;
    matrixStackIn.push();
    matrixStackIn.translate(0D, npcEntity.getHeight() + 0.5D, 0D);
    matrixStackIn.rotate(minecraft.getRenderManager().getCameraOrientation());
    matrixStackIn.scale(-0.025F, -0.025F, 0.025F);
    matrixStackIn.translate(0D, -1D, 0D);

    float size = 12f;
    float xOffset = -size/2;
    float yOffset = -4f - size - MathHelper.sin(((float) npcEntity.ticksExisted + partialTicks) / 10.0F);

    IVertexBuilder builder = bufferIn.getBuffer(RenderType.getText(icon));
    int alpha = 32;

    if (npcEntity.isDiscrete()) {
      vertex(builder, matrixStackIn, xOffset, yOffset + size, 0F, 0F, 1F, alpha, packedLightIn);
      vertex(builder, matrixStackIn, xOffset + size, yOffset + size, 0F, 1F, 1F, alpha, packedLightIn);
      vertex(builder, matrixStackIn, xOffset + size, yOffset, 0F, 1F, 0F, alpha, packedLightIn);
      vertex(builder, matrixStackIn, xOffset, yOffset, 0F, 0F, 0F, alpha, packedLightIn);
    } else {
      vertex(builder, matrixStackIn, xOffset, yOffset + size, 0F, 0F, 1F, packedLightIn);
      vertex(builder, matrixStackIn, xOffset + size, yOffset + size, 0F, 1F, 1F, packedLightIn);
      vertex(builder, matrixStackIn, xOffset + size, yOffset, 0F, 1F, 0F, packedLightIn);
      vertex(builder, matrixStackIn, xOffset, yOffset, 0F, 0F, 0F, packedLightIn);

      IVertexBuilder builderSeeThrough = bufferIn.getBuffer(RenderType.getTextSeeThrough(icon));
      vertex(builderSeeThrough, matrixStackIn, xOffset, yOffset+size, 0F, 0F, 1F, alpha, packedLightIn);
      vertex(builderSeeThrough, matrixStackIn, xOffset + size, yOffset+size, 0F, 1F, 1F, alpha, packedLightIn);
      vertex(builderSeeThrough, matrixStackIn, xOffset + size, yOffset+0F, 0F, 1F, 0F, alpha, packedLightIn);
      vertex(builderSeeThrough, matrixStackIn, xOffset, yOffset+0F, 0F, 0F, 0F, alpha, packedLightIn);
    }

    matrixStackIn.pop();
  }

  private static void vertex(IVertexBuilder builder, MatrixStack matrixStack, float x, float y, float z, float u, float v, int light) {
    vertex(builder, matrixStack, x, y, z, u, v, 255, light);
  }

  private static void vertex(IVertexBuilder builder, MatrixStack matrixStack, float x, float y, float z, float u, float v, int alpha, int light) {
    MatrixStack.Entry entry = matrixStack.getLast();
    builder.pos(entry.getMatrix(), x, y, z)
        .color(255, 255, 255, alpha)
        .tex(u, v)
        .overlay(OverlayTexture.NO_OVERLAY)
        .lightmap(light)
        .normal(entry.getNormal(), 0F, 0F, -1F)
        .endVertex();
  }

}
