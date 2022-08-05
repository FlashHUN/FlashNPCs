package flash.npcmod.core.client.quests;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.Main;
import flash.npcmod.core.FileUtil;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CRequestQuestInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientQuestUtil {

  public static final List<Quest> QUESTS = new ArrayList<>();

  @Nullable
  public static Quest fromName(String name) {
    for (Quest quest : QUESTS) {
      if (quest.getName().equals(name))
        return quest;
    }

    return loadQuest(name);
  }

  @Nullable
  public static Quest loadQuest(String name) {
    PacketDispatcher.sendToServer(new CRequestQuestInfo(name));
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.getJsonFile("quests", name)), StandardCharsets.UTF_8);
      JsonObject object = FileUtil.GSON.fromJson(is, JsonObject.class);

      Quest quest = Quest.fromJson(object);
      QUESTS.remove(quest);

      QUESTS.add(quest);
      is.close();
      return quest;
    } catch (Exception ignored) {}

    return null;
  }

}
