package flash.npcmod.core.quests;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.Main;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.core.FileUtil;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSendQuestInfo;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CommonQuestUtil {

  public static final List<Quest> QUESTS = new ArrayList<>();
  public static final List<QuestInstance> QUEST_INSTANCE_LIST = new ArrayList<>();

  private static Writer fw;

  public static void loadAllQuests() {
    QUESTS.clear();
    String path = Main.MODID+"/quests";
    if (FileUtil.shouldReadFromWorld()) {
      path = FileUtil.getWorldName() + "/" + path;
    }
    File folder = FileUtil.readDirectory(path);
    for (File file : folder.listFiles()) {
      if (!file.isDirectory()) {
        loadQuestFile(FilenameUtils.removeExtension(file.getName()));
      }
    }
  }

  public static void syncPlayerQuests(ServerPlayer player) {
    if (player != null && player.isAlive()) {
      IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
      List<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
      List<QuestInstance> markedForRemoval = new ArrayList<>();
      acceptedQuests.forEach(questInstance -> {
        JsonObject quest = loadQuest(questInstance.getQuest().getName());
        if (quest != null) {
          PacketDispatcher.sendTo(new SSendQuestInfo(questInstance.getQuest().getName(), quest.toString()), player);
        } else {
          markedForRemoval.add(questInstance);
        }
      });
      for (QuestInstance instance : markedForRemoval) {
        capability.abandonQuest(instance);
      }
    }
  }

  public static void buildQuest(String name, String jsonText) {
    try {
      File jsonFile = FileUtil.getJsonFile("quests", name);
      JsonObject jsonObject = new Gson().fromJson(jsonText, JsonObject.class);
      fw = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8);
      fw.write(jsonObject.toString());
    } catch (Exception e) {
      Main.LOGGER.warn("Could not build quest file " + name + ".json");
    } finally {
      try {
        fw.flush();
        fw.close();
      } catch (IOException e) {
        Main.LOGGER.warn("Could not close FileWriter for quest file " + name + ".json");
      }
    }
  }

  @Nullable
  public static Quest fromName(String name) {
    for (Quest quest : QUESTS) {
      if (quest.getName().equals(name))
        return quest;
    }

    return loadQuestFile(name);
  }

  @Nullable
  public static JsonObject loadQuest(String name) {
    Quest fromName = fromName(name);
    if (fromName != null) return fromName.toJson();

    return null;
  }

  @Nullable
  public static Quest loadQuestFile(String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/quests", name+".json")), StandardCharsets.UTF_8);
      JsonObject object = new Gson().fromJson(is, JsonObject.class);

      Quest quest = Quest.fromJson(object);
      if (QUESTS.contains(quest)) QUESTS.remove(quest);

      QUESTS.add(quest);

      return quest;
    } catch (FileNotFoundException e) {
      if (!name.isEmpty())
        Main.LOGGER.warn("Could not find quests file " + name + ".json");
    }

    return null;
  }

}
