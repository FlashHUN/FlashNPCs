package flash.npcmod.network;

import flash.npcmod.Main;
import flash.npcmod.network.packets.client.*;
import flash.npcmod.network.packets.server.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

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
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CCallFunction.class, CCallFunction::encode, CCallFunction::decode, CCallFunction::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CBuildFunction.class, CBuildFunction::encode, CBuildFunction::decode, CBuildFunction::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CEditDialogue.class, CEditDialogue::encode, CEditDialogue::decode, CEditDialogue::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CEditNpc.class, CEditNpc::encode, CEditNpc::decode, CEditNpc::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CHandleNpcEditorRightClick.class, CHandleNpcEditorRightClick::encode, CHandleNpcEditorRightClick::decode, CHandleNpcEditorRightClick::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CRequestContainer.class, CRequestContainer::encode, CRequestContainer::decode, CRequestContainer::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CRequestDialogue.class, CRequestDialogue::encode, CRequestDialogue::decode, CRequestDialogue::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), CRequestDialogueEditor.class, CRequestDialogueEditor::encode, CRequestDialogueEditor::decode, CRequestDialogueEditor::handle);

    PacketDispatcher.INSTANCE.registerMessage(nextID(), SMoveToDialogue.class, SMoveToDialogue::encode, SMoveToDialogue::decode, SMoveToDialogue::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SOpenScreen.class, SOpenScreen::encode, SOpenScreen::decode, SOpenScreen::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SResetFunctionNames.class, SResetFunctionNames::encode, SResetFunctionNames::decode, SResetFunctionNames::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSendDialogue.class, SSendDialogue::encode, SSendDialogue::decode, SSendDialogue::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSendDialogueEditor.class, SSendDialogueEditor::encode, SSendDialogueEditor::decode, SSendDialogueEditor::handle);
    PacketDispatcher.INSTANCE.registerMessage(nextID(), SSendFunctionName.class, SSendFunctionName::encode, SSendFunctionName::decode, SSendFunctionName::handle);
  }

  public static <MSG> void sendTo(MSG msg, PlayerEntity player) {
    INSTANCE.sendTo(msg, ((ServerPlayerEntity)player).connection.getNetworkManager(), NetworkDirection.PLAY_TO_CLIENT);
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
