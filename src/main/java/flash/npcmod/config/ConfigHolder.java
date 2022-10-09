package flash.npcmod.config;

import com.google.gson.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import flash.npcmod.Main;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

public class ConfigHolder {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private enum ConfigType {
    CLIENT,
    COMMON,
    SERVER
  }

  private static abstract class JsonConfig {

    private JsonElement config;
    private ConfigType type;

    JsonConfig(ConfigType type) {
      this.type = type;
      reloadConfig();
    }

    public void reloadConfig() {
      JsonElement config;
      if (type == ConfigType.SERVER) {
        config = ConfigHolder.loadPerWorldJsonConfigOrDefault(Main.MODID + "-" + type.name().toLowerCase(), getDefaultConfig());
      } else {
        config = ConfigHolder.loadJsonConfigOrDefault(Main.MODID + "-" + type.name().toLowerCase(), getDefaultConfig());
      }
      if (isConfigRightType(config) && getConfigValidator().test(config.toString())) {
        this.config = config;
      } else {
        Main.LOGGER.warn("Config file "+Main.MODID+"-"+type.name().toLowerCase()+".json has invalid data, returning default config instead.");
        this.config = getDefaultConfig();
      }
    }

    abstract boolean isConfigRightType(JsonElement config);

    JsonElement prevalidateConfig(Object text) {
      if (text == null) return null;
      if (!text.getClass().isAssignableFrom(String.class)) return null;
      JsonElement jsonElement = GSON.fromJson(String.valueOf(text), JsonElement.class);
      if (isConfigRightType(jsonElement)) return jsonElement;
      return null;
    }

    abstract Predicate<Object> getConfigValidator();

    abstract JsonElement getDefaultConfig();

    public JsonElement getConfig() {
      return config.deepCopy();
    }

    // In case we need to sync the server's config to clients
    @OnlyIn(Dist.CLIENT)
    public void setConfig(JsonElement config) {
      this.config = config;
    }

  }

  public static class Common extends JsonConfig {

    private static final List<String> DEFAULT_INVALID_COMMANDS = Arrays.asList(
            "/ban", "/ban-ip", "/deop", "/forceload", "/op", "/pardon", "/pardon-ip", "/save-off",
            "/setidletimeout", "/setworldspawn", "/stop", "/whitelist", "/"+Main.MODID
    );

    private static final String INVALID_COMMANDS_KEY = "invalid_commands";

    public Common() {
      super(ConfigType.COMMON);
    }

    @Override
    public void reloadConfig() {
      super.reloadConfig();
      JsonArray invalidCommands = getConfig().getAsJsonObject().getAsJsonArray(INVALID_COMMANDS_KEY);
      for (int i = 0; i < invalidCommands.size(); i++) {
        String command = invalidCommands.get(i).getAsString();
        if (!command.startsWith("/")) {
          invalidCommands.set(i, new JsonPrimitive("/".concat(command)));
        }
      }
      getConfig().getAsJsonObject().add(INVALID_COMMANDS_KEY, invalidCommands);
    }

    @Override
    boolean isConfigRightType(JsonElement config) {
      return config.isJsonObject();
    }

    @Override
    Predicate<Object> getConfigValidator() {
      return (text) -> {
        try {
          JsonElement jsonElement = prevalidateConfig(text);
          if (jsonElement == null) return false;
          JsonObject config = jsonElement.getAsJsonObject();
          if (!config.has(INVALID_COMMANDS_KEY) || !config.get(INVALID_COMMANDS_KEY).isJsonArray()) return false;
          return true;
        } catch(Exception e) {
          return false;
        }
      };
    }

    @Override
    JsonElement getDefaultConfig() {
      JsonObject config = new JsonObject();

      JsonArray defaultInvalidCommands = new JsonArray();
      DEFAULT_INVALID_COMMANDS.forEach(defaultInvalidCommands::add);
      config.add(INVALID_COMMANDS_KEY, defaultInvalidCommands);

      return config;
    }

    public boolean isInvalidCommand(String command) {
      JsonObject config = getConfig().getAsJsonObject();
      JsonArray invalidCommands = config.getAsJsonArray(INVALID_COMMANDS_KEY);
      for (int i = 0; i < invalidCommands.size(); i++) {
        String invalidCommand = invalidCommands.get(i).getAsString();
        if (command.equals(invalidCommand)) {
          return true;
        }
      }

      return false;
    }
  }

