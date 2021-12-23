package flash.npcmod.core;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class EntityUtil {

  public static void loadAllEntitiesIntoEnum(Level world) {
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
