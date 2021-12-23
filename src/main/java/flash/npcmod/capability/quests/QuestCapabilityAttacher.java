package flash.npcmod.capability.quests;

import flash.npcmod.Main;
import flash.npcmod.core.quests.QuestInstance;
import flash.npcmod.init.CapabilityInit;
import flash.npcmod.network.PacketDispatcher;
import flash.npcmod.network.packets.server.SSyncQuestCapability;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuestCapabilityAttacher {

  private static class QuestCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final ResourceLocation IDENTIFIER = new ResourceLocation(Main.MODID, "quests");

    private final IQuestCapability backend = new QuestCapability();
    private final LazyOptional<IQuestCapability> optionalData = LazyOptional.of(() -> backend);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
      return CapabilityInit.QUEST_CAPABILITY.orEmpty(cap, this.optionalData);
    }

    void invalidate() {
      this.optionalData.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
      return this.backend.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
      this.backend.deserializeNBT(nbt);
    }
  }

  public static IQuestCapability getCapability(Player player) {
    return player.getCapability(CapabilityInit.QUEST_CAPABILITY).orElse(new QuestCapability());
  }

  @Mod.EventBusSubscriber(modid = Main.MODID)
  private static class EventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void attach(final AttachCapabilitiesEvent<Entity> event) {
      if (event.getObject() instanceof Player) {
        final QuestCapabilityProvider provider = new QuestCapabilityProvider();

        event.addCapability(QuestCapabilityProvider.IDENTIFIER, provider);
      }
    }

    @SubscribeEvent
    public static void playerClone(final PlayerEvent.Clone event) {
      final IQuestCapability oldCap = getCapability(event.getOriginal());
      final IQuestCapability newCap = getCapability(event.getPlayer());

      newCap.setTrackedQuest(oldCap.getTrackedQuest());
      newCap.setAcceptedQuests(oldCap.getAcceptedQuests());
      newCap.setCompletedQuests(oldCap.getCompletedQuests());
      newCap.setQuestProgressMap(oldCap.getQuestProgressMap());
    }

    @SubscribeEvent
    public static void serverLoginEvent(final PlayerEvent.PlayerLoggedInEvent event) {
      Player player = event.getPlayer();
      if (player != null && player.isAlive()) {
        IQuestCapability capability = getCapability(player);
        capability.getAcceptedQuests().forEach(instance -> instance.setPlayer(player));

        syncCapability(player);
      }
    }

    @SubscribeEvent
    public static void changeDimesionEvent(final PlayerEvent.PlayerChangedDimensionEvent event) {
      Player player = event.getPlayer();
      if (player != null && player.isAlive()) {
        syncCapability(player);
      }
    }

    @SubscribeEvent
    public static void respawnEvent(final PlayerEvent.PlayerRespawnEvent event) {
      Player player = event.getPlayer();
      if (player != null && player.isAlive()) {
        syncCapability(player);
      }
    }

    private static void syncCapability(Player player) {
      IQuestCapability questCapability = getCapability(player);

      if (questCapability.getTrackedQuest() != null)
        PacketDispatcher.sendTo(new SSyncQuestCapability(questCapability.getTrackedQuest()), player);

      PacketDispatcher.sendTo(new SSyncQuestCapability(questCapability.getAcceptedQuests().toArray(new QuestInstance[0])), player);
      PacketDispatcher.sendTo(new SSyncQuestCapability(questCapability.getCompletedQuests().toArray(new String[0])), player);
    }
  }

}
