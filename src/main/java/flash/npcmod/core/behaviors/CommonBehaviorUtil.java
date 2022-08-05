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
  public static final String DEFAULT_BEHAVIOR_JSON = "{\"name\":\"init\",\"dialogueName\":\"\",\"function\":\"\"}";
  public static final String DEFAULT_BEHAVIOR_EDITOR_JSON = "{\"entries\":[{\"name\":\"init\",\"x\":10,\"y\":10}]}";

  public static final int MAX_BEHAVIOR_LENGTH = 100000;

  private static Writer fw;

  public static void buildBehavior(String name, String jsonText) {
    Writer fw = null;
    try {
      File jsonFile = FileUtil.getJsonFileForWriting("behaviors", name);
      JsonObject jsonObject = FileUtil.GSON.fromJson(jsonText, JsonObject.class);
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
      File jsonFile = FileUtil.getJsonFileForWriting("behavior_editor", name);
      JsonObject jsonObject = FileUtil.GSON.fromJson(jsonText, JsonObject.class);
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
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.getJsonFile("behaviors", name)), StandardCharsets.UTF_8);
      JsonObject object = FileUtil.GSON.fromJson(is, JsonObject.class);
      is.close();
      return object;
    } catch (Exception e) {
      Main.LOGGER.warn("Could not find behavior editor file " + name + ".json, creating it now...");
    }
    return null;
  }

  public static JsonObject loadBehaviorEditorFile(String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.getJsonFile("behavior_editor", name)), StandardCharsets.UTF_8);
      JsonObject object = FileUtil.GSON.fromJson(is, JsonObject.class);
      is.close();
      return object;
    } catch (Exception e) {
      Main.LOGGER.warn("Could not find behavior editor file " + name + ".json, creating it now...");
    }
    return null;
  }

  public static List<String> readAllBehaviorFileNames() {
    File[] files = FileUtil.getAllFiles("behaviors");
    return Arrays.stream(files).map(file -> FilenameUtils.removeExtension(file.getName())).collect(Collectors.toList());
  }
}
