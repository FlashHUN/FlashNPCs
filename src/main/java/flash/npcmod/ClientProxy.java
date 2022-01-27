package flash.npcmod;

import flash.npcmod.capability.quests.IQuestCapability;
import flash.npcmod.capability.quests.QuestCapabilityProvider;
import flash.npcmod.client.gui.screen.FunctionBuilderScreen;
import flash.npcmod.client.gui.screen.NpcBuilderScreen;
import flash.npcmod.client.gui.screen.SavedNpcsScreen;
import flash.npcmod.client.gui.screen.dialogue.DialogueBuilderScreen;
import flash.npcmod.client.gui.screen.dialogue.DialogueScreen;
import flash.npcmod.client.gui.screen.quests.QuestEditorScreen;
import flash.npcmod.core.client.dialogues.ClientDialogueUtil;
import flash.npcmod.core.client.quests.ClientQuestUtil;
import flash.npcmod.core.quests.Quest;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.core.quests.QuestObjective;
import flash.npcmod.core.trades.TradeOffers;
import flash.npcmod.entity.NpcEntity;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.client.CCallFunction;
import flash.npcmod.network.packets.client.CRequestQuestInfo;
import flash.npcmod.network.packets.server.SOpenScreen;
import flash.npcmod.network.packets.server.SSyncQuestCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {

  public static List<String> SAVED_NPCS = new ArrayList<>();

  Minecraft minecraft = Minecraft.getInstance();

  public void openScreen(SOpenScreen.EScreens screen, String data, int entityid) {
    Screen toOpen = null;
    NpcEntity npcEntity = (NpcEntity) minecraft.player.world.getEntityByID(entityid);
    switch (screen) {
      case DIALOGUE: toOpen = new DialogueScreen(data, npcEntity); break;
      case EDITDIALOGUE: toOpen = new DialogueBuilderScreen(data); break;
      case FUNCTIONBUILDER: toOpen = new FunctionBuilderScreen(); break;
      case EDITNPC: toOpen = new NpcBuilderScreen(npcEntity); break;
      case QUESTEDITOR:
        if (data.isEmpty())
          toOpen = new QuestEditorScreen();
        else {
          PacketDispatcher.sendToServer(new CRequestQuestInfo(data));
          Quest quest = ClientQuestUtil.fromName(data);
          if (quest != null)
            toOpen = QuestEditorScreen.fromQuest(quest);
        }
        break;
      case SAVEDNPCS: toOpen = new SavedNpcsScreen(data); break;
    }
    minecraft.displayGuiScreen(toOpen);
  }



  public void randomDialogueOption() {
    if (minecraft.currentScreen instanceof DialogueScreen) {
      DialogueScreen screen = (DialogueScreen)minecraft.currentScreen;
      screen.chooseRandomOption();
    }
  }

  public void moveToDialogue(String dialogueName, int entityid) {
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
        PacketDispatcher.sendToServer(new CCallFunction(ClientDialogueUtil.getCurrentFunction(), entityid));
      }
      screen.resetOptionButtons();
    }
  }

  public void closeDialogue() {
    if (minecraft.currentScreen instanceof DialogueScreen) {
      minecraft.currentScreen.closeScreen();
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



  public boolean shouldSaveInWorld() { return minecraft.isSingleplayer(); }



  public void syncTrackedQuest(String trackedQuest) {
    IQuestCapability questCapability = QuestCapabilityProvider.getCapability(minecraft.player);
    questCapability.setTrackedQuest(trackedQuest);
  }

  public void syncAcceptedQuests(ArrayList<QuestInstance> acceptedQuests) {
    IQuestCapability questCapability = QuestCapabilityProvider.getCapability(minecraft.player);
    questCapability.setAcceptedQuests(acceptedQuests);
  }

  public void syncCompletedQuests(ArrayList<String> completedQuests) {
    IQuestCapability questCapability = QuestCapabilityProvider.getCapability(minecraft.player);
    questCapability.setCompletedQuests(completedQuests);
  }

  public void syncQuestProgressMap(Map<QuestObjective, Integer> progressMap) {
    IQuestCapability questCapability = QuestCapabilityProvider.getCapability(minecraft.player);
    Map<QuestObjective, Integer> map = questCapability.getQuestProgressMap();
    progressMap.forEach((objective, progress) -> {
      map.put(objective, progress);
    });
  }


  public void acceptQuest(String name, int entityid) {
    PacketDispatcher.sendToServer(new CRequestQuestInfo(name));
    Quest quest = ClientQuestUtil.fromName(name);
    Entity entity = minecraft.player.world.getEntityByID(entityid);
    if (quest != null && entity instanceof NpcEntity) {
      IQuestCapability capability = QuestCapabilityProvider.getCapability(minecraft.player);

      QuestInstance questInstance = new QuestInstance(quest, entity.getUniqueID(), entity.getName().getString(), minecraft.player);
      capability.acceptQuest(questInstance);
    }
  }

  public void completeQuest(String name, UUID uuid) {
    IQuestCapability capability = QuestCapabilityProvider.getCapability(minecraft.player);

    QuestInstance instance = null;
    for (QuestInstance current : capability.getAcceptedQuests()) {
      if (current.getQuest().getName().equals(name) && current.getPickedUpFrom().equals(uuid)) {
        instance = current; break;
      }
    }

    if (instance != null)
      capability.completeQuest(instance);
      
    ClientQuestUtil.loadQuest(name);
  }


  public PlayerEntity getPlayer() {
    return minecraft.player;
  }

  @Override
  public World getWorld() {
    return minecraft.world;
  }

  public SSyncQuestCapability decodeQuestCapabilitySync(PacketBuffer buf) {
    int typeInt = buf.readInt();
    if (typeInt < 0 || typeInt >= SSyncQuestCapability.CapabilityType.values().length) return new SSyncQuestCapability();
    SSyncQuestCapability.CapabilityType type = SSyncQuestCapability.CapabilityType.values()[typeInt];
    switch (type) {
      default:
        String trackedName = buf.readString(51);
        if (!trackedName.isEmpty()) {
          Quest quest = ClientQuestUtil.fromName(trackedName);
          if (quest != null)
            return new SSyncQuestCapability(trackedName);
          else
            return new SSyncQuestCapability();
        } else
          return new SSyncQuestCapability();
      case ACCEPTED_QUESTS:
        List<QuestInstance> acceptedQuests = new ArrayList<>();
        int acceptedAmount = buf.readInt();
        for (int i = 0; i < acceptedAmount; i++) {
          String questName = buf.readString(51);
          UUID pickedUpFrom = buf.readUniqueId();
          String pickedUpFromName = buf.readString(200);
          Quest quest = ClientQuestUtil.fromName(questName);
          if (quest != null) {
            acceptedQuests.add(new QuestInstance(quest, pickedUpFrom, pickedUpFromName, minecraft.player));
            for (int j = 0; j < quest.getObjectives().size(); j++) {
              int id = buf.readInt();
              for (int k = 0; k < quest.getObjectives().size(); k++) {
                QuestObjective objective = quest.getObjectives().get(k);
                if (objective.getId() == id) {
                  int progress = buf.readInt();
                  boolean isHidden = buf.readBoolean();
                  objective.setProgress(progress);
                  objective.onComplete(minecraft.player);
                  objective.setHidden(isHidden);
                  break;
                }
              }
            }
          }
        }
        return new SSyncQuestCapability(acceptedQuests.toArray(new QuestInstance[0]));
      case COMPLETED_QUESTS:
        List<String> completedQuests = new ArrayList<>();
        int completedAmount = buf.readInt();
        for (int i = 0; i < completedAmount; i++) {
          String name = buf.readString(51);
          completedQuests.add(name);
        }
        return new SSyncQuestCapability(completedQuests.toArray(new String[0]));
      case PROGRESS_MAP:
        Map<QuestObjective, Integer> objectiveProgressMap = new HashMap<>();
        int progressMapAmount = buf.readInt();
        for (int i = 0; i < progressMapAmount; i++) {
          String key = buf.readString();
          String[] splitKey = key.split(":::");
          int progress = buf.readInt();
          Quest quest = ClientQuestUtil.fromName(splitKey[0]);
          if (quest != null) {
            QuestObjective objective = Quest.getObjectiveFromName(quest, splitKey[1]);
            if (objective != null) {
              objectiveProgressMap.put(objective, progress);
              objective.setProgress(progress);
            }
          }
        }
        return new SSyncQuestCapability(objectiveProgressMap);
    }
  }

  public void syncTrades(int entityid, TradeOffers tradeOffers) {
    Entity entity = minecraft.player.world.getEntityByID(entityid);
    if (entity instanceof NpcEntity) {
      ((NpcEntity) entity).setTradeOffers(tradeOffers);
    }
  }

  public void loadSavedNpcs(List<String> savedNpcs) {
    SAVED_NPCS = savedNpcs;
  }

}
