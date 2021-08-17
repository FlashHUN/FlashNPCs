package flash.npcmod.core.client;

import com.google.common.hash.Hashing;
import flash.npcmod.core.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DownloadingTexture;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.File;

@OnlyIn(Dist.CLIENT)
public class SkinUtil {

  private static Minecraft minecraft;
  private static File skinCacheDir;

  static {
    minecraft = Minecraft.getInstance();
    skinCacheDir = FileUtil.readDirectory("assets/npc_skins/");
  }

  public static ResourceLocation loadSkin(String url) {
    String s = Hashing.sha1().hashUnencodedChars(url).toString();
    ResourceLocation resourcelocation = new ResourceLocation("loaded_skins/" + s);
    Texture texture = minecraft.getTextureManager().getTexture(resourcelocation);
    if (texture == null) {
      File file1 = new File(skinCacheDir, s.length() > 2 ? s.substring(0, 2) : "xx");
      File file2 = new File(file1, s);
      DownloadingTexture downloadingtexture = new DownloadingTexture(file2, url, DefaultPlayerSkin.getDefaultSkinLegacy(), true, () -> { });
      minecraft.getTextureManager().loadTexture(resourcelocation, downloadingtexture);
    }

    return resourcelocation;
  }

}
