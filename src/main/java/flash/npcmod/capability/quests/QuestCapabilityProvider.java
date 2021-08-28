package flash.npcmod.capability.quests;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class QuestCapabilityProvider  implements ICapabilitySerializable<INBT> {

  @CapabilityInject(IQuestCapability.class)
  public static Capability<IQuestCapability> QUEST_CAPABILITY;

  private LazyOptional<IQuestCapability> instance = LazyOptional.of(QUEST_CAPABILITY::getDefaultInstance);

  @Override
  public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
    return cap == QUEST_CAPABILITY ? instance.cast() : LazyOptional.empty();
  }

  @Override
  public INBT serializeNBT() {
    return QUEST_CAPABILITY.getStorage().writeNBT(QUEST_CAPABILITY, this.instance.orElseThrow(() -> new IllegalArgumentException("LazyOptional must not be empty!")), null);
  }

  @Override
  public void deserializeNBT(INBT nbt) {
    QUEST_CAPABILITY.getStorage().readNBT(QUEST_CAPABILITY, this.instance.orElseThrow(() -> new IllegalArgumentException("LazyOptional must not be empty!")), null, nbt);
  }

  public static IQuestCapability getCapability(PlayerEntity playerEntity) {
    return playerEntity.getCapability(QuestCapabilityProvider.QUEST_CAPABILITY).orElseThrow(() -> new RuntimeException("No quest capability found!"));
  }
}
