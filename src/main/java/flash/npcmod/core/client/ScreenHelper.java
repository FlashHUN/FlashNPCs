package flash.npcmod.core.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;

public class ScreenHelper {

  public static void sidewaysFillGradient(MatrixStack matrixStack, int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
    RenderSystem.disableTexture();
    RenderSystem.enableBlend();
    RenderSystem.disableAlphaTest();
    RenderSystem.defaultBlendFunc();
    RenderSystem.shadeModel(7425);
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferbuilder = tessellator.getBuffer();
    bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
    sidewaysFillGradient(matrixStack.getLast().getMatrix(), bufferbuilder, x1, y1, x2, y2, 0, colorFrom, colorTo);
    tessellator.draw();
    RenderSystem.shadeModel(7424);
    RenderSystem.disableBlend();
    RenderSystem.enableAlphaTest();
    RenderSystem.enableTexture();
  }

  public static void sidewaysFillGradient(Matrix4f matrix, BufferBuilder builder, int x1, int y1, int x2, int y2, int z, int colorA, int colorB) {
    float a1 = (float)(colorA >> 24 & 255) / 255.0F;
    float r1 = (float)(colorA >> 16 & 255) / 255.0F;
    float g1 = (float)(colorA >> 8 & 255) / 255.0F;
    float b1 = (float)(colorA & 255) / 255.0F;
    float a2 = (float)(colorB >> 24 & 255) / 255.0F;
    float r2 = (float)(colorB >> 16 & 255) / 255.0F;
    float g2 = (float)(colorB >> 8 & 255) / 255.0F;
    float b2 = (float)(colorB & 255) / 255.0F;
    // Top right
    builder.pos(matrix, (float)x2, (float)y1, (float)z).color(r2, g2, b2, a2).endVertex();
    // Top left
    builder.pos(matrix, (float)x1, (float)y1, (float)z).color(r1, g1, b1, a1).endVertex();
    // Bottom left
    builder.pos(matrix, (float)x1, (float)y2, (float)z).color(r1, g1, b1, a1).endVertex();
    // Bottom right
    builder.pos(matrix, (float)x2, (float)y2, (float)z).color(r2, g2, b2, a2).endVertex();
  }

}
