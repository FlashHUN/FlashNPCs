package flash.npcmod.core;

import com.google.gson.Gson;
import flash.npcmod.Main;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class FileUtil {
  
  public static final Gson GSON = new Gson();
  private static final String SEPARATOR = Path.of(".").getFileSystem().getSeparator();

  public static String getGlobalDirectoryName() {
    return Main.MODID + "/global";
  }

  /**
   * Gets the path to the current world. If for some reason we get an IOException,
   * this'll grab the world name instead of the world directory.
   *
   * @return The path from "." to the current world
   */
  public static String getWorldDirectory() {
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    Path rootPath = Path.of(".");
    try {
      String worldPath = server.getWorldPath(LevelResource.ROOT).toFile().getCanonicalPath();
      return worldPath.replace(rootPath.toFile().getCanonicalPath() + SEPARATOR, "");
    } catch (IOException e) {
      Main.LOGGER.warn("Error while getting world directory, falling back to the old method");
      e.printStackTrace();
      String worldName = server.getWorldData().getLevelName();
      if (server.isDedicatedServer()) {
        return worldName;
      }
      return "saves" + SEPARATOR + worldName;
    }
  }

  @Nullable
  public static File getFileFromPath(String path, String name) {
    if (shouldGetFromWorld()) {
      path = getWorldDirectory() + SEPARATOR + path;
    }
    File directory = getOrCreateDirectory(path);
    try {
      return new File(directory.getCanonicalPath(), name);
    } catch (IOException e) {
      Main.LOGGER.warn("Could not get file " + path + SEPARATOR + name);
    }
    return null;
  }

  @Nullable
  public static File getFileFromGlobal(String path, String name) {
    File directory = getOrCreateDirectory(getGlobalDirectoryName() + SEPARATOR + path);
    try {
      return new File(directory.getCanonicalPath(), name);
    } catch (IOException e) {
      Main.LOGGER.warn("Could not get file " + path + SEPARATOR + name);
    }
    return null;
  }

  public static File getOrCreateDirectory(String path) {
    File directory = new File(".", path);
    if (!directory.exists()) {
      directory.mkdirs();
    }
    return directory;
  }

  private static File getFile(String path, String name, String extension) {
    if (!name.endsWith(extension)) {
      name = name + extension;
    }
    File globalFile = getFileFromGlobal(path, name);
    if (globalFile != null && globalFile.exists()) {
      return globalFile;
    }
    return getJsonFileForWriting(path, name);
  }

  private static File getFileForWriting(String path, String name, String extension) {
    if (!name.endsWith(extension)) {
      name = name + extension;
    }
    path = Main.MODID + SEPARATOR + path;
    return FileUtil.getFileFromPath(path, name);
  }

  public static File getJsonFile(String path, String name) {
    return getFile(path, name, ".json");
  }

  public static File getJsonFileForWriting(String path, String name) {
    return getFileForWriting(path, name, ".json");
  }
  public static File getFunctionFile(String path, String name) {
    return getFile(path, name, ".npcfunction");
  }

  public static File getFunctionFileForWriting(String path, String name) {
    return getFileForWriting(path, name, ".npcfunction");
  }

  public static boolean shouldGetFromWorld() {
    return Main.PROXY.shouldSaveInWorld();
  }

  public static File[] getAllFiles(String path) {
    File[] globalDirectory = getAllFromGlobal(path);
    File[] worldDirectory = getAllFromWorld(path);

    File[] out = new File[globalDirectory.length + worldDirectory.length];
    System.arraycopy(globalDirectory, 0, out, 0, globalDirectory.length);
    System.arraycopy(worldDirectory, 0, out, globalDirectory.length, worldDirectory.length);

    return out;
  }

  private static File[] getAllFromGlobal(String path) {
    try {
      return FileUtil.getOrCreateDirectory(FileUtil.getGlobalDirectoryName()+"/"+path).listFiles();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return new File[0];
  }

  private static File[] getAllFromWorld(String path) {
    try {
      return FileUtil.getOrCreateDirectory((shouldGetFromWorld() ? FileUtil.getWorldDirectory() + "/" : "") + Main.MODID + "/" + path).listFiles();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return new File[0];
  }

}
