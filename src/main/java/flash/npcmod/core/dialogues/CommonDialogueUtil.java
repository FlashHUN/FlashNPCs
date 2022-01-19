package flash.npcmod.core.dialogues;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.Main;
import flash.npcmod.core.FileUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CommonDialogueUtil {

  public static final String DEFAULT_DIALOGUE_JSON = "{\"name\":\"init\",\"text\":\"Hi @p!\",\"function\":\"\",\"children\":[{\"name\":\"hello\",\"text\":\"Hey @npc.\"}]}";
  public static final String DEFAULT_DIALOGUE_EDITOR_JSON = "{\"entries\":[{\"name\":\"init\",\"x\":10,\"y\":10},{\"name\":\"hello\",\"x\":150,\"y\":10}]}";
  public static final String DEFAULT_DIALOGUE_JSON_HELLO_THERE = "{\"name\":\"init\",\"text\":\"Hello there!\",\"children\":[{\"name\":\"generalKenobi\",\"text\":\"General Kenobi! You are a bold one. I'll deal with you Jedi slime myself.\",\"response\":\"Your move.\",\"children\":[{\"name\":\"youFool\",\"text\":\"You fool. I've been trained in your Jedi arts by Count Dooku.\",\"response\":\"[draws lightsaber]\"}]}]}";
  public static final String DEFAULT_DIALOGUE_EDITOR_JSON_HELLO_THERE = "{\"entries\":[{\"name\":\"init\",\"x\":10,\"y\":10},{\"name\":\"generalKenobi\",\"x\":150,\"y\":10},{\"name\":\"youFool\",\"x\":300,\"y\":10}]}";
  public static final String DEFAULT_DIALOGUE_JSON_KICK_GUM = "{\"name\":\"init\",\"text\":\"It's Time To Kick Gum and Chew Ass. And I'm All Out Of Ass.\",\"children\":[]}";
  public static final String DEFAULT_DIALOGUE_EDITOR_JSON_KICK_GUM = "{\"entries\":[{\"name\":\"init\",\"x\":10,\"y\":10}]}";
  public static final String[] HELLO_THERE_NAMES = new String[] { "Obi-Wan Kenobi", "Obi Wan Kenobi", "Obi-Wan", "Obi Wan", "Kenobi", "General Kenobi" };
  public static final String KICK_GUM_NAME = "Dick Kickem";

  public static final int MAX_DIALOGUE_LENGTH = 100000;

  private static Writer fw;

  public static void buildDialogue(String name, String jsonText) {
    Writer fw = null;
    try {
      File jsonFile = FileUtil.getJsonFile("dialogues", name);
      JsonObject jsonObject = new Gson().fromJson(jsonText, JsonObject.class);
      fw = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8);
      fw.write(jsonObject.toString());
    } catch (Exception e) {
      Main.LOGGER.warn("Could not build dialogue file " + name + ".json");
    } finally {
      try {
        if (fw != null) {
          fw.flush();
          fw.close();
        } else {
          Main.LOGGER.debug("Could not close FileWriter for dialogue file " + name + ".json, fw is null");
        }
      } catch (IOException e) {
        Main.LOGGER.warn("Could not close FileWriter for dialogue file " + name + ".json");
      }
    }
  }

  public static void buildDialogueEditor(String name, String jsonText) {
    try {
      File jsonFile = FileUtil.getJsonFile("dialogue_editor", name);
      JsonObject jsonObject = new Gson().fromJson(jsonText, JsonObject.class);
      fw = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8);
      fw.write(jsonObject.toString());
    } catch (Exception e) {
      Main.LOGGER.warn("Could not build dialogue editor file " + name + ".json");
    } finally {
      try {
        fw.flush();
        fw.close();
      } catch (IOException e) {
        Main.LOGGER.warn("Could not close FileWriter for dialogue editor file " + name + ".json");
      }
    }
  }

  public static JsonObject loadDialogueFile(String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/dialogues", name+".json")), StandardCharsets.UTF_8);
      JsonObject object = new Gson().fromJson(is, JsonObject.class);

      return object;
    } catch (FileNotFoundException e) {
      Main.LOGGER.warn("Could not find dialogue file " + name + ".json, creating it now...");
    }
    return null;
  }

  public static JsonObject loadDialogueEditorFile(String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/dialogue_editor", name+".json")), StandardCharsets.UTF_8);
      JsonObject object = new Gson().fromJson(is, JsonObject.class);

      return object;
    } catch (FileNotFoundException e) {
      Main.LOGGER.warn("Could not find dialogue editor file " + name + ".json, creating it now...");
    }
    return null;
  }

}
