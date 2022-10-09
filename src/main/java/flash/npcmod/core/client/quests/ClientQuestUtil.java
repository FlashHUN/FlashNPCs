package flash.npcmod.core.client.quests;

import com.google.gson.JsonObject;
import flash.npcmod.core.FileUtil;
import flash.npcmod.core.quests.Quest;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@OnlyIn(Dist.CLIENT)
public class ClientQuestUtil {

  @Nullable
  public static Quest loadQuest(String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.getJsonFile("quests", name)), StandardCharsets.UTF_8);
      JsonObject object = FileUtil.GSON.fromJson(is, JsonObject.class);
      is.close();

      return Quest.fromJson(object);
    } catch (Exception ignored) {}

    return null;
  }

}
