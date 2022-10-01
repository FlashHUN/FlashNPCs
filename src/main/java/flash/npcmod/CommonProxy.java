package flash.npcmod;

import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.core.trades.TradeOffers;
import flash.npcmod.network.packets.server.SOpenScreen;
import flash.npcmod.network.packets.server.SSyncQuestCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommonProxy {

  public void openScreen(SOpenScreen.EScreens screen, String data, int entityid) {}

  public void randomDialogueOption() {}
  public void moveToDialogue(String dialogueName, int entityid) {}
  public void closeDialogue() {}

  public void addFunctionName(String functionName) {}
  public void resetFunctionNames() {}

  public boolean shouldSaveInWorld() { return FMLEnvironment.dist.isDedicatedServer(); }

  public void syncTrackedQuest(String trackedQuest) {}
  public void syncAcceptedQuests(ArrayList<QuestInstance> acceptedQuests) {}
  public void syncCompletedQuests(ArrayList<String> completedQuests) {}
  public void syncQuestProgressMap(Map<QuestObjective, Integer> progressMap) {}

  public void acceptQuest(String name, int entityid, QuestInstance.TurnInType turnInType, UUID uuid) {}
  public void completeQuest(String name, UUID uuid) {}

  public Player getPlayer() {
    return null;
  }
  public Level getWorld() { return ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD); }

  public SSyncQuestCapability decodeQuestCapabilitySync(FriendlyByteBuf buf) { return new SSyncQuestCapability(); }
  public void syncTrades(int entityid, TradeOffers tradeOffers) {}

  public void loadSavedNpcs(List<String> savedNpcs) {}

  public void loadEntities(String[] entities) {}

}
