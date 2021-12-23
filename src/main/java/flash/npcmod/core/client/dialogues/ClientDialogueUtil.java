package flash.npcmod.core.client.dialogues;

import flash.npcmod.Main;
import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityAttacher;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ClientDialogueUtil {

  public static final String INIT_DIALOGUE_NAME = "init";

  public static final List<String> FUNCTION_NAMES = new ArrayList<>();

  @Nullable
  public static JSONObject currentDialogue = null;
  @Nullable
  public static JSONObject currentDialogueEditor = null;

  private static String currentText = "";
  private static String currentResponse = "";
  private static String currentFunction = "";
  private static JSONArray currentChildren = new JSONArray();

  public static void loadDialogue(String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/dialogues", name+".json")), StandardCharsets.UTF_8);
      JSONTokener tokener = new JSONTokener(is);
      JSONObject object = new JSONObject(tokener);

      currentDialogue = object;
    } catch (FileNotFoundException e) {
      PacketDispatcher.sendToServer(new CRequestDialogue(name));
    }
  }

  public static void loadDialogueEditor(String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/dialogue_editor", name+".json")), StandardCharsets.UTF_8);
      JSONTokener tokener = new JSONTokener(is);
      JSONObject object = new JSONObject(tokener);

      currentDialogueEditor = object;
    } catch (FileNotFoundException e) {
      PacketDispatcher.sendToServer(new CRequestDialogueEditor(name));
    }
  }

  public static void initDialogue() {
    loadDialogueOption(INIT_DIALOGUE_NAME);
  }

  public static void loadDialogueOption(String name) {
    findText(name, currentDialogue);
  }

  private static boolean findText(String name, JSONObject currentObject) {
    if (!currentObject.has("entries")) {
      boolean result;
      if (name.equals(currentObject.getString("name"))) {
        setVars(currentObject);
        return true;
      } else {
        if (currentObject.has("children")) {
          JSONArray children = currentObject.getJSONArray("children");
          for (int i = 0; i < children.length(); i++) {
            JSONObject currentChild = children.getJSONObject(i);

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
      JSONArray entries = currentObject.getJSONArray("entries");
      for (int i = 0; i < entries.length(); i++) {
        JSONObject entry = entries.getJSONObject(i);
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
    currentChildren = new JSONArray();
  }

  private static void setVars(JSONObject object) {
    currentText = object.getString("text");
    if (object.has("response")) {
      currentResponse = object.getString("response");
    } else {
      currentResponse = "";
    }
    if (object.has("function")) {
      currentFunction = object.getString("function");
    } else {
      currentFunction = "";
    }
    if (object.has("children")) {
      currentChildren = object.getJSONArray("children");
    } else {
      currentChildren = new JSONArray();
    }

    // Quest Talk Objective Check
    if (Minecraft.getInstance().screen instanceof DialogueScreen) {
      DialogueScreen dialogueScreen = (DialogueScreen) Minecraft.getInstance().screen;
      Player player = Minecraft.getInstance().player;
      if (player != null && player.isAlive()) {
        IQuestCapability capability = QuestCapabilityAttacher.getCapability(player);
        List<QuestInstance> acceptedQuests = capability.getAcceptedQuests();
        acceptedQuests.forEach(questInstance -> {
          questInstance.getQuest().getObjectives().forEach(objective -> {
            if (objective.getType().equals(QuestObjective.ObjectiveType.Talk)) {
              if (objective.getObjective().equals(dialogueScreen.getNpcName())
                  && objective.getSecondaryObjective().equals(object.getString("name"))) {
                String objectiveName = questInstance.getQuest().getName() + ":::" + objective.getName();
                PacketDispatcher.sendToServer(new CTalkObjectiveComplete(objectiveName));
              }
            }
          });
        });
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

  public static String[] getDialogueOptionNamesFromChildren() {
    int childrenAmount = currentChildren.length();
    if (childrenAmount > 0) {
      String[] options = new String[childrenAmount];
      for (int i = 0; i < childrenAmount; i++) {
        options[i] = currentChildren.getJSONObject(i).getString("name");
      }
      return options;
    }
    return new String[]{};
  }

  public static String[] getDialogueOptionsFromChildren() {
    int childrenAmount = currentChildren.length();
    if (childrenAmount > 0) {
      String[] options = new String[childrenAmount];
      for (int i = 0; i < childrenAmount; i++) {
        options[i] = currentChildren.getJSONObject(i).getString("text");
      }
      return options;
    }
    return new String[]{};
  }

}
