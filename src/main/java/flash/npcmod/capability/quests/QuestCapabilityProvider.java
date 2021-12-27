package flash.npcmod.capability.quests;

import flash.npcmod.Main;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QuestCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

  public static final Capability<IQuestCapability> QUEST_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

  public static final ResourceLocation IDENTIFIER = new ResourceLocation(Main.MODID, "quests");

  private IQuestCapability instance = new QuestCapability();

  @SuppressWarnings("unchecked")
  @Override
  public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
    if (cap == QUEST_CAPABILITY)
      return (LazyOptional<T>) LazyOptional.of(QuestCapability::new);

    return LazyOptional.empty();
  }

  @Override
  public CompoundTag serializeNBT() {
    return this.instance.serializeNBT();
  }

  @Override
  public void deserializeNBT(CompoundTag nbt) {
    this.instance.deserializeNBT(nbt);
  }

  public static IQuestCapability getCapability(Player player) {
    return player.getCapability(QUEST_CAPABILITY).orElse(new QuestCapability());
  }
}
