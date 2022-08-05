package flash.npcmod.core.client;

import com.google.common.hash.Hashing;
import flash.npcmod.core.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;

@OnlyIn(Dist.CLIENT)
public class SkinUtil {

  private static Minecraft minecraft;
  private static File skinCacheDir;

  static {
    minecraft = Minecraft.getInstance();
    skinCacheDir = FileUtil.getOrCreateDirectory("assets/npc_skins/");
  }

  public static ResourceLocation loadSkin(String url) {

    String s = Hashing.sha256().hashUnencodedChars(url).toString();
    ResourceLocation resourcelocation = new ResourceLocation("loaded_skins/" + s);
    AbstractTexture abstracttexture = minecraft.textureManager.getTexture(resourcelocation, MissingTextureAtlasSprite.getTexture());
    if (abstracttexture == MissingTextureAtlasSprite.getTexture()) {
      File file1 = new File(skinCacheDir, s.length() > 2 ? s.substring(0, 2) : "xx");
      File file2 = new File(file1, s);
      HttpTexture httptexture = new HttpTexture(file2, url, DefaultPlayerSkin.getDefaultSkin(), true, () -> {});
      minecraft.textureManager.register(resourcelocation, httptexture);
    }

    return resourcelocation;
  }

}
