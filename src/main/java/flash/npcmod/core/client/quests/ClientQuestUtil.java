package flash.npcmod.core.client.quests;

import flash.npcmod.Main;
import flash.npcmod.core.FileUtil;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CRequestQuestInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/quests", name+".json")), StandardCharsets.UTF_8);
      JSONTokener tokener = new JSONTokener(is);
      JSONObject object = new JSONObject(tokener);

      Quest quest = Quest.fromJson(object);
      if (QUESTS.contains(quest))
        QUESTS.remove(quest);

      QUESTS.add(quest);
      is.close();
      return quest;
    } catch (Exception ignored) {}

    return null;
  }

}
