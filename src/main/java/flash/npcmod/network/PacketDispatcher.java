package flash.npcmod.network;

import flash.npcmod.Main;
import flash.npcmod.network.packets.client.*;
import flash.npcmod.network.packets.server.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketDispatcher {

  private static int packetId = 0;

  private static final String PROTOCOL_VERSION = "1";
  private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
      new ResourceLocation(Main.MODID, "main"),
      () -> PROTOCOL_VERSION,
      PROTOCOL_VERSION::equals,
      PROTOCOL_VERSION::equals
  );

  public PacketDispatcher() {}

  public static int nextID() {
    return packetId++;
  }

  public static void registerMessages() {
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CAbandonQuest.class, CAbandonQuest::encode, CAbandonQuest::decode, CAbandonQuest::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CBuildFunction.class, CBuildFunction::encode, CBuildFunction::decode, CBuildFunction::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CBuildQuest.class, CBuildQuest::encode, CBuildQuest::decode, CBuildQuest::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CCallFunction.class, CCallFunction::encode, CCallFunction::decode, CCallFunction::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CCallTrigger.class, CCallTrigger::encode, CCallTrigger::decode, CCallTrigger::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CEditBehavior.class, CEditBehavior::encode, CEditBehavior::decode, CEditBehavior::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CEditDialogue.class, CEditDialogue::encode, CEditDialogue::decode, CEditDialogue::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CEditNpc.class, CEditNpc::encode, CEditNpc::decode, CEditNpc::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CHandleNpcEditorRightClick.class, CHandleNpcEditorRightClick::encode, CHandleNpcEditorRightClick::decode, CHandleNpcEditorRightClick::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CHandleNpcSaveToolRightClick.class, CHandleNpcSaveToolRightClick::encode, CHandleNpcSaveToolRightClick::decode, CHandleNpcSaveToolRightClick::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CHandleSavedNpc.class, CHandleSavedNpc::encode, CHandleSavedNpc::decode, CHandleSavedNpc::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CRequestBehavior.class, CRequestBehavior::encode, CRequestBehavior::decode, CRequestBehavior::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CRequestBehaviorEditor.class, CRequestBehaviorEditor::encode, CRequestBehaviorEditor::decode, CRequestBehaviorEditor::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CRequestContainer.class, CRequestContainer::encode, CRequestContainer::decode, CRequestContainer::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CRequestDialogue.class, CRequestDialogue::encode, CRequestDialogue::decode, CRequestDialogue::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CRequestDialogueEditor.class, CRequestDialogueEditor::encode, CRequestDialogueEditor::decode, CRequestDialogueEditor::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CRequestQuestCapabilitySync.class, CRequestQuestCapabilitySync::encode, CRequestQuestCapabilitySync::decode, CRequestQuestCapabilitySync::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CRequestQuestEditor.class, CRequestQuestEditor::encode, CRequestQuestEditor::decode, CRequestQuestEditor::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CRequestQuestInfo.class, CRequestQuestInfo::encode, CRequestQuestInfo::decode, CRequestQuestInfo::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CRequestTrades.class, CRequestTrades::encode, CRequestTrades::decode, CRequestTrades::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CTalkObjectiveComplete.class, CTalkObjectiveComplete::encode, CTalkObjectiveComplete::decode, CTalkObjectiveComplete::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CTrackQuest.class, CTrackQuest::encode, CTrackQuest::decode, CTrackQuest::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CTradeWithNpc.class, CTradeWithNpc::encode, CTradeWithNpc::decode, CTradeWithNpc::handle);

    PacketDispatcher.INSTANCE.registerMessage(nextID(), SAcceptQuest.class, SAcceptQuest::encode, SAcceptQuest::decode, SAcceptQuest::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SCloseDialogue.class, SCloseDialogue::encode, SCloseDialogue::decode, SCloseDialogue::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SCompleteQuest.class, SCompleteQuest::encode, SCompleteQuest::decode, SCompleteQuest::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SMoveToDialogue.class, SMoveToDialogue::encode, SMoveToDialogue::decode, SMoveToDialogue::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SOpenScreen.class, SOpenScreen::encode, SOpenScreen::decode, SOpenScreen::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SRandomOptionFunction.class, SRandomOptionFunction::encode, SRandomOptionFunction::decode, SRandomOptionFunction::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SResetFunctionNames.class, SResetFunctionNames::encode, SResetFunctionNames::decode, SResetFunctionNames::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSendBehavior.class, SSendBehavior::encode, SSendBehavior::decode, SSendBehavior::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSendBehaviorEditor.class, SSendBehaviorEditor::encode, SSendBehaviorEditor::decode, SSendBehaviorEditor::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSendDialogue.class, SSendDialogue::encode, SSendDialogue::decode, SSendDialogue::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSendDialogueEditor.class, SSendDialogueEditor::encode, SSendDialogueEditor::decode, SSendDialogueEditor::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSendFunctionName.class, SSendFunctionName::encode, SSendFunctionName::decode, SSendFunctionName::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSendQuestInfo.class, SSendQuestInfo::encode, SSendQuestInfo::decode, SSendQuestInfo::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSyncEntityList.class, SSyncEntityList::encode, SSyncEntityList::decode, SSyncEntityList::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSyncQuestCapability.class, SSyncQuestCapability::encode, SSyncQuestCapability::decode, SSyncQuestCapability::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSyncSavedNpcs.class, SSyncSavedNpcs::encode, SSyncSavedNpcs::decode, SSyncSavedNpcs::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSyncTrades.class, SSyncTrades::encode, SSyncTrades::decode, SSyncTrades::handle);
  }

  public static <MSG> void sendTo(MSG msg, Player player) {
    INSTANCE.sendTo(msg, ((ServerPlayer)player).connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
  }

  public static <MSG> void sendToAllTracking(MSG msg, LivingEntity entityToTrack) {
    INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entityToTrack), msg);
  }

  public static <MSG> void sendToAll(MSG msg) {
    INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
  }

  public static <MSG> void sendToServer(MSG msg) {
    INSTANCE.sendToServer(msg);
  }

}
