package flash.npcmod.core.dialogues;

import flash.npcmod.Main;
import flash.npcmod.core.FileUtil;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;


public class CommonDialogueUtil {

  public static final String DEFAULT_DIALOGUE_JSON = "{\"name\":\"init\",\"text\":\"Hi @p!\",\"function\":\"\",\"children\":[{\"name\":\"hello\",\"text\":\"Hey @npc.\"}]}";
  public static final String DEFAULT_DIALOGUE_EDITOR_JSON = "{\"entries\":[{\"name\":\"init\",\"x\":10,\"y\":10},{\"name\":\"hello\",\"x\":150,\"y\":10}]}";
  public static final String DEFAULT_DIALOGUE_JSON_HELLO_THERE = "{\"name\":\"init\",\"text\":\"Hello there!\",\"children\":[{\"name\":\"generalKenobi\",\"text\":\"General Kenobi! You are a bold one. I'll deal with you Jedi slime myself.\",\"response\":\"Your move.\",\"children\":[{\"name\":\"youFool\",\"text\":\"You fool. I've been trained in your Jedi arts by Count Dooku.\",\"response\":\"[draws lightsaber]\"}]}]}";
  public static final String DEFAULT_DIALOGUE_EDITOR_JSON_HELLO_THERE = "{\"entries\":[{\"name\":\"init\",\"x\":10,\"y\":10},{\"name\":\"generalKenobi\",\"x\":150,\"y\":10},{\"name\":\"youFool\",\"x\":300,\"y\":10}]}";
  public static final String[] HELLO_THERE_NAMES = new String[] { "Obi-Wan Kenobi", "Obi Wan Kenobi", "Obi-Wan", "Obi Wan", "Kenobi", "General Kenobi" };

  public static final int MAX_DIALOGUE_LENGTH = 100000;

  private static FileWriter fw;

  public static void buildDialogue(String name, String jsonText) {
    try {
      File jsonFile = FileUtil.getJsonFile("dialogues", name);
      JSONObject jsonObject = new JSONObject(jsonText);
      fw = new FileWriter(jsonFile);
      fw.write(jsonObject.toString());
    } catch (Exception e) {
      Main.LOGGER.warn("Could not build dialogue file " + name + ".json");
    } finally {
      try {
        fw.flush();
        fw.close();
      } catch (IOException e) {
        Main.LOGGER.warn("Could not close FileWriter for dialogue file " + name + ".json");
      }
    }
  }

  public static void buildDialogueEditor(String name, String jsonText) {
    try {
      File jsonFile = FileUtil.getJsonFile("dialogue_editor", name);
      JSONObject jsonObject = new JSONObject(jsonText);
      fw = new FileWriter(jsonFile);
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

  public static JSONObject loadDialogueFile(String name) {
    try {
      InputStream is = new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/dialogues", name+".json"));
      JSONTokener tokener = new JSONTokener(is);
      JSONObject object = new JSONObject(tokener);

      return object;
    } catch (FileNotFoundException e) {
      Main.LOGGER.warn("Could not find dialogue file " + name + ".json, creating it now...");
    }
    return null;
  }

  public static JSONObject loadDialogueEditorFile(String name) {
    try {
      InputStream is = new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/dialogue_editor", name+".json"));
      JSONTokener tokener = new JSONTokener(is);
      JSONObject object = new JSONObject(tokener);

      return object;
    } catch (FileNotFoundException e) {
      Main.LOGGER.warn("Could not find dialogue editor file " + name + ".json, creating it now...");
    }
    return null;
  }

}
