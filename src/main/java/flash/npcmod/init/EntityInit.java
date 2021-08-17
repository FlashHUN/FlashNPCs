package flash.npcmod.init;

import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class EntityInit {

  public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Main.MODID);

  public static final RegistryObject<EntityType<NpcEntity>> NPC_ENTITY = ENTITIES.register("npc",
      () -> EntityType.Builder.create(NpcEntity::new, EntityClassification.MISC)
          .size(0.6f, 1.8f).setShouldReceiveVelocityUpdates(true).setTrackingRange(52).disableSummoning()
          .build(new ResourceLocation(Main.MODID, "npc").toString()));

}
