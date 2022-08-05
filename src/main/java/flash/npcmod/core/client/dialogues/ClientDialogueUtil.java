package flash.npcmod.core.client.dialogues;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import flash.npcmod.Main;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.client.gui.screen.dialogue.DialogueScreen;
import flash.npcmod.core.FileUtil;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CRequestDialogue;
import flash.npcmod.network.packets.client.CRequestDialogueEditor;
import flash.npcmod.network.packets.client.CTalkObjectiveComplete;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class ClientDialogueUtil {

  public static final String INIT_DIALOGUE_NAME = "init";

  public static final List<String> FUNCTION_NAMES = new ArrayList<>();

  @Nullable
  public static JsonObject currentDialogue = null;
  @Nullable
  public static JsonObject currentDialogueEditor = null;

  private static String currentText = "";
  private static String currentResponse = "";
  private static String currentFunction = "";
  private static String currentTrigger = "";
  private static JsonArray currentChildren = new JsonArray();

  public static void loadDialogue(String name) {
    if (name == null || name.isEmpty()) {
      currentDialogue = null;
      return;
    }
    try {
      File jsonFile = FileUtil.getJsonFile("dialogues", name);
      FileInputStream fis = new FileInputStream(jsonFile);
      InputStreamReader is = new InputStreamReader(fis, StandardCharsets.UTF_8);

      currentDialogue = FileUtil.GSON.fromJson(is, JsonObject.class);
      is.close();
    } catch (Exception e) {
      PacketDispatcher.sendToServer(new CRequestDialogue(name));
    }
  }

  public static void loadDialogueEditor(String name) {
    if (name == null || name.isEmpty()) {
      currentDialogue = null;
      return;
    }
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.getJsonFile("dialogue_editor", name)), StandardCharsets.UTF_8);

      currentDialogueEditor = FileUtil.GSON.fromJson(is, JsonObject.class);
      is.close();
    } catch (Exception e) {
      PacketDispatcher.sendToServer(new CRequestDialogueEditor(name));
    }
  }

  public static void initDialogue() {
    loadDialogueOption(INIT_DIALOGUE_NAME);
  }

  public static void loadDialogueOption(String name) {
    findText(name, currentDialogue);
  }

  private static boolean findText(String name, JsonObject currentObject) {
    if (!currentObject.has("entries")) {
      boolean result;
      if (name.equals(currentObject.get("name").getAsString())) {
        setVars(currentObject);
        return true;
      } else {
        if (currentObject.has("children")) {
          JsonArray children = currentObject.getAsJsonArray("children");
          for (int i = 0; i < children.size(); i++) {
            JsonObject currentChild = children.get(i).getAsJsonObject();

            result = findText(name, currentChild);

            if (result) {
              currentDialogue = currentChild;
              setVars(currentChild);
              return true;
            }
          }
        }
      }
      resetVars();
      return false;
    }
    else {
      JsonArray entries = currentObject.getAsJsonArray("entries");
      for (int i = 0; i < entries.size(); i++) {
        JsonObject entry = entries.get(i).getAsJsonObject();
        boolean b = findText(name, entry);
        if (b) {
          return true;
        }
      }
    }
    resetVars();
    return false;
  }

  private static void resetVars() {
    currentText = "";
    currentResponse = "";
    currentFunction = "";
    currentTrigger = "";
    currentChildren = new JsonArray();
  }

  private static void setVars(JsonObject object) {

    currentText = object.get("text").getAsString();
    if (object.has("response")) {
      currentResponse = object.get("response").getAsString();
    } else {
      currentResponse = "";
    }
    if (object.has("function")) {
      currentFunction = object.get("function").getAsString();
    } else {
      currentFunction = "";
    }
    if (object.has("trigger")) {
      currentTrigger = object.get("trigger").getAsString();
    } else {
      currentTrigger = "";
    }
    Main.LOGGER.info("current trigger is " + currentTrigger);
    if (object.has("children")) {
      currentChildren = object.getAsJsonArray("children");
    } else {
      currentChildren = new JsonArray();
    }

    // Quest Talk Objective Check
    if (Minecraft.getInstance().screen instanceof DialogueScreen dialogueScreen) {
      Player player = Minecraft.getInstance().player;
      if (player != null && player.isAlive()) {
        IQuestCapability capability = QuestCapabilityProvider.getCapability(player);
        List<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
        for (QuestInstance questInstance : acceptedQuests) {
          List<QuestObjective> objectives = questInstance.getQuest().getObjectives();
          for (QuestObjective objective : objectives) {
            if (objective.getType().equals(QuestObjective.ObjectiveType.Talk)) {
              if (objective.getObjective().equals(dialogueScreen.getNpcName())
                  && Objects.equals(objective.getSecondaryObjective(), object.get("name").getAsString())) {
                String objectiveName = questInstance.getQuest().getName() + ":::" + objective.getName();
                PacketDispatcher.sendToServer(new CTalkObjectiveComplete(objectiveName));
              }
            }
          }
        }
      }
    }
  }

  public static String getCurrentText() {
    return currentText;
  }

  public static String getCurrentResponse() {
    return currentResponse;
  }

  public static String getCurrentFunction() {
    return currentFunction;
  }

  public static String getCurrentTrigger() {
    return currentTrigger;
  }

  public static String[] getDialogueOptionNamesFromChildren() {
    int childrenAmount = currentChildren.size();
    if (childrenAmount > 0) {
      String[] options = new String[childrenAmount];
      for (int i = 0; i < childrenAmount; i++) {
        options[i] = currentChildren.get(i).getAsJsonObject().get("name").getAsString();
      }
      return options;
    }
    return new String[]{};
  }

  public static String[] getDialogueOptionsFromChildren() {
    int childrenAmount = currentChildren.size();
    if (childrenAmount > 0) {
      String[] options = new String[childrenAmount];
      for (int i = 0; i < childrenAmount; i++) {
        JsonObject childJson = currentChildren.get(i).getAsJsonObject();
        options[i] = childJson.get("text").getAsString();
      }
      return options;
    }
    return new String[]{};
  }

}
