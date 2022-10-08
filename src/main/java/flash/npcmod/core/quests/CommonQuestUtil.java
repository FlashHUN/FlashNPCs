package flash.npcmod.core.quests;

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
    if (FileUtil.shouldGetFromWorld()) {
      path = FileUtil.getWorldDirectory() + "/" + path;
    }
    File directory = FileUtil.getOrCreateDirectory(path);
    for (File file : directory.listFiles()) {
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
        JsonObject quest = loadQuestAsJson(questInstance.getQuest().getName());
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
    removeQuest(name);
    try {
      File jsonFile = FileUtil.getJsonFileForWriting("quests", name);
      JsonObject jsonObject = FileUtil.GSON.fromJson(jsonText, JsonObject.class);
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
  public static JsonObject loadQuestAsJson(String name) {
    Quest fromName = fromName(name);
    if (fromName != null) return fromName.toJson();

    return null;
  }

  @Nullable
  public static Quest loadQuestFile(String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.getJsonFile("quests", name)), StandardCharsets.UTF_8);
      JsonObject object = FileUtil.GSON.fromJson(is, JsonObject.class);
      is.close();

      Quest quest = Quest.fromJson(object);
      removeQuest(name);
      QUESTS.add(quest);

      return quest;
    } catch (Exception e) {
      if (!name.isEmpty())
        Main.LOGGER.warn("Could not find quests file " + name + ".json");
    }

    return null;
  }

  private static void removeQuest(String name) {
    int toRemoveIndex = -1;
    for (int i = 0; i < QUESTS.size(); i++) {
      if (QUESTS.get(i).getName().equals(name)) {
        toRemoveIndex = i;
        break;
      }
    }
    if (toRemoveIndex != -1)
      QUESTS.remove(toRemoveIndex);
  }

}
