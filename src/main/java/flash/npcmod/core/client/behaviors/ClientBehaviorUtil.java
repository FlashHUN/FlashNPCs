package flash.npcmod.core.client.behaviors;

import com.google.gson.JsonObject;
import flash.npcmod.core.FileUtil;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CRequestBehaviorEditor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@OnlyIn(Dist.CLIENT)
public class ClientBehaviorUtil {

  public static final String INIT_BEHAVIOR_NAME = "init";
  @Nullable
  public static JsonObject currentBehavior = null;
  @Nullable
  public static JsonObject currentBehaviorEditor = null;

  /**
   * Load the node data of each behavior.
   * @param name The file name.
   */
  public static void loadBehavior(String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.getJsonFile("behaviors", name)), StandardCharsets.UTF_8);

      currentBehavior = FileUtil.GSON.fromJson(is, JsonObject.class);
      is.close();
    } catch (Exception e) {
      PacketDispatcher.sendToServer(new CRequestBehaviorEditor(name));
    }
  }

  /**
   * Load the Position of all behaviors for the editor.
   * @param name The file name.
   */
  public static void loadBehaviorEditor(String name) {
    try {
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.getJsonFile("behavior_editor", name)), StandardCharsets.UTF_8);

      currentBehaviorEditor = FileUtil.GSON.fromJson(is, JsonObject.class);
      is.close();
    } catch (Exception e) {
      PacketDispatcher.sendToServer(new CRequestBehaviorEditor(name));
    }
  }
}
