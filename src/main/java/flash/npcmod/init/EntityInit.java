package flash.npcmod.init;

import flash.npcmod.Main;
import flash.npcmod.entity.NpcEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityInit {

  public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, Main.MODID);

  public static final RegistryObject<EntityType<NpcEntity>> NPC_ENTITY = ENTITIES.register("npc",
      () -> EntityType.Builder.of(NpcEntity::new, MobCategory.MISC)
          .sized(0.6f, 1.8f).setShouldReceiveVelocityUpdates(true).setTrackingRange(52)
          .build(new ResourceLocation(Main.MODID, "npc").toString()));

}
