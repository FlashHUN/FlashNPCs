package flash.npcmod.core.saves;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.Main;
import flash.npcmod.core.FileUtil;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NpcSaveUtil {

  public static final byte MAX_SAVED_NPCS = 20;

  public enum BuildResult {
    SUCCESS,
    TOOMANY,
    FAILED,
    EXISTS
  }

  public static BuildResult build(String uuid, String npcJson) {
    Writer fw = null;
    JsonObject jsonObject = new Gson().fromJson(npcJson, JsonObject.class);
    String name = jsonObject.get("name").getAsString();
    try {
      File folder = FileUtil.readDirectory(FileUtil.getWorldName()+"/"+ Main.MODID+"/saves/"+uuid);
      String path = "saves/"+uuid;
      try {
        if (folder.listFiles().length + 1 > MAX_SAVED_NPCS) {
          return BuildResult.TOOMANY;
        }
      } catch (Exception ignored) {}
      jsonObject.addProperty("internalName", name);

      File jsonFile = FileUtil.getJsonFile(path, name);
      if (jsonFile.exists()) {
        return BuildResult.EXISTS;
      }

      fw = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8);
      fw.write(jsonObject.toString());

      return BuildResult.SUCCESS;
    } catch (Exception e) {
      Main.LOGGER.warn("Could not build Saved NPC file " + name + ".json");
    } finally {
      try {
        if (fw != null) {
          fw.flush();
          fw.close();
        } else {
          Main.LOGGER.debug("Could not close FileWriter for Saved NPC " + name + ".json, fw is null");
        }
      } catch (IOException e) {
        Main.LOGGER.warn("Could not close FileWriter for Saved NPC " + name + ".json");
      }
    }
    return BuildResult.FAILED;
  }

  public static boolean rename(String uuid, String previousName, String newName) {
    String path = "saves/"+uuid;
    File jsonFile = FileUtil.getJsonFile(path, previousName);
    Writer fw = null;
    if (jsonFile.exists()) {
      try {
        File newFile = FileUtil.getJsonFile(path, newName);
        boolean success = jsonFile.renameTo(newFile);

        if (success) {
          InputStreamReader is = new InputStreamReader(new FileInputStream(newFile), StandardCharsets.UTF_8);
          JsonObject jsonObject = new Gson().fromJson(is, JsonObject.class);
          jsonObject.addProperty("internalName", newName);
          is.close();

          fw = new OutputStreamWriter(new FileOutputStream(newFile), StandardCharsets.UTF_8);
          fw.write(jsonObject.toString());
        }

        return success;
      } catch (Exception e) {
        Main.LOGGER.warn("Could not rename Saved NPC file " + previousName + ".json");
      } finally {
        try {
          if (fw != null) {
            fw.flush();
            fw.close();
          } else {
            Main.LOGGER.debug("Could not close FileWriter for Saved NPC "+newName+".json, fw is null");
          }
        } catch (IOException e) {
          Main.LOGGER.warn("Could not close FileWriter for Saved NPC "+newName+".json");
        }
      }
    }
    return false;
  }

  public static boolean delete(ServerPlayerEntity sender, String name) {
    String path = "saves/"+sender.getCachedUniqueIdString();
    File file = FileUtil.getJsonFile(path, name);
    return file.exists() && file.delete();
  }

  public static List<String> load(String uuid) {
    List<String> savedNpcs = new ArrayList<>();
    File folder = FileUtil.readDirectory(FileUtil.getWorldName()+"/"+Main.MODID+"/saves/"+uuid);
    File[] files = folder.listFiles();
    if (files != null) {
      for (File file : files) {
        try {
          InputStreamReader is = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
          String jsonString = new Gson().fromJson(is, JsonObject.class).toString();
          savedNpcs.add(jsonString);
          is.close();
        } catch (Exception ignored) {
        }
      }
    }
    return savedNpcs;
  }

}
