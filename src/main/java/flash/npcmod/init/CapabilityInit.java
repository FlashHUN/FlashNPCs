package flash.npcmod.init;

import flash.npcmod.capability.quests.IQuestCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CapabilityInit {

  public static final Capability<IQuestCapability> QUEST_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

  @SubscribeEvent
  public void register(RegisterCapabilitiesEvent event) {
    event.register(IQuestCapability.class);
  }
}
