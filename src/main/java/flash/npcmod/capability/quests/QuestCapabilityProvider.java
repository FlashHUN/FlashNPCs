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

  private LazyOptional<IQuestCapability> instance = LazyOptional.of(QuestCapability::new);

  @SuppressWarnings("unchecked")
  @Override
  public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
    if (cap == QUEST_CAPABILITY)
      return this.instance.cast();

    return LazyOptional.empty();
  }

  @Override
  public CompoundTag serializeNBT() {
    return this.instance.map(IQuestCapability::serializeNBT).orElse(new CompoundTag());
  }

  @Override
  public void deserializeNBT(CompoundTag nbt) {
    this.instance.ifPresent(capability -> capability.deserializeNBT(nbt));
  }

  public static IQuestCapability getCapability(Player player) {
    return player.getCapability(QUEST_CAPABILITY).orElse(new QuestCapability());
  }
}
