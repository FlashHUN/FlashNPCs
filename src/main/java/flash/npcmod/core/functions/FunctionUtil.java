package flash.npcmod.core.functions;

import flash.npcmod.Main;
import flash.npcmod.core.FileUtil;
import flash.npcmod.core.functions.defaultfunctions.*;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FunctionUtil {

  private static boolean DEBUG_MODE = false;

  public static final List<AbstractFunction> FUNCTIONS = new ArrayList<>();
  private static List<AbstractFunction> DEFAULT_FUNCTIONS;

  private static final AcceptQuestFunction ACCEPT_QUEST = new AcceptQuestFunction();
  private static final CloseDialogueFunction CLOSE_DIALOGUE = new CloseDialogueFunction();
  private static final MoveOnAcceptedQuestFunction MOVE_ON_ACCEPTED_QUEST = new MoveOnAcceptedQuestFunction();
  private static final MoveOnCompleteQuestFunction MOVE_ON_COMPLETE_QUEST = new MoveOnCompleteQuestFunction();
  private static final MoveOnDataFunction MOVE_ON_DATA_FUNCTION = new MoveOnDataFunction();
  private static final MoveOnScoreboardFunction MOVE_ON_SCOREBOARD = new MoveOnScoreboardFunction();
  private static final MoveOnTagFunction MOVE_ON_TAG = new MoveOnTagFunction();
  private static final MoveOnTeamFunction MOVE_ON_TEAM = new MoveOnTeamFunction();
  private static final MoveToDialogueFunction MOVE_TO_DIALOGUE = new MoveToDialogueFunction();
  private static final OpenTradesFunction OPEN_TRADES = new OpenTradesFunction();
  private static final PlaySoundFunction PLAY_SOUND = new PlaySoundFunction();
  private static final RandomOptionFunction RANDOM_OPTION = new RandomOptionFunction();

  private static void addDefaultFunctions() {
    FUNCTIONS.add(ACCEPT_QUEST);
    FUNCTIONS.add(CLOSE_DIALOGUE);
    FUNCTIONS.add(MOVE_ON_ACCEPTED_QUEST);
    FUNCTIONS.add(MOVE_ON_COMPLETE_QUEST);
    FUNCTIONS.add(MOVE_ON_DATA_FUNCTION);
    FUNCTIONS.add(MOVE_ON_SCOREBOARD);
    FUNCTIONS.add(MOVE_ON_TAG);
    FUNCTIONS.add(MOVE_ON_TEAM);
    FUNCTIONS.add(MOVE_TO_DIALOGUE);
    FUNCTIONS.add(OPEN_TRADES);
    FUNCTIONS.add(PLAY_SOUND);
    FUNCTIONS.add(RANDOM_OPTION);
    DEFAULT_FUNCTIONS = List.copyOf(FUNCTIONS);
  }

  public static List<AbstractFunction> getDefaultFunctions() {
    return DEFAULT_FUNCTIONS;
  }

  public static void toggleDebugMode() {
    DEBUG_MODE = !DEBUG_MODE;
  }

  public static boolean isDebugMode() {
    return DEBUG_MODE;
  }

  public static void build(String name, String function) {
    try {
      String[] lines = function.split("\n");

      String[] paramNames = lines[0].split(",");
      List<String> callables = new ArrayList<>();

      File functionFile = FileUtil.getFunctionFileForWriting("functions", name);

      if (functionFile.exists()) {
        functionFile.delete();
      }

      Writer writer = new OutputStreamWriter(new FileOutputStream(functionFile), StandardCharsets.UTF_8);

      for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        if (i > 0) {
          callables.add(line);
        }
        writer.write(line);
        if (i < lines.length-1)
          writer.write(System.lineSeparator());
      }

      writer.flush();
      writer.close();

      AbstractFunction newFunction = new Function(name, paramNames, callables.toArray(new String[0]));
      FUNCTIONS.remove(newFunction);

      FUNCTIONS.add(newFunction);

    } catch (IOException e) {
      Main.LOGGER.warn("Could not build function file " + name + ".npcfunction");
    }
  }

  public static void loadAllFunctions() {
    FUNCTIONS.clear();
    addDefaultFunctions();

    File[] files = FileUtil.getAllFiles("functions");
    for (File entry : files) {
      if (!entry.isDirectory()) {
        loadFunctionFile(FilenameUtils.removeExtension(entry.getName()));
      }
    }
  }

  public static boolean loadFunctionFile(String name) {
    try {
      InputStream is = new FileInputStream(FileUtil.getFunctionFile("functions", name));
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

      List<String> lines = new ArrayList<>();

      // First line should always be the parameter names
      String line = reader.readLine();
      String[] paramNames = line.split(",");

      // Reading all the other lines (commands, functions)
      while((line = reader.readLine()) != null) {
        lines.add(line);
      }

      AbstractFunction function = new Function(name, paramNames, lines.toArray(new String[0]));
      FUNCTIONS.remove(function);

      FUNCTIONS.add(function);
      reader.close();
      is.close();
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

  public static boolean deleteFunction(String name) {
    try {
      File file = FileUtil.getFunctionFileForWriting("functions", name);
      if (file != null) {
        FUNCTIONS.removeIf(function -> function.getName().equals(name));
        return file.delete();
      }
    } catch (Exception e) {
      Main.LOGGER.warn("Error deleting function " + name);
    }
    return false;
  }

  public static void callFromName(String name, ServerPlayer sender, NpcEntity npcEntity) {
    name = name.replaceFirst("function:", "");
    String[] params = new String[0];
    if (name.contains("::")) {
      String[] splitName = name.split("::");
      name = splitName[0];
      // functionName::param1,param2 would return String[]{param1, param2}
      params = splitName[1].split(",");
    }

    boolean found = false;
    for (AbstractFunction function : FUNCTIONS) {
      if (function.getName().equals(name)) {
        function.call(params, sender, npcEntity);
        found = true;
        break;
      }
    }

    if (!found) {
      if (loadFunctionFile(name)) {
        callFromName(name, sender, npcEntity);
      }
    }
  }

  public static String replaceSelectors(String s, ServerPlayer sender, NpcEntity npcEntity) {
    return s.replaceAll("@p", sender.getName().getString())
            .replaceAll("@npc", npcEntity.getStringUUID());
  }

  public static String replaceParameters(String s, String paramName, String param) {
    return s.replaceAll(paramName, param);
  }

}
