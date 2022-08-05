package flash.npcmod.core.behaviors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.Main;
import flash.npcmod.core.FileUtil;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommonBehaviorUtil {
  public static final String INVALID_BEHAVIOR = "_invalid";
  public static final String DEFAULT_BEHAVIOR_JSON = "{\"name\":\"init\",\"dialogueName\":\"\",\"function\":\"\"}";
  public static final String DEFAULT_BEHAVIOR_EDITOR_JSON = "{\"entries\":[{\"name\":\"init\",\"x\":10,\"y\":10}]}";

  public static final int MAX_BEHAVIOR_LENGTH = 100000;

  private static Writer fw;

  public static void buildBehavior(String name, String jsonText) {
    Writer fw = null;
    try {
      File jsonFile = FileUtil.getJsonFile("behaviors", name);
      JsonObject jsonObject = new Gson().fromJson(jsonText, JsonObject.class);
      fw = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8);
      fw.write(jsonObject.toString());
    } catch (Exception e) {
      Main.LOGGER.warn("Could not build behavior file " + name + ".json");
    } finally {
      try {
        if (fw != null) {
          fw.flush();
          fw.close();
        } else {
          Main.LOGGER.debug("Could not close FileWriter for behavior file " + name + ".json, fw is null");
        }
      } catch (IOException e) {
        Main.LOGGER.warn("Could not close FileWriter for behavior file " + name + ".json");
      }
    }
  }

  public static void buildBehaviorEditor(String name, String jsonText) {
    try {
      File jsonFile = FileUtil.getJsonFile("behavior_editor", name);
      JsonObject jsonObject = new Gson().fromJson(jsonText, JsonObject.class);
      fw = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8);
      fw.write(jsonObject.toString());
    } catch (Exception e) {
      Main.LOGGER.warn("Could not build behavior editor file " + name + ".json");
    } finally {
      try {
        fw.flush();
        fw.close();
      } catch (IOException e) {
        Main.LOGGER.warn("Could not close FileWriter for behavior editor file " + name + ".json");
      }
    }
  }

  public static JsonObject loadBehaviorFile(String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/behaviors", name+".json")), StandardCharsets.UTF_8);
      JsonObject object = new Gson().fromJson(is, JsonObject.class);
      is.close();
      return object;
    } catch (Exception e) {
      Main.LOGGER.warn("Could not find behavior editor file " + name + ".json, creating it now...");
    }
    return null;
  }

  public static JsonObject loadBehaviorEditorFile(String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/behavior_editor", name+".json")), StandardCharsets.UTF_8);
      JsonObject object = new Gson().fromJson(is, JsonObject.class);
      is.close();
      return object;
    } catch (Exception e) {
      Main.LOGGER.warn("Could not find behavior editor file " + name + ".json, creating it now...");
    }
    return null;
  }

  public static List<String> readAllBehaviorFileNames() {
    File folder = FileUtil.readDirectory(FileUtil.getWorldName()+"/"+Main.MODID+"/behaviors");
    Stream<String> files = Arrays.stream(folder.listFiles()).map(file -> FilenameUtils.removeExtension(file.getName()));
    return files.collect(Collectors.toList());
  }
}
