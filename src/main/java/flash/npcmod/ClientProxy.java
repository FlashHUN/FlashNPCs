package flash.npcmod;

import flash.npcmod.client.gui.screen.DialogueBuilderScreen;
import flash.npcmod.client.gui.screen.DialogueScreen;
import flash.npcmod.client.gui.screen.FunctionBuilderScreen;
import flash.npcmod.client.gui.screen.NpcBuilderScreen;
import flash.npcmod.core.client.dialogues.ClientDialogueUtil;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CCallFunction;
import flash.npcmod.network.packets.server.SOpenScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public class ClientProxy extends CommonProxy {

  Minecraft minecraft = Minecraft.getInstance();

  public void openScreen(SOpenScreen.EScreens screen, String data, int entityid) {
    Screen toOpen = null;
    NpcEntity npcEntity = (NpcEntity) minecraft.player.world.getEntityByID(entityid);
    switch (screen) {
      case DIALOGUE: toOpen = new DialogueScreen(data, npcEntity); break;
      case EDITDIALOGUE: toOpen = new DialogueBuilderScreen(data); break;
      case FUNCTIONBUILDER: toOpen = new FunctionBuilderScreen(); break;
      case EDITNPC: toOpen = new NpcBuilderScreen(npcEntity); break;
    }
    minecraft.displayGuiScreen(toOpen);
  }

  public void moveToDialogue(String dialogueName) {
    if (minecraft.currentScreen instanceof DialogueScreen) {
      DialogueScreen screen = (DialogueScreen)minecraft.currentScreen;
      ClientDialogueUtil.loadDialogue(screen.getDialogueName());
      ClientDialogueUtil.loadDialogueOption(dialogueName);
      if (!ClientDialogueUtil.getCurrentResponse().isEmpty()) {
        if (!ClientDialogueUtil.getCurrentText().isEmpty()) {
          screen.addDisplayedPlayerText(ClientDialogueUtil.getCurrentText());
        }
        screen.addDisplayedNPCText(ClientDialogueUtil.getCurrentResponse());
      }
      else if (!ClientDialogueUtil.getCurrentText().isEmpty()) {
        screen.addDisplayedNPCText(ClientDialogueUtil.getCurrentText());
      }
      if (!ClientDialogueUtil.getCurrentFunction().isEmpty()) {
        PacketDispatcher.sendToServer(new CCallFunction(ClientDialogueUtil.getCurrentFunction()));
      }
      screen.resetOptionButtons();
    }
  }

  public void resetFunctionNames() {
    ClientDialogueUtil.FUNCTION_NAMES.clear();
  }

  public void addFunctionName(String functionName) {
    if (!ClientDialogueUtil.FUNCTION_NAMES.contains(functionName)) {
      ClientDialogueUtil.FUNCTION_NAMES.add(functionName);
    }
  }

  public boolean isOnClient() { return Minecraft.getInstance().isSingleplayer(); }

}
