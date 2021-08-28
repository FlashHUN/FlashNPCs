package flash.npcmod.core;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

public class EntityUtil {

  public static void loadAllEntitiesIntoEnum(World world) {
    ForgeRegistries.ENTITIES.forEach(entityType -> {
      if (entityType.create(world) instanceof LivingEntity) {
        String name = EntityType.getKey(entityType).toString().replaceAll(":", "_");
        LivingEntities.create(name, entityType);
      }
    });
  }

  public enum LivingEntities implements net.minecraftforge.common.IExtensibleEnum {
    ;

    public final EntityType<?> entityType;

    LivingEntities(EntityType<?> type) {
      this.entityType = type;
    }

    public static LivingEntities create(String name, EntityType<?> type) {
      throw new IllegalStateException("Enum not extended");
    }

  }

}
