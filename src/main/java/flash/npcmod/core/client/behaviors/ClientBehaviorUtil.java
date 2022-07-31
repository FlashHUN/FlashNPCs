package flash.npcmod.core.client.behaviors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import flash.npcmod.Main;
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
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/behaviors", name+".json")), StandardCharsets.UTF_8);
      JsonObject object = new Gson().fromJson(is, JsonObject.class);

      currentBehavior = object;
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
      InputStreamReader is = new InputStreamReader(new FileInputStream(FileUtil.readFileFrom(Main.MODID+"/behavior_editor", name+".json")), StandardCharsets.UTF_8);
      JsonObject object = new Gson().fromJson(is, JsonObject.class);

      currentBehaviorEditor = object;
      is.close();
    } catch (Exception e) {
      PacketDispatcher.sendToServer(new CRequestBehaviorEditor(name));
    }
  }
}