  @OnlyIn(Dist.CLIENT)
  public static class Client extends JsonConfig {

    public Client() {
      super(ConfigType.CLIENT);
    }

    @Override
    boolean isConfigRightType(JsonElement config) {
      return true;
    }

    @Override
    Predicate<Object> getConfigValidator() {
      return (text) -> {
        JsonElement jsonElement = prevalidateConfig(text);
        if (jsonElement == null) return false;
        return true;
      };
    }

    @Override
    JsonElement getDefaultConfig() {
      return null;
    }
  }

  public static class Server extends JsonConfig {

    public Server() {
      super(ConfigType.SERVER);
    }

    @Override
    boolean isConfigRightType(JsonElement config) {
      return true;
    }

    @Override
    Predicate<Object> getConfigValidator() {
      return (text) -> {
        JsonElement jsonElement = prevalidateConfig(text);
        if (jsonElement == null) return false;
        return true;
      };
    }

    @Override
    JsonElement getDefaultConfig() {
      return null;
    }
  }

  private static JsonElement buildJsonConfig(String name, JsonElement config) {
    try {
      File jsonFile = getJsonConfigFile("", name);
      Writer fw = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8);
      GSON.toJson(config, fw);
      fw.flush();
      fw.close();
    } catch (Exception e) {
      Main.LOGGER.warn("Could not create config file " + name + ".json");
      e.printStackTrace();
    }
    return config;
  }

  private static JsonElement loadJsonConfig(String path, String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(getJsonConfigFile(path, name)), StandardCharsets.UTF_8);
      return GSON.fromJson(is, JsonElement.class);
    } catch (FileNotFoundException e) {
      Main.LOGGER.warn("Could not find config file " + name + ".json");
    }
    return null;
  }

  private static JsonElement loadPerWorldJsonConfigOrDefault(String name, JsonElement defaultConfig) {
    JsonElement loadedConfig = null;
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(getPerWorldJsonConfigFile("", name)), StandardCharsets.UTF_8);
      loadedConfig = GSON.fromJson(is, JsonElement.class);
    } catch (Exception ignored) {}

    if (loadedConfig == null)  {
      Main.LOGGER.warn("Creating new config file " + name + ".json");
      return buildJsonConfig(name, defaultConfig);
    }
    else {
      return loadedConfig;
    }
  }

  private static JsonElement loadJsonConfigOrDefault(String name, JsonElement defaultConfig) {
    JsonElement loadedConfig = loadJsonConfig("", name);
    if (loadedConfig == null)  {
      Main.LOGGER.warn("Creating new config file " + name + ".json");
      return buildJsonConfig(name, defaultConfig);
    }
    else {
      return loadedConfig;
    }
  }

  @Nullable
  private static File readFileFrom(String path, String name) {
    File directory = readDirectory(path);
    try {
      File file = new File(directory.getCanonicalPath(), name);
      return file;
    } catch (IOException e) {
      Main.LOGGER.warn("Could not read file " + path + "/" + name);
    }
    return null;
  }

  @Nullable
  private static File readDirectory(String path) {
    File directory = new File(".", path);
    if (!directory.exists()) {
      directory.mkdirs();
    }
    return directory;
  }

  private static File getJsonConfigFile(String path, String name) {
    path = "config/"+path;
    return readFileFrom(path, name+".json");
  }

  private static File getPerWorldJsonConfigFile(String path, String name) {
    path = getWorldName()+"/serverconfig/"+path;
    return readFileFrom(path, name+".json");
  }

  private static String getWorldName() {
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    String worldName = server.getWorldData().getLevelName();
    if (server.isDedicatedServer()) {
      return worldName;
    }
    return "saves/"+worldName;
  }


  @OnlyIn(Dist.CLIENT)
  public static Client CLIENT;

  public static Common COMMON;
  public static Server SERVER;

  static {
    COMMON = new Common();
  }

  /**
   * Call from WorldEvent.Load
   */
  public static void initServer() {
    SERVER = new Server();
  }

  /**
   * Call from FMLClientSetupEvent
   */
  @OnlyIn(Dist.CLIENT)
  public static void initClient() {
    if (CLIENT == null) {
      CLIENT = new Client();
    }
  }
}
