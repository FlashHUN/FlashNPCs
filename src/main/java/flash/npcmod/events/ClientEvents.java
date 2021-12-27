package flash.npcmod.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import flash.npcmod.Main;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.client.gui.screen.quests.QuestLogScreen;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.item.NpcEditorItem;
import flash.npcmod.item.QuestEditorItem;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CHandleNpcEditorRightClick;
import flash.npcmod.network.packets.client.CRequestQuestEditor;
import flash.npcmod.network.packets.client.CTrackQuest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

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
        minecraft.setScreen(new QuestLogScreen());
      }
    },
    UNTRACK("untrack", GLFW.GLFW_KEY_PERIOD) {
      @Override
      public void onPress() {
        PacketDispatcher.sendToServer(new CTrackQuest(""));
      }
    };

    KeyMapping keyBinding;

    KeyBindings(String name, int keyCode) {
      keyBinding = new KeyMapping("key."+name, keyCode, "key.categories.flashnpcs");
    }

    public KeyMapping get() {
      return keyBinding;
    }

    public void onPress() {}
  }

  @SubscribeEvent
  public void onKeyInput(InputEvent.KeyInputEvent event) {
    Player player = minecraft.player;
    if (minecraft.isWindowActive() && player != null && player.isAlive() && event.getAction() == 1) {
      for (KeyBindings keyBind : KeyBindings.values()) {
        if (keyBind.get().consumeClick())
          keyBind.onPress();
      }
    }
  }

  // Item Usage
  @SubscribeEvent
  public void onClick(InputEvent.ClickInputEvent event) {
    if (minecraft.player != null && minecraft.player.isAlive()) {
      if (event.isUseItem()) {
        ItemStack stack = minecraft.player.getItemInHand(event.getHand());
        if (minecraft.player.hasPermissions(4) && minecraft.player.isCreative()) {
          if (stack.getItem() instanceof NpcEditorItem) {
            HitResult rayTraceResult = minecraft.hitResult;
            if (rayTraceResult.getType().equals(HitResult.Type.MISS) && minecraft.player.isDiscrete()) {
              // If we right click on the air while sneaking, open the function builder
              PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick());
            } else if (rayTraceResult.getType().equals(HitResult.Type.ENTITY)) {
              // If we right click on an entity and it is an NPC, edit it
              EntityHitResult result = (EntityHitResult) rayTraceResult;
              Entity entity = result.getEntity();
              if (entity instanceof NpcEntity) {
                PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick(entity.getId()));
              }
            } else if (rayTraceResult.getType().equals(HitResult.Type.BLOCK)) {
              // If we right click on a block, create a new npc and edit it
              BlockHitResult result = (BlockHitResult) rayTraceResult;
              BlockPos pos = result.getBlockPos();
              PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick(pos));
            }
          } else if (stack.getItem() instanceof QuestEditorItem) {
            PacketDispatcher.sendToServer(new CRequestQuestEditor());
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
      if (instance.getPickedUpFrom().equals(npcEntity.getUUID())) shouldDrawIcon = true; canComplete = instance.getQuest().canComplete(); break;
    }

    if (!shouldDrawIcon) return;

    PoseStack matrixStack = event.getPoseStack();
    MultiBufferSource bufferIn = event.getMultiBufferSource();
    int lightIn = event.getPackedLight();
    float partialTicks = event.getPartialTick();

    renderIcon(npcEntity, matrixStack, bufferIn, lightIn, partialTicks, canComplete);
  }

  protected void renderIcon(NpcEntity npcEntity, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, float partialTicks, boolean canComplete) {
    ResourceLocation icon = canComplete ? QUEST_COMPLETE_ICON : QUEST_IN_PROGRESS_ICON;
    matrixStackIn.pushPose();
    matrixStackIn.translate(0D, npcEntity.getBbHeight() + 0.5D, 0D);
    matrixStackIn.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
    matrixStackIn.scale(-0.025F, -0.025F, 0.025F);
    matrixStackIn.translate(0D, -1D, 0D);

    float size = 12f;
    float xOffset = -size/2;
    float yOffset = -4f - size - Mth.sin(((float) npcEntity.tickCount + partialTicks) / 10.0F);

    VertexConsumer builder = bufferIn.getBuffer(RenderType.text(icon));
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

      VertexConsumer builderSeeThrough = bufferIn.getBuffer(RenderType.textSeeThrough(icon));
      vertex(builderSeeThrough, matrixStackIn, xOffset, yOffset+size, 0F, 0F, 1F, alpha, packedLightIn);
      vertex(builderSeeThrough, matrixStackIn, xOffset + size, yOffset+size, 0F, 1F, 1F, alpha, packedLightIn);
      vertex(builderSeeThrough, matrixStackIn, xOffset + size, yOffset+0F, 0F, 1F, 0F, alpha, packedLightIn);
      vertex(builderSeeThrough, matrixStackIn, xOffset, yOffset+0F, 0F, 0F, 0F, alpha, packedLightIn);
    }

    matrixStackIn.popPose();
  }

  private static void vertex(VertexConsumer builder, PoseStack matrixStack, float x, float y, float z, float u, float v, int light) {
    vertex(builder, matrixStack, x, y, z, u, v, 255, light);
  }

  private static void vertex(VertexConsumer builder, PoseStack matrixStack, float x, float y, float z, float u, float v, int alpha, int light) {
    PoseStack.Pose entry = matrixStack.last();
    builder.vertex(entry.pose(), x, y, z)
        .color(255, 255, 255, alpha)
        .uv(u, v)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(light)
        .normal(entry.normal(), 0F, 0F, -1F)
        .endVertex();
  }

}
