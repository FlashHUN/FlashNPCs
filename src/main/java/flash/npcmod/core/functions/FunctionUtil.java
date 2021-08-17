package flash.npcmod.core.functions;

import flash.npcmod.Main;
import flash.npcmod.core.FileUtil;
import flash.npcmod.core.functions.defaultfunctions.MoveToDialogueFunction;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FunctionUtil {

  public static final List<Function> FUNCTIONS = new ArrayList<>();

  private static final MoveToDialogueFunction MOVE_TO_DIALOGUE_FUNCTION = new MoveToDialogueFunction();

  private static void addDefaultFunctions() {
    FUNCTIONS.add(MOVE_TO_DIALOGUE_FUNCTION);
  }

  public static void build(String name, String function) {
    File folder = FileUtil.readDirectory(FileUtil.getWorldName()+"/"+Main.MODID+"/functions");
    try {
      String[] lines = function.split("\n");

      String[] paramNames = lines[0].split(",");
      List<String> callables = new ArrayList<>();

      File newFunctionFile = new File(folder.getCanonicalPath(), name+".npcfunction");

      if (newFunctionFile.exists()) {
        newFunctionFile.delete();
      }

      FileWriter fileWriter = new FileWriter(newFunctionFile, true);

      for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        if (i > 0) {
          callables.add(line);
        }
        fileWriter.write(line);
        if (i < lines.length-1)
          fileWriter.write(System.lineSeparator());
      }

      fileWriter.flush();
      fileWriter.close();

      Function newFunction = new Function(name, paramNames, callables.toArray(new String[0]));
      if (FUNCTIONS.contains(newFunction)) {
        FUNCTIONS.remove(newFunction);
      }
      FUNCTIONS.add(newFunction);

    } catch (IOException e) {
      Main.LOGGER.warn("Could not build function file " + name + ".npcfunction");
    }
  }

  public static void loadAllFunctions() {
    FUNCTIONS.clear();
    addDefaultFunctions();

    File folder = FileUtil.readDirectory(FileUtil.getWorldName()+"/"+Main.MODID+"/functions");
    for (File entry : folder.listFiles()) {
      if (!entry.isDirectory()) {
        loadFunctionFile(FilenameUtils.removeExtension(entry.getName()));
      }
    }
  }

  public static boolean loadFunctionFile(String name) {
    try {
      InputStream is = new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/functions", name + ".npcfunction"));
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));

      List<String> lines = new ArrayList<>();

      // First line should always be the parameter names
      String line = reader.readLine();
      String[] paramNames = line.split(",");

      // Reading all the other lines (commands, functions)
      while((line = reader.readLine()) != null) {
        lines.add(line);
      }

      Function function = new Function(name, paramNames, lines.toArray(new String[0]));
      if (FUNCTIONS.contains(function)) {
        FUNCTIONS.remove(function);
      }
      FUNCTIONS.add(function);

      return true;
    }
    catch (FileNotFoundException e) {
      Main.LOGGER.warn("Could not find function file " + name + ".npcfunction");
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  public static void callFromName(String name, ServerPlayerEntity sender) {
    name = name.replaceFirst("function:", "");
    String[] params = new String[0];
    if (name.contains("::")) {
      String[] splitName = name.split("::");
      name = splitName[0];
      // functionName::param1,param2 would return String[]{param1, param2}
      params = splitName[1].split(",");
    }

    boolean found = false;
    for (Function function : FUNCTIONS) {
      if (function.getName().equals(name)) {
        function.call(params, sender);
        found = true;
        break;
      }
    }

    if (!found) {
      if (loadFunctionFile(name)) {
        callFromName(name, sender);
      }
    }
  }

  public static String replaceSelectors(String s, ServerPlayerEntity sender) {
    return s.replaceAll("@p", sender.getName().getString());
  }

  public static String replaceParameters(String s, String paramName, String param) {
    return s.replaceAll(paramName, param);
  }

}
