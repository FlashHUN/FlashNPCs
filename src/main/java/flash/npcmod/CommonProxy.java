package flash.npcmod;

import flash.npcmod.network.packets.server.SOpenScreen;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class CommonProxy {

  public void openScreen(SOpenScreen.EScreens screen, String data, int entityid) {}

  public void moveToDialogue(String dialogueName) {}

  public void addFunctionName(String functionName) {}
  public void resetFunctionNames() {}

  public boolean isOnClient() { return FMLEnvironment.dist.isDedicatedServer(); }
}
