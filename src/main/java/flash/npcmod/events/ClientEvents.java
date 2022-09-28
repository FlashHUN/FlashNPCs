package flash.npcmod.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector4f;
import flash.npcmod.Main;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.client.gui.screen.quests.QuestLogScreen;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.item.BehaviorEditorItem;
import flash.npcmod.item.NpcEditorItem;
import flash.npcmod.item.NpcSaveToolItem;
import flash.npcmod.item.QuestEditorItem;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CHandleNpcEditorRightClick;
import flash.npcmod.network.packets.client.CHandleNpcSaveToolRightClick;
import flash.npcmod.network.packets.client.CRequestQuestEditor;
import flash.npcmod.network.packets.client.CTrackQuest;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
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
            if (rayTraceResult.getType().equals(HitResult.Type.MISS)) {
              if (minecraft.player.isDiscrete()) {
                // If we right-click on the air while sneaking, open the function builder
                PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick());
              }  else {
                // Otherwise open the dialogue builder.
                PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick(""));
              }
            } else if (rayTraceResult.getType().equals(HitResult.Type.ENTITY)) {
              // If we right-click on an entity and if it is an NPC, edit it
              EntityHitResult result = (EntityHitResult) rayTraceResult;
              Entity entity = result.getEntity();
              if (entity instanceof NpcEntity) {
                PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick(entity.getId()));
              }
            } else if (rayTraceResult.getType().equals(HitResult.Type.BLOCK)) {
              // If we right-click on a block, create a new npc and edit it
              BlockHitResult result = (BlockHitResult) rayTraceResult;
              BlockPos pos = result.getBlockPos();
              PacketDispatcher.sendToServer(new CHandleNpcEditorRightClick(pos));
            }
          } else if (stack.getItem() instanceof QuestEditorItem) {
            PacketDispatcher.sendToServer(new CRequestQuestEditor());
          } else if (stack.getItem() instanceof NpcSaveToolItem) {
            if (minecraft.player.isDiscrete()) {
              HitResult rayTraceResult = minecraft.hitResult;
              if (rayTraceResult.getType().equals(HitResult.Type.ENTITY)) {
                // If we right-click on an NPC, save it
                EntityHitResult result = (EntityHitResult) rayTraceResult;
                Entity entity = result.getEntity();
                if (entity instanceof NpcEntity) {
                  PacketDispatcher.sendToServer(new CHandleNpcSaveToolRightClick(entity.getId()));
                }
              } else if (rayTraceResult.getType().equals(HitResult.Type.BLOCK)) {
                // If we right-click on a block, open our saved npcs gui
                BlockHitResult result = (BlockHitResult) rayTraceResult;
                BlockPos pos = result.getBlockPos();
                PacketDispatcher.sendToServer(new CHandleNpcSaveToolRightClick(pos));
              }
            }
          } else if (stack.getItem() instanceof BehaviorEditorItem) {
            HitResult rayTraceResult = minecraft.hitResult;
            if (rayTraceResult.getType().equals(HitResult.Type.MISS)) {
              if (minecraft.player.isDiscrete()) {
                stack.setTag(null);
              } else if (stack.hasTag()) {
                CompoundTag stackTag = stack.getTag();
                if (stackTag != null && stackTag.contains("Path")) {
                  long[] oldPath = stackTag.getLongArray("Path");
                  if (oldPath.length == 1) {
                    stack.setTag(null);
                  } else {
                    stackTag.putLongArray("Path", Arrays.copyOf(oldPath, oldPath.length - 1));
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Render the blocks selected by a behavior editor item.
   * @param event
   */
  @SubscribeEvent
  public void renderHighlightedBlocks(RenderLevelStageEvent event) {
    if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
      Player player = Minecraft.getInstance().player;
      if (player == null) return;
      ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
      if (!(heldItem.getItem() instanceof BehaviorEditorItem) || !heldItem.hasTag()) {
        return;
      }

      // Get the block positions to highlight.
      long[] blockLongs = heldItem.getTag().getLongArray("Path");
      BlockPos[] blockPositions = new BlockPos[blockLongs.length];
      for (int i = 0; i < blockLongs.length; i++) blockPositions[i] = BlockPos.of(blockLongs[i]);

      // Set up variables for drawing the path lines.
      float speed = 10;
      float percentage = 0;
      float holdTimeMod = 2.5f; // Determines how long the path ray will remain.
      float percPerSegment = -1;
      if (blockPositions.length > 1) {
        int numEdges = blockPositions.length - 1;
        percentage = (event.getRenderTick() % (numEdges * speed * holdTimeMod)) / (numEdges * speed);
        percPerSegment = (1.0f / numEdges);
      }
      BlockPos prevBlockPos = null;

      // Set up the drawing objects.
      PoseStack poseStack = event.getPoseStack();
      poseStack.pushPose();
      MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
      VertexConsumer vertexconsumer = buffers.getBuffer(RenderType.lines());
      Vector4f color = new Vector4f(227F / 255, 28F / 255, 121F / 255, 0.8F);

      for (BlockPos blockPos : blockPositions) {
        BlockState blockstate = player.level.getBlockState(blockPos);
        // figure out the percentage of the segment to draw.
        float segmentPercentage;
        if (prevBlockPos == null) {
          segmentPercentage = 0;
        }else if (percPerSegment > percentage) {
          segmentPercentage = percentage / percPerSegment;
          percentage = 0;
        } else {
          percentage -= percPerSegment;
          segmentPercentage = 1;
        }
        // Draw the block highlight.
        if (!blockstate.isAir()) {
          renderHitOutline(
                  event.getPoseStack(), vertexconsumer, player.level, event.getCamera(), blockPos, blockstate, color);
        }
        // Draw the animated path segment.
        if (prevBlockPos != null && segmentPercentage > 0) {
          Vec3 pos1 = new Vec3(0, 0, 0);
          Vec3 pos2 = new Vec3(blockPos.getX() - prevBlockPos.getX(), blockPos.getY() - prevBlockPos.getY(), blockPos.getZ() - prevBlockPos.getZ());
          pos2 = pos2.multiply(segmentPercentage, segmentPercentage, segmentPercentage);
          renderLine(poseStack, pos1, pos2, vertexconsumer, prevBlockPos.above(), event.getCamera().getPosition(), color);
        }
        prevBlockPos = blockPos;

      }

      buffers.endBatch();
      poseStack.popPose();
    }
  }

  private void renderHitOutline(PoseStack poseStack, VertexConsumer vertexConsumer, Level level, Camera camera, BlockPos blockPos, BlockState blockState, Vector4f color) {
    Position pos = camera.getPosition();
    renderShape(poseStack, vertexConsumer, blockState.getShape(level, blockPos, CollisionContext.of(camera.getEntity())),
            (double)blockPos.getX() - pos.x(), (double)blockPos.getY() - pos.y(), (double)blockPos.getZ() - pos.z(), color);
  }

  /**
   * Render the shape using the vertex consumer. Projects the voxel shape onto the camera's screen.
   * @param poseStack The poseStack.
   * @param vertexConsumer The renderer of the shape.
   * @param voxelShape The shape.
   * @param x The x offset from the camera.
   * @param y The x offset from the camera.
   * @param z The x offset from the camera.
   * @param color The color
   */
  private static void renderShape(PoseStack poseStack, VertexConsumer vertexConsumer, VoxelShape voxelShape, double x, double y, double z, Vector4f color) {
    PoseStack.Pose pose = poseStack.last();
    // Create the vertex consumer shape by creating every edge.
    voxelShape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
      float f = (float)(x2 - x1);
      float f1 = (float)(y2 - y1);
      float f2 = (float)(z2 - z1);
      float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
      f /= f3;
      f1 /= f3;
      f2 /= f3;
      vertexConsumer.vertex(pose.pose(), (float)(x1 + x), (float)(y1 + y), (float)(z1 + z))
              .color(color.x(), color.y(), color.z(), color.w()).normal(pose.normal(), f, f1, f2).endVertex();
      vertexConsumer.vertex(pose.pose(), (float)(x2 + x), (float)(y2 + y), (float)(z2 + z))
              .color(color.x(), color.y(), color.z(), color.w()).normal(pose.normal(), f, f1, f2).endVertex();
    });
  }

  /**
   * Render a line. Going to clean this up later.
   * @param poseStack
   * @param pos1
   * @param pos2
   * @param vertexConsumer
   * @param blockPos
   * @param camPos
   * @param color
   */
  private static void renderLine(PoseStack poseStack, Vec3 pos1, Vec3 pos2, VertexConsumer vertexConsumer, BlockPos blockPos, Vec3 camPos, Vector4f color) {
    PoseStack.Pose pose = poseStack.last();
    float f = (float)(pos2.x - pos1.x);
    float f1 = (float)(pos2.y - pos1.y);
    float f2 = (float)(pos2.z - pos1.z);
    float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
    f /= f3;
    f1 /= f3;
    f2 /= f3;
    vertexConsumer.vertex(pose.pose(), (float)(pos1.x + (blockPos.getX() - camPos.x + 0.5)), (float)(pos1.y + (blockPos.getY() + 0.5 - camPos.y)), (float)(pos1.z + (blockPos.getZ() - camPos.z + 0.5)))
            .color(color.x(), color.y(), color.z(), color.w()).normal(pose.normal(), f, f1, f2).endVertex();
    vertexConsumer.vertex(pose.pose(), (float)(pos2.x + (blockPos.getX() - camPos.x + 0.5)), (float)(pos2.y + (blockPos.getY() + 0.5 - camPos.y)), (float)(pos2.z + (blockPos.getZ() - camPos.z + 0.5)))
            .color(color.x(), color.y(), color.z(), color.w()).normal(pose.normal(), f, f1, f2).endVertex();
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
